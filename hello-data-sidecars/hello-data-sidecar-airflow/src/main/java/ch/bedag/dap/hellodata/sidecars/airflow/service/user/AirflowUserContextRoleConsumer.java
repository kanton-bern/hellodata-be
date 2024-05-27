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
package ch.bedag.dap.hellodata.sidecars.airflow.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUser;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserResponse;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRole;
import ch.bedag.dap.hellodata.sidecars.airflow.client.user.response.AirflowUserRolesUpdate;
import ch.bedag.dap.hellodata.sidecars.airflow.service.provider.AirflowClientProvider;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowRoleResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.airflow.service.resource.AirflowUserResourceProviderService;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;

@Log4j2
@Service
@AllArgsConstructor
public class AirflowUserContextRoleConsumer {
    private static final String DATA_DOMAIN_ROLE_PREFIX = "DD_";
    private static final String PUBLIC_ROLE_NAME = "Public";
    private static final String VIEWER_ROLE_NAME = "Viewer";
    private static final String ADMIN_ROLE_NAME = "Admin";
    private static final String AF_OPERATOR_ROLE_NAME = "AF_OPERATOR";
    private final AirflowClientProvider airflowClientProvider;
    private final AirflowRoleResourceProviderService airflowRoleResourceProviderService;
    private final AirflowUserResourceProviderService airflowUserResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public CompletableFuture<Void> subscribe(UserContextRoleUpdate userContextRoleUpdate) {
        log.info("Update user context roles {}", userContextRoleUpdate);
        AirflowClient airflowClient = airflowClientProvider.getAirflowClientInstance();
        try {
            List<AirflowRole> allAirflowRoles = CollectionUtils.emptyIfNull(airflowClient.roles().getRoles()).stream().toList();
            List<UserContextRoleUpdate.ContextRole> dataDomainContextRoles =
                    userContextRoleUpdate.getContextRoles().stream().filter(contextRole -> contextRole.getRoleName().getContextType() == HdContextType.DATA_DOMAIN).toList();
            AirflowUserResponse airflowUser =
                    airflowClient.users().getUsers().stream().filter(user -> userContextRoleUpdate.getEmail().equalsIgnoreCase(user.getEmail())).findFirst().orElse(null);
            if (!dataDomainContextRoles.isEmpty()) {
                allAirflowRoles = createContextRolesIfNotExist(dataDomainContextRoles, allAirflowRoles, airflowClient);
                if (airflowUser != null) {
                    if (airflowUser.getRoles() == null) {
                        airflowUser.setRoles(new ArrayList<>());
                    }
                    addOrRemoveAdminRole(userContextRoleUpdate, allAirflowRoles, airflowUser);
                    updateBusinessContextRoleForUser(airflowUser, dataDomainContextRoles, allAirflowRoles, airflowClient);
                } else {
                    log.warn("User {} not found in airflow", userContextRoleUpdate.getEmail());
                }
            } else {
                removeAllDataDomainRolesFromUser(airflowUser);
                leavePublicRoleIfNoneOthersSet(airflowUser, allAirflowRoles);
                updateUser(airflowUser, airflowClient);
            }
        } catch (URISyntaxException | IOException e) {
            log.error("Could not update user {}", userContextRoleUpdate.getEmail(), e);
        }

        return null;
    }

    private void addOrRemoveAdminRole(UserContextRoleUpdate userContextRoleUpdate, List<AirflowRole> allAirflowRoles, AirflowUserResponse airflowUser) {
        if (userContextRoleUpdate.getContextRoles().stream().anyMatch(contextRole -> contextRole.getRoleName() == HdRoleName.HELLODATA_ADMIN)) {
            addRoleToUser(airflowUser, ADMIN_ROLE_NAME, allAirflowRoles);
        } else {
            removeRoleFromUser(airflowUser, ADMIN_ROLE_NAME, allAirflowRoles);
        }
    }

    private List<AirflowRole> createContextRolesIfNotExist(List<UserContextRoleUpdate.ContextRole> dataDomainContextRoles, List<AirflowRole> roles,
                                                           AirflowClient airflowClient) throws URISyntaxException, IOException {
        for (UserContextRoleUpdate.ContextRole contextRole : dataDomainContextRoles) {
            Optional<AirflowRole> airflowRoleResult =
                    roles.stream().filter(airflowRole -> airflowRole.getName().equalsIgnoreCase(DATA_DOMAIN_ROLE_PREFIX + contextRole.getContextKey())).findFirst();
            if (airflowRoleResult.isEmpty()) {
                AirflowRole role = new AirflowRole();
                role.setName(DATA_DOMAIN_ROLE_PREFIX + contextRole.getContextKey());
                airflowClient.createRole(role);
            }
        }
        airflowRoleResourceProviderService.publishRoles();
        //fetch roles again
        return airflowClient.roles().getRoles();
    }

