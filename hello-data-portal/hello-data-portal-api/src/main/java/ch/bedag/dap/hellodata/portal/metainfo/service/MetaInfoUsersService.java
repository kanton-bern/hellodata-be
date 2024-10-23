package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repositories.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.response.SupersetRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.RoleToDashboardName;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUsersResultDto;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.bedag.dap.hellodata.commons.SlugifyUtil.*;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO;
import static ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_DASHBOARDS;

@Log4j2
@Service
@AllArgsConstructor
public class MetaInfoUsersService {
    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final HdContextRepository contextRepository;

    private static @NotNull Map<String, List<RoleToDashboardName>> mapDashboardRoleWithDashboardNamePerInstance(List<DashboardResource> supersetDashboards) {
        // for performance - collect mapping between a 'D_*' role and a dashboard name in each instance
        Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName = new HashMap<>();
        Map<String, List<SupersetDashboard>> instanceNamesToSupersetDashboards = supersetDashboards.stream()
                .collect(Collectors.toMap(DashboardResource::getInstanceName, DashboardResource::getData));
        for (Map.Entry<String, List<SupersetDashboard>> entry : instanceNamesToSupersetDashboards.entrySet()) {
            String instanceName = entry.getKey();
            List<RoleToDashboardName> roleToDashboardNameList = new LinkedList<>();
            List<SupersetDashboard> supersetDashboardList = entry.getValue();
            for (SupersetDashboard supersetDashboard : supersetDashboardList) {
                Optional<SupersetRole> dashboardNameRole = supersetDashboard.getRoles().stream().filter(role -> role.getName().startsWith(DASHBOARD_ROLE_PREFIX)).findFirst();
                if (dashboardNameRole.isPresent()) {
                    roleToDashboardNameList.add(new RoleToDashboardName(dashboardNameRole.get().getName(), supersetDashboard.getDashboardTitle()));
                }
            }
            roleNameToDashboardNamesPerInstanceName.put(instanceName, roleToDashboardNameList);
        }
        return roleNameToDashboardNamesPerInstanceName;
    }

    @Transactional(readOnly = true)
    public List<SubsystemUsersResultDto> getAllUsersWithRoles() {
        List<SubsystemUsersResultDto> result = new ArrayList<>();
        List<HdResource> userPacksForSubsystems = metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS);
        for (HdResource usersPack : userPacksForSubsystems) {
            List<SubsystemUser> subsystemUsers = ((List<SubsystemUser>) usersPack.getData()).stream().toList();
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>(subsystemUsers.size());
            for (SubsystemUser u : subsystemUsers) {
                SubsystemUserDto subsystemUserDto = new SubsystemUserDto(
                        u.getFirstName(), u.getLastName(), u.getEmail(), u.getUsername(),
                        u.getRoles().stream().map(r -> r.getName()).toList(), usersPack.getInstanceName()
                );
                subsystemUserDtos.add(subsystemUserDto);
            }
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboards() {
        List<DashboardUsersResultDto> result = new ArrayList<>();

        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        List<DashboardResource> supersetDashboards = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName = mapDashboardRoleWithDashboardNamePerInstance(supersetDashboards);
        Map<String, String> contextKeyToNameMap = contextRepository.findAll().stream().collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName));
        Set<String> supersetsNames = supersetAppInfos.stream().map(superset -> superset.getInstanceName()).collect(Collectors.toSet());
        List<UserDto> allPortalUsers = userService.getAllUsers();
        List<String> allPortalUsersEmails = allPortalUsers.stream().map(u -> u.getEmail()).toList();
        List<MetaInfoResourceEntity> userPacksForSubsystems = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)
                .stream().filter(uPack -> supersetsNames.contains(uPack.getInstanceName())).toList();

        for (MetaInfoResourceEntity usersPack : userPacksForSubsystems) {
            List<SubsystemUser> subsystemUsers = ((List<SubsystemUser>) usersPack.getMetainfo().getData()).stream().filter(u -> allPortalUsersEmails.contains(u.getEmail())).toList();
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>(subsystemUsers.size());
            for (SubsystemUser subsystemUser : subsystemUsers) {
                SubsystemUserDto user = generateUserDto(usersPack, subsystemUser, allPortalUsers, roleNameToDashboardNamesPerInstanceName);
                subsystemUserDtos.add(user);
            }
            result.add(new DashboardUsersResultDto(contextKeyToNameMap.get(usersPack.getContextKey()), usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    private SubsystemUserDto generateUserDto(MetaInfoResourceEntity usersPack, SubsystemUser subsystemUser, List<UserDto> allPortalUsers,
                                             Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName) {
        String usersInstanceName = usersPack.getInstanceName();
        List<RoleToDashboardName> roleToDashboardNameList = roleNameToDashboardNamesPerInstanceName.get(usersInstanceName);
        Optional<UserDto> portalUser = allPortalUsers.stream().filter(usr -> usr.getEmail().equalsIgnoreCase(subsystemUser.getEmail())).findFirst();
        if (portalUser.isPresent()) {
            List<String> roles;
            if (CollectionUtils.containsAny(subsystemUser.getRoles().stream().map(SupersetRole::getName).toList(), ADMIN_ROLE_NAME, BI_ADMIN_ROLE_NAME, BI_EDITOR_ROLE_NAME)) {
                // concat user roles and dashboard roles as these users have access to all dashboards in instance
                roles = Stream.concat(roleNameToDashboardNamesPerInstanceName.get(usersInstanceName).stream().map(r -> r.roleName()), subsystemUser.getRoles().stream().map(r -> r.getName()))
                        .filter(r -> complies(r))
                        .sorted()
                        .map(r -> mapRoleNameToDashboardName(r, roleToDashboardNameList))
                        .toList();
            } else {
                roles = subsystemUser.getRoles().stream()
                        .map(r -> r.getName())
                        .filter(r -> complies(r))
                        .sorted()
                        .map(r -> mapRoleNameToDashboardName(r, roleToDashboardNameList))
                        .toList();
            }
            return new SubsystemUserDto(
                    subsystemUser.getFirstName(),
                    subsystemUser.getLastName(),
                    subsystemUser.getEmail(),
                    subsystemUser.getUsername(),
                    roles,
                    usersInstanceName
            );
        }
        return null;
    }

    /**
     * if a user has a D_dashboard_name role (slugified dashboard role name) then return the dashboard name instead
     *
     * @param roleName
     * @param roleToDashboardNameList
     * @return
     */
    private String mapRoleNameToDashboardName(String roleName, List<RoleToDashboardName> roleToDashboardNameList) {
        Optional<RoleToDashboardName> dashboardNameForRoleName = roleToDashboardNameList.stream().filter(r -> r.roleName().equalsIgnoreCase(roleName)).findFirst();
        if (dashboardNameForRoleName.isPresent()) {
            return dashboardNameForRoleName.get().dashboardName();
        }
        // this should never occur
        log.warn("No role name for dashboard found! Check the roles and dashboards mapping in superset! {}", roleName);
        return roleName;
    }

    private boolean complies(String r) {
        return r.equalsIgnoreCase(ADMIN_ROLE_NAME) ||
                r.startsWith(DASHBOARD_ROLE_PREFIX) ||
                r.equalsIgnoreCase(BI_ADMIN_ROLE_NAME) ||
                r.equalsIgnoreCase(BI_VIEWER_ROLE_NAME) ||
                r.equalsIgnoreCase(BI_EDITOR_ROLE_NAME);
    }
}
