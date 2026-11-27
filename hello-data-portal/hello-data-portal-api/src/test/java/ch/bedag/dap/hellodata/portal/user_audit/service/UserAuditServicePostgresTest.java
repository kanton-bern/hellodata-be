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

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.base.config.PersistenceConfig;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupUserEntry;
import ch.bedag.dap.hellodata.portal.rls_role.entity.UserRlsRoleEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditAction;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditChange;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditChangeDto;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditLogDto;
import ch.bedag.dap.hellodata.portal.user_audit.data.UserAuditSource;
import ch.bedag.dap.hellodata.portal.user_audit.repository.UserAuditLogRepository;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import io.nats.client.Connection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the audit against a real PostgreSQL to verify the snapshot SQL and the JSONB round trip.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("tc-postgres")
@ContextConfiguration(classes = {PersistenceConfig.class})
@Import({UserAuditService.class, UserStateSnapshotReader.class})
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
class UserAuditServicePostgresTest {

    private static final String DATA_DOMAIN = "dd_one";

    @Autowired
    private UserAuditService userAuditService;
    @Autowired
    private UserAuditLogRepository userAuditLogRepository;
    @Autowired
    private EntityManager entityManager;
    @MockitoBean
    private SecurityFilterChain securityFilterChain;
    @MockitoBean
    private Connection connection;

    @Test
    void should_record_one_entry_with_diff_of_all_changed_tables() {
        UserEntity user = persistUser("john@example.com");
        RoleEntity viewer = persistRole(HdRoleName.DATA_DOMAIN_VIEWER);
        RoleEntity none = persistRole(HdRoleName.NONE);
        UserContextRoleEntity contextRole = persistContextRole(user, none);

        userAuditService.audit(user.getId(), UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> {
            // bulk JPQL update - not visible for Envers, but visible for the SQL snapshot
            entityManager.createQuery("UPDATE user_context_role u SET u.role = :role WHERE u.id = :id")
                    .setParameter("role", viewer).setParameter("id", contextRole.getId()).executeUpdate();
            persistRlsRole(user.getId(), "RLS_01");
            persistDashboardGroup("Finance", user);
            // nested audit for the same user is folded into the outer one
            userAuditService.audit(user.getId(), UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL, () -> user.setEnabled(false));
        });

        List<UserAuditLogDto> entries = userAuditService.getAuditLog(user.getId(), PageRequest.of(0, 10)).getContent();
        assertThat(entries).hasSize(1);
        UserAuditLogDto entry = entries.get(0);
        assertThat(entry.action()).isEqualTo(UserAuditAction.ROLES_CHANGED);
        assertThat(entry.source()).isEqualTo(UserAuditSource.MANUAL);
        assertThat(entry.targetEmail()).isEqualTo("john@example.com");
        assertThat(entry.contextKey()).isNull();

        Map<String, UserAuditChangeDto> byField = entry.changes().stream().collect(Collectors.toMap(UserAuditChangeDto::field, c -> c));
        assertThat(byField).containsOnlyKeys("enabled", "role", "rlsRoles", "dashboardGroups");
        assertThat(byField.get("enabled").before()).isEqualTo(true);
        assertThat(byField.get("enabled").after()).isEqualTo(false);
        assertThat(byField.get("role").contextKey()).isEqualTo(DATA_DOMAIN);
        assertThat(byField.get("role").before()).isEqualTo("NONE");
        assertThat(byField.get("role").after()).isEqualTo("DATA_DOMAIN_VIEWER");
        assertThat(byField.get("rlsRoles").before()).isNull();
        assertThat(byField.get("rlsRoles").after()).isEqualTo(List.of("RLS_01"));
        assertThat(byField.get("dashboardGroups").after()).isEqualTo(List.of("Finance"));
    }

    @Test
    void should_not_record_when_nothing_changed() {
        UserEntity user = persistUser("nochange@example.com");

        userAuditService.audit(user.getId(), UserAuditAction.ROLES_CHANGED, UserAuditSource.MANUAL, () -> user.setEnabled(true));

        assertThat(userAuditLogRepository.findAll()).isEmpty();
    }

