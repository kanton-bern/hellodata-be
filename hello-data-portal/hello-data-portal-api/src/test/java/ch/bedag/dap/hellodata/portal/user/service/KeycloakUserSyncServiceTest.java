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

import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakUserSyncServiceTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Mock
    private KeycloakService keycloakService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private KeycloakUserSyncService keycloakUserSyncService;

    @BeforeEach
    void setUp() {
        keycloakUserSyncService = new KeycloakUserSyncService(keycloakService, userRepository, userService, applicationEventPublisher);
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void syncUsers_keepsThePortalEnabledFlag(boolean enabledInPortal, boolean enabledInKeycloak) {
        UserEntity userEntity = portalUser(enabledInPortal);
        when(userRepository.findAll()).thenReturn(List.of(userEntity));
        when(keycloakService.getUserRepresentationByEmail(EMAIL)).thenReturn(keycloakUser(enabledInKeycloak));
        when(userService.searchUserOmitCreated(anyString())).thenReturn(List.of());

        keycloakUserSyncService.syncUsers();

        assertThat(userEntity.isEnabled()).isEqualTo(enabledInPortal);
    }

    @Test
    void syncUsers_stillTakesOverTheNameFromKeycloak() {
        UserEntity userEntity = portalUser(true);
        when(userRepository.findAll()).thenReturn(List.of(userEntity));
        when(keycloakService.getUserRepresentationByEmail(EMAIL)).thenReturn(keycloakUser(true));
        when(userService.searchUserOmitCreated(anyString())).thenReturn(List.of());

        keycloakUserSyncService.syncUsers();

        assertThat(userEntity.getFirstName()).isEqualTo("Jane");
        assertThat(userEntity.getLastName()).isEqualTo("Doe");
        assertThat(userEntity.getAuthId()).isEqualTo("kc-id");
    }

    private static UserEntity portalUser(boolean enabled) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID());
        userEntity.setEmail(EMAIL);
        userEntity.setEnabled(enabled);
        return userEntity;
    }

    private static UserRepresentation keycloakUser(boolean enabled) {
        UserRepresentation representation = new UserRepresentation();
        representation.setId("kc-id");
        representation.setEmail(EMAIL);
        representation.setFirstName("Jane");
        representation.setLastName("Doe");
        representation.setEnabled(enabled);
        return representation;
    }
}
