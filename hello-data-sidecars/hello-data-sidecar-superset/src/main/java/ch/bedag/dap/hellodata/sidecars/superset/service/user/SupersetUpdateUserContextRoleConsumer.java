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
import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboardResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRolesResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.*;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.sidecars.superset.client.SupersetClient;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.IdResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUserUpdateResponse;
import ch.bedag.dap.hellodata.sidecars.superset.client.data.SupersetUsersResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.resource.UserResourceProviderService;
import ch.bedag.dap.hellodata.sidecars.superset.service.user.data.SupersetUserRolesUpdate;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;
import static ch.bedag.dap.hellodata.sidecars.superset.service.user.RoleUtil.*;

@Log4j2
@Service
@AllArgsConstructor
public class SupersetUpdateUserContextRoleConsumer {

    private static final String ADMIN_ROLE_NAME = "Admin";
    private static final String SQL_LAB_ROLE_NAME = "sql_lab";

    private final SupersetClientProvider supersetClientProvider;
    private final HelloDataContextConfig helloDataContextConfig;
    private final UserResourceProviderService userResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public void subscribe(UserContextRoleUpdate userContextRoleUpdate) throws URISyntaxException, IOException {
        log.info("-=-=-=-= RECEIVED USER CONTEXT ROLES UPDATE: payload: {}", userContextRoleUpdate);
        SupersetClient supersetClient = supersetClientProvider.getSupersetClientInstance();
        SupersetRolesResponse allRoles = supersetClient.roles();
        updateUserRoles(userContextRoleUpdate, supersetClient, allRoles);
    }

