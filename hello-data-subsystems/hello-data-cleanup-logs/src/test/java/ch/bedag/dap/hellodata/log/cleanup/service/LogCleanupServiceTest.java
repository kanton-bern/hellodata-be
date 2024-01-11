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
package ch.bedag.dap.hellodata.log.cleanup.service;

import ch.bedag.dap.hellodata.log.cleanup.model.airflow.AirflowLogEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthAttemptEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthAttemptInfoEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverAuthTokenEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.cloudbeaver.CloudbeaverSessionEntity;
import ch.bedag.dap.hellodata.log.cleanup.model.superset.SupersetLogEntity;
import ch.bedag.dap.hellodata.log.cleanup.repo.airflow.AirflowLogRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthAttemptInfoTokenRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthAttemptRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverAuthTokenRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.cloudbeaver.CloudBeaverSessionRepository;
import ch.bedag.dap.hellodata.log.cleanup.repo.superset.SupersetLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class LogCleanupServiceTest {

    @Value("${hello-data.log-cleanup.olderThanInDays:365}")
    private int deleteEntriesOlderThanDays;

    @Autowired
    private AirflowLogRepository airflowLogRepository;

    @Autowired
    private SupersetLogRepository supersetLogRepository;

    @Autowired
    private CloudBeaverSessionRepository cloudBeaverSessionRepository;

    @Autowired
    private CloudBeaverAuthAttemptRepository cloudBeaverAuthAttemptRepository;

    @Autowired
    private CloudBeaverAuthAttemptInfoTokenRepository cloudBeaverAuthAttemptInfoTokenRepository;

    @Autowired
    private CloudBeaverAuthTokenRepository cloudBeaverAuthTokenRepository;

    @Autowired
    private LogCleanupService logCleanupService;

    @Container
    private static final PostgreSQLContainer<?> airflowSQLContainer = new PostgreSQLContainer<>("postgres:15.4")
        .withDatabaseName("airflow").withUsername("airflow").withPassword("airflow").withExposedPorts(5432);

    @Container
    private static final PostgreSQLContainer<?> supersetSQLContainer = new PostgreSQLContainer<>("postgres:15.4")
        .withDatabaseName("superset").withUsername("superset").withPassword("superset").withExposedPorts(5432);

    @Container
    private static final PostgreSQLContainer<?> cloudbeaverSQLContainer = new PostgreSQLContainer<>("postgres:15.4")
        .withDatabaseName("cloudbeaver").withUsername("cloudbeaver").withPassword("cloudbeaver").withExposedPorts(5432);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.jdbc-url",
            () -> String.format("jdbc:postgresql://%s:%d/airflow", airflowSQLContainer.getHost(), airflowSQLContainer.getFirstMappedPort()));
        registry.add("spring.datasource.username", airflowSQLContainer::getUsername);
        registry.add("spring.datasource.password", airflowSQLContainer::getPassword);

        registry.add("spring.superset.jdbc-url",
            () -> String.format("jdbc:postgresql://%s:%d/superset", supersetSQLContainer.getHost(), supersetSQLContainer.getFirstMappedPort()));
        registry.add("spring.superset.username", supersetSQLContainer::getUsername);
        registry.add("spring.superset.password", supersetSQLContainer::getPassword);

        registry.add("spring.cloudbeaver.jdbc-url",
            () -> String.format("jdbc:postgresql://%s:%d/cloudbeaver", cloudbeaverSQLContainer.getHost(), cloudbeaverSQLContainer.getFirstMappedPort()));
        registry.add("spring.cloudbeaver.username", cloudbeaverSQLContainer::getUsername);
        registry.add("spring.cloudbeaver.password", cloudbeaverSQLContainer::getPassword);
    }

    @BeforeEach
    public void setup() {
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(airflowSQLContainer, ""), "sql/airflow-init.sql");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(airflowSQLContainer, ""), "sql/airflow-test-data.sql");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(supersetSQLContainer, ""), "sql/superset-init.sql");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(supersetSQLContainer, ""), "sql/superset-test-data.sql");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(cloudbeaverSQLContainer, ""), "sql/cloudbeaver-init.sql");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(cloudbeaverSQLContainer, ""), "sql/cloudbeaver-test-data.sql");
    }

    @Test
    void cleanup_withDataForGivenTime_shouldCleanSpecifiedData() {
        //Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime creationDateTime = now.minusDays(deleteEntriesOlderThanDays);

        //Pre-Check that data is available
        List<AirflowLogEntity> all = airflowLogRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(all).isNotEmpty();

        List<SupersetLogEntity> supersetLogEntites = supersetLogRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(supersetLogEntites).isNotEmpty();

        List<CloudbeaverSessionEntity> cloudbeaverSessionEntities = cloudBeaverSessionRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverSessionEntities).isNotEmpty();

        List<CloudbeaverAuthAttemptEntity> cloudbeaverAuthAttemptEntities = cloudBeaverAuthAttemptRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthAttemptEntities).isNotEmpty();

        List<CloudbeaverAuthAttemptInfoEntity> cloudbeaverAuthAttemptInfo = cloudBeaverAuthAttemptInfoTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthAttemptInfo).isNotEmpty();

        List<CloudbeaverAuthTokenEntity> cloudbeaverAuthTokenEntities = cloudBeaverAuthTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthTokenEntities).isNotEmpty();

        //When
        logCleanupService.cleanup();

        //Then
        List<AirflowLogEntity> airflowLogEntries = airflowLogRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(airflowLogEntries).isEmpty();

        supersetLogEntites = supersetLogRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(supersetLogEntites).isEmpty();

        cloudbeaverSessionEntities = cloudBeaverSessionRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverSessionEntities).isEmpty();

        cloudbeaverAuthAttemptEntities = cloudBeaverAuthAttemptRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthAttemptEntities).isEmpty();

        cloudbeaverAuthAttemptInfo = cloudBeaverAuthAttemptInfoTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthAttemptInfo).isEmpty();

        cloudbeaverAuthTokenEntities = cloudBeaverAuthTokenRepository.findAllWithCreationDateTimeBefore(creationDateTime);
        assertThat(cloudbeaverAuthTokenEntities).isEmpty();
    }
}