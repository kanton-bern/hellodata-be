package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.IdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserByIdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.UserResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SupersetUpdateUserContextRoleConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private SupersetClientProvider supersetClientProvider;
    @Mock
    private HelloDataContextConfig helloDataContextConfig;
    @Mock
    private UserResourceProviderService userResourceProviderService;
    @Mock
    private SupersetClient supersetClient;
    @InjectMocks
    private SupersetUpdateUserContextRoleConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(supersetClientProvider.getSupersetClientInstance()).thenReturn(supersetClient);
    }

    @Test
    void testSubscribe() throws URISyntaxException, IOException {
        String json = """
                {
                  "email": "testadmin@yahoo.com",
                  "contextRoles": [
                    {
                      "contextKey": "dd01",
                      "parentContextKey": null,
                      "roleName": "DATA_DOMAIN_ADMIN"
                    },
                    {
                      "contextKey": "dev",
                      "parentContextKey": null,
                      "roleName": "HELLODATA_ADMIN"
                    }
                  ],
                  "extraModuleRoles": {
                    "dd01": [
                      {
                        "moduleType": "SUPERSET",
                        "roleNames": []
                      }
                    ]
                  }
                }""";

        UserContextRoleUpdate userContextRoleUpdate = objectMapper.readValue(json, UserContextRoleUpdate.class);

        SupersetUserUpdateResponse supersetUserUpdateResponse = new SupersetUserUpdateResponse();
        SubsystemUserUpdate result = new SubsystemUserUpdate();
        result.setRoles(List.of(2, 4, 5, 6));
        supersetUserUpdateResponse.setResult(result);
        when(supersetClient.updateUserRoles(any(SupersetUserRolesUpdate.class), any(Integer.class))).thenReturn(supersetUserUpdateResponse);
        HelloDataContextConfig.Context context = new HelloDataContextConfig.Context();
        context.setType(HdContextType.DATA_DOMAIN.name());
        context.setName("dd01");
        context.setKey("dd01");
        when(helloDataContextConfig.getContext()).thenReturn(context);

        SupersetUsersResponse existingSupersetUsers = new SupersetUsersResponse();
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setEmail("testadmin@yahoo.com");
        subsystemUser.setRoles(Collections.emptyList());
        subsystemUser.setActive(true);
        subsystemUser.setId(1);
        subsystemUser.setFirstName("Some");
        subsystemUser.setLastName("Name");
        existingSupersetUsers.setResult(Collections.singletonList(subsystemUser));
        when(supersetClient.users()).thenReturn(existingSupersetUsers);

        IdResponse idResponse = new IdResponse();
        idResponse.setId(1);
        when(supersetClient.createUser(any(SubsystemUserUpdate.class))).thenReturn(idResponse);

        SupersetUserByIdResponse supersetUserByIdResponse = new SupersetUserByIdResponse();
        supersetUserByIdResponse.setResult(subsystemUser);
        when(supersetClient.user(any(Integer.class))).thenReturn(supersetUserByIdResponse);

        SupersetRolesResponse supersetRolesResponse = new SupersetRolesResponse();
        SubsystemRole supersetRole = new SubsystemRole();
        supersetRole.setId(1);
        supersetRole.setName("Public");
        SubsystemRole supersetRole2 = new SubsystemRole();
        supersetRole2.setId(2);
        supersetRole2.setName("Admin");
        SubsystemRole supersetRole3 = new SubsystemRole();
        supersetRole3.setId(3);
        supersetRole3.setName("BI_VIEWER");
        SubsystemRole supersetRole4 = new SubsystemRole();
        supersetRole4.setId(4);
        supersetRole4.setName("BI_EDITOR");
        SubsystemRole supersetRole5 = new SubsystemRole();
        supersetRole5.setId(5);
        supersetRole5.setName("BI_ADMIN");
        SubsystemRole supersetRole6 = new SubsystemRole();
        supersetRole6.setId(6);
        supersetRole6.setName("sql_lab");
        supersetRolesResponse.setResult(List.of(supersetRole, supersetRole2, supersetRole3, supersetRole4, supersetRole5, supersetRole6));
        when(supersetClient.roles()).thenReturn(supersetRolesResponse);

        consumer.subscribe(userContextRoleUpdate);

        verify(supersetClientProvider, times(1)).getSupersetClientInstance();
        SupersetUserRolesUpdate expectedRolesUpdate = new SupersetUserRolesUpdate();
        expectedRolesUpdate.setRoles(List.of(6, 4, 5, 2));
        verify(supersetClient, times(1)).updateUserRoles(eq(expectedRolesUpdate), any(Integer.class));
        verify(userResourceProviderService, times(1)).publishUsers();
    }

    @Test
    void shouldReplaceRlsRolesForViewer() throws URISyntaxException, IOException {
        UserContextRoleUpdate update = rlsUserContextRoleUpdate(HdRoleName.DATA_DOMAIN_VIEWER, Map.of("dd01", List.of("RLS_02", "RLS_03")));
        mockSupersetForRlsTest(List.of(3, 10));

        consumer.subscribe(update);

        assertThat(captureUpdatedRoles()).containsExactlyInAnyOrder(3, 11, 12);
    }

    @Test
    void shouldRemoveAllRlsRolesForViewerWithEmptyRlsSelection() throws URISyntaxException, IOException {
        UserContextRoleUpdate update = rlsUserContextRoleUpdate(HdRoleName.DATA_DOMAIN_VIEWER, Map.of("dd01", List.of()));
        mockSupersetForRlsTest(List.of(3, 10, 11));

        consumer.subscribe(update);

        assertThat(captureUpdatedRoles()).containsExactly(3);
    }

    @Test
    void shouldKeepRlsRolesIfPayloadHasNoRlsRolesForContext() throws URISyntaxException, IOException {
        UserContextRoleUpdate update = rlsUserContextRoleUpdate(HdRoleName.DATA_DOMAIN_VIEWER, null);
        mockSupersetForRlsTest(List.of(3, 10));

        consumer.subscribe(update);

        assertThat(captureUpdatedRoles()).containsExactlyInAnyOrder(3, 10);
    }

    @Test
    void shouldNotAssignRlsRolesForEditor() throws URISyntaxException, IOException {
        UserContextRoleUpdate update = rlsUserContextRoleUpdate(HdRoleName.DATA_DOMAIN_EDITOR, Map.of("dd01", List.of("RLS_02")));
        mockSupersetForRlsTest(List.of(10));

        consumer.subscribe(update);

        assertThat(captureUpdatedRoles()).containsExactlyInAnyOrder(4, 6);
    }

    private UserContextRoleUpdate rlsUserContextRoleUpdate(HdRoleName roleName, Map<String, List<String>> rlsRolesPerContext) {
        UserContextRoleUpdate.ContextRole contextRole = new UserContextRoleUpdate.ContextRole();
        contextRole.setContextKey("dd01");
        contextRole.setRoleName(roleName);
        UserContextRoleUpdate update = new UserContextRoleUpdate();
        update.setEmail("viewer@example.com");
        update.setUsername("viewer@example.com");
        update.setContextRoles(List.of(contextRole));
        update.setDashboardsPerContext(Map.of("dd01", List.of()));
        update.setRlsRolesPerContext(rlsRolesPerContext);
        update.setSendBackUsersList(false);
        return update;
    }

    private void mockSupersetForRlsTest(List<Integer> currentUserRoleIds) throws URISyntaxException, IOException {
        HelloDataContextConfig.Context context = new HelloDataContextConfig.Context();
        context.setType(HdContextType.DATA_DOMAIN.name());
        context.setName("dd01");
        context.setKey("dd01");
        when(helloDataContextConfig.getContext()).thenReturn(context);

        List<SubsystemRole> allRoles = List.of(role(1, "Public"), role(2, "Admin"), role(3, "BI_VIEWER"), role(4, "BI_EDITOR"),
                role(5, "BI_ADMIN"), role(6, "sql_lab"), role(10, "RLS_01"), role(11, "RLS_02"), role(12, "RLS_03"));
        SupersetRolesResponse supersetRolesResponse = new SupersetRolesResponse();
        supersetRolesResponse.setResult(allRoles);
        when(supersetClient.roles()).thenReturn(supersetRolesResponse);

        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setId(1);
        subsystemUser.setEmail("viewer@example.com");
        subsystemUser.setRoles(allRoles.stream().filter(r -> currentUserRoleIds.contains(r.getId())).toList());
        SupersetUsersResponse usersResponse = new SupersetUsersResponse();
        usersResponse.setResult(List.of(subsystemUser));
        when(supersetClient.getUser(anyString(), anyString())).thenReturn(usersResponse);

        SupersetDashboardResponse dashboardResponse = new SupersetDashboardResponse();
        dashboardResponse.setResult(List.of());
        when(supersetClient.dashboards()).thenReturn(dashboardResponse);

        SupersetUserUpdateResponse updateResponse = new SupersetUserUpdateResponse();
        SubsystemUserUpdate result = new SubsystemUserUpdate();
        result.setRoles(List.of());
        updateResponse.setResult(result);
        when(supersetClient.updateUserRoles(any(SupersetUserRolesUpdate.class), any(Integer.class))).thenReturn(updateResponse);
    }

    private List<Integer> captureUpdatedRoles() throws URISyntaxException, IOException {
        ArgumentCaptor<SupersetUserRolesUpdate> captor = ArgumentCaptor.forClass(SupersetUserRolesUpdate.class);
        verify(supersetClient).updateUserRoles(captor.capture(), any(Integer.class));
        return captor.getValue().getRoles();
    }

    private static SubsystemRole role(int id, String name) {
        SubsystemRole role = new SubsystemRole();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
