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
package ch.bedag.dap.hellodata.portal.base.config;

import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.service.KeycloakService;
import ch.bedag.dap.hellodata.portal.user.service.KeycloakUserSyncService;
import ch.bedag.dap.hellodata.portal.user.service.UserRoleSyncService;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Checks that the @SchedulerLock expressions resolve against the configured intervals the way ShedLock evaluates them.
 */
class SchedulerLockConfigTest {

    private final List<LockConfiguration> lockConfigurations = new ArrayList<>();
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "hello-data.check-user-context-roles-in-minutes", "5",
                "hello-data.auth-server.sync-users-schedule-hours", "2")));
        context.register(TestConfig.class);
        context.registerBean(LockProvider.class, () -> this::recordLock);
        context.registerBean(UserRoleSyncService.class, () -> new UserRoleSyncService(
                mock(UserRepository.class), mock(RoleService.class), mock(HdContextRepository.class), mock(ApplicationEventPublisher.class)));
        context.registerBean(KeycloakUserSyncService.class, () -> new KeycloakUserSyncService(
                mock(KeycloakService.class), mock(UserRepository.class), mock(UserService.class), mock(ApplicationEventPublisher.class)));
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void userRolesCheckIsLockedForMostOfItsInterval() {
        context.getBean(UserRoleSyncService.class).checkUserRolesSync();

        LockConfiguration lock = lockConfigurations.get(0);
        assertEquals("checkUserRolesSync", lock.getName());
        assertEquals(Duration.ofMinutes(5), lock.getLockAtMostFor());
        assertEquals(Duration.ofSeconds(270), lock.getLockAtLeastFor());
    }

    @Test
    void keycloakUsersSyncIsLockedForMostOfItsInterval() {
        context.getBean(KeycloakUserSyncService.class).syncUsers();

        LockConfiguration lock = lockConfigurations.get(0);
        assertEquals("syncUsersWithKeycloak", lock.getName());
        assertEquals(Duration.ofHours(2), lock.getLockAtMostFor());
        assertEquals(Duration.ofMinutes(108), lock.getLockAtLeastFor());
    }

    private Optional<SimpleLock> recordLock(LockConfiguration lockConfiguration) {
        lockConfigurations.add(lockConfiguration);
        return Optional.of(() -> {
        });
    }

    @Configuration
    @EnableSchedulerLock(defaultLockAtMostFor = "PT10M", order = Ordered.HIGHEST_PRECEDENCE)
    static class TestConfig {
    }
}
