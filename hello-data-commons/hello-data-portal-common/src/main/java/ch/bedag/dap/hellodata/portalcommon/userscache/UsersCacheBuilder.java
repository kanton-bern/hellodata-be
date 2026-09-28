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
package ch.bedag.dap.hellodata.portalcommon.userscache;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DataDomainRoleDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.RoleToDashboardName;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUsersResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.*;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_DASHBOARDS;

/**
 * Builds the content of the {@link UsersCacheNames} caches out of the portal users and the published subsystem resources.
 */
@Service
@RequiredArgsConstructor
public class UsersCacheBuilder {

    private final UserRepository userRepository;
    private final HdContextRepository contextRepository;
    private final MetaInfoResourceService metaInfoResourceService;

    @Transactional(readOnly = true)
    public List<SubsystemUsersResultDto> buildSubsystemUsers() {
        Map<String, PortalUser> emailToUserMap = getPortalUsers(contextRepository.findAll()).stream()
                .collect(Collectors.toMap(PortalUser::email, u -> u));
        List<HdResource> userPacks = metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS);

        List<SubsystemUsersResultDto> result = new ArrayList<>();
        for (HdResource usersPack : userPacks) {
            List<SubsystemUserDto> subsystemUserDtos = mapSubsystemUsers(usersPack, emailToUserMap);
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DashboardUsersResultDto> buildDashboardUsers() {
        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        List<DashboardResource> supersetDashboards = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Map<String, List<RoleToDashboardName>> dashboardRolesMap = mapDashboardRolesToNames(supersetDashboards);
        List<HdContextEntity> contexts = contextRepository.findAll();
        Map<String, String> contextKeyToName = contexts.stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName));
        Set<String> supersetNames = supersetAppInfos.stream().map(AppInfoResource::getInstanceName).collect(Collectors.toSet());
        List<PortalUser> allPortalUsers = getPortalUsers(contexts);

