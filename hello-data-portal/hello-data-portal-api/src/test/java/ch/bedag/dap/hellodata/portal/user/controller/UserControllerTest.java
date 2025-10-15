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
package ch.bedag.dap.hellodata.portal.user.controller;

import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.base.HDControllerTest;
import ch.bedag.dap.hellodata.portal.base.config.SystemProperties;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.service.KeycloakService;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {UserController.class})
class UserControllerTest extends HDControllerTest {

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private HelloDataContextConfig helloDataContextConfig;
    @MockitoBean
    private SystemProperties systemProperties;
    @MockitoBean
    private KeycloakService keycloakService;

    @Test
    void createUser_noPrivilegesShouldReturnForbidden() throws Exception {
        // given
        CreateUserRequestDto createUserRequestDto = new CreateUserRequestDto();
        AdUserDto adUserDto = new AdUserDto();
        adUserDto.setEmail("test@test.com");
        adUserDto.setFirstName("Test");
        adUserDto.setLastName("User");
        createUserRequestDto.setUser(adUserDto);
        UUID currentUserId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userService.createUser("test@test.com", "Test", "User", AdUserOrigin.LOCAL)).thenReturn(userId.toString());

        // when then
        mockMvc.perform(post("/users").header("authorization", generateToken(currentUserId)).contentType(MediaType.APPLICATION_JSON).content(asJsonString(createUserRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_noPrivilegesShouldReturnOk() throws Exception {
        // given
        CreateUserRequestDto createUserRequestDto = new CreateUserRequestDto();
        AdUserDto adUserDto = new AdUserDto();
        adUserDto.setEmail("test@test.com");
        adUserDto.setFirstName("Test");
        adUserDto.setLastName("User");
        adUserDto.setOrigin(AdUserOrigin.LOCAL);
        createUserRequestDto.setUser(adUserDto);

        UUID currentUserId = UUID.randomUUID();
        UUID createdUserId = UUID.randomUUID();
        when(userService.createUser("test@test.com", "Test", "User", AdUserOrigin.LOCAL)).thenReturn(createdUserId.toString());

        // when then
        mockMvc.perform(post("/users").header("authorization", generateToken(currentUserId, Set.of("USER_MANAGEMENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createUserRequestDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{'userId':'" + createdUserId + "'}"));
    }

    @Test
    void getAllUsers() throws Exception {
        // given
        UserDto userDto1 = new UserDto();
        userDto1.setId(UUID.randomUUID().toString());
        userDto1.setEmail("user1@test.com");
        userDto1.setFirstName("User");
        userDto1.setLastName("One");
        UserDto userDto2 = new UserDto();
        userDto2.setId(UUID.randomUUID().toString());
        userDto2.setEmail("user2@test.com");
        userDto2.setFirstName("User");
        userDto2.setLastName("Two");
        List<UserDto> users = List.of(userDto1, userDto2);
        Page<UserDto> pagedUsers = new PageImpl<>(users, PageRequest.of(0, 10, Sort.by("id")), users.size());


        // Mock the service method
        when(userService.getAllUsersPageable(any(), any())).thenReturn(pagedUsers);

        // when then
        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
                        .header("authorization", generateToken(Set.of("USER_MANAGEMENT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(userDto1.getId()))
                .andExpect(jsonPath("$.content[0].email").value("user1@test.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("User"))
                .andExpect(jsonPath("$.content[0].lastName").value("One"))
                .andExpect(jsonPath("$.content[1].id").value(userDto2.getId()))
                .andExpect(jsonPath("$.content[1].email").value("user2@test.com"))
                .andExpect(jsonPath("$.content[1].firstName").value("User"))
                .andExpect(jsonPath("$.content[1].lastName").value("Two"))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(users.size()))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getUserById() throws Exception {
        // given
        UUID userId = UUID.fromString("42012620-df0d-4157-bf67-3683e7106a66");
        UserDto userDto1 = new UserDto();
        userDto1.setId(userId.toString());
        userDto1.setEmail("user1@test.com");
        userDto1.setFirstName("User");
        userDto1.setLastName("One");

        when(userService.getUserById(userId.toString())).thenReturn(userDto1);

        // when then
        mockMvc.perform(get("/users/" + userId).header("authorization", generateToken(Set.of("USER_MANAGEMENT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                            "createdTimestamp": null,
                            "email": "user1@test.com",
                            "emailVerified": null,
                            "enabled": null,
                            "firstName": "User",
                            "id": "42012620-df0d-4157-bf67-3683e7106a66",
                            "invitationsCount": 0,
                            "lastAccess": null,
                            "lastName": "One",
                            "permissions": null,
                            "superuser": null,
                            "username": null
                        }
                        """));
    }

    @Test
    void getPermissionsForCurrentUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        CurrentUserDto currentUser = new CurrentUserDto("user@test.com", Set.of("PERMISSION1", "PERMISSION2"), false, "ContextName", false, false, Locale.ENGLISH);

        when(userService.getUserPortalPermissions(userId)).thenReturn(currentUser.permissions());
        HelloDataContextConfig.BusinessContext businessContext = new HelloDataContextConfig.BusinessContext();
        businessContext.setName("BusinessDomain");
        businessContext.setKey("ContextKey");
        when(helloDataContextConfig.getBusinessContext()).thenReturn(businessContext);

        // when then
        mockMvc.perform(get("/users/current/profile").header("authorization", generateToken(userId, Set.of("USER_MANAGEMENT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{'email':'email@test.com', 'permissions':['PERMISSION1', 'PERMISSION2'], 'isSuperuser':false, 'businessDomain':'BusinessDomain'}"));
    }

    @Test
    void deleteAndEnableAndDisableUserById() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Set<String> userManagementRole = Set.of("USER_MANAGEMENT");

        // when then
        mockMvc.perform(delete("/users/" + userId).header("authorization", generateToken(userManagementRole))).andExpect(status().isOk());

        mockMvc.perform(patch("/users/" + userId + "/disable").header("authorization", generateToken(userManagementRole))).andExpect(status().isOk());

        mockMvc.perform(patch("/users/" + userId + "/enable").header("authorization", generateToken(userManagementRole))).andExpect(status().isOk());
    }

    @Test
    void getDashboardsMarkUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        DashboardForUserDto dashboard1 = new DashboardForUserDto();
        dashboard1.setId(1);
        dashboard1.setTitle("Dashboard 1");
        dashboard1.setInstanceName("Instance 1");
        dashboard1.setInstanceUserId(1);
        dashboard1.setViewer(false);
        dashboard1.setChangedOnUtc("2021-01-01T00:00:00.000Z");
        dashboard1.setCompositeId("1");
        dashboard1.setContextKey("Context1");
        DashboardForUserDto dashboard2 = new DashboardForUserDto();
        dashboard2.setId(2);
        dashboard2.setTitle("Dashboard 2");
        dashboard2.setInstanceName("Instance 2");
        dashboard2.setInstanceUserId(2);
        dashboard2.setViewer(true);
        dashboard2.setChangedOnUtc("2021-01-01T00:00:00.000Z");
        dashboard2.setCompositeId("2");
        dashboard2.setContextKey("Context2");

        DashboardsDto dashboards = new DashboardsDto();
        dashboards.setDashboards(List.of(dashboard1, dashboard2));

        when(userService.getDashboardsMarkUser(userId.toString())).thenReturn(dashboards);

        // when then
        mockMvc.perform(get("/users/" + userId + "/dashboards").header("authorization", generateToken(userId, "test", "test", "test", false, Set.of("USER_MANAGEMENT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                            "dashboards": [
                                {
                                    "changedOnUtc": "2021-01-01T00:00:00.000Z",
                                    "compositeId": "1",
                                    "contextKey": "Context1",
                                    "id": 1,
                                    "instanceName": "Instance 1",
                                    "instanceUserId": 1,
                                    "title": "Dashboard 1",
                                    "viewer": false
                                },
                                {
                                    "changedOnUtc": "2021-01-01T00:00:00.000Z",
                                    "compositeId": "2",
                                    "contextKey": "Context2",
                                    "id": 2,
                                    "instanceName": "Instance 2",
                                    "instanceUserId": 2,
                                    "title": "Dashboard 2",
                                    "viewer": true
                                }
                            ]
                        }
                        """));
    }

    @Test
    void getContextRolesForCurrentUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserContextRoleDto contextRole1 = new UserContextRoleDto();
        ContextDto contextDto1 = new ContextDto();
        contextDto1.setId(UUID.fromString("00af77f3-9f5b-47e0-8cad-68a1b89a43e8"));
        contextDto1.setName("Context1");
        contextDto1.setContextKey("Context1");
        contextDto1.setExtra(false);
        contextRole1.setContext(contextDto1);
        RoleDto roleDto = new RoleDto();
        roleDto.setId(UUID.fromString("f87047fc-779b-4f92-ad47-4decec7c0a2f"));
        roleDto.setName("ROLE1");
        contextRole1.setRole(roleDto);

        UserContextRoleDto contextRole2 = new UserContextRoleDto();
        ContextDto contextDto2 = new ContextDto();
        contextDto2.setId(UUID.fromString("bcd7a61f-f851-480a-a316-92c087fd150f"));
        contextDto2.setName("Context2");
        contextDto2.setContextKey("Context2");
        contextDto2.setExtra(false);
        contextRole2.setContext(contextDto2);
        RoleDto roleDto2 = new RoleDto();
        roleDto2.setId(UUID.fromString("2b62bb31-5b76-461e-884e-4902c5e539a2"));
        roleDto2.setName("ROLE2");
        contextRole2.setRole(roleDto2);

        List<UserContextRoleDto> contextRoles = List.of(contextRole1, contextRole2);
        when(userService.getContextRolesForUser(userId)).thenReturn(contextRoles);

        // when then
        mockMvc.perform(get("/users/current/context-roles").header("authorization", generateToken(userId, "test", "test", "test", false, Set.of("USER_MANAGEMENT"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                            {
                                "context": {
                                    "contextKey": "Context1",
                                    "createdBy": null,
                                    "createdDate": null,
                                    "extra": false,
                                    "id": "00af77f3-9f5b-47e0-8cad-68a1b89a43e8",
                                    "modifiedBy": null,
                                    "modifiedDate": null,
                                    "name": "Context1",
                                    "parentContextKey": null,
                                    "type": null
                                },
                                "role": {
                                    "contextType": null,
                                    "id": "f87047fc-779b-4f92-ad47-4decec7c0a2f",
                                    "name": "ROLE1"
                                }
                            },
                            {
                                "context": {
                                    "contextKey": "Context2",
                                    "createdBy": null,
                                    "createdDate": null,
                                    "extra": false,
                                    "id": "bcd7a61f-f851-480a-a316-92c087fd150f",
                                    "modifiedBy": null,
                                    "modifiedDate": null,
                                    "name": "Context2",
                                    "parentContextKey": null,
                                    "type": null
                                },
                                "role": {
                                    "contextType": null,
                                    "id": "2b62bb31-5b76-461e-884e-4902c5e539a2",
                                    "name": "ROLE2"
                                }
                            }
                        ]
                        """));
    }

    @Test
    void updateContextRolesForUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UpdateContextRolesForUserDto updateContextRoles = new UpdateContextRolesForUserDto();

        // when then
        mockMvc.perform(patch("/users/" + userId + "/context-roles").header("authorization", generateToken(userId, Set.of("USER_MANAGEMENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateContextRoles))).andExpect(status().isOk());
    }
}