    private void updateBusinessContextRoleForUser(AirflowUserResponse airflowUser, List<UserContextRoleUpdate.ContextRole> dataDomainContextRoles,
                                                  List<AirflowRole> allAirflowRoles, AirflowClient airflowClient) throws IOException, URISyntaxException {
        removeAllDataDomainRolesFromUser(airflowUser);
        for (UserContextRoleUpdate.ContextRole contextRole : dataDomainContextRoles) {
            String dataDomainRole = DATA_DOMAIN_ROLE_PREFIX + contextRole.getContextKey();
            if (contextRole.getRoleName() == HdRoleName.DATA_DOMAIN_ADMIN) {
                addRoleToUser(airflowUser, AF_OPERATOR_ROLE_NAME, allAirflowRoles);
                addRoleToUser(airflowUser, dataDomainRole, allAirflowRoles);
                removeRoleFromUser(airflowUser, PUBLIC_ROLE_NAME, allAirflowRoles);
            }
        }
        removeRoleFromUser(airflowUser, VIEWER_ROLE_NAME, allAirflowRoles);
        leavePublicRoleIfNoneOthersSet(airflowUser, allAirflowRoles);
        updateUser(airflowUser, airflowClient);
    }

    private void leavePublicRoleIfNoneOthersSet(AirflowUserResponse airflowUser, List<AirflowRole> allAirflowRoles) {
        if (airflowUser == null) {
            return;
        }
        if (airflowUser.getRoles().isEmpty()) {
            addRoleToUser(airflowUser, PUBLIC_ROLE_NAME, allAirflowRoles);
        }
    }

    private void updateUser(AirflowUserResponse airflowUser, AirflowClient airflowClient) throws IOException, URISyntaxException {
        if (airflowUser == null) {
            return;
        }
        AirflowUserRolesUpdate airflowUserRolesUpdate = new AirflowUserRolesUpdate();
        airflowUserRolesUpdate.setRoles(airflowUser.getRoles());
        airflowUserRolesUpdate.setEmail(airflowUser.getEmail());
        airflowUserRolesUpdate.setUsername(airflowUser.getUsername());
        airflowUserRolesUpdate.setPassword(airflowUser.getPassword() == null ? "" : airflowUser.getPassword());
        airflowUserRolesUpdate.setFirstName(airflowUser.getFirstName());
        airflowUserRolesUpdate.setLastName(airflowUser.getLastName());
        airflowClient.updateUser(airflowUserRolesUpdate, airflowUser.getUsername());
        airflowUserResourceProviderService.publishUsers();
    }

    private void removeAllDataDomainRolesFromUser(AirflowUser airflowUser) {
        if (airflowUser == null) {
            return;
        }
        List<AirflowUserRole> airflowUserRoles = CollectionUtils.emptyIfNull(airflowUser.getRoles())
                                                                .stream()
                                                                .filter(airflowRole -> !airflowRole.getName().startsWith(DATA_DOMAIN_ROLE_PREFIX))
                                                                .filter(airflowRole -> !airflowRole.getName().equalsIgnoreCase(AF_OPERATOR_ROLE_NAME))
                                                                .toList();
        airflowUser.setRoles(new ArrayList<>(airflowUserRoles));
    }

    private void addRoleToUser(AirflowUserResponse airflowUser, String name, List<AirflowRole> allAirflowRoles) {
        Optional<AirflowUserRole> roleResult = CollectionUtils.emptyIfNull(allAirflowRoles)
                                                              .stream()
                                                              .filter(airflowRole -> airflowRole.getName().equalsIgnoreCase(name))
                                                              .map(airflowRole -> new AirflowUserRole(airflowRole.getName()))
                                                              .findFirst();
        if (roleResult.isPresent()) {
            AirflowUserRole airflowUserRoleToBeAdded = roleResult.get();
            if (!CollectionUtils.emptyIfNull(airflowUser.getRoles()).contains(airflowUserRoleToBeAdded)) {
                airflowUser.getRoles().add(airflowUserRoleToBeAdded);
            }
        }
    }

    private void removeRoleFromUser(AirflowUserResponse airflowUser, String name, List<AirflowRole> allAirflowRoles) {
        Optional<AirflowUserRole> roleResult = CollectionUtils.emptyIfNull(allAirflowRoles)
                                                              .stream()
                                                              .filter(airflowUserRole -> airflowUserRole.getName().equalsIgnoreCase(name))
                                                              .map(airflowRole -> new AirflowUserRole(airflowRole.getName()))
                                                              .findFirst();
        if (roleResult.isPresent()) {
            AirflowUserRole airflowUserRoleToBeRemoved = roleResult.get();
            if (CollectionUtils.emptyIfNull(airflowUser.getRoles()).contains(airflowUserRoleToBeRemoved)) {
                airflowUser.getRoles().remove(airflowUserRoleToBeRemoved);
            }
        }
    }
}
