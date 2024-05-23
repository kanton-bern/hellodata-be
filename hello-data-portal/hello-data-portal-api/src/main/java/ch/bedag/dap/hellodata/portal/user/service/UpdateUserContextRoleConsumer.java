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

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.portal.role.entity.PortalRoleEntity;
import ch.bedag.dap.hellodata.portal.role.entity.SystemDefaultPortalRoleName;
import ch.bedag.dap.hellodata.portal.role.entity.UserPortalRoleEntity;
import ch.bedag.dap.hellodata.portal.role.repository.PortalRoleRepository;
import ch.bedag.dap.hellodata.portal.role.repository.UserPortalRoleRepository;
import ch.bedag.dap.hellodata.portal.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portal.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class UpdateUserContextRoleConsumer {

    private final UserRepository userRepository;
    private final UserPortalRoleRepository userPortalRoleRepository;
    private final PortalRoleRepository portalRoleRepository;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public CompletableFuture<Void> subscribe(UserContextRoleUpdate userContextRoleUpdate) {
        log.info("Update user context role received {}", userContextRoleUpdate);
        Optional<UserEntity> userEntityByEmail = userRepository.findUserEntityByEmailIgnoreCase(userContextRoleUpdate.getEmail());
        List<UserContextRoleUpdate.ContextRole> domainContextRoles = userContextRoleUpdate.getContextRoles(); // Filter by context necessary? Only BD is available here
        if (userEntityByEmail.isPresent()) {
            UserEntity userEntity = userEntityByEmail.get();
            Set<UserPortalRoleEntity> userPortalRoleEntities = userEntity.getPortalRoles() != null ? userEntity.getPortalRoles() : new HashSet<>();
            if (!userPortalRoleEntities.isEmpty()) {
                deleteExistingUserPortalRoles(userEntity, userPortalRoleEntities);
            }
            updateContextRoles(domainContextRoles, userEntity);
        }
        return null;
    }

    private void updateContextRoles(List<UserContextRoleUpdate.ContextRole> domainContextRoles, UserEntity userEntity) {
        for (UserContextRoleUpdate.ContextRole contextRole : domainContextRoles) {
            HdRoleName roleName = contextRole.getRoleName();
            String contextKey = contextRole.getContextKey();
            SystemDefaultPortalRoleName portalRoleName = getSystemDefaultPortalRoleName(roleName);
            if (portalRoleName != null) {
                Optional<PortalRoleEntity> role = portalRoleRepository.findByName(portalRoleName.name());
                if (role.isPresent()) {
                    UserPortalRoleEntity userPortalRoleEntity = new UserPortalRoleEntity();
                    userPortalRoleEntity.setRole(role.get());
                    userPortalRoleEntity.setUser(userEntity);
                    userPortalRoleEntity.setContextKey(contextKey);
                    userPortalRoleEntity.setContextType(roleName.getContextType());
                    userPortalRoleRepository.saveAndFlush(userPortalRoleEntity);
                } else {
                    log.warn("Couldn't find a portal role with name {}", portalRoleName);
                }
            }
        }
    }

    @Nullable
    private static SystemDefaultPortalRoleName getSystemDefaultPortalRoleName(HdRoleName roleName) {
        SystemDefaultPortalRoleName portalRoleName;
        switch (roleName) {
            case HELLODATA_ADMIN -> portalRoleName = SystemDefaultPortalRoleName.HELLODATA_ADMIN;
            case BUSINESS_DOMAIN_ADMIN -> portalRoleName = SystemDefaultPortalRoleName.BUSINESS_DOMAIN_ADMIN;
            case DATA_DOMAIN_ADMIN -> portalRoleName = SystemDefaultPortalRoleName.DATA_DOMAIN_ADMIN;
            case DATA_DOMAIN_EDITOR -> portalRoleName = SystemDefaultPortalRoleName.DATA_DOMAIN_EDITOR;
            case DATA_DOMAIN_VIEWER -> portalRoleName = SystemDefaultPortalRoleName.DATA_DOMAIN_VIEWER;
            default -> portalRoleName = null;
        }
        return portalRoleName;
    }

    private void deleteExistingUserPortalRoles(UserEntity userEntity, Set<UserPortalRoleEntity> userPortalRoleEntities) {
        for (UserPortalRoleEntity userPortalRoleEntity : userPortalRoleEntities) {
            userPortalRoleRepository.deleteById(userPortalRoleEntity.getId());
        }
        userEntity.getPortalRoles().removeAll(userEntity.getPortalRoles());
        userPortalRoleRepository.flush();
        userRepository.flush();
    }
}
