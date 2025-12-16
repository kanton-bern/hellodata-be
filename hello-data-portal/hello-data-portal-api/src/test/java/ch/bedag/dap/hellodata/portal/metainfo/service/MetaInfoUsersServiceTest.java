package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUsersResultDto;
import ch.bedag.dap.hellodata.portal.user.data.UserWithBusinessRoleDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MetaInfoUsersServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private MetaInfoResourceService metaInfoResourceService;
    @Mock
    private HdContextRepository contextRepository;

    @InjectMocks
    private MetaInfoUsersService metaInfoUsersService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsersWithRoles_returnsSubsystemUsersResultDtoList() {
        UserWithBusinessRoleDto user = mock(UserWithBusinessRoleDto.class);
        when(user.getEmail()).thenReturn("john.doe@example.com");
        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));

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

        List<SubsystemUsersResultDto> result = metaInfoUsersService.getAllUsersWithRoles();

        assertEquals(1, result.size());
        assertEquals("instance1", result.get(0).instanceName());
        assertEquals(1, result.get(0).users().size());
    }

    @Test
    void getAllUsersWithRolesForDashboards_returnsDashboardUsersResultDtoList() {
        AppInfoResource appInfo = mock(AppInfoResource.class);
        when(appInfo.getInstanceName()).thenReturn("instance1");
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(AppInfoResource.class)))
                .thenReturn(List.of(appInfo));

        DashboardResource dashboardResource = mock(DashboardResource.class);
        SupersetDashboard dashboard = mock(SupersetDashboard.class);
        SubsystemRole dashboardRole = mock(SubsystemRole.class);
        when(dashboardRole.getName()).thenReturn("DASHBOARD_ROLE_1");
        when(dashboard.getRoles()).thenReturn(List.of(dashboardRole));
        when(dashboard.getDashboardTitle()).thenReturn("Dashboard 1");
        when(dashboardResource.getInstanceName()).thenReturn("instance1");
        when(dashboardResource.getData()).thenReturn(List.of(dashboard));
        when(metaInfoResourceService.findAllByModuleTypeAndKind(eq(ModuleType.SUPERSET), any(), eq(DashboardResource.class)))
                .thenReturn(List.of(dashboardResource));

        UserWithBusinessRoleDto user = mock(UserWithBusinessRoleDto.class);
        when(user.getEmail()).thenReturn("john.doe@example.com");
        when(user.getFirstName()).thenReturn("John");
        when(user.getLastName()).thenReturn("Doe");
        when(user.getUsername()).thenReturn("jdoe");
        when(user.getBusinessDomainRole()).thenReturn(HdRoleName.BUSINESS_DOMAIN_ADMIN);
        when(user.getEnabled()).thenReturn(true);
        when(userService.getAllUsersWithBusinessDomainRole()).thenReturn(List.of(user));

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

        HdContextEntity contextEntity = mock(HdContextEntity.class);
        when(contextEntity.getContextKey()).thenReturn("ctx1");
        when(contextEntity.getName()).thenReturn("Context 1");
        when(contextRepository.findAll()).thenReturn(List.of(contextEntity));

        List<DashboardUsersResultDto> result = metaInfoUsersService.getAllUsersWithRolesForDashboards();

        assertEquals(1, result.size());
        assertEquals("Context 1", result.get(0).contextName());
        assertEquals("instance1", result.get(0).instanceName());
        assertEquals(1, result.get(0).users().size());
    }

}
