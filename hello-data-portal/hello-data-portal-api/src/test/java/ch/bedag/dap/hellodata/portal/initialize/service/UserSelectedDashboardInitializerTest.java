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
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.user.service.UserSelectedDashboardService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.BI_VIEWER_ROLE_NAME;
import static ch.bedag.dap.hellodata.commons.SlugifyUtil.DASHBOARD_ROLE_PREFIX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSelectedDashboardInitializerTest {

    @InjectMocks
    private UserSelectedDashboardInitializer initializer;

    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private UserContextRoleRepository userContextRoleRepository;

    @Mock
    private HdContextRepository hdContextRepository;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private MigrationService migrationService;

    @Test
    void testMigrationSkippedWhenAlreadyCompleted() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(true);

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(hdContextRepository, never()).findAllByTypeIn(any());
        verify(userSelectedDashboardService, never()).saveSelectedDashboards(any(), any(), any());
        verify(migrationService, never()).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void testMigrationRunsWhenNotYetCompleted() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of());
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of());

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(hdContextRepository).findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));
        verify(migrationService).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void testMigrationWithEligibleUsersAndDashboards() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);

        // Setup data domain
        HdContextEntity dataDomain = mock(HdContextEntity.class);
        when(dataDomain.getContextKey()).thenReturn("test-context");
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(dataDomain));

        // Setup user with eligible role
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setEnabled(true);

        UserContextRoleEntity userContextRole = mock(UserContextRoleEntity.class);
        when(userContextRole.getUser()).thenReturn(user);

        when(userContextRoleRepository.findByContextKeyAndRoleNames("test-context",
                eq(List.of("DATA_DOMAIN_VIEWER", "DATA_DOMAIN_BUSINESS_SPECIALIST"))
        )).thenReturn(List.of(userContextRole));

        // Setup dashboard resource
        MetaInfoResourceEntity dashboardResource = mock(MetaInfoResourceEntity.class);
        when(dashboardResource.getContextKey()).thenReturn("test-context");

        DashboardResource resource = mock(DashboardResource.class);
        when(resource.getInstanceName()).thenReturn("superset-instance");

        SupersetDashboard dashboard = mock(SupersetDashboard.class);
        when(dashboard.isPublished()).thenReturn(true);
        when(dashboard.getId()).thenReturn(1);
        when(dashboard.getDashboardTitle()).thenReturn("Test Dashboard");

        // Setup roles for dashboard
        SubsystemRole dashboardRole = new SubsystemRole();
        dashboardRole.setName(DASHBOARD_ROLE_PREFIX + "test-dashboard");
        when(dashboard.getRoles()).thenReturn(List.of(dashboardRole));

        when(resource.getData()).thenReturn(List.of(dashboard));
        when(dashboardResource.getMetainfo()).thenReturn(resource);

        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(dashboardResource));

        // Setup subsystem user with the required roles
        SubsystemUser subsystemUser = mock(SubsystemUser.class);
        SubsystemRole biViewerRole = new SubsystemRole();
        biViewerRole.setName(BI_VIEWER_ROLE_NAME);

        List<SubsystemRole> userRoles = new ArrayList<>();
        userRoles.add(dashboardRole);
        userRoles.add(biViewerRole);
        when(subsystemUser.getRoles()).thenReturn(userRoles);

        when(metaInfoResourceService.findUserInInstance("test@example.com", "superset-instance"))
                .thenReturn(subsystemUser);

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(userSelectedDashboardService).saveSelectedDashboards(
                eq(user.getId()),
                eq("test-context"),
                anyList()
        );
        verify(migrationService).recordMigrationSuccess(anyString(), anyString());
    }

    @Test
    void testMigrationSkipsDisabledUsers() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);

        HdContextEntity dataDomain = mock(HdContextEntity.class);
        when(dataDomain.getContextKey()).thenReturn("test-context");
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(dataDomain));

        // Setup disabled user
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("disabled@example.com");
        user.setEnabled(false);

        UserContextRoleEntity userContextRole = mock(UserContextRoleEntity.class);
        when(userContextRole.getUser()).thenReturn(user);

        when(userContextRoleRepository.findByContextKeyAndRoleNames(
                eq("test-context"),
                anyList()
        )).thenReturn(List.of(userContextRole));

        // Setup dashboard resource so contextDashboards is not empty (code needs to reach user loop)
        MetaInfoResourceEntity dashboardResource = mock(MetaInfoResourceEntity.class);
        when(dashboardResource.getContextKey()).thenReturn("test-context");
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(dashboardResource));

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(userSelectedDashboardService, never()).saveSelectedDashboards(any(), any(), any());
    }

    @Test
    void testMigrationSkipsUsersWithNullUser() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);

        HdContextEntity dataDomain = mock(HdContextEntity.class);
        when(dataDomain.getContextKey()).thenReturn("test-context");
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(dataDomain));

        // Setup user context role with null user
        UserContextRoleEntity userContextRole = mock(UserContextRoleEntity.class);
        when(userContextRole.getUser()).thenReturn(null);

        when(userContextRoleRepository.findByContextKeyAndRoleNames(
                eq("test-context"),
                anyList()
        )).thenReturn(List.of(userContextRole));

        // Setup dashboard resource so contextDashboards is not empty (code needs to reach user loop)
        MetaInfoResourceEntity dashboardResource = mock(MetaInfoResourceEntity.class);
        when(dashboardResource.getContextKey()).thenReturn("test-context");
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(dashboardResource));

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(userSelectedDashboardService, never()).saveSelectedDashboards(any(), any(), any());
    }

    @Test
    void testMigrationSkipsWhenNoSubsystemUserFound() {
        // given
        when(migrationService.isMigrationCompleted(anyString())).thenReturn(false);

        HdContextEntity dataDomain = mock(HdContextEntity.class);
        when(dataDomain.getContextKey()).thenReturn("test-context");
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(dataDomain));

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setEnabled(true);

        UserContextRoleEntity userContextRole = mock(UserContextRoleEntity.class);
        when(userContextRole.getUser()).thenReturn(user);

        when(userContextRoleRepository.findByContextKeyAndRoleNames(
                eq("test-context"),
                anyList()
        )).thenReturn(List.of(userContextRole));

        // Setup dashboard resource
        MetaInfoResourceEntity dashboardResource = mock(MetaInfoResourceEntity.class);
        when(dashboardResource.getContextKey()).thenReturn("test-context");

        DashboardResource resource = mock(DashboardResource.class);
        when(resource.getInstanceName()).thenReturn("superset-instance");
        when(dashboardResource.getMetainfo()).thenReturn(resource);

        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(dashboardResource));

        // No subsystem user found
        when(metaInfoResourceService.findUserInInstance("test@example.com", "superset-instance"))
                .thenReturn(null);

        // when
        initializer.migrateUserSelectedDashboards();

        // then
        verify(userSelectedDashboardService, never()).saveSelectedDashboards(any(), any(), any());
    }
}
