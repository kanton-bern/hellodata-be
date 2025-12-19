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
import ch.bedag.dap.hellodata.commons.security.Permission;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portal.user.util.UserDtoMapper;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class HellodataAuthenticationConverter implements Converter<Jwt, HellodataAuthenticationToken> {

    private final UserRepository userRepository;

    @Value("${hello-data.cache.user-database-ttl-minutes:2}")
    private int userDatabaseTtlCacheMinutes;
    private final Cache<String, UserDto> userDatabaseCache = Caffeine.newBuilder()
            .expireAfterWrite(userDatabaseTtlCacheMinutes, java.util.concurrent.TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    @Value("${hello-data.cache.user-permission-ttl-minutes:2}")
    private int getUserDatabaseTtlCacheMinutes;
    private final Cache<String, List<String>> userPermissionsCache = Caffeine.newBuilder()
            .expireAfterWrite(getUserDatabaseTtlCacheMinutes, java.util.concurrent.TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    @Transactional
    public HellodataAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaims().get("email").toString();
        String givenName = jwt.getClaims().get("given_name").toString();
        String familyName = jwt.getClaims().get("family_name").toString();
        boolean isSuperuser = false;
        Set<String> permissions = new HashSet<>();
        UUID userId = null;

        UserDto userDto = getUserDto(email);
        if (userDto != null) { //NOSONAR
            userId = UUID.fromString(userDto.getId());
            isSuperuser = BooleanUtils.isTrue(userDto.getSuperuser());
            if (isSuperuser) {
                permissions.addAll(Arrays.stream(Permission.values()).map(Enum::name).toList());
            } else {
                List<String> portalPermissions = getPortalPermissions(email);
                if (!portalPermissions.isEmpty()) {
                    permissions.addAll(portalPermissions);
                }
            }
        }
        return new HellodataAuthenticationToken(userId, givenName, familyName, email, isSuperuser, permissions);
    }

    /**
     * Get user entity from database and map to UserDto. Keeps a copy in local caffeine cache
     *
     * @param email - user email
     * @return UserDto or null if not found
     */
    private UserDto getUserDto(String email) {
        return userDatabaseCache.get(email, emailKey -> {
            UserEntity userEntity = userRepository.findUserEntityByEmailIgnoreCase(emailKey).orElse(null);
            if (userEntity != null) {
                return UserDtoMapper.map(userEntity);
            }
            return null;
        });
    }

    /**
     * Get portal permissions for user from database. Keeps a copy in local caffeine cache
     *
     * @param email - user email
     * @return List of permissions or empty list if not found
     */
    private List<String> getPortalPermissions(String email) {
        return userPermissionsCache.get(email, emailKey -> {
            UserEntity userEntity = userRepository.findUserEntityByEmailIgnoreCase(emailKey).orElse(null);
            if (userEntity != null) {
                return new ArrayList<>(userEntity.getPermissionsFromAllRoles());
            }
            return Collections.emptyList();
        });
    }
}
