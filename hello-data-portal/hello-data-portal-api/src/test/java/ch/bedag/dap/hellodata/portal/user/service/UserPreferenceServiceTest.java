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

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Test
    void setSelectedLanguage_otherUser_throwsForbidden() {
        UUID currentUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(otherUserId);
        when(userRepository.getByIdOrAuthId(otherUserId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            assertThrows(ResponseStatusException.class,
                    () -> userPreferenceService.setSelectedLanguage(otherUserId.toString(), Locale.GERMAN));
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void setSelectedLanguage_sameUser_savesLanguage() {
        UUID currentUserId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentUserId);
        when(userRepository.getByIdOrAuthId(currentUserId.toString())).thenReturn(userEntity);

        try (MockedStatic<SecurityUtils> utilities = Mockito.mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(currentUserId);

            userPreferenceService.setSelectedLanguage(currentUserId.toString(), Locale.GERMAN);

            assertEquals(Locale.GERMAN, userEntity.getSelectedLanguage());
            verify(userRepository).save(userEntity);
        }
    }

    @Test
    void getSelectedLanguage_returnsLanguage() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setSelectedLanguage(Locale.FRENCH);
        when(userRepository.getByIdOrAuthId(userId.toString())).thenReturn(userEntity);

        Locale result = userPreferenceService.getSelectedLanguage(userId.toString());
        assertEquals(Locale.FRENCH, result);
    }
}
