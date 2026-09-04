/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.superset.service.comment;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.DashboardCommentsPublished;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.data.PublishedComment;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JDBC repository for writing published dashboard comments to the datadomain DWH.
 * Creates the target table on startup if it doesn't exist.
 */
@Log4j2
@Repository
@ConditionalOnProperty(name = "hello-data.dashboard-comments.dwh-sync-enabled", havingValue = "true")
public class DwhCommentRepository {

    /** Postgres unquoted identifier charset; guards the role names we interpolate into GRANT statements. */
    private static final Pattern SAFE_ROLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;
    private final DwhCommentSyncProperties properties;

    public DwhCommentRepository(@Qualifier("dwhJdbcTemplate") JdbcTemplate jdbcTemplate,
                                DwhCommentSyncProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureTableExists() {
        String schema = properties.getDwhSchema();
        String table = properties.getDwhTable();
        String qualifiedTable = schema + "." + table;

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);

        String ddl = """
                CREATE TABLE IF NOT EXISTS %s (
                    comment_id      VARCHAR(36) PRIMARY KEY,
                    dashboard_id    INTEGER NOT NULL,
                    dashboard_title VARCHAR(500),
                    dashboard_slug  VARCHAR(500),
                    context_key     VARCHAR(100) NOT NULL,
                    author          VARCHAR(200),
                    author_email    VARCHAR(200),
                    created_date    BIGINT,
                    published_date  BIGINT,
                    text            TEXT,
                    tags            TEXT,
                    pointer_url     VARCHAR(2000),
                    synced_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(qualifiedTable);

        jdbcTemplate.execute(ddl);

        // Grant read access to all existing roles in the database so Superset users can query
        jdbcTemplate.execute("GRANT USAGE ON SCHEMA " + schema + " TO PUBLIC");
        jdbcTemplate.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + schema + " TO PUBLIC");
        jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + schema + " GRANT SELECT ON TABLES TO PUBLIC");

        grantReadWriteRoles(schema);

        log.info("Ensured DWH table {} exists with read grants", qualifiedTable);
    }

    /**
     * Grants read/write on the comment schema to the configured RW roles (typically the data domain MODELER),
     * on top of the read-only grant to PUBLIC. Idempotent: safe to re-run on every startup. A role that does
     * not exist or cannot be granted is logged and skipped so it never blocks sidecar startup.
     */
    private void grantReadWriteRoles(String schema) {
        for (String role : resolveRwRoles()) {
            if (!SAFE_ROLE_NAME.matcher(role).matches()) {
                log.warn("Skipping dashboard-comments RW grant for role '{}': not a valid identifier", role);
                continue;
            }
            try {
                jdbcTemplate.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + role);
                jdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA " + schema + " TO " + role);
                jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + schema +
                        " GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO " + role);
                log.info("Granted RW on comment schema {} to role {}", schema, role);
            } catch (Exception e) {
                log.warn("Could not grant RW on comment schema {} to role '{}': {}", schema, role, e.getMessage());
            }
        }
    }

    /**
     * Resolves the RW roles: explicit {@code dwh-rw-roles} config wins; otherwise, if enabled and the connecting
     * user matches the {@code <db>_u_owner} convention, derive the matching {@code <db>_u_modeler} role.
     */
    private List<String> resolveRwRoles() {
        List<String> configured = properties.getDwhRwRoles();
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        List<String> derived = new ArrayList<>();
        String owner = properties.getDwhUsername();
        if (properties.isDwhRwDeriveModeler() && owner != null && owner.endsWith("_u_owner")) {
            derived.add(owner.substring(0, owner.length() - "_u_owner".length()) + "_u_modeler");
        }
        return derived;
    }

    /**
     * Full-replace: delete all comments for a dashboard, then insert the current set.
     */
    @Transactional
    public void replaceCommentsForDashboard(DashboardCommentsPublished payload) {
        String schema = properties.getDwhSchema();
        String table = properties.getDwhTable();
        String qualifiedTable = schema + "." + table;

        jdbcTemplate.update("DELETE FROM " + qualifiedTable + " WHERE dashboard_id = ? AND context_key = ?",
                payload.getDashboardId(), payload.getContextKey());

        List<PublishedComment> comments = payload.getComments();
        if (comments == null || comments.isEmpty()) {
            log.debug("No published comments for dashboard {}/{}, table cleared", payload.getContextKey(), payload.getDashboardId());
            return;
        }

        String insertSql = """
                INSERT INTO %s (comment_id, dashboard_id, dashboard_title, dashboard_slug, context_key,
                    author, author_email, created_date, published_date, text, tags, pointer_url, synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(qualifiedTable);

        Timestamp now = Timestamp.from(Instant.now());

        for (PublishedComment comment : comments) {
            jdbcTemplate.update(insertSql,
                    comment.getCommentId(),
                    comment.getDashboardId(),
                    comment.getDashboardTitle(),
                    comment.getDashboardSlug(),
                    payload.getContextKey(),
                    comment.getAuthor(),
                    comment.getAuthorEmail(),
                    comment.getCreatedDate(),
                    comment.getPublishedDate(),
                    comment.getText(),
                    comment.getTags(),
                    comment.getPointerUrl(),
                    now);
        }

        log.debug("Inserted {} comments for dashboard {}/{}", comments.size(), payload.getContextKey(), payload.getDashboardId());
    }
}
