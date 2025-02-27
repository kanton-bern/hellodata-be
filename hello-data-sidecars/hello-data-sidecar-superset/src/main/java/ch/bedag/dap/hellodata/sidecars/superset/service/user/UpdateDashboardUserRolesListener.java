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

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.SupersetDashboardsForUserUpdate;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.IdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class UpdateDashboardUserRolesListener {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final SupersetClientProvider supersetClientProvider;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @PostConstruct
    public void listenForRequests() {
        String supersetSidecarSubject = SlugifyUtil.slugify(instanceName + RequestReplySubject.UPDATE_DASHBOARD_ROLES_FOR_USER.getSubject());
        log.info("/*-/*- Listening for messages on subject {}", supersetSidecarSubject);
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            try {
                SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
                SupersetDashboardsForUserUpdate supersetDashboardsForUserUpdate = objectMapper.readValue(msg.getData(), SupersetDashboardsForUserUpdate.class);
                log.info("-=-=-= RECEIVED DASHBOARD ROLES UPDATE {}", supersetDashboardsForUserUpdate);
                SubsystemUser user = getSupersetUser(supersetClient, supersetDashboardsForUserUpdate);
                SupersetRolesResponse allRoles = supersetClient.roles();

                SupersetUserRolesUpdate supersetUserRolesUpdate = new SupersetUserRolesUpdate();
                supersetUserRolesUpdate.setRoles(user.getRoles().stream().map(SubsystemRole::getId).toList());
                removeAllDashboardRoles(allRoles, supersetUserRolesUpdate);
                List<DashboardForUserDto> dashboards = supersetDashboardsForUserUpdate.getDashboards();
                SupersetDashboardResponse dashboardsFromSuperset = supersetClient.dashboards();
                for (DashboardForUserDto dashboardForUserDto : dashboards) {
                    SupersetDashboard dashboard = dashboardsFromSuperset.getResult()
                            .stream()
                            .filter(supersetDashboard -> supersetDashboard.getId() == dashboardForUserDto.getId())
                            .findFirst()
                            .orElse(null);
                    assignDashboardRoleToUser(user, dashboard, allRoles, supersetUserRolesUpdate);
                }

                RoleUtil.leaveOnlyBiViewerRoleIfNoneAttached(allRoles, supersetUserRolesUpdate);

                log.debug("Roles that user {} should now have: {}", user.getEmail(), supersetUserRolesUpdate.getRoles());
                SupersetUserUpdateResponse updatedUser = supersetClient.updateUserRoles(supersetUserRolesUpdate, user.getId());
                log.debug("\t-=-=-=-= received message from the superset: {}", new String(msg.getData()));
                natsConnection.publish(msg.getReplyTo(), objectMapper.writeValueAsBytes(updatedUser));
                msg.ack();
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        dispatcher.subscribe(supersetSidecarSubject);
    }

    private SubsystemUser getSupersetUser(SupersetClient supersetClient, SupersetDashboardsForUserUpdate supersetDashboardsForUserUpdate) throws URISyntaxException, IOException {
        SupersetUsersResponse response = supersetClient.getUser(supersetDashboardsForUserUpdate.getSupersetUserName(), supersetDashboardsForUserUpdate.getSupersetUserEmail());
        if (response == null || response.getResult().isEmpty()) {
            log.warn("[Couldn't find user by email: {} and username: {}, creating...]", supersetDashboardsForUserUpdate.getSupersetUserEmail(), supersetDashboardsForUserUpdate.getSupersetUserName());
            SupersetRolesResponse allRoles = supersetClient.roles();
            List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(PUBLIC_ROLE_NAME)).map(SubsystemRole::getId).toList();
            SubsystemUserUpdate subsystemUserUpdate = getSubsystemUserUpdate(supersetDashboardsForUserUpdate, roles);
            IdResponse createdUser = supersetClient.createUser(subsystemUserUpdate);
            return supersetClient.user(createdUser.id).getResult();
        }
        return response.getResult().get(0);
    }

    private SubsystemUserUpdate getSubsystemUserUpdate(SupersetDashboardsForUserUpdate supersetDashboardsForUserUpdate, List<Integer> roles) {
        SubsystemUserUpdate subsystemUserUpdate = new SubsystemUserUpdate();
        subsystemUserUpdate.setUsername(supersetDashboardsForUserUpdate.getSupersetUserName());
        subsystemUserUpdate.setEmail(supersetDashboardsForUserUpdate.getSupersetUserEmail());
        subsystemUserUpdate.setActive(supersetDashboardsForUserUpdate.getActive());
        subsystemUserUpdate.setFirstName(supersetDashboardsForUserUpdate.getSupersetFirstName());
        subsystemUserUpdate.setLastName(supersetDashboardsForUserUpdate.getSupersetLastName());
        subsystemUserUpdate.setPassword(supersetDashboardsForUserUpdate.getSupersetFirstName());
        subsystemUserUpdate.setRoles(roles);
        return subsystemUserUpdate;
    }

    private void assignDashboardRoleToUser(SubsystemUser user, @Nullable SupersetDashboard dashboard, SupersetRolesResponse allRoles,
                                           SupersetUserRolesUpdate supersetUserRolesUpdate) {
        if (dashboard == null) {
            return;
        }
        log.debug("\tAssigning dashboard roles {} to user {}", dashboard.getRoles(), user.getEmail());
        List<Integer> rolesFromDashboard = CollectionUtils.emptyIfNull(dashboard.getRoles()).stream().map(SubsystemRole::getId).toList();
        List<Integer> userRoles = supersetUserRolesUpdate.getRoles();
        List<Integer> userRolesPlusDashboardRoles = Stream.concat(rolesFromDashboard.stream(), userRoles.stream()).distinct().toList();
        supersetUserRolesUpdate.setRoles(userRolesPlusDashboardRoles);
        log.debug("\tsuperset update request roles: {}", supersetUserRolesUpdate.getRoles());
        removePublicRoleIfAdded(allRoles, supersetUserRolesUpdate);
    }
}
