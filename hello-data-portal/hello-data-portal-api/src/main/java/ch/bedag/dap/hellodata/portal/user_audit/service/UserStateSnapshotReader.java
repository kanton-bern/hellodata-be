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
package ch.bedag.dap.hellodata.portal.user_audit.service;

import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditChange;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reads the current state of a user straight from the database with plain SQL.
 * Plain SQL (instead of the JPA entities) is used on purpose: bulk JPQL updates and delete / re-insert flows
 * are not reflected in already loaded entities, but they are in the database once the session is flushed.
 */
@Component
@RequiredArgsConstructor
class UserStateSnapshotReader {

    static final String EMAIL = "email";
    static final String ENABLED = "enabled";
    static final String SUPERUSER = "superuser";
    static final String ROLE = "role";
    static final String PORTAL_ROLES = "portalRoles";
    static final String RLS_ROLES = "rlsRoles";
    static final String DASHBOARDS = "dashboards";
    static final String DASHBOARD_GROUPS = UserAuditChange.FIELD_DASHBOARD_GROUPS;
    static final String COMMENT_PERMISSIONS = "commentPermissions";

    private final NamedParameterJdbcTemplate jdbc;

    UserStateSnapshot read(UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId).addValue("userIdText", userId.toString());
        List<Map<String, Object>> users = jdbc.queryForList("SELECT email, enabled, superuser FROM user_ WHERE id = :userId", params);
        if (users.isEmpty()) {
            return UserStateSnapshot.EMPTY;
        }
        Map<String, Object> user = users.get(0);
        String email = (String) user.get(EMAIL);
        SortedMap<UserStateSnapshot.Key, Object> values = new TreeMap<>();
        values.put(new UserStateSnapshot.Key(EMAIL, null), email);
        values.put(new UserStateSnapshot.Key(ENABLED, null), user.get(ENABLED));
        values.put(new UserStateSnapshot.Key(SUPERUSER, null), user.get(SUPERUSER));

        RowCallbackHandler roles = rs -> values.put(new UserStateSnapshot.Key(ROLE, rs.getString(1)), rs.getString(2));
        jdbc.query("SELECT ucr.context_key, r.name FROM user_context_role ucr JOIN role r ON r.id = ucr.role_id WHERE ucr.user_id = :userId",
                params, roles);

        putGrouped(values, PORTAL_ROLES,
                "SELECT upr.context_key, pr.name FROM user_portal_role upr JOIN portal_role pr ON pr.id = upr.portal_role_id WHERE upr.user_id = :userId", params);
        putGrouped(values, RLS_ROLES,
                "SELECT context_key, role_key FROM user_rls_role WHERE user_id = :userId", params);
        putGrouped(values, DASHBOARDS,
                "SELECT context_key, dashboard_title || ' (' || dashboard_id || ')' FROM user_selected_dashboard WHERE user_id = :userId", params);
        putGrouped(values, DASHBOARD_GROUPS,
                "SELECT dg.context_key, dg.name FROM dashboard_group dg " +
                        "WHERE EXISTS (SELECT 1 FROM jsonb_array_elements(COALESCE(dg.users::jsonb, '[]'::jsonb)) u WHERE u->>'id' = :userIdText)", params);

        jdbc.query("SELECT context_key, read_comments, write_comments, review_comments FROM dashboard_comment_permission WHERE user_id = :userId",
                params, rs -> {
                    List<String> granted = new ArrayList<>();
                    if (rs.getBoolean(2)) {
                        granted.add("READ");
                    }
                    if (rs.getBoolean(3)) {
                        granted.add("WRITE");
                    }
                    if (rs.getBoolean(4)) {
                        granted.add("REVIEW");
                    }
                    values.put(new UserStateSnapshot.Key(COMMENT_PERMISSIONS, rs.getString(1)), granted);
                });
        return new UserStateSnapshot(email, values);
    }

    private void putGrouped(SortedMap<UserStateSnapshot.Key, Object> values, String field, String sql, MapSqlParameterSource params) {
        Map<String, SortedSet<String>> grouped = new HashMap<>();
        RowCallbackHandler groupByContext = rs -> grouped.computeIfAbsent(rs.getString(1), k -> new TreeSet<>()).add(rs.getString(2));
        jdbc.query(sql, params, groupByContext);
        grouped.forEach((contextKey, items) -> values.put(new UserStateSnapshot.Key(field, contextKey), new ArrayList<>(items)));
    }
}
