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
package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Privilege;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.Role;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.entities.User;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.RoleRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.repository.UserRepository;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.resource.CbUserResourceProviderService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;
import static ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener.CbUserUtil.generateCbUser;

@Log4j2
@Service
@AllArgsConstructor
public class CbUserContextRoleConsumer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CbUserResourceProviderService userResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public void subscribeToUserContextRoleUpdate(UserContextRoleUpdate userContextRoleUpdate) {
        log.info("[UPDATE_USER_CONTEXT_ROLE] Received user context role update {}", userContextRoleUpdate);
        processContextRoleUpdate(userContextRoleUpdate);
    }

    public void processContextRoleUpdate(UserContextRoleUpdate userContextRoleUpdate) {
        log.info("--> process email: {}", userContextRoleUpdate.getEmail());
        log.info("--> process username: {}", userContextRoleUpdate.getUsername());
        User user = userRepository.findByEmailIgnoreCase(userContextRoleUpdate.getEmail());
        if (user == null) {
            log.info("User {} not found, creating...", userContextRoleUpdate.getUsername());
            User dbtDocUser = toCbUser(userContextRoleUpdate);
            user = userRepository.saveAndFlush(dbtDocUser);
        }
        Set<UserContextRoleUpdate.ContextRole> userDataDomainKeys = getRelevantUserDataDomainContextKeys(userContextRoleUpdate);
        if (!userDataDomainKeys.isEmpty()) {
            List<Role> userRoles = mapUserRoles(userDataDomainKeys);
            user.setRoles(userRoles);
            log.info("Update roles {} for user: {}", userRoles, user.getEmail());
        } else {
            log.info("No roles assigned to user: {}", user.getEmail());
            user.setRoles(new ArrayList<>());
        }
        userRepository.saveAndFlush(user);

        //ToDo: Move this to subscribeToUserContextRoleUpdate method
        if (userContextRoleUpdate.isSendBackUsersList()) {
            userResourceProviderService.publishUsers();
        }
    }

    private Set<UserContextRoleUpdate.ContextRole> getRelevantUserDataDomainContextKeys(UserContextRoleUpdate userContextRoleUpdate) {
        return userContextRoleUpdate.getContextRoles()
                .stream()
                .filter(contextRole -> (contextRole.getRoleName().getContextType() == HdContextType.DATA_DOMAIN ||
                        contextRole.getRoleName().getContextType() == HdContextType.BUSINESS_DOMAIN) &&
                        contextRole.getRoleName() != HdRoleName.NONE)
                .collect(Collectors.toSet());
    }

    private User toCbUser(UserContextRoleUpdate userContextRoleUpdate) {
        return generateCbUser(userContextRoleUpdate.getUsername(),
                userContextRoleUpdate.getEmail(),
                userContextRoleUpdate.getFirstName(),
                userContextRoleUpdate.getLastName());
    }

    private List<Role> mapUserRoles(Set<UserContextRoleUpdate.ContextRole> dataDomainContexts) {
        log.debug("--> mapping context to roles {}", dataDomainContexts);
        List<Role> allCbRoles = roleRepository.findAll();
        return mapRoles(dataDomainContexts, allCbRoles);
    }

    static List<Role> mapRoles(Set<UserContextRoleUpdate.ContextRole> dataDomainContexts, List<Role> allCbRoles) {
        Set<Role> newDistinctUserRoles = new HashSet<>();
        for (Role role : allCbRoles) {
            dataDomainContexts.forEach(dataDomainContext -> {
                if (dataDomainContext.getRoleName() == HdRoleName.HELLODATA_ADMIN && role.getKey().equalsIgnoreCase(Role.ADMIN_ROLE_KEY)) {
                    newDistinctUserRoles.add(role);
                    return;
                }

                String contextKeyUppercase = dataDomainContext.getContextKey().toUpperCase(Locale.ROOT);

                if (dataDomainContext.getRoleName() == HdRoleName.DATA_DOMAIN_ADMIN &&
                        role.getKey().equalsIgnoreCase(contextKeyUppercase + "_" + Privilege.READ_DWH_PRIVILEGE)) {
                    newDistinctUserRoles.add(role);
                    return;
                }

                List<HdRoleName> dmViewRoleNames = List.of(HdRoleName.DATA_DOMAIN_ADMIN, HdRoleName.DATA_DOMAIN_EDITOR, HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST);
                if (dmViewRoleNames.contains(dataDomainContext.getRoleName()) &&
                        role.getKey().equalsIgnoreCase(contextKeyUppercase + "_" + Privilege.READ_DM_PRIVILEGE)) {
                    newDistinctUserRoles.add(role);
                }
            });
        }
        return new ArrayList<>(newDistinctUserRoles);
    }

}
