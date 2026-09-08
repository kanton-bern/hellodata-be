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
package ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user;

import ch.bedag.dap.hellodata.jupyterhub.sidecar.config.props.HellodataJupyterhubProperties;
import ch.bedag.dap.hellodata.jupyterhub.sidecar.service.user.dto.TemporaryUserResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Covers cleanupExpiredUsers() against a real PostgreSQL: the defect is in how PostgreSQL treats DROP USER.
class TemporaryUserServiceCleanupTest {

    private static final String SCHEMA = "dwh_schema";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdwh")
            .withUsername("dwhadmin")
            .withPassword("dwhadmin");

    static {
        POSTGRES.start();
    }

    private JdbcTemplate jdbcTemplate;
    private TemporaryUserService temporaryUserService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + SCHEMA + ".notebook_output");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + SCHEMA + ".measurements (id int)");
        dropLeftoverTempUsers();

        HellodataJupyterhubProperties properties = new HellodataJupyterhubProperties();
        properties.setTempUserPasswordValidInDays(7);
        properties.setDwhUrl(POSTGRES.getJdbcUrl());
        properties.setDwhTempUserSchemas(List.of(SCHEMA));

        temporaryUserService = new TemporaryUserService(jdbcTemplate, properties);
    }

    @Test
    void cleanupExpiredUsers_ShouldDropExpiredUser() {
        TemporaryUserResponseDto created = temporaryUserService.createTemporaryUser();
        expire(created.getUsername());

        assertDoesNotThrow(() -> temporaryUserService.cleanupExpiredUsers());

        assertFalse(temporaryUsers().contains(created.getUsername()));
    }

    @Test
    void cleanupExpiredUsers_ShouldKeepUser_WhenNotExpired() {
        TemporaryUserResponseDto created = temporaryUserService.createTemporaryUser();

        temporaryUserService.cleanupExpiredUsers();

        assertTrue(temporaryUsers().contains(created.getUsername()));
    }

    @Test
    void cleanupExpiredUsers_ShouldPreserveTablesOwnedByUser() {
        TemporaryUserResponseDto created = temporaryUserService.createTemporaryUser();
        jdbcTemplate.execute("CREATE TABLE " + SCHEMA + ".notebook_output (id int)");
        jdbcTemplate.execute("ALTER TABLE " + SCHEMA + ".notebook_output OWNER TO " + created.getUsername());
        expire(created.getUsername());

        temporaryUserService.cleanupExpiredUsers();

        assertFalse(temporaryUsers().contains(created.getUsername()));
        assertTrue(tableExists("notebook_output"));
    }

    @Test
    void cleanupExpiredUsers_ShouldDropAllExpiredUsers() {
        TemporaryUserResponseDto first = temporaryUserService.createTemporaryUser();
        TemporaryUserResponseDto second = temporaryUserService.createTemporaryUser();
        TemporaryUserResponseDto third = temporaryUserService.createTemporaryUser();
        expire(first.getUsername());
        expire(second.getUsername());
        expire(third.getUsername());

        temporaryUserService.cleanupExpiredUsers();

        assertTrue(temporaryUsers().isEmpty());
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class, SCHEMA, tableName);
        return count != null && count > 0;
    }

    private void expire(String username) {
        jdbcTemplate.execute("ALTER USER " + username + " VALID UNTIL '2020-01-01 00:00:00'");
    }

    private List<String> temporaryUsers() {
        return jdbcTemplate.queryForList(
                "SELECT usename FROM pg_catalog.pg_user WHERE usename LIKE 'temp_user_%'", String.class);
    }

    private void dropLeftoverTempUsers() {
        for (String username : temporaryUsers()) {
            jdbcTemplate.execute("DROP OWNED BY " + username);
            jdbcTemplate.execute("DROP USER IF EXISTS " + username);
        }
    }
}