    @Test
    void should_record_creation_and_deletion() {
        UserEntity user = persistUser("created@example.com");
        UUID userId = user.getId();

        userAuditService.recordCreated(userId, UserAuditSource.MANUAL);
        userAuditService.audit(userId, UserAuditAction.USER_DELETED, UserAuditSource.MANUAL, () -> entityManager.remove(user));

        List<UserAuditLogDto> entries = userAuditService.getAuditLog(userId, PageRequest.of(0, 10)).getContent();
        assertThat(entries).extracting(UserAuditLogDto::action)
                .containsExactlyInAnyOrder(UserAuditAction.USER_CREATED, UserAuditAction.USER_DELETED);
        UserAuditLogDto deleted = entries.stream().filter(e -> e.action() == UserAuditAction.USER_DELETED).findFirst().orElseThrow();
        assertThat(deleted.targetEmail()).isEqualTo("created@example.com");
        assertThat(deleted.changes()).allSatisfy(change -> assertThat(change.after()).isNull());
    }

    @Test
    void should_search_the_actor_values_domain_names_and_translated_codes() {
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(DATA_DOMAIN);
        context.setName("Finance Domain");
        context.setType(HdContextType.DATA_DOMAIN);
        entityManager.persist(context);
        UserEntity user = persistUser("search@example.com");
        UUID userId = user.getId();
        userAuditService.recordChanges(userId, user.getEmail(), UserAuditAction.ROLES_CHANGED, UserAuditSource.BULK_ASSIGNMENT,
                List.of(new UserAuditChange("role", DATA_DOMAIN, "NONE", "DATA_DOMAIN_VIEWER")));
        userAuditService.recordChanges(userId, user.getEmail(), UserAuditAction.USER_DISABLED, UserAuditSource.MANUAL,
                List.of(new UserAuditChange("enabled", null, true, false)));
        PageRequest page = PageRequest.of(0, 10);

        assertThat(userAuditService.getAuditLog(userId, "  ", List.of(), page).getTotalElements()).isEqualTo(2);
        // a value in the changes, the name of the data domain, the source and the action, case insensitive
        assertThat(userAuditService.getAuditLog(userId, "domain_viewer", List.of(), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.ROLES_CHANGED);
        assertThat(userAuditService.getAuditLog(userId, "finance", List.of(), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.ROLES_CHANGED);
        assertThat(userAuditService.getAuditLog(userId, "Bulk", List.of(), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.ROLES_CHANGED);
        assertThat(userAuditService.getAuditLog(userId, "user_disabled", List.of(), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.USER_DISABLED);
        // the codes whose translated label matched ("Aktiviert" -> the field enabled, "Massen" -> the source)
        assertThat(userAuditService.getAuditLog(userId, "Aktiviert", List.of("enabled"), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.USER_DISABLED);
        assertThat(userAuditService.getAuditLog(userId, "Massen", List.of("BULK_ASSIGNMENT"), page).getContent())
                .extracting(UserAuditLogDto::action).containsExactly(UserAuditAction.ROLES_CHANGED);
        // % and _ are taken literally
        assertThat(userAuditService.getAuditLog(userId, "%", List.of(), page).getTotalElements()).isZero();
        assertThat(userAuditService.getAuditLog(userId, "nothing like this", List.of(), page).getTotalElements()).isZero();
    }

    private UserEntity persistUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setUsername(email);
        user.setEnabled(true);
        entityManager.persist(user);
        return user;
    }

    private RoleEntity persistRole(HdRoleName name) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName(name);
        role.setContextType(HdContextType.DATA_DOMAIN);
        entityManager.persist(role);
        return role;
    }

    private UserContextRoleEntity persistContextRole(UserEntity user, RoleEntity role) {
        UserContextRoleEntity contextRole = new UserContextRoleEntity();
        contextRole.setId(UUID.randomUUID());
        contextRole.setUser(user);
        contextRole.setContextKey(DATA_DOMAIN);
        contextRole.setRole(role);
        entityManager.persist(contextRole);
        return contextRole;
    }

    private void persistRlsRole(UUID userId, String roleKey) {
        UserRlsRoleEntity rlsRole = new UserRlsRoleEntity();
        rlsRole.setId(UUID.randomUUID());
        rlsRole.setUserId(userId);
        rlsRole.setContextKey(DATA_DOMAIN);
        rlsRole.setRoleKey(roleKey);
        entityManager.persist(rlsRole);
    }

    private void persistDashboardGroup(String name, UserEntity member) {
        DashboardGroupUserEntry entry = new DashboardGroupUserEntry();
        entry.setId(member.getId().toString());
        entry.setEmail(member.getEmail());
        DashboardGroupEntity group = new DashboardGroupEntity();
        group.setId(UUID.randomUUID());
        group.setName(name);
        group.setContextKey(DATA_DOMAIN);
        group.setEntries(List.of());
        group.setUsers(List.of(entry));
        group.setRlsRoles(List.of());
        entityManager.persist(group);
    }
}
