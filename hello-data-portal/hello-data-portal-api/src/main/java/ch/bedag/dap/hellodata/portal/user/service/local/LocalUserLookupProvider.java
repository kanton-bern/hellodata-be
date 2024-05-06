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
package ch.bedag.dap.hellodata.portal.user.service.local;

import ch.bedag.dap.hellodata.portal.profiles.LocalUserLookupProfile;
import ch.bedag.dap.hellodata.portal.user.data.AdUserDto;
import ch.bedag.dap.hellodata.portal.user.service.UserLookupProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
@LocalUserLookupProfile
public class LocalUserLookupProvider implements UserLookupProvider {
    @Override
    public List<AdUserDto> searchUserByEmail(String email) {
        log.info("Looking up users locally with email: {}", email);
        if (null == email || email.length() < 3 || email.length() > 255) {
            return new ArrayList<>();
        }
        return IntStream.range(0, 1).mapToObj(index -> {
            AdUserDto user = new AdUserDto();
            user.setEmail(email);
            String firstName = getFirstName(email);
            user.setFirstName(firstName + "-" + index);
            user.setLastName(getLastName(email) + "-" + index);
            return user;
        }).toList();
    }

    @NotNull
    private String getFirstName(String email) {
        if (email.indexOf(".") >= 1) {
            return email.substring(0, email.indexOf("."));
        }
        if (email.indexOf("@") >= 1) {
            return email.substring(0, email.indexOf("@"));
        }
        return email;
    }

    private String getLastName(String email) {
        if (email.indexOf("@") >= 1) {
            String cut = email.substring(0, email.indexOf("@"));
            return getSecondPart(cut);
        }
        return getSecondPart(email);
    }

    private String getSecondPart(String string) {
        if (string.lastIndexOf(".") > 0) {
            int i = string.lastIndexOf(".");
            return string.substring(i + 1);
        }
        return string;
    }
}
