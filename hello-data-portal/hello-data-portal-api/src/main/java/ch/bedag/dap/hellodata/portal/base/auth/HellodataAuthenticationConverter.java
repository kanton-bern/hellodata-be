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
import ch.bedag.dap.hellodata.commons.sidecars.cache.admin.UserCache;
import ch.bedag.dap.hellodata.portal.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portal.user.repository.UserRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import static ch.bedag.dap.hellodata.commons.sidecars.cache.admin.UserCache.USER_CACHE_PREFIX;

@Component
@AllArgsConstructor
public class HellodataAuthenticationConverter implements Converter<Jwt, HellodataAuthenticationToken> {

    public static final String WORKSPACES_PERMISSION = "WORKSPACES";
    private final UserRepository userRepository;
    private final RedisTemplate<String, UserCache> redisTemplate;

    @Override
    public HellodataAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaims().get("email").toString();
        String givenName = jwt.getClaims().get("given_name").toString();
        String familyName = jwt.getClaims().get("family_name").toString();
        String authId = jwt.getClaims().get("sub").toString();
        boolean isSuperuser = false;
        Set<String> permissions = new HashSet<>();
        UUID userId = null;
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(email);
        if (userEntityByEmail.isPresent()) {
            UserEntity userEntity = userEntityByEmail.get();
            userId = userEntity.getId();
            isSuperuser = userEntity.getSuperuser();
            if (isSuperuser) {
                permissions.addAll(Arrays.stream(Permission.values()).map(Enum::name).toList());
            } else {
                List<String> portalPermissions = userEntity.getPermissionsFromAllRoles();
                if (portalPermissions != null) {
                    permissions.addAll(portalPermissions);
                }
            }
            if (!userEntity.getId().toString().equalsIgnoreCase(authId) && !authId.equalsIgnoreCase(userEntity.getAuthId())) {
                //user could be removed and added again in auth subsystem (keycloak) thus the id is different
                userEntity.setAuthId(authId);
                userRepository.saveAndFlush(userEntity);
            }
            UserCache userCache = redisTemplate.opsForValue().get(USER_CACHE_PREFIX + email);
            if (isSuperuser || (userCache != null && userCache.getSupersetInstancesAdmin() != null && !userCache.getSupersetInstancesAdmin().isEmpty())) {
                permissions.add(WORKSPACES_PERMISSION);
            }
        }
        return new HellodataAuthenticationToken(userId, givenName, familyName, email, isSuperuser, permissions);
    }
}
