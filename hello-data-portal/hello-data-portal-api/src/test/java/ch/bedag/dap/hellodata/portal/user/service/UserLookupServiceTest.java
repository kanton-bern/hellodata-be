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

import ch.bedag.dap.hellodata.portal.user.data.AdUserDto;
import ch.bedag.dap.hellodata.portal.user.data.AdUserOrigin;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserLookupProviderManager userLookupProviderManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupService userLookupService;

    @Test
    void searchUser_shortEmail_returnsEmpty() {
        List<AdUserDto> result = userLookupService.searchUser("ab");
        assertTrue(result.isEmpty());
        verifyNoInteractions(userLookupProviderManager);
    }

    @Test
    void searchUser_validEmail_callsProviderManager() {
        AdUserDto user = new AdUserDto();
        user.setEmail("test@example.com");
        when(userLookupProviderManager.searchUserByEmail("test@example.com")).thenReturn(List.of(user));

        List<AdUserDto> result = userLookupService.searchUser("test@example.com");

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    void searchUserOmitCreated_omitsAlreadyExistingAndInvalidEmails() {
        AdUserDto existingUser = new AdUserDto();
        existingUser.setEmail("existing@example.com");
        existingUser.setOrigin(AdUserOrigin.LOCAL);

        AdUserDto newUser = new AdUserDto();
        newUser.setEmail("new@example.com");
        newUser.setOrigin(AdUserOrigin.LOCAL);

        AdUserDto invalidEmailUser = new AdUserDto();
        invalidEmailUser.setEmail("not-an-email");
        invalidEmailUser.setOrigin(AdUserOrigin.LOCAL);

        when(userLookupProviderManager.searchUserByEmail("search@test.com"))
                .thenReturn(List.of(existingUser, newUser, invalidEmailUser));
        when(userRepository.findAllEmails()).thenReturn(List.of("existing@example.com"));

        List<AdUserDto> result = userLookupService.searchUserOmitCreated("search@test.com");

        assertEquals(1, result.size());
        assertEquals("new@example.com", result.get(0).getEmail());
    }
}
