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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.base.auth.HellodataAuthenticationConverter;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.*;
import ch.bedag.dap.hellodata.portal.user.event.UserFullSyncEvent;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private UserSelectedDashboardService userSelectedDashboardService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private HellodataAuthenticationConverter authenticationConverter;

    @Mock
    private UserLookupService userLookupService;

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private UserContextRoleService userContextRoleService;

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
    void testDeleteUserById_UserFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());

            // when
            userService.deleteUserById(userId.toString());

            // then
            verify(userContextRoleService).removeUserFromDashboardGroupsForAllDomains(userId);
            verify(userRepository).delete(userEntity);
            verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.DELETE_USER), any());
        }
    }

    @Test
    void testDisableUserById_UserFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setEnabled(true);

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);
        when(userPreferenceService.getSelectedLanguageByEmail("test@example.com")).thenReturn(Locale.GERMAN);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            UserDto result = userService.disableUserById(userId.toString());

            // then
            assertFalse(userEntity.isEnabled());
            verify(userRepository).save(userEntity);
            verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.DISABLE_USER), any());
            verify(emailNotificationService).notifyAboutUserDeactivation(any(), eq("test@example.com"), eq(Locale.GERMAN));
        }
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
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setEnabled(false);

        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::isSuperuser).thenReturn(true);

            // when
            UserDto result = userService.enableUserById(userId.toString());

            // then
            assertTrue(userEntity.isEnabled());
            verify(userRepository).saveAndFlush(userEntity);
            verify(natsSenderService).publishMessageToJetStream(eq(HDEvent.ENABLE_USER), any());
            verify(emailNotificationService).notifyAboutUserActivation(any(), eq("test@example.com"), any());
        }
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
    void testGetAvailableDataDomains_delegates() {
        DataDomainDto domain = new DataDomainDto();
        domain.setKey("domain-1");
        when(userContextRoleService.getAvailableDataDomains()).thenReturn(List.of(domain));

        List<DataDomainDto> result = userService.getAvailableDataDomains();

        assertEquals(1, result.size());
        assertEquals("domain-1", result.get(0).getKey());
        verify(userContextRoleService).getAvailableDataDomains();
    }

    @Test
    void testUpdateContextRolesForUser_delegates() {
        UUID userId = UUID.randomUUID();
        UpdateContextRolesForUserDto updateDto = new UpdateContextRolesForUserDto();

        userService.updateContextRolesForUser(userId, updateDto, true);

        verify(userContextRoleService).updateContextRolesForUser(userId, updateDto, true);
    }

    @Test
    void testSearchUser_delegates() {
        AdUserDto user = new AdUserDto();
        user.setEmail("user@test.com");
        when(userLookupService.searchUser("user@test.com")).thenReturn(List.of(user));

        List<AdUserDto> result = userService.searchUser("user@test.com");

        assertEquals(1, result.size());
        assertEquals("user@test.com", result.get(0).getEmail());
        verify(userLookupService).searchUser("user@test.com");
    }

    @Test
    void testSetSelectedLanguage_delegates() {
        userService.setSelectedLanguage("user-1", Locale.FRENCH);
        verify(userPreferenceService).setSelectedLanguage("user-1", Locale.FRENCH);
    }
}
