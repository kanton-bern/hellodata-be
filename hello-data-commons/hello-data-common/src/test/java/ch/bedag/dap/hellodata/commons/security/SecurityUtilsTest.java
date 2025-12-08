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
package ch.bedag.dap.hellodata.commons.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    @Test
    void testGetCurrentUsernameWithJwtAuthentication() {
        // given
        JwtAuthenticationToken jwtToken = createJwtAuthenticationToken();
        mockSecurityContextHolder(jwtToken);

        // when
        String username = SecurityUtils.getCurrentUsername();

        // then
        assertEquals("TEST@EMAIL.COM", username);
    }

    @Test
    void testIsSuperuserWithHellodataAuthentication() {
        // given
        HellodataAuthenticationToken hdToken = createHellodataAuthenticationToken(true);
        mockSecurityContextHolder(hdToken);

        // when then
        assertTrue(SecurityUtils.isSuperuser());
    }

    @Test
    void testIsSuperuserWithOtherAuthentication() {
        // given
        Authentication authentication = mock(Authentication.class);
        mockSecurityContextHolder(authentication);

        // when then
        assertFalse(SecurityUtils.isSuperuser());
    }

    @Test
    void testGetCurrentUserPermissionsWithHellodataAuthentication() {
        // given
        Set<String> permissions = new HashSet<>(Collections.singletonList("read"));
        HellodataAuthenticationToken hdToken = createHellodataAuthenticationToken(true, permissions);
        mockSecurityContextHolder(hdToken);

        // when
        Set<String> userPermissions = SecurityUtils.getCurrentUserPermissions();

        // then
        assertEquals(permissions, userPermissions);
    }

    @Test
    void testGetCurrentUserPermissionsWithOtherAuthentication() {
        // given
        Authentication authentication = mock(Authentication.class);
        mockSecurityContextHolder(authentication);

        // when
        Set<String> userPermissions = SecurityUtils.getCurrentUserPermissions();

        // then
        assertTrue(userPermissions.isEmpty());
    }

    @Test
    void testGetCurrentUserIdWithHellodataAuthentication() {
        // given
        UUID userId = UUID.randomUUID();
        HellodataAuthenticationToken hdToken = createHellodataAuthenticationToken(userId);
        mockSecurityContextHolder(hdToken);

        // when
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // then
        assertEquals(userId, currentUserId);
    }

    @Test
    void testGetCurrentUserIdWithOtherAuthentication() {
        // given
        Authentication authentication = mock(Authentication.class);
        mockSecurityContextHolder(authentication);

        // when then
        assertNull(SecurityUtils.getCurrentUserId());
    }

    @Test
    void testGetCurrentUserEmailWithHellodataAuthentication() {
        // given
        String email = "testuser@example.com";
        HellodataAuthenticationToken hdToken = createHellodataAuthenticationToken(true, email);
        mockSecurityContextHolder(hdToken);

        // when
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();

        // then
        assertEquals(email, currentUserEmail);
    }

    @Test
    void testGetCurrentUserEmailWithOtherAuthentication() {
        // given
        Authentication authentication = mock(Authentication.class);
        mockSecurityContextHolder(authentication);

        // when then
        assertNull(SecurityUtils.getCurrentUserEmail());
    }

    @Test
    void testGetCurrentUserFullNameWithHellodataAuthentication() {
        // given
        String firstName = "John";
        String lastName = "Doe";
        HellodataAuthenticationToken hdToken = createHellodataAuthenticationToken(true, "testuser@test.com", firstName, lastName);
        mockSecurityContextHolder(hdToken);

        // when
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();

        // then
        assertEquals(firstName + " " + lastName, currentUserFullName);
    }

    @Test
    void testGetCurrentUserFullNameWithOtherAuthentication() {
        // given
        Authentication authentication = mock(Authentication.class);
        mockSecurityContextHolder(authentication);

        // when then
        assertNull(SecurityUtils.getCurrentUserFullName());
    }

    private JwtAuthenticationToken createJwtAuthenticationToken() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(Collections.singletonMap("email", "test@email.com"));
        return new JwtAuthenticationToken(jwt);
    }

    private HellodataAuthenticationToken createHellodataAuthenticationToken(boolean superuser) {
        return new HellodataAuthenticationToken(UUID.randomUUID(), "John", "Doe", "test@example.com", superuser, Collections.emptySet());
    }

    private HellodataAuthenticationToken createHellodataAuthenticationToken(UUID userId) {
        return new HellodataAuthenticationToken(userId, "John", "Doe", "test@example.com", false, Collections.emptySet());
    }

    private HellodataAuthenticationToken createHellodataAuthenticationToken(boolean superuser, Set<String> permissions) {
        return new HellodataAuthenticationToken(UUID.randomUUID(), "John", "Doe", "test@example.com", superuser, permissions);
    }

    private HellodataAuthenticationToken createHellodataAuthenticationToken(boolean superuser, String email) {
        return new HellodataAuthenticationToken(UUID.randomUUID(), "John", "Doe", email, superuser, Collections.emptySet());
    }

    private HellodataAuthenticationToken createHellodataAuthenticationToken(boolean superuser, String email, String firstName, String lastName) {
        return new HellodataAuthenticationToken(UUID.randomUUID(), firstName, lastName, email, superuser, Collections.emptySet());
    }

    private void mockSecurityContextHolder(Authentication authentication) {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
