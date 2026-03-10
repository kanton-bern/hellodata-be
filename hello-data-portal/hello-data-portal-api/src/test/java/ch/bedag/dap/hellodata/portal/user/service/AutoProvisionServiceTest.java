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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.portal.base.config.SystemProperties;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portalcommon.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserPortalRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.PortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserPortalRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoProvisionServiceTest {

    @Mock
    private SystemProperties systemProperties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private NatsSenderService natsSenderService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private PortalRoleRepository portalRoleRepository;

    @Mock
    private UserPortalRoleRepository userPortalRoleRepository;

    @Mock
    private HdContextRepository hdContextRepository;

    @Mock
    private DashboardCommentPermissionRepository dashboardCommentPermissionRepository;

    @InjectMocks
    private AutoProvisionService autoProvisionService;

    @Test
    void featureDisabled_returnsNull() {
        when(systemProperties.isAutoProvisionViewerOnLogin()).thenReturn(false);

        UserEntity result = autoProvisionService.autoProvisionIfEnabled("user@example.com", "John", "Doe", UUID.randomUUID().toString());

        assertNull(result);
        verify(userRepository, never()).saveAndFlush(any());
        verify(natsSenderService, never()).publishMessageToJetStream(any(), any());
    }

    @Test
    void featureEnabled_userAlreadyExists_returnsExisting() {
        when(systemProperties.isAutoProvisionViewerOnLogin()).thenReturn(true);
        UserEntity existing = new UserEntity();
        existing.setEmail("user@example.com");
        when(userRepository.findUserEntityByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));

        UserEntity result = autoProvisionService.autoProvisionIfEnabled("user@example.com", "John", "Doe", UUID.randomUUID().toString());

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void featureEnabled_newUser_createsWithViewerRolesAndDashboards() {
        String keycloakSubject = UUID.randomUUID().toString();
        when(systemProperties.isAutoProvisionViewerOnLogin()).thenReturn(true);
        when(userRepository.findUserEntityByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());

        // Set up published dashboards
        SupersetDashboard publishedDashboard = mock(SupersetDashboard.class);
        when(publishedDashboard.isPublished()).thenReturn(true);
        when(publishedDashboard.getId()).thenReturn(42);
        when(publishedDashboard.getDashboardTitle()).thenReturn("Sales Dashboard");

        SupersetDashboard unpublishedDashboard = mock(SupersetDashboard.class);
        when(unpublishedDashboard.isPublished()).thenReturn(false);

        DashboardResource dashboardResource = mock(DashboardResource.class);
        when(dashboardResource.getData()).thenReturn(List.of(publishedDashboard, unpublishedDashboard));
        when(dashboardResource.getInstanceName()).thenReturn("superset-1");

        MetaInfoResourceEntity resourceEntity = mock(MetaInfoResourceEntity.class);
        when(resourceEntity.getMetainfo()).thenReturn(dashboardResource);
        when(resourceEntity.getContextKey()).thenReturn("dd-key-1");

        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS))
                .thenReturn(List.of(resourceEntity));

        // Set up portal role for DATA_DOMAIN_VIEWER
        PortalRoleEntity viewerPortalRole = new PortalRoleEntity();
        viewerPortalRole.setId(UUID.randomUUID());
        viewerPortalRole.setName(SystemDefaultPortalRoleName.DATA_DOMAIN_VIEWER.name());
        when(portalRoleRepository.findByName(SystemDefaultPortalRoleName.DATA_DOMAIN_VIEWER.name()))
                .thenReturn(Optional.of(viewerPortalRole));

        // Set up data domains
        HdContextEntity dataDomain = new HdContextEntity();
        dataDomain.setContextKey("dd-key-1");
        dataDomain.setType(HdContextType.DATA_DOMAIN);
        when(hdContextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN)))
                .thenReturn(List.of(dataDomain));

        // No existing comment permissions
        when(dashboardCommentPermissionRepository.findByUserIdAndContextKey(any(), eq("dd-key-1")))
                .thenReturn(Optional.empty());

        UserEntity result = autoProvisionService.autoProvisionIfEnabled("user@example.com", "John", "Doe", keycloakSubject);

        // Verify user entity was created
        assertNotNull(result);
        assertEquals(UUID.fromString(keycloakSubject), result.getId());
        assertEquals("user@example.com", result.getEmail());
        assertTrue(result.isEnabled());
        assertTrue(result.isFederated());
        verify(userRepository).saveAndFlush(result);

        // Verify context roles
        verify(roleService).setBusinessDomainRoleForUser(result, HdRoleName.NONE);
        verify(roleService).setAllDataDomainRolesForUser(result, HdRoleName.DATA_DOMAIN_VIEWER);

        // Verify portal roles, dashboards, NATS, sync event and comment permissions
        verifyPortalRoles(result, viewerPortalRole);
        verifyDashboardsSaved(result);
        verifyNatsCreateUserEvent();
        verifyFullSyncEvent(result);
        verifyCommentPermissions(result);
    }

    private void verifyPortalRoles(UserEntity user, PortalRoleEntity expectedRole) {
        ArgumentCaptor<UserPortalRoleEntity> portalRoleCaptor = ArgumentCaptor.forClass(UserPortalRoleEntity.class);
        verify(userPortalRoleRepository).saveAndFlush(portalRoleCaptor.capture());
        UserPortalRoleEntity savedPortalRole = portalRoleCaptor.getValue();
        assertEquals(user, savedPortalRole.getUser());
        assertEquals(expectedRole, savedPortalRole.getRole());
        assertEquals("dd-key-1", savedPortalRole.getContextKey());
        assertEquals(HdContextType.DATA_DOMAIN, savedPortalRole.getContextType());
    }

    private void verifyDashboardsSaved(UserEntity user) {
        verify(userSelectedDashboardService).saveSelectedDashboards(eq(user.getId()), eq("dd-key-1"), argThat(selections ->
                selections.size() == 1
                        && selections.get(0).dashboardId() == 42
                        && selections.get(0).dashboardTitle().equals("Sales Dashboard")
                        && selections.get(0).instanceName().equals("superset-1")
        ));
    }

    private void verifyNatsCreateUserEvent() {
        ArgumentCaptor<SubsystemUserUpdate> natsCaptor = ArgumentCaptor.forClass(SubsystemUserUpdate.class);
        verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.CREATE_USER), natsCaptor.capture());
        assertEquals("user@example.com", natsCaptor.getValue().getEmail());
        assertFalse(natsCaptor.getValue().isSendBackUsersList());
    }

    private void verifyFullSyncEvent(UserEntity user) {
        ArgumentCaptor<UserFullSyncEvent> eventCaptor = ArgumentCaptor.forClass(UserFullSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserFullSyncEvent event = eventCaptor.getValue();
        assertEquals(user.getId(), event.userId());
        assertNotNull(event.dashboardsPerContext());
        assertTrue(event.dashboardsPerContext().containsKey("dd-key-1"));
        assertTrue(event.dashboardsPerContext().get("dd-key-1").get(0).isViewer());
    }

    private void verifyCommentPermissions(UserEntity user) {
        ArgumentCaptor<DashboardCommentPermissionEntity> commentPermCaptor = ArgumentCaptor.forClass(DashboardCommentPermissionEntity.class);
        verify(dashboardCommentPermissionRepository).saveAndFlush(commentPermCaptor.capture());
        DashboardCommentPermissionEntity savedPermission = commentPermCaptor.getValue();
        assertEquals(user.getId(), savedPermission.getUserId());
        assertEquals("dd-key-1", savedPermission.getContextKey());
        assertTrue(savedPermission.isReadComments());
        assertFalse(savedPermission.isWriteComments());
        assertFalse(savedPermission.isReviewComments());
    }
}
