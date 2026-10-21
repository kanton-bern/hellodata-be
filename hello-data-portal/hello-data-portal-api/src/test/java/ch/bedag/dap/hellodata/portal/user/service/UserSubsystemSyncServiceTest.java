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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.rls_role.service.RlsRoleService;
import ch.bedag.dap.hellodata.portal.role.service.AirflowKeycloakRoleService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSubsystemSyncServiceTest {

    @Mock
    private NatsSenderService natsSenderService;

    @Mock
    private RoleService roleService;

    @Mock
    private HdContextRepository contextRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private DashboardGroupService dashboardGroupService;

    @Mock
    private RlsRoleService rlsRoleService;

    @Mock
    private DashboardCommentPermissionService dashboardCommentPermissionService;

    @Mock
    private AirflowKeycloakRoleService airflowKeycloakRoleService;

    @InjectMocks
    private UserSubsystemSyncService userSubsystemSyncService;

    @Test
    void synchronizeUserWithDashboards_sendsDashboardsOfAllContexts_notOnlyTheChangedOne() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("viewer@example.com");
        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(user);

        MetaInfoResourceEntity demoResource = dashboardResource("demo", "superset-demo", 1);
        MetaInfoResourceEntity showcaseResource = dashboardResource("showcase", "superset-showcase", 7);
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(demoResource, showcaseResource));

        // demo dashboard comes from a dashboard group, showcase dashboard is a direct selection
        DashboardGroupEntity demoGroup = new DashboardGroupEntity();
        demoGroup.setEntries(List.of(new DashboardGroupEntry(1, "Demo Dashboard 1", "superset-demo")));
        when(dashboardGroupService.findGroupsByContextKeyAndUserId("demo", userId.toString())).thenReturn(List.of(demoGroup));
        when(dashboardGroupService.findGroupsByContextKeyAndUserId("showcase", userId.toString())).thenReturn(List.of());
        when(userSelectedDashboardService.getSelectedDashboardIds(userId, "demo")).thenReturn(Set.of());
        when(userSelectedDashboardService.getSelectedDashboardIds(userId, "showcase")).thenReturn(Set.of(7));

        // a dashboard group change in demo triggers the sync
        userSubsystemSyncService.synchronizeUserWithDashboards(userId, "demo");

        ArgumentCaptor<UserContextRoleUpdate> captor = ArgumentCaptor.forClass(UserContextRoleUpdate.class);
        verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.UPDATE_USER_CONTEXT_ROLE), captor.capture());
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = captor.getValue().getDashboardsPerContext();

        assertEquals(Set.of("demo", "showcase"), dashboardsPerContext.keySet());
        assertViewerOf(dashboardsPerContext.get("demo"), 1);
        assertViewerOf(dashboardsPerContext.get("showcase"), 7);
    }

    private static void assertViewerOf(List<DashboardForUserDto> dashboards, int dashboardId) {
        assertEquals(1, dashboards.size());
        assertEquals(dashboardId, dashboards.get(0).getId());
        assertTrue(dashboards.get(0).isViewer());
    }

    private static MetaInfoResourceEntity dashboardResource(String contextKey, String instanceName, int dashboardId) {
        SupersetDashboard dashboard = mock(SupersetDashboard.class);
        when(dashboard.isPublished()).thenReturn(true);
        when(dashboard.getId()).thenReturn(dashboardId);
        when(dashboard.getDashboardTitle()).thenReturn("Dashboard " + dashboardId);

        MetaInfoResourceEntity resourceEntity = mock(MetaInfoResourceEntity.class);
        when(resourceEntity.getMetainfo()).thenReturn(new DashboardResource(instanceName, List.of(dashboard)));
        when(resourceEntity.getContextKey()).thenReturn(contextKey);
        return resourceEntity;
    }
}
