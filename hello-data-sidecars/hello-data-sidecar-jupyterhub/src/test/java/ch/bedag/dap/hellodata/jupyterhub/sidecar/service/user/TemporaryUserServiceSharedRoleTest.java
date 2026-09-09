/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 */
package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the shared-role temporary-user lifecycle against a real PostgreSQL.
 * <p>
 * The DWH admin here is a non-superuser {@code CREATEROLE} role that owns the database, mirroring the real
 * deployment's {@code *_u_owner} role - not the superuser that {@code docker-compose} and a default
 * Testcontainers setup would hand you. That distinction is the whole point: privilege-sensitive statements
 * (DROP USER of an object owner, REASSIGN/DROP OWNED) behave differently for a non-superuser, and this test
 * exercises exactly those. Postgres 15 matches the cluster the fix targets. Requires Docker.
 */
class TemporaryUserServiceSharedRoleTest {

    private static final String SHARED_ROLE = "hd_jupyter_users";
    private static final String SHARED_SCHEMA = "jupyter_shared";
    private static final String MODELED_SCHEMA = "modeled";
    private static final String ADMIN_USER = "dwh_owner";
    private static final String ADMIN_PASSWORD = "dwh_owner";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdwh")
            .withUsername("superuser")
            .withPassword("superuser");

    static {
        POSTGRES.start();
    }

    private JdbcTemplate adminJdbc;
    private TemporaryUserService temporaryUserService;

