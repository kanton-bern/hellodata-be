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
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserLookupProviderManager userLookupProviderManager;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUser(String email) {
        return searchUserInternal(email);
    }

    @Transactional(readOnly = true)
    public List<AdUserDto> searchUserOmitCreated(String email) {
        List<AdUserDto> users = searchUserInternal(email);
        Map<String, AdUserDto> emailToUserDto = users.stream().collect(Collectors.toMap(AdUserDto::getEmail, user -> user, (existing, replacement) -> {
            if (existing.getOrigin() == AdUserOrigin.LOCAL && replacement.getOrigin() != AdUserOrigin.LOCAL) {
                return replacement;
            }
            return existing;
        }));

        List<String> usersAlreadyAdded = userRepository.findAllEmails().stream().map(eMail -> eMail.toLowerCase(Locale.ROOT)).toList();
        Set<String> uniqueEmails = new HashSet<>();
        List<AdUserDto> uniqueUsers = new ArrayList<>();
        for (Map.Entry<String, AdUserDto> entry : emailToUserDto.entrySet()) {
            String emailKey = entry.getKey().toLowerCase(Locale.ROOT);
            AdUserDto user = entry.getValue();
            if (uniqueEmails.add(emailKey) && !usersAlreadyAdded.contains(emailKey) && isValidEmail(user.getEmail())) {
                uniqueUsers.add(user);
            }
        }
        return uniqueUsers;
    }

    private List<AdUserDto> searchUserInternal(String email) {
        if (email == null || email.length() < 3) {
            return Collections.emptyList();
        }
        return userLookupProviderManager.searchUserByEmail(email);
    }

    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }
}
