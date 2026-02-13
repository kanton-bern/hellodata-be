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
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private Connection connection;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private NatsSenderService natsSenderService;

    @Mock
    private HdContextRepository contextRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private DashboardCommentPermissionService dashboardCommentPermissionService;

    @Mock
    private DashboardGroupService dashboardGroupService;

    @InjectMocks
    private UserService userService;

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testCreateUser() {
        // given
        String email = "test@example.com";
        String firstName = "John";
        String lastName = "Doe";
        UUID uuid = UUID.randomUUID();
        String createdUserId = uuid.toString();
        UserRepresentation userRepresentation = mock(UserRepresentation.class, Mockito.RETURNS_DEEP_STUBS);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(uuid);
        userEntity.setEmail(email);

        when(userRepository.getByIdOrAuthId(any(String.class))).thenReturn(userEntity);
        when(keycloakService.getUserRepresentationById(any())).thenReturn(userRepresentation);
        when(keycloakService.createUser(any())).thenReturn(createdUserId);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(new UserEntity());
        when(userRepresentation.getEmail()).thenReturn("some_email@example.com");
        when(userRepresentation.getUsername()).thenReturn("username");

        // when
        String result = userService.createUser(email, firstName, lastName, AdUserOrigin.LOCAL);

        // then
        assertEquals(createdUserId, result);
    }

    @Test
    void testSyncAllUsers() {
        // given
        String email = "test@example.com";
        String firstName = "John";
        String lastName = "Doe";
        String createdUserId = UUID.randomUUID().toString();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.fromString(createdUserId));
        userEntity.setEmail(email);
        userEntity.setPortalRoles(Collections.emptySet());
        UserRepresentation userRepresentation = mock(UserRepresentation.class, Mockito.RETURNS_DEEP_STUBS);

        when(userRepository.getUserEntitiesByEnabled(true)).thenReturn(List.of(userEntity));
        when(userRepository.existsByIdOrAuthId(any(UUID.class), any(String.class))).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(new UserEntity());

        // when
        userService.syncAllUsers();

        // then
        verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.SYNC_USERS), any(AllUsersContextRoleUpdate.class));
    }

    @Test
    void testDeleteUserById_UserFound() {
        // given
        UUID uuid = UUID.randomUUID();
        String userId = uuid.toString();
        UserResource userResourceMock = mock(UserResource.class, Mockito.RETURNS_DEEP_STUBS);
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("some_email@example.com");
        userEntity.setId(uuid);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEmail(userEntity.getEmail());

        when(userRepository.getByIdOrAuthId(any(String.class))).thenReturn(userEntity);
        when(keycloakService.getUserResourceById(any())).thenReturn(userResourceMock);
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentation);

        // when
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
            userService.deleteUserById(userId);
        }

        // then
        verify(userRepository).delete(userEntity);
    }

    @Test
    void testDisableUserById_UserFound() {
        // given
        UUID uuid = UUID.randomUUID();
        String userId = uuid.toString();
        UserResource userResourceMock = mock(UserResource.class, Mockito.RETURNS_DEEP_STUBS);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        userRepresentation.setEmail("some_email@example.com");
        userRepresentation.setUsername("username");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(uuid);

        when(userRepository.getByIdOrAuthId(any(String.class))).thenReturn(userEntity);
        when(keycloakService.getUserResourceById(any())).thenReturn(userResourceMock);
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentation);

        // when
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);
            userService.disableUserById(userId);
        }

        // then
        assertFalse(userRepresentation.isEnabled());
    }

    @Test
    void testDisableUserById_UserNotFound() {
        // given
        String userId = UUID.randomUUID().toString();

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when then
            assertThrows(NotFoundException.class, () -> {
                userService.disableUserById(userId);
            });
        }
    }

    @Test
    void testEnableUserById_UserFound() {
        // given
        UUID uuid = UUID.randomUUID();
        String userId = uuid.toString();
        UserResource userResourceMock = mock(UserResource.class, Mockito.RETURNS_DEEP_STUBS);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(false);
        userRepresentation.setEmail("some_email@example.com");
        userRepresentation.setUsername("username");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(uuid);
        when(userRepository.getByIdOrAuthId(any(String.class))).thenReturn(userEntity);
        when(keycloakService.getUserResourceById(any())).thenReturn(userResourceMock);
        when(userResourceMock.toRepresentation()).thenReturn(userRepresentation);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);
            // when
            userService.enableUserById(userId);
        }

        // then
        assertTrue(userRepresentation.isEnabled());
    }

    @Test
    void testEnableUserById_UserNotFound() {
        // given
        String userId = UUID.randomUUID().toString();

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when then
            assertThrows(NotFoundException.class, () -> {
                userService.enableUserById(userId);
            });
        }
    }

    @Test
    void testGetAvailableDataDomains() {
        //given
        UserEntity userResourceMock = mock(UserEntity.class, Mockito.RETURNS_DEEP_STUBS);
        HdContextEntity contextEntityMock = mock(HdContextEntity.class, Mockito.RETURNS_DEEP_STUBS);

        UUID dataDomainId = UUID.randomUUID();
        when(contextEntityMock.getId()).thenReturn(dataDomainId);
        when(userRepository.getByIdOrAuthId(any(String.class))).thenReturn(userResourceMock);

        //when
        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // when then
            List<DataDomainDto> availableDataDomains = userService.getAvailableDataDomains();
            assertTrue(availableDataDomains.isEmpty());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToAdmin_removesUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        HdContextEntity dataDomain = new HdContextEntity();
        dataDomain.setContextKey(contextKey);

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_ADMIN"); // Not eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in this domain
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToViewer_doesNotRemoveUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_VIEWER"); // Eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was NOT removed from dashboard groups
            verify(dashboardGroupService, never()).removeUserFromDashboardGroupsInDomain(anyString(), anyString());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToBusinessSpecialist_doesNotRemoveUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_BUSINESS_SPECIALIST"); // Eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was NOT removed from dashboard groups
            verify(dashboardGroupService, never()).removeUserFromDashboardGroupsInDomain(anyString(), anyString());
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_businessDomainRoleNotNone_removesUserFromAllDomains() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey1 = "ctx1";
        String contextKey2 = "ctx2";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        HdContextEntity dataDomain1 = new HdContextEntity();
        dataDomain1.setContextKey(contextKey1);

        HdContextEntity dataDomain2 = new HdContextEntity();
        dataDomain2.setContextKey(contextKey2);

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("BUSINESS_DOMAIN_ADMIN"); // Not NONE - user becomes admin in all domains
        updateDto.setBusinessDomainRole(businessRole);

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);
        when(contextRepository.findAllByTypeIn(anyList())).thenReturn(List.of(dataDomain1, dataDomain2));

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in ALL domains
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey1);
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey2);
        }
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateContextRoles_roleChangedToEditor_removesUserFromDashboardGroups() {
        // given
        UUID userId = UUID.randomUUID();
        String contextKey = "ctx1";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();
        RoleDto businessRole = new RoleDto();
        businessRole.setName("NONE");
        updateDto.setBusinessDomainRole(businessRole);

        ContextDto contextDto = new ContextDto();
        contextDto.setContextKey(contextKey);

        RoleDto dataDomainRole = new RoleDto();
        dataDomainRole.setName("DATA_DOMAIN_EDITOR"); // Not eligible for dashboard groups

        UserContextRoleDto userContextRoleDto = new UserContextRoleDto();
        userContextRoleDto.setContext(contextDto);
        userContextRoleDto.setRole(dataDomainRole);
        updateDto.setDataDomainRoles(List.of(userContextRoleDto));

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            userService.updateContextRolesForUser(userId, updateDto, false);

            // then - verify user was removed from dashboard groups in this domain
            verify(dashboardGroupService).removeUserFromDashboardGroupsInDomain(userId.toString(), contextKey);
        }
    }
}
