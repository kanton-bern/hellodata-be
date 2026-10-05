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
package ch.bedag.dap.hellodata.portal.initialize.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRlsRoleInitializerTest {

    private static final String CONTEXT_KEY = "dd01";
    private static final String INSTANCE_NAME = "superset-dd01";

    @InjectMocks
    private UserRlsRoleInitializer initializer;
    @Mock
    private RlsRoleService rlsRoleService;
    @Mock
    private UserContextRoleRepository userContextRoleRepository;
    @Mock
    private HdContextRepository hdContextRepository;
    @Mock
    private MetaInfoResourceService metaInfoResourceService;
    @Mock
    private MigrationService migrationService;

    @Test
    void shouldSkipWhenAlreadyCompleted() {
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(true);

        initializer.migrateUserRlsRoles();

        verify(hdContextRepository, never()).findAllByTypeIn(any());
        verify(migrationService, never()).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void shouldMigrateRlsRolesFromSupersetUser() {
        UserEntity user = mockDomainWithUser(true, false);
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setRoles(List.of(role("BI_VIEWER"), role("RLS_03"), role("RLS_01"), role("D_dashboard_1")));
        when(metaInfoResourceService.findUserInInstance(user.getEmail(), INSTANCE_NAME)).thenReturn(subsystemUser);

        initializer.migrateUserRlsRoles();

        verify(rlsRoleService).saveSelectedRlsRoles(user.getId(), CONTEXT_KEY, List.of("RLS_03", "RLS_01"));
        verify(migrationService).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void shouldNotOverwriteExistingPortalSelection() {
        UserEntity user = mockDomainWithUser(true, true);

        initializer.migrateUserRlsRoles();

        verify(metaInfoResourceService, never()).findUserInInstance(anyString(), anyString());
        verify(rlsRoleService, never()).saveSelectedRlsRoles(any(), anyString(), anyCollection());
        verify(migrationService).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void shouldSkipDisabledUsers() {
        mockDomainWithUser(false, false);

        initializer.migrateUserRlsRoles();

        verify(rlsRoleService, never()).saveSelectedRlsRoles(any(), anyString(), anyCollection());
    }

    private UserEntity mockDomainWithUser(boolean enabled, boolean hasPortalSelection) {
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);
        HdContextEntity dataDomain = new HdContextEntity();
        dataDomain.setContextKey(CONTEXT_KEY);
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN))).thenReturn(List.of(dataDomain));
        when(metaInfoResourceService.findSupersetInstanceNameByContextKey(CONTEXT_KEY)).thenReturn(INSTANCE_NAME);
        UserEntity user = mock(UserEntity.class);
        UUID userId = UUID.randomUUID();
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn("viewer@example.com");
        when(user.isEnabled()).thenReturn(enabled);
        lenient().when(rlsRoleService.hasSelectedRlsRoles(userId, CONTEXT_KEY)).thenReturn(hasPortalSelection);
        UserContextRoleEntity userContextRole = mock(UserContextRoleEntity.class);
        when(userContextRole.getUser()).thenReturn(user);
        when(userContextRoleRepository.findByContextKeyAndRoleNames(eq(CONTEXT_KEY), anyList())).thenReturn(List.of(userContextRole));
        return user;
    }

    private static SubsystemRole role(String name) {
        SubsystemRole role = new SubsystemRole();
        role.setName(name);
        return role;
    }
}
