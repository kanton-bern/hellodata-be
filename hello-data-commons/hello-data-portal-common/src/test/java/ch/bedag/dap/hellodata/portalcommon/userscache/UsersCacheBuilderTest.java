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
package ch.bedag.dap.hellodata.portalcommon.userscache;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portalcommon.role.entity.RoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DataDomainRoleDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUsersResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UsersCacheBuilderTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MetaInfoResourceService metaInfoResourceService;
    @Mock
    private HdContextRepository contextRepository;

    @InjectMocks
    private UsersCacheBuilder usersCacheBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void buildSubsystemUsers_returnsSubsystemUsersResultDtoList() {
        when(userRepository.findAll()).thenReturn(List.of(portalUser()));
        when(contextRepository.findAll()).thenReturn(List.of(
                context("business", "Business Domain", HdContextType.BUSINESS_DOMAIN),
                context("ctx1", "Context 1", HdContextType.DATA_DOMAIN)));

        SubsystemRole role = mock(SubsystemRole.class);
        when(role.getName()).thenReturn("role1");
        SubsystemUser subsystemUser = mock(SubsystemUser.class);
        when(subsystemUser.getEmail()).thenReturn("john.doe@example.com");
        when(subsystemUser.getFirstName()).thenReturn("John");
        when(subsystemUser.getLastName()).thenReturn("Doe");
        when(subsystemUser.getUsername()).thenReturn("jdoe");
        when(subsystemUser.getRoles()).thenReturn(List.of(role));
        HdResource usersPack = mock(HdResource.class);
        when(usersPack.getData()).thenReturn(List.of(subsystemUser));
        when(usersPack.getInstanceName()).thenReturn("instance1");
        when(metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS)).thenReturn(List.of(usersPack));

        List<SubsystemUsersResultDto> result = usersCacheBuilder.buildSubsystemUsers();

        assertEquals(1, result.size());
        assertEquals("instance1", result.get(0).instanceName());
        assertEquals(1, result.get(0).users().size());
        SubsystemUserDto user = result.get(0).users().get(0);
        assertEquals(List.of("role1"), user.roles());
        assertEquals(HdRoleName.BUSINESS_DOMAIN_ADMIN, user.businessDomainRole());
        assertEquals(List.of(new DataDomainRoleDto("Context 1", "ctx1", HdRoleName.DATA_DOMAIN_VIEWER)), user.dataDomainRoles());
        assertTrue(user.enabled());
    }

    @Test
    void buildDashboardUsers_returnsDashboardUsersResultDtoList() {
        AppInfoResource appInfo = mock(AppInfoResource.class);
        when(appInfo.getInstanceName()).thenReturn("instance1");
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(AppInfoResource.class)))
                .thenReturn(List.of(appInfo));

        DashboardResource dashboardResource = mock(DashboardResource.class);
        SupersetDashboard dashboard = mock(SupersetDashboard.class);
        SubsystemRole dashboardRole = mock(SubsystemRole.class);
        when(dashboardRole.getName()).thenReturn("D_DASHBOARD_ROLE_1");
        when(dashboard.getRoles()).thenReturn(List.of(dashboardRole));
        when(dashboard.getDashboardTitle()).thenReturn("Dashboard 1");
        when(dashboardResource.getInstanceName()).thenReturn("instance1");
        when(dashboardResource.getData()).thenReturn(List.of(dashboard));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(DashboardResource.class)))
                .thenReturn(List.of(dashboardResource));

        when(userRepository.findAll()).thenReturn(List.of(portalUser()));

        MetaInfoResourceEntity userPack = mock(MetaInfoResourceEntity.class);
        SubsystemUser subsystemUser = mock(SubsystemUser.class);
        when(subsystemUser.getEmail()).thenReturn("john.doe@example.com");
        when(subsystemUser.getRoles()).thenReturn(List.of(dashboardRole));
        HdResource metainfoResource = mock(HdResource.class);
        when(metainfoResource.getData()).thenReturn(List.of(subsystemUser));
        when(userPack.getMetainfo()).thenReturn(metainfoResource);
        when(userPack.getInstanceName()).thenReturn("instance1");
        when(userPack.getContextKey()).thenReturn("ctx1");
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)).thenReturn(List.of(userPack));

        when(contextRepository.findAll()).thenReturn(List.of(
                context("business", "Business Domain", HdContextType.BUSINESS_DOMAIN),
                context("ctx1", "Context 1", HdContextType.DATA_DOMAIN)));

        List<DashboardUsersResultDto> result = usersCacheBuilder.buildDashboardUsers();

        assertEquals(1, result.size());
        assertEquals("Context 1", result.get(0).contextName());
        assertEquals("instance1", result.get(0).instanceName());
        assertEquals(1, result.get(0).users().size());
        SubsystemUserDto user = result.get(0).users().get(0);
        assertEquals("John", user.name());
        assertEquals(List.of("Dashboard 1"), user.roles());
        assertEquals(HdRoleName.BUSINESS_DOMAIN_ADMIN, user.businessDomainRole());
    }

    @Test
    void buildDashboardUsers_showsBiNoAccessRole() {
        AppInfoResource appInfo = mock(AppInfoResource.class);
        when(appInfo.getInstanceName()).thenReturn("instance1");
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(AppInfoResource.class)))
                .thenReturn(List.of(appInfo));
        DashboardResource dashboardResource = mock(DashboardResource.class);
        when(dashboardResource.getInstanceName()).thenReturn("instance1");
        when(dashboardResource.getData()).thenReturn(List.of());
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(DashboardResource.class)))
                .thenReturn(List.of(dashboardResource));

        when(userRepository.findAll()).thenReturn(List.of(portalUser()));

        SubsystemRole noAccessRole = mock(SubsystemRole.class);
        when(noAccessRole.getName()).thenReturn("BI_NO_ACCESS");
        SubsystemRole publicRole = mock(SubsystemRole.class);
        when(publicRole.getName()).thenReturn("Public");
        SubsystemUser subsystemUser = mock(SubsystemUser.class);
        when(subsystemUser.getEmail()).thenReturn("john.doe@example.com");
        when(subsystemUser.getRoles()).thenReturn(List.of(noAccessRole, publicRole));
        HdResource metainfoResource = mock(HdResource.class);
        when(metainfoResource.getData()).thenReturn(List.of(subsystemUser));
        MetaInfoResourceEntity userPack = mock(MetaInfoResourceEntity.class);
        when(userPack.getMetainfo()).thenReturn(metainfoResource);
        when(userPack.getInstanceName()).thenReturn("instance1");
        when(userPack.getContextKey()).thenReturn("ctx1");
        when(metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)).thenReturn(List.of(userPack));

        when(contextRepository.findAll()).thenReturn(List.of(context("ctx1", "Context 1", HdContextType.DATA_DOMAIN)));

        List<DashboardUsersResultDto> result = usersCacheBuilder.buildDashboardUsers();

        assertEquals(List.of("BI_NO_ACCESS"), result.get(0).users().get(0).roles());
    }

    private UserEntity portalUser() {
        UserEntity user = new UserEntity();
        user.setEmail("john.doe@example.com");
        user.setUsername("jdoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEnabled(true);
        user.setContextRoles(Set.of(
                contextRole("business", HdRoleName.BUSINESS_DOMAIN_ADMIN),
                contextRole("ctx1", HdRoleName.DATA_DOMAIN_VIEWER)));
        return user;
    }

    private UserContextRoleEntity contextRole(String contextKey, HdRoleName roleName) {
        RoleEntity role = new RoleEntity();
        role.setName(roleName);
        UserContextRoleEntity contextRole = new UserContextRoleEntity();
        contextRole.setContextKey(contextKey);
        contextRole.setRole(role);
        return contextRole;
    }

    private HdContextEntity context(String contextKey, String name, HdContextType type) {
        HdContextEntity context = new HdContextEntity();
        context.setContextKey(contextKey);
        context.setName(name);
        context.setType(type);
        return context;
    }
}