    @BeforeAll
    static void bootstrapNonSuperuserAdmin() {
        JdbcTemplate superuser = jdbc(POSTGRES.getUsername(), POSTGRES.getPassword());
        superuser.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + ADMIN_USER + "') THEN "
                + "CREATE ROLE " + ADMIN_USER + " WITH LOGIN PASSWORD '" + ADMIN_PASSWORD + "' CREATEROLE NOSUPERUSER; END IF; END $$;");
        // The admin owns the database, so it may create schemas and grant CONNECT - just like the real *_u_owner role.
        superuser.execute("ALTER DATABASE " + POSTGRES.getDatabaseName() + " OWNER TO " + ADMIN_USER);
    }

    @BeforeEach
    void setUp() {
        adminJdbc = jdbc(ADMIN_USER, ADMIN_PASSWORD);
        resetState();

        // A modelled, read-only schema owned by the admin (stands in for the dbt output schemas).
        adminJdbc.execute("CREATE SCHEMA " + MODELED_SCHEMA);
        adminJdbc.execute("CREATE TABLE " + MODELED_SCHEMA + ".fact (id int)");

        HellodataJupyterhubProperties properties = new HellodataJupyterhubProperties();
        properties.setTempUserPasswordValidInDays(7);
        properties.setDwhUrl(POSTGRES.getJdbcUrl());
        properties.setDwhTempUserSchemas(List.of(MODELED_SCHEMA));
        properties.setDwhSharedRole(SHARED_ROLE);
        properties.setDwhSharedSchema(SHARED_SCHEMA);

        temporaryUserService = new TemporaryUserService(adminJdbc, properties);
        temporaryUserService.ensureSharedRole();
    }

    @Test
    void ensureSharedRole_isIdempotent() {
        assertDoesNotThrow(() -> temporaryUserService.ensureSharedRole());
        assertTrue(roleExists(SHARED_ROLE));
        assertTrue(schemaExists(SHARED_SCHEMA));
    }

    @Test
    void createTemporaryUser_makesMemberOfSharedRole_owningNothing() {
        TemporaryUserResponseDto user = temporaryUserService.createTemporaryUser();

        assertTrue(roleExists(user.getUsername()));
        assertTrue(isMember(user.getUsername(), SHARED_ROLE), "temp user must be a member of the shared role");
        assertEquals(0L, objectsOwnedBy(user.getUsername()), "temp user must own no objects");
    }

    @Test
    void sharedOwnership_letsOneUserManageAnotherUsersTables() {
        TemporaryUserResponseDto userA = temporaryUserService.createTemporaryUser();
        TemporaryUserResponseDto userB = temporaryUserService.createTemporaryUser();

        // User A creates a table; because its session runs as the shared role, the shared role owns it.
        jdbc(userA.getUsername(), userA.getPassword()).execute("CREATE TABLE " + SHARED_SCHEMA + ".from_a (id int)");
        assertEquals(SHARED_ROLE, ownerOf(SHARED_SCHEMA, "from_a"));

        // User B - a different temp user - can drop it, since both act as the owning shared role.
        assertDoesNotThrow(() -> jdbc(userB.getUsername(), userB.getPassword()).execute("DROP TABLE " + SHARED_SCHEMA + ".from_a"));
        assertFalse(tableExists(SHARED_SCHEMA, "from_a"));
    }

    @Test
    void cleanupExpiredUsers_dropsExpiredUser_butKeepsSharedTables() {
        TemporaryUserResponseDto user = temporaryUserService.createTemporaryUser();
        jdbc(user.getUsername(), user.getPassword()).execute("CREATE TABLE " + SHARED_SCHEMA + ".keep_me (id int)");
        expire(user.getUsername());

        assertDoesNotThrow(() -> temporaryUserService.cleanupExpiredUsers());

        assertFalse(roleExists(user.getUsername()), "expired user should be dropped");
        assertTrue(tableExists(SHARED_SCHEMA, "keep_me"), "tables owned by the shared role must survive");
        assertEquals(SHARED_ROLE, ownerOf(SHARED_SCHEMA, "keep_me"));
    }

    @Test
    void cleanupExpiredUsers_keepsActiveUser() {
        TemporaryUserResponseDto user = temporaryUserService.createTemporaryUser();

        temporaryUserService.cleanupExpiredUsers();

        assertTrue(roleExists(user.getUsername()), "a non-expired user must not be touched");
    }

    @Test
    void cleanupExpiredUsers_reclaimsLegacyUser_preservingItsTable() {
        // A user in the pre-shared-role shape: expired, holds direct grants, and owns a table.
        adminJdbc.execute("CREATE USER temp_user_legacy01 WITH PASSWORD 'legacy' VALID UNTIL '2000-01-01'");
        adminJdbc.execute("GRANT USAGE ON SCHEMA " + MODELED_SCHEMA + " TO temp_user_legacy01");
        adminJdbc.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + MODELED_SCHEMA + " TO temp_user_legacy01");
        adminJdbc.execute("CREATE TABLE " + SHARED_SCHEMA + ".legacy_output (id int)");
        adminJdbc.execute("GRANT temp_user_legacy01 TO CURRENT_USER");
        // PostgreSQL requires the new owner to hold CREATE on the schema before it can own the table.
        adminJdbc.execute("GRANT USAGE, CREATE ON SCHEMA " + SHARED_SCHEMA + " TO temp_user_legacy01");
        adminJdbc.execute("ALTER TABLE " + SHARED_SCHEMA + ".legacy_output OWNER TO temp_user_legacy01");

        assertDoesNotThrow(() -> temporaryUserService.cleanupExpiredUsers());

        assertFalse(roleExists("temp_user_legacy01"), "legacy user must be reclaimed and dropped");
        assertTrue(tableExists(SHARED_SCHEMA, "legacy_output"), "its table must be handed over, not deleted");
        assertEquals(ADMIN_USER, ownerOf(SHARED_SCHEMA, "legacy_output"), "table should be reassigned to the admin");
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private void resetState() {
        for (String username : adminJdbc.queryForList(
                "SELECT rolname FROM pg_roles WHERE rolname LIKE 'temp_user_%'", String.class)) {
            reclaimAndDropSilently(username);
        }
        silently("DROP SCHEMA IF EXISTS " + SHARED_SCHEMA + " CASCADE");
        silently("DROP SCHEMA IF EXISTS " + MODELED_SCHEMA + " CASCADE");
        if (roleExists(SHARED_ROLE)) {
            silently("GRANT " + SHARED_ROLE + " TO CURRENT_USER");
            silently("REASSIGN OWNED BY " + SHARED_ROLE + " TO CURRENT_USER");
            silently("DROP OWNED BY " + SHARED_ROLE);
            silently("DROP ROLE IF EXISTS " + SHARED_ROLE);
        }
    }

    private void reclaimAndDropSilently(String username) {
        silently("GRANT " + username + " TO CURRENT_USER");
        silently("REASSIGN OWNED BY " + username + " TO CURRENT_USER");
        silently("DROP OWNED BY " + username);
        silently("DROP USER IF EXISTS " + username);
    }

    private void silently(String sql) {
        try {
            adminJdbc.execute(sql);
        } catch (RuntimeException ignored) {
            // best-effort teardown between tests
        }
    }

    private void expire(String username) {
        adminJdbc.execute("ALTER USER " + username + " VALID UNTIL '2000-01-01'");
    }

    private boolean roleExists(String role) {
        Long count = adminJdbc.queryForObject("SELECT count(*) FROM pg_roles WHERE rolname = ?", Long.class, role);
        return count != null && count > 0;
    }

    private boolean schemaExists(String schema) {
        // Query the catalog, not information_schema, which is filtered to the current role's privileges.
        Long count = adminJdbc.queryForObject(
                "SELECT count(*) FROM pg_namespace WHERE nspname = ?", Long.class, schema);
        return count != null && count > 0;
    }

    private boolean tableExists(String schema, String table) {
        // pg_class is visible regardless of privileges; information_schema.tables is not (the admin is not a
        // member of the shared role that owns these tables, so it would not see them there).
        Long count = adminJdbc.queryForObject(
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = ? AND c.relname = ? AND c.relkind = 'r'",
                Long.class, schema, table);
        return count != null && count > 0;
    }

    private boolean isMember(String member, String group) {
        return Boolean.TRUE.equals(adminJdbc.queryForObject("SELECT pg_has_role(?, ?, 'member')", Boolean.class, member, group));
    }

    private long objectsOwnedBy(String role) {
        Long count = adminJdbc.queryForObject(
                "SELECT count(*) FROM pg_class c JOIN pg_roles r ON r.oid = c.relowner WHERE r.rolname = ?", Long.class, role);
        return count == null ? 0 : count;
    }

    private String ownerOf(String schema, String table) {
        return adminJdbc.queryForObject(
                "SELECT r.rolname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_roles r ON r.oid = c.relowner WHERE n.nspname = ? AND c.relname = ?",
                String.class, schema, table);
    }

    private static JdbcTemplate jdbc(String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }
}