    public void updateUserRoles(UserContextRoleUpdate userContextRoleUpdate, SupersetClient supersetClient, SupersetRolesResponse allRoles) throws URISyntaxException, IOException {
        String dataDomainKey = helloDataContextConfig.getContext().getKey();
        SubsystemUser supersetUser = getSupersetUser(userContextRoleUpdate, supersetClient, allRoles);
        Optional<UserContextRoleUpdate.ContextRole> dataDomainContextRole =
                userContextRoleUpdate.getContextRoles().stream().filter(contextRole -> contextRole.getContextKey().equalsIgnoreCase(dataDomainKey)).findFirst();
        if (dataDomainContextRole.isPresent()) {
            UserContextRoleUpdate.ContextRole contextRole = dataDomainContextRole.get();
            SupersetUserRolesUpdate supersetUserRolesUpdate = new SupersetUserRolesUpdate();

            Map<String, List<ModuleRoleNames>> extraModuleRoles = MapUtils.emptyIfNull(userContextRoleUpdate.getExtraModuleRoles());
            List<ModuleRoleNames> moduleRoleNamesList = new ArrayList<>(CollectionUtils.emptyIfNull(extraModuleRoles.get(dataDomainKey)));
            Optional<ModuleRoleNames> moduleExtraRoles = moduleRoleNamesList.stream().filter(moduleRoleNames -> moduleRoleNames.moduleType() == ModuleType.SUPERSET).findFirst();
            if (moduleExtraRoles.isPresent()) {
                // reset all roles if extra roles present (here roles come from the csv file)
                supersetUserRolesUpdate.setRoles(new ArrayList<>());
                moduleExtraRoles.get().roleNames().forEach(roleName -> assignRoleToUser(roleName, allRoles, supersetUserRolesUpdate));
            } else {
                supersetUserRolesUpdate.setRoles(supersetUser.getRoles().stream().map(SubsystemRole::getId).toList());
                removeBiRoles(allRoles, supersetUserRolesUpdate);
            }

            assignAdminRoleIfSuperuser(userContextRoleUpdate, allRoles, supersetUserRolesUpdate);
            removeAllDashboardRoles(allRoles, supersetUserRolesUpdate);
            switch (contextRole.getRoleName()) {
                case DATA_DOMAIN_ADMIN -> {
                    assignRoleToUser(SlugifyUtil.BI_ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                case DATA_DOMAIN_EDITOR -> {
                    assignRoleToUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                    assignRoleToUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                case DATA_DOMAIN_VIEWER, DATA_DOMAIN_BUSINESS_SPECIALIST -> {
                    SupersetDashboardResponse dashboardsFromSuperset = supersetClient.dashboards();
                    List<SupersetDashboard> allDashboards = dashboardsFromSuperset.getResult();
                    assignDashboardSpecificRoles(userContextRoleUpdate, allDashboards, allRoles, dataDomainKey, contextRole, supersetUser, supersetUserRolesUpdate);
                    assignRoleToUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);
                }
                case NONE -> assignRoleToUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);

                default -> log.debug("Irrelevant role name? {}", contextRole.getRoleName());
            }


            removePublicRoleIfAdded(allRoles, supersetUserRolesUpdate);
            List<SubsystemRole> usersRoles = allRoles.getResult().stream().filter(role -> supersetUserRolesUpdate.getRoles().contains(role.getId())).toList();
            log.debug("\tRoles that user {} should now have: {}", userContextRoleUpdate.getEmail(), usersRoles);
            SupersetUserUpdateResponse updatedUser = supersetClient.updateUserRoles(supersetUserRolesUpdate, supersetUser.getId());
            usersRoles = allRoles.getResult().stream().filter(role -> updatedUser.getResult().getRoles().contains(role.getId())).toList();
            log.debug("\t-=-=-=-= UPDATED USER ROLES: user: {}, roles: {}", userContextRoleUpdate.getEmail(), usersRoles);
            if (userContextRoleUpdate.isSendBackUsersList()) {
                userResourceProviderService.publishUsers();
            }
        }
    }

    // Assign dashboard-specific roles (only for VIEWER/BUSINESS_SPECIALIST — admins/editors already had removeAllDashboardRoles called)
    private void assignDashboardSpecificRoles(UserContextRoleUpdate userContextRoleUpdate, List<SupersetDashboard> dashboardsFromSuperset,
                                              SupersetRolesResponse allRoles, String dataDomainKey, UserContextRoleUpdate.ContextRole contextRole,
                                              SubsystemUser supersetUser, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        Map<String, List<DashboardForUserDto>> dashboards = userContextRoleUpdate.getDashboardsPerContext();
        if (shouldAssignDashboardRoles(dashboards, dataDomainKey, contextRole)) {
            List<DashboardForUserDto> contextDashboards = dashboards.get(dataDomainKey);
            assignViewerDashboardRoles(contextDashboards, dashboardsFromSuperset, allRoles, supersetUser, supersetUserRolesUpdate, userContextRoleUpdate.getEmail());
        }

        leaveOnlyBiViewerRoleIfNoneAttached(allRoles, supersetUserRolesUpdate);
    }

    private boolean shouldAssignDashboardRoles(Map<String, List<DashboardForUserDto>> dashboards, String dataDomainKey, UserContextRoleUpdate.ContextRole contextRole) {
        if (dashboards == null || !dashboards.containsKey(dataDomainKey)) {
            return false;
        }
        HdRoleName roleName = contextRole.getRoleName();
        return roleName == HdRoleName.DATA_DOMAIN_VIEWER || roleName == HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST;
    }

    private void assignViewerDashboardRoles(List<DashboardForUserDto> contextDashboards, List<SupersetDashboard> dashboardsFromSuperset,
                                            SupersetRolesResponse allRoles, SubsystemUser supersetUser,
                                            SupersetUserRolesUpdate supersetUserRolesUpdate, String userEmail) {
        contextDashboards.stream()
                .filter(DashboardForUserDto::isViewer)
                .forEach(dto -> {
                    SupersetDashboard dashboard = findDashboardById(dashboardsFromSuperset, dto.getId());
                    String dashboardName = dashboard != null ? dashboard.getDashboardTitle() : String.valueOf(dto.getId());
                    log.info("\tAssigning dashboard-specific role for dashboard {} to user {}", dashboardName, userEmail);
                    assignDashboardRoleToUser(supersetUser, dashboard, allRoles, supersetUserRolesUpdate);
                });
    }

    private SupersetDashboard findDashboardById(List<SupersetDashboard> dashboards, int dashboardId) {
        return dashboards.stream()
                .filter(sd -> sd.getId() == dashboardId)
                .findFirst()
                .orElse(null);
    }

    private SubsystemUser getSupersetUser(UserContextRoleUpdate userContextRoleUpdate, SupersetClient supersetClient, SupersetRolesResponse allRoles) throws URISyntaxException, IOException {
        SupersetUsersResponse response = supersetClient.getUser(userContextRoleUpdate.getUsername(), userContextRoleUpdate.getEmail());
        if (response == null || response.getResult().isEmpty()) {
            log.warn("[Couldn't find user by email: {} and username: {}, creating...]", userContextRoleUpdate.getEmail(), userContextRoleUpdate.getUsername());
            SubsystemUserUpdate subsystemUserUpdate = new SubsystemUserUpdate();
            subsystemUserUpdate.setUsername(userContextRoleUpdate.getUsername());
            subsystemUserUpdate.setEmail(userContextRoleUpdate.getEmail());
            subsystemUserUpdate.setActive(userContextRoleUpdate.getActive());
            subsystemUserUpdate.setFirstName(userContextRoleUpdate.getFirstName());
            subsystemUserUpdate.setLastName(userContextRoleUpdate.getLastName());
            subsystemUserUpdate.setPassword(userContextRoleUpdate.getFirstName());

            List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(PUBLIC_ROLE_NAME)).map(SubsystemRole::getId).toList();
            subsystemUserUpdate.setRoles(roles);
            IdResponse createdUser = supersetClient.createUser(subsystemUserUpdate);
            return supersetClient.user(createdUser.id).getResult();
        }
        return response.getResult().get(0);
    }

    private void removeBiRoles(SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        removeRoleFromUser(SlugifyUtil.BI_ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SlugifyUtil.BI_VIEWER_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SlugifyUtil.BI_EDITOR_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        removeRoleFromUser(SQL_LAB_ROLE_NAME, allRoles, supersetUserRolesUpdate);
    }

    private void assignAdminRoleIfSuperuser(UserContextRoleUpdate userContextRoleUpdate, SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        if (userContextRoleUpdate.getContextRoles().stream().anyMatch(ctxRole -> ctxRole.getRoleName() == HdRoleName.HELLODATA_ADMIN)) {
            assignRoleToUser(ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        } else {
            removeRoleFromUser(ADMIN_ROLE_NAME, allRoles, supersetUserRolesUpdate);
        }
    }

    private void assignRoleToUser(String roleName, SupersetRolesResponse allRoles, SupersetUserRolesUpdate supersetUserRolesUpdate) {
        List<Integer> roles = allRoles.getResult().stream().filter(role -> role.getName().equalsIgnoreCase(roleName)).map(SubsystemRole::getId).toList();
        List<Integer> userRoles = supersetUserRolesUpdate.getRoles();
        supersetUserRolesUpdate.setRoles(Stream.concat(roles.stream(), userRoles.stream()).distinct().toList());
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