        List<MetaInfoResourceEntity> userPacks = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)
                .stream()
                .filter(uPack -> supersetNames.contains(uPack.getInstanceName()))
                .toList();

        List<DashboardUsersResultDto> result = new ArrayList<>();
        for (MetaInfoResourceEntity userPack : userPacks) {
            List<SubsystemUserDto> subsystemUserDtos = mapDashboardSubsystemUsers(userPack, allPortalUsers, dashboardRolesMap);
            String contextName = contextKeyToName.get(userPack.getContextKey());
            result.add(new DashboardUsersResultDto(contextName, userPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    private List<PortalUser> getPortalUsers(List<HdContextEntity> contexts) {
        Map<String, String> contextKeyToName = contexts.stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName, (a, b) -> a));
        Set<String> businessContextKeys = contexts.stream()
                .filter(context -> context.getType() == HdContextType.BUSINESS_DOMAIN)
                .map(HdContextEntity::getContextKey)
                .collect(Collectors.toSet());
        return userRepository.findAll().stream()
                .map(user -> toPortalUser(user, contextKeyToName, businessContextKeys))
                .toList();
    }

    private PortalUser toPortalUser(UserEntity userEntity, Map<String, String> contextKeyToName, Set<String> businessContextKeys) {
        HdRoleName businessDomainRole = null;
        List<DataDomainRoleDto> dataDomainRoles = new ArrayList<>();
        Set<UserContextRoleEntity> contextRoles = userEntity.getContextRoles() != null ? userEntity.getContextRoles() : Set.of();
        for (UserContextRoleEntity ucr : contextRoles) {
            if (businessContextKeys.contains(ucr.getContextKey())) {
                businessDomainRole = ucr.getRole().getName();
            } else {
                String contextName = contextKeyToName.getOrDefault(ucr.getContextKey(), ucr.getContextKey());
                dataDomainRoles.add(new DataDomainRoleDto(contextName, ucr.getContextKey(), ucr.getRole().getName()));
            }
        }
        return new PortalUser(userEntity.getEmail(), userEntity.getUsername(), userEntity.getFirstName(), userEntity.getLastName(),
                userEntity.isEnabled(), businessDomainRole, dataDomainRoles);
    }

    @SuppressWarnings("unchecked")
    private List<SubsystemUserDto> mapSubsystemUsers(HdResource usersPack, Map<String, PortalUser> emailToUserMap) {
        List<SubsystemUser> subsystemUsers = (List<SubsystemUser>) usersPack.getData();
        List<SubsystemUserDto> dtos = new ArrayList<>(subsystemUsers.size());
        for (SubsystemUser u : subsystemUsers) {
            PortalUser portalUser = emailToUserMap.get(u.getEmail());
            if (portalUser == null) continue;
            dtos.add(new SubsystemUserDto(
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getUsername(),
                    u.getRoles().stream().map(SubsystemRole::getName).toList(),
                    usersPack.getInstanceName(),
                    portalUser.businessDomainRole(),
                    portalUser.dataDomainRoles(),
                    portalUser.enabled()
            ));
        }
        return dtos;
    }

    @SuppressWarnings("unchecked")
    private List<SubsystemUserDto> mapDashboardSubsystemUsers(MetaInfoResourceEntity userInfo, List<PortalUser> allPortalUsers,
                                                              Map<String, List<RoleToDashboardName>> dashboardRolesMap) {
        List<SubsystemUser> subsystemUsers = (List<SubsystemUser>) userInfo.getMetainfo().getData();
        List<SubsystemUserDto> dtos = new ArrayList<>();
        for (PortalUser portalUser : allPortalUsers) {
            SubsystemUser subsystemUser = subsystemUsers.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(portalUser.email()))
                    .findFirst()
                    .orElse(null);
            List<SubsystemRole> roles = subsystemUser != null ? subsystemUser.getRoles() : List.of();
            dtos.add(createDashboardUserDto(userInfo.getInstanceName(), roles, portalUser, dashboardRolesMap));
        }
        return dtos;
    }

    private SubsystemUserDto createDashboardUserDto(String instanceName, List<SubsystemRole> subsystemUserRoles, PortalUser portalUser,
                                                    Map<String, List<RoleToDashboardName>> dashboardRolesMap) {
        List<RoleToDashboardName> roleToDashboardNameList = dashboardRolesMap.getOrDefault(instanceName, List.of());
        List<String> roles = mapRolesForUser(subsystemUserRoles, roleToDashboardNameList);
        return new SubsystemUserDto(
                portalUser.firstName(),
                portalUser.lastName(),
                portalUser.email(),
                portalUser.username(),
                roles,
                instanceName,
                portalUser.businessDomainRole(),
                portalUser.dataDomainRoles(),
                portalUser.enabled()
        );
    }

    private List<String> mapRolesForUser(List<SubsystemRole> subsystemUserRoles, List<RoleToDashboardName> roleToDashboardNameList) {
        List<String> subsystemRoleNames = subsystemUserRoles.stream().map(SubsystemRole::getName).toList();
        boolean isAdmin = subsystemRoleNames.stream()
                .anyMatch(name -> List.of(ADMIN_ROLE_NAME, BI_ADMIN_ROLE_NAME, BI_EDITOR_ROLE_NAME).contains(name));

        Stream<String> rolesStream = isAdmin
                ? Stream.concat(roleToDashboardNameList.stream().map(RoleToDashboardName::roleName), subsystemRoleNames.stream())
                : subsystemRoleNames.stream();

        return rolesStream
                .filter(this::isRelevantRole)
                .sorted()
                .map(r -> mapRoleNameToDashboardName(r, roleToDashboardNameList))
                .toList();
    }

    private boolean isRelevantRole(String role) {
        return role.equalsIgnoreCase(ADMIN_ROLE_NAME) ||
                role.startsWith(DASHBOARD_ROLE_PREFIX) ||
                role.equalsIgnoreCase(BI_ADMIN_ROLE_NAME) ||
                role.equalsIgnoreCase(BI_VIEWER_ROLE_NAME) ||
                role.equalsIgnoreCase(BI_EDITOR_ROLE_NAME);
    }

    private String mapRoleNameToDashboardName(String roleName, List<RoleToDashboardName> roleToDashboardNameList) {
        return roleToDashboardNameList.stream()
                .filter(r -> r.roleName().equalsIgnoreCase(roleName))
                .map(RoleToDashboardName::dashboardName)
                .findFirst()
                .orElse(roleName);
    }

    private Map<String, List<RoleToDashboardName>> mapDashboardRolesToNames(List<DashboardResource> dashboards) {
        return dashboards.stream()
                .collect(Collectors.toMap(
                        DashboardResource::getInstanceName,
                        dr -> dr.getData().stream()
                                .flatMap(dashboard -> dashboard.getRoles().stream()
                                        .filter(role -> role.getName().startsWith(DASHBOARD_ROLE_PREFIX))
                                        .map(role -> new RoleToDashboardName(role.getName(), dashboard.getDashboardTitle()))
                                )
                                .toList()
                ));
    }

    private record PortalUser(String email, String username, String firstName, String lastName, boolean enabled,
                              HdRoleName businessDomainRole, List<DataDomainRoleDto> dataDomainRoles) {
    }
}
