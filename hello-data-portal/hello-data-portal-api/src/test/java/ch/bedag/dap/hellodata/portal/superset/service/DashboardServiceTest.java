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
package ch.bedag.dap.hellodata.portal.superset.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.security.HellodataAuthenticationToken;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardDto;
import ch.bedag.dap.hellodata.portal.superset.data.SupersetDashboardWithMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.data.UpdateSupersetDashboardMetadataDto;
import ch.bedag.dap.hellodata.portal.superset.repository.DashboardMetadataRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private DashboardMetadataRepository dashboardMetadataRepository;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private HdContextRepository contextRepository;

    private final String email = "test@bedag.ch";
    private final String instanceName = "bd01";
    private final int subsystemId = 1;

    @BeforeEach
    public void initSecurityContext() {
        HellodataAuthenticationToken hellodataAuthenticationToken = new HellodataAuthenticationToken(UUID.randomUUID(), "admin", "user", email, true, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(hellodataAuthenticationToken);
    }

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateDashboard_asAdminUserWithAdminRoles_shouldAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createAdminUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());
        given(dashboardMetadataRepository.findBySubsystemIdAndInstanceName(subsystemId, instanceName)).willReturn(Optional.empty());

        //When
        dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto());

        //Then
        verify(dashboardMetadataRepository, times(1)).findBySubsystemIdAndInstanceName(subsystemId, instanceName);
    }

    @Test
    void updateDashboard_asViewer_shouldNotAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());

        //When
        Throwable exception =
                assertThrows(ResponseStatusException.class, () -> dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto()));

        //Then
        assertTrue(exception.getMessage().contains("User is not allowed to update dashboard metadata"));
    }

    @Test
    void updateDashboard_asEditor_shouldAllowCreateOrUpdate() {
        //Given
        SubsystemUser subsystemUser = createEditorUser();
        UserResource userResources = createUserResources(subsystemUser);
        configureBaseMocks(userResources, createDashboardResource());
        given(dashboardMetadataRepository.findBySubsystemIdAndInstanceName(subsystemId, instanceName)).willReturn(Optional.empty());

        //When
        dashboardService.updateDashboard(instanceName, subsystemId, new UpdateSupersetDashboardMetadataDto());
        //Then
        verify(dashboardMetadataRepository, times(1)).findBySubsystemIdAndInstanceName(subsystemId, instanceName);
    }

    @Test
    void fetchMyDashboards_asEditor_shouldReturnAllPublishedDashboards() {
        //Given
        DashboardResource dashboardResource = createDashboardResource();
        SubsystemUser subsystemUser = createEditorUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).hasSize(dashboardResource.getData().size());
        assertThat(supersetDashboardDtos.stream()
                                        .allMatch(d -> d instanceof SupersetDashboardWithMetadataDto && ((SupersetDashboardWithMetadataDto) d).isCurrentUserEditor())).isTrue();
    }

    @Test
    void fetchMyDashboards_asViewerWithOneCorrespondingDashboardRole_shouldReturnOneDashboard() {
        //Given
        DashboardResource dashboardResource = createDashboardResource(true);
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).hasSize(1);
        assertThat(supersetDashboardDtos.stream()
                                        .allMatch(d -> d instanceof SupersetDashboardWithMetadataDto && ((SupersetDashboardWithMetadataDto) d).isCurrentUserViewer())).isTrue();
    }

    @Test
    void fetchMyDashboards_asViewerWithoutCorrespondingDashboardRole_shouldReturnEmptyList() {
        //Given
        DashboardResource dashboardResource = createDashboardResource();
        SubsystemUser subsystemUser = createViewerUser();
        UserResource userResources = createUserResources(subsystemUser);
        Optional<HdContextEntity> optionalHdContext = Optional.of(new HdContextEntity());
        List<MetaInfoResourceEntity> metaInfoResourceEntities = createMetaInfoResourceEntities(dashboardResource);

        configureBaseMocks(userResources, dashboardResource);
        given(metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS)).willReturn(metaInfoResourceEntities);
        given(contextRepository.getByContextKey(instanceName)).willReturn(optionalHdContext);

        //When
        Set<SupersetDashboardDto> supersetDashboardDtos = dashboardService.fetchMyDashboards();

        //Then
        assertThat(supersetDashboardDtos).isEmpty();
    }

    private void configureBaseMocks(UserResource userResources, DashboardResource dashboardResource) {
        given(metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_USERS, UserResource.class)).willReturn(
                userResources);
        given(metaInfoResourceService.findByModuleTypeInstanceNameAndKind(ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                                                                          DashboardResource.class)).willReturn(dashboardResource);
    }

    @NotNull
    private UserResource createUserResources(SubsystemUser subsystemUser) {
        return new UserResource(ModuleType.SUPERSET, instanceName, "", List.of(subsystemUser));
    }

    @NotNull
    private SubsystemUser createAdminUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SupersetRole adminRole = createSupersetRole(1, SlugifyUtil.BI_ADMIN_ROLE_NAME);
        subsystemUser.setRoles(List.of(adminRole));
        return subsystemUser;
    }

    @NotNull
    private SubsystemUser createViewerUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SupersetRole viewerRole = createSupersetRole(1, SlugifyUtil.BI_VIEWER_ROLE_NAME);
        SupersetRole dashboardRole = createSupersetRole(2, "D_dashboard-2"); // Specific Dashboard-Role
        subsystemUser.setRoles(List.of(viewerRole, dashboardRole));
        return subsystemUser;
    }

    @NotNull
    private SubsystemUser createEditorUser() {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail(email);
        SupersetRole editorRole = createSupersetRole(1, SlugifyUtil.BI_EDITOR_ROLE_NAME);
        subsystemUser.setRoles(List.of(editorRole));
        return subsystemUser;
    }

    @NotNull
    private static SupersetRole createSupersetRole(int id, String roleName) {
        SupersetRole role = new SupersetRole();
        role.setId(id);
        role.setName(roleName);
        return role;
    }

    @NotNull
    private DashboardResource createDashboardResource() {
        return createDashboardResource(false);
    }

    @NotNull
    private DashboardResource createDashboardResource(boolean withAdditionalViewerRole) {
        List<SupersetDashboard> supersetDashboards = new ArrayList<>();
        SupersetDashboard dashboard = new SupersetDashboard();
        dashboard.setId(1);
        dashboard.setDashboardTitle("Dashboard " + 1);
        dashboard.setPublished(true);
        dashboard.setRoles(List.of(createSupersetRole(1, SlugifyUtil.slugify("dashboard-1"))));
        supersetDashboards.add(dashboard);
        if (withAdditionalViewerRole) {
            SupersetDashboard dashboard2 = new SupersetDashboard();
            dashboard2.setId(2);
            dashboard2.setDashboardTitle("Dashboard " + 2);
            dashboard2.setPublished(true);
            dashboard2.setRoles(List.of(createSupersetRole(2, SlugifyUtil.slugify("dashboard-2"))));
            supersetDashboards.add(dashboard2);
        }
        return new DashboardResource(instanceName, "", supersetDashboards);
    }

    private List<MetaInfoResourceEntity> createMetaInfoResourceEntities(DashboardResource dashboardResource) {
        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        metaInfoResourceEntity.setMetainfo(dashboardResource);
        metaInfoResourceEntity.setContextKey(instanceName);
        return List.of(metaInfoResourceEntity);
    }
}
