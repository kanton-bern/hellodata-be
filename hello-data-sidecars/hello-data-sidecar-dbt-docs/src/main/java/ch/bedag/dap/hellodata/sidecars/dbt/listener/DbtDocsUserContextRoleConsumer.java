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
package ch.bedag.dap.hellodata.sidecars.dbt.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.dbt.entities.Role;
import ch.bedag.dap.hellodata.sidecars.dbt.entities.User;
import ch.bedag.dap.hellodata.sidecars.dbt.repository.RoleRepository;
import ch.bedag.dap.hellodata.sidecars.dbt.repository.UserRepository;
import ch.bedag.dap.hellodata.sidecars.dbt.service.resource.DbtDocsUserResourceProviderService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;

@Log4j2
@Service
@AllArgsConstructor
public class DbtDocsUserContextRoleConsumer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DbtDocsUserResourceProviderService userResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public void processContextRoleUpdate(UserContextRoleUpdate userContextRoleUpdate) {
        User user = userRepository.findByUserNameOrEmail(userContextRoleUpdate.getUsername(), userContextRoleUpdate.getEmail());
        if (user == null) {
            log.info("User {} not found, creating", userContextRoleUpdate.getUsername());
            User dbtDocUser = toDbtDocUser(userContextRoleUpdate);
            user = userRepository.saveAndFlush(dbtDocUser);
        }
        Set<String> userDataDomainKeys = getRelevantUserDataDomainContextKeys(userContextRoleUpdate);
        if (!userDataDomainKeys.isEmpty()) {
            log.info("Update roles for user: {}", user.getEmail());
            List<Role> userRoles = getUserRoles(user, userDataDomainKeys);
            user.setRoles(userRoles);
        } else {
            log.info("No roles assigned to user: {}", user.getEmail());
            user.setRoles(new ArrayList<>());
        }
        userRepository.saveAndFlush(user);
        if (userContextRoleUpdate.isSendBackUsersList()) {
            userResourceProviderService.publishUsers();
        }
    }

    private User toDbtDocUser(UserContextRoleUpdate userContextRoleUpdate) {
        User dbtDocUser = new User(userContextRoleUpdate.getUsername(), userContextRoleUpdate.getEmail());
        dbtDocUser.setRoles(new ArrayList<>());
        dbtDocUser.setFirstName(userContextRoleUpdate.getFirstName());
        dbtDocUser.setLastName(userContextRoleUpdate.getLastName());
        dbtDocUser.setEnabled(true);
        dbtDocUser.setSuperuser(false);
        return dbtDocUser;
    }

    private Set<String> getRelevantUserDataDomainContextKeys(UserContextRoleUpdate userContextRoleUpdate) {
        return userContextRoleUpdate.getContextRoles()
                .stream()
                .filter(contextRole -> contextRole.getRoleName().getContextType() == HdContextType.DATA_DOMAIN && contextRole.getRoleName() != HdRoleName.NONE)
                .map(UserContextRoleUpdate.ContextRole::getContextKey)
                .collect(Collectors.toSet());
    }

    private List<Role> getUserRoles(User user, Set<String> dataDomainContextKeys) {
        List<Role> allDocRoles = roleRepository.findAll();
        List<Role> newUserRoles = new ArrayList<>();
        dataDomainContextKeys.forEach(contextKey -> allDocRoles.stream().filter(r -> r.getKey().equalsIgnoreCase(contextKey)).findFirst().ifPresent(newUserRoles::add));
        if (user.isSuperuser()) {
            allDocRoles.stream().filter(r -> r.getKey().equals(Role.ADMIN_ROLE_KEY)).findFirst().ifPresent(newUserRoles::add);
        }
        return newUserRoles;
    }
}
