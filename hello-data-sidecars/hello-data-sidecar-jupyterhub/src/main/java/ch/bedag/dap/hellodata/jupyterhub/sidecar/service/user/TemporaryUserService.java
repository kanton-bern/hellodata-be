package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Manages the temporary DWH users handed out to JupyterHub notebooks.
 * <p>
 * Access is modelled through a single shared group role (see {@link #ensureSharedRole()}): every temporary
 * user is a member of that role and runs each session as it, so all privileges and all objects the users
 * create belong to the shared role, never to the individual user. This keeps the notebook workspace shared
 * between users (they are all admins and may manage each other's tables) and, crucially, lets an expired user
 * be removed with a plain {@code DROP USER} - it owns nothing and holds no direct grants that would block it.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TemporaryUserService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate dwhJdbcTemplate;
    private final HellodataJupyterhubProperties hellodataProperties;

    // Not @Transactional on purpose: in PostgreSQL a failed statement aborts the whole transaction,
    // so one un-droppable role would otherwise block the cleanup of every other expired user.
    @Scheduled(cron = "0 0 0 * * ?") // Runs at midnight every day
    public void cleanupExpiredUsers() {
        String findExpiredUsersSql = "SELECT usename FROM pg_catalog.pg_user WHERE valuntil < '"
                + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "' AND usename LIKE 'temp_user_%'";
        List<String> expiredUsernames = dwhJdbcTemplate.queryForList(findExpiredUsersSql, String.class); //NOSONAR

        for (String username : expiredUsernames) {
            dropExpiredUser(username);
        }
    }

    private void dropExpiredUser(String username) {
        try {
            // Users created through the shared-role model own nothing and hold no direct grants,
            // so a plain DROP USER succeeds even for a non-superuser admin.
            dwhJdbcTemplate.execute(String.format("DROP USER IF EXISTS %s", username)); //NOSONAR
            log.info("Dropped expired temporary user: {}", username);
        } catch (DataAccessException e) {
            // Legacy users (created before the shared-role model) still own objects and/or hold direct
            // privileges, which PostgreSQL refuses to drop (SQLSTATE 2BP01). Reclaim them and retry.
            log.warn("Plain drop of expired user {} failed, attempting to reclaim its objects and grants", username, e);
            reclaimAndDropLegacyUser(username);
        }
    }

    /**
     * Cleanup path for users predating the shared-role model. Their tables are handed over to the admin
     * (rather than deleted) and their remaining direct grants are removed, so the role can be dropped.
     * {@code REASSIGN OWNED} / {@code DROP OWNED} require membership in the target role, hence the initial
     * self-grant - a non-superuser {@code CREATEROLE} admin cannot run them otherwise.
     */
    private void reclaimAndDropLegacyUser(String username) {
        try {
            dwhJdbcTemplate.execute(String.format("GRANT %s TO CURRENT_USER", username)); //NOSONAR
            dwhJdbcTemplate.execute(String.format("REASSIGN OWNED BY %s TO CURRENT_USER", username)); //NOSONAR
            dwhJdbcTemplate.execute(String.format("DROP OWNED BY %s", username)); //NOSONAR
            dwhJdbcTemplate.execute(String.format("DROP USER IF EXISTS %s", username)); //NOSONAR
            log.info("Reclaimed and dropped legacy temporary user: {}", username);
        } catch (DataAccessException e) {
            // e.g. the role also holds grants in another database of the cluster, which DROP OWNED
            // cannot reach from here. Log and move on so the rest of the cleanup still runs.
            log.error("Could not drop expired temporary user {}", username, e);
        }
    }

    @Transactional
    public TemporaryUserResponseDto createTemporaryUser() {
        // Self-healing: make sure the shared role exists even if provisioning at startup could not reach the DWH.
        ensureSharedRole();

        String username = "temp_user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(hellodataProperties.getTempUserPasswordValidInDays());

        String createUserSql = String.format("CREATE USER %s WITH PASSWORD '%s' VALID UNTIL '%s'", username, password, expiryDate.format(TIMESTAMP_FORMATTER));
        dwhJdbcTemplate.execute(createUserSql);

        String sharedRole = hellodataProperties.getDwhSharedRole();
        // All access flows through the shared role; the user itself receives no direct object privileges.
        dwhJdbcTemplate.execute(String.format("GRANT %s TO %s", sharedRole, username)); //NOSONAR
        // Every session of this user runs as the shared role, so objects it creates are owned by the shared
        // role - shared between all temporary users and never left behind blocking the user's own removal.
        dwhJdbcTemplate.execute(String.format("ALTER ROLE %s SET role TO %s", username, sharedRole)); //NOSONAR

        log.debug("Created temporary user {} as member of shared role {}", username, sharedRole);
        TemporaryUserResponseDto responseDto = createResponseDto(hellodataProperties.getDwhUrl());
        responseDto.setPassword(password);
        responseDto.setUsername(username);
        responseDto.setExpiryDate(expiryDate);
        return responseDto;
    }

    /**
     * Idempotently provisions the shared group role, its read grants on the modelled schemas and a shared
     * read-write schema it owns. Safe to call repeatedly; re-running also refreshes the read grants so tables
     * added to the modelled schemas after the last run become visible to temporary users.
     */
    public void ensureSharedRole() {
        String sharedRole = hellodataProperties.getDwhSharedRole();
        String sharedSchema = hellodataProperties.getDwhSharedSchema();
        String databaseName = createResponseDto(hellodataProperties.getDwhUrl()).getDatabaseName();

        // PostgreSQL has no CREATE ROLE IF NOT EXISTS, so guard it.
        dwhJdbcTemplate.execute(String.format(
                "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s') THEN CREATE ROLE %s NOLOGIN; END IF; END $$;",
                sharedRole, sharedRole)); //NOSONAR

        // Members must be able to connect to the DWH.
        dwhJdbcTemplate.execute(String.format("GRANT CONNECT ON DATABASE %s TO %s", databaseName, sharedRole)); //NOSONAR

        // Read-only access to the modelled schemas.
        for (String schema : hellodataProperties.getDwhTempUserSchemas()) {
            dwhJdbcTemplate.execute(String.format("GRANT USAGE ON SCHEMA %s TO %s", schema, sharedRole)); //NOSONAR
            dwhJdbcTemplate.execute(String.format("GRANT SELECT ON ALL TABLES IN SCHEMA %s TO %s", schema, sharedRole)); //NOSONAR
            dwhJdbcTemplate.execute(String.format("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA %s TO %s", schema, sharedRole)); //NOSONAR
        }

        // Shared read-write workspace; objects created here are owned by the shared role (users run as it).
        dwhJdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", sharedSchema)); //NOSONAR
        dwhJdbcTemplate.execute(String.format("GRANT USAGE, CREATE ON SCHEMA %s TO %s", sharedSchema, sharedRole)); //NOSONAR
    }

    private TemporaryUserResponseDto createResponseDto(String jdbcUrl) {
        try {
            URI uri = new URI(jdbcUrl.replace("jdbc:", ""));
            String host = uri.getHost();
            int port = uri.getPort();
            String databaseName = uri.getPath().replaceFirst("/", "");
            // Remove query parameters if present
            int questionMarkIndex = databaseName.indexOf('?');
            if (questionMarkIndex != -1) {
                databaseName = databaseName.substring(0, questionMarkIndex);
            }
            TemporaryUserResponseDto responseDto = new TemporaryUserResponseDto();
            responseDto.setDatabaseName(databaseName);
            responseDto.setHost(host);
            responseDto.setPort(port);
            return responseDto;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid JDBC URL: " + jdbcUrl);
        }
    }
}
