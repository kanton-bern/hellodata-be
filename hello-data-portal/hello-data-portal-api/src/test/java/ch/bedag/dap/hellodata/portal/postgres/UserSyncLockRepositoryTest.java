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
package ch.bedag.dap.hellodata.portal.postgres;

import ch.bedag.dap.hellodata.portal.base.config.PersistenceConfig;
import ch.bedag.dap.hellodata.portal.sync.entity.UserSyncLockEntity;
import ch.bedag.dap.hellodata.portal.sync.repository.UserSyncLockRepository;
import io.nats.client.Connection;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.COMPLETED;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.RUNNING;
import static ch.bedag.dap.hellodata.portal.sync.entity.UserSyncStatus.STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@SpringBootTest
@Transactional
@ActiveProfiles("tc-postgres")
@ContextConfiguration(classes = {PersistenceConfig.class})
@EnableAutoConfiguration(excludeName = {
        "org.springdoc.webmvc.ui.SwaggerConfig",
        "org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration",
        "org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration",
        "org.springdoc.core.configuration.SpringDocConfiguration",
        "org.springdoc.core.properties.SpringDocConfigProperties",
        "org.springdoc.core.properties.SwaggerUiConfigProperties",
        "org.springdoc.core.properties.SwaggerUiOAuthProperties",
        "org.springdoc.core.configuration.SpringDocSecurityConfiguration",
        "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration",
        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration",
        "org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration",
        "io.nats.spring.boot.autoconfigure.NatsAutoConfiguration"
})
class UserSyncLockRepositoryTest {

    @Autowired
    private UserSyncLockRepository userSyncLockRepository;
    @Autowired
    private EntityManager entityManager;
    @MockitoBean
    private SecurityFilterChain securityFilterChain;
    @MockitoBean
    private Connection connection;

    @BeforeEach
    void setUp() {
        userSyncLockRepository.deleteAll();
        UserSyncLockEntity lock = new UserSyncLockEntity();
        lock.setStatus(COMPLETED);
        entityManager.persist(lock);
        entityManager.flush();
    }

    @Test
    void changes_status_only_from_the_expected_one() {
        LocalDateTime now = LocalDateTime.now();

        assertThat(userSyncLockRepository.changeStatus(STARTED, RUNNING, now), equalTo(0));
        assertThat(userSyncLockRepository.changeStatus(COMPLETED, STARTED, now), equalTo(1));
        assertThat(userSyncLockRepository.changeStatus(COMPLETED, STARTED, now), equalTo(0));
        assertThat(userSyncLockRepository.changeStatus(STARTED, RUNNING, now), equalTo(1));

        assertThat(userSyncLockRepository.findAll().get(0).getStatus(), equalTo(RUNNING));
    }

    @Test
    void changes_stale_status_only() {
        LocalDateTime now = LocalDateTime.now();
        userSyncLockRepository.changeStatus(COMPLETED, RUNNING, now.minusHours(1));

        assertThat(userSyncLockRepository.changeStatusIfOlderThan(RUNNING, COMPLETED, now.minusHours(2), now), equalTo(0));
        assertThat(userSyncLockRepository.changeStatusIfOlderThan(RUNNING, COMPLETED, now.minusMinutes(30), now), equalTo(1));

        assertThat(userSyncLockRepository.findAll().get(0).getStatus(), equalTo(COMPLETED));
    }
}
