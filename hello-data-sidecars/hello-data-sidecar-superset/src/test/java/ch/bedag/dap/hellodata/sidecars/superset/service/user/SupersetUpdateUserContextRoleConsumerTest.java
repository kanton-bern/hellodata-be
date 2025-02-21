package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

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
}