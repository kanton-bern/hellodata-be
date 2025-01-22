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
package ch.bedag.dap.hellodata.sidecars.superset.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.UserResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserActiveUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.CREATE_USER;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.ENABLE_USER;
import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.PUBLIC_ROLE_NAME;

@Log4j2
@Service
@SuppressWarnings("java:S3516")
@RequiredArgsConstructor
public class SupersetCreateUserConsumer {

    private final UserResourceProviderService userResourceProviderService;
    private final SupersetClientProvider supersetClientProvider;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = CREATE_USER, asyncRun = false)
    public void createUser(SubsystemUserUpdate supersetUserCreate) {
        try {
            log.info("------- Received superset user creation request {}", supersetUserCreate);
            SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
            Optional<Integer> aPublicRoleId = supersetClient.roles()
                    .getResult()
                    .stream()
                    .filter(supersetRole -> supersetRole.getName().equalsIgnoreCase(PUBLIC_ROLE_NAME))
                    .map(SubsystemRole::getId)
                    .findFirst();
            SupersetUsersResponse response = supersetClient.getUser(supersetUserCreate.getUsername(), supersetUserCreate.getEmail());
            if (response != null && !response.getResult().isEmpty()) {
                SubsystemUser user = response.getResult().get(0);
                log.debug("User {} already exists in instance, omitting creation", supersetUserCreate.getEmail());
                enableUser(supersetUserCreate, user, supersetClient, aPublicRoleId);
            } else {
                createUser(supersetUserCreate, aPublicRoleId, supersetClient);
            }
            if (supersetUserCreate.isSendBackUsersList()) {
                userResourceProviderService.publishUsers();
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Could not create/enable user {}", supersetUserCreate.getEmail(), e);
        }
    }

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = ENABLE_USER, asyncRun = false)
    public void enableUser(SubsystemUserUpdate subsystemUserUpdate) {
        try {
            log.info("------- Received superset user enable request {}", subsystemUserUpdate);
            SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
            SupersetUsersResponse response = supersetClient.getUser(subsystemUserUpdate.getUsername(), subsystemUserUpdate.getEmail());
            if (response != null && response.getResult().size() > 0) {
                SubsystemUser user = response.getResult().get(0);
                enableUser(subsystemUserUpdate, user, supersetClient);
            } else {
                log.warn("Couldn't find user {}", subsystemUserUpdate.getEmail());
            }
            if (subsystemUserUpdate.isSendBackUsersList()) {
                userResourceProviderService.publishUsers();
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Could not enable user {}", subsystemUserUpdate.getEmail(), e);
        }
    }

    private void createUser(SubsystemUserUpdate supersetUserCreate, Optional<Integer> aPublicRoleId, SupersetClient supersetClient) throws URISyntaxException, IOException {
        aPublicRoleId.ifPresentOrElse(roleId -> supersetUserCreate.setRoles(List.of(roleId)),
                () -> log.warn("Couldn't find a Public role to set for created/enabled user {}", supersetUserCreate.getEmail()));
        // logging with keycloak changes the password in the superset DB
        supersetUserCreate.setPassword(supersetUserCreate.getFirstName());
        supersetClient.createUser(supersetUserCreate);
    }

    private void enableUser(SubsystemUserUpdate supersetUserCreate, SubsystemUser supersetUser, SupersetClient supersetClient, Optional<Integer> aPublicRoleId) throws
            URISyntaxException, IOException {
        if (!supersetUser.isActive()) {
            int supersetUserId = enableUser(supersetUserCreate, supersetUser, supersetClient);
            if (aPublicRoleId.isPresent()) {
                int roleId = aPublicRoleId.get();
                SupersetUserRolesUpdate supersetUserRolesUpdate = new SupersetUserRolesUpdate();
                supersetUserRolesUpdate.setRoles(List.of(roleId));
                supersetClient.updateUserRoles(supersetUserRolesUpdate, supersetUserId);
            } else {
                log.warn("Couldn't find a Public role to set for created/enabled user {}", supersetUserCreate.getEmail());
            }
        }
    }

    private int enableUser(SubsystemUserUpdate supersetUserCreate, SubsystemUser supersetUser, SupersetClient supersetClient) throws URISyntaxException, IOException {
        log.info("User {} has been activated", supersetUserCreate.getEmail());
        SupersetUserActiveUpdate supersetUserActiveUpdate = new SupersetUserActiveUpdate();
        supersetUserActiveUpdate.setActive(true);
        int supersetUserId = supersetUser.getId();
        supersetClient.updateUsersActiveFlag(supersetUserActiveUpdate, supersetUserId);
        return supersetUserId;
    }
}
