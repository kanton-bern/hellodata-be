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
package ch.bedag.dap.hellodata.portal.base.auth;

import ch.bedag.dap.hellodata.commons.security.HellodataAuthenticationToken;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HellodataAuthenticationConverterTest {

    private static final String EMAIL = "disabled.user@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HellodataAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter.initCaches();
    }

    @Test
    void convert_enabledUser_getsPermissionsFromRoles() {
        UserEntity user = user(true, false);
        when(userRepository.findUserEntityByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        HellodataAuthenticationToken token = converter.convert(jwt());

        assertEquals(user.getId(), token.getUserId());
        assertTrue(token.getPermissions().contains("DASHBOARDS"));
    }

    @Test
    void convert_disabledUser_getsNoPermissionsButKeepsId() {
        UserEntity user = user(false, false);
        when(userRepository.findUserEntityByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        HellodataAuthenticationToken token = converter.convert(jwt());

        // the id stays so the profile endpoint can report the user as disabled
        assertEquals(user.getId(), token.getUserId());
        assertTrue(token.getPermissions().isEmpty());
        assertFalse(token.isSuperuser());
    }

    @Test
    void convert_userDisabledAfterCaching_getsNoPermissionsOnceCacheInvalidated() {
        UserEntity enabled = user(true, false);
        UserEntity disabled = user(false, false);
        disabled.setId(enabled.getId());
        when(userRepository.findUserEntityByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(enabled), Optional.of(disabled));
        converter.convert(jwt());

        converter.invalidateUserCache(EMAIL);
        HellodataAuthenticationToken token = converter.convert(jwt());

        assertTrue(token.getPermissions().isEmpty());
    }

    private UserEntity user(boolean enabled, boolean superuser) {
        UserEntity user = spy(new UserEntity());
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setUsername(EMAIL);
        user.setEnabled(enabled);
        user.setSuperuser(superuser);
        lenient().doReturn(List.of("DASHBOARDS")).when(user).getPermissionsFromAllRoles();
        return user;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .claim("email", EMAIL)
                .claim("given_name", "Disabled")
                .claim("family_name", "User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
