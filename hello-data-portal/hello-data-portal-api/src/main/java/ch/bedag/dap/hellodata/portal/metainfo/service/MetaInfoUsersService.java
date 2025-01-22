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
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "subsystem_users")
    @Transactional(readOnly = true)
    public List<SubsystemUsersResultDto> getAllUsersWithRoles() {
        return getAllUsersWithRolesRefreshCache();
    }

    @Cacheable(value = "users_with_dashboards")
    @Transactional(readOnly = true)
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboards() {
        return getAllUsersWithRolesForDashboardsRefreshCache();
    }

    @CachePut(value = "subsystem_users")
    public List<SubsystemUsersResultDto> getAllUsersWithRolesRefreshCache() {
        List<SubsystemUsersResultDto> result = new ArrayList<>();
        List<UserDto> allPortalUsers = userService.getAllUsers();
        Map<String, UserDto> emailToPortalUserDtoMap = allPortalUsers.stream().collect(Collectors.toMap(UserDto::getEmail, u -> u));
        List<HdResource> userPacksForSubsystems = metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS);
        for (HdResource usersPack : userPacksForSubsystems) {
            List<SubsystemUser> subsystemUsers = ((List<SubsystemUser>) usersPack.getData()).stream().toList();
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>(subsystemUsers.size());
            for (SubsystemUser u : subsystemUsers) {
                UserDto userDto = emailToPortalUserDtoMap.get(u.getEmail());
                if (userDto == null) {
                    continue;
                }
                SubsystemUserDto subsystemUserDto = new SubsystemUserDto(
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getUsername(),
                        u.getRoles().stream().map(r -> r.getName()).toList(),
                        usersPack.getInstanceName(), userDto.getEnabled()
                );
                subsystemUserDtos.add(subsystemUserDto);
            }
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    @CachePut(value = "users_with_dashboards")
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboardsRefreshCache() {
        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        List<DashboardResource> supersetDashboards = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName = mapDashboardRoleWithDashboardNamePerInstance(supersetDashboards);
        Map<String, String> contextKeyToNameMap = contextRepository.findAll().stream().collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName));
        Set<String> supersetsNames = supersetAppInfos.stream().map(AppInfoResource::getInstanceName).collect(Collectors.toSet());
        List<UserDto> allPortalUsers = userService.getAllUsers();

        List<MetaInfoResourceEntity> userPacksForSubsystems = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)
                .stream().filter(uPack -> supersetsNames.contains(uPack.getInstanceName())).toList();

        List<DashboardUsersResultDto> list = new ArrayList<>();
        for (MetaInfoResourceEntity userPacksForSubsystem : userPacksForSubsystems) {
            List<SubsystemUserDto> subsystemUserDtos = new ArrayList<>();
            List<SubsystemUser> subsystemUsers = (List<SubsystemUser>) userPacksForSubsystem.getMetainfo().getData();
            for (UserDto portalUser : allPortalUsers) {
                SubsystemUser subsystemUser = subsystemUsers.stream().filter(u -> u.getEmail().equals(portalUser.getEmail())).findFirst().orElse(null);
                SubsystemUserDto applied;
                if (subsystemUser == null) {
                    applied = generateUserDto(userPacksForSubsystem.getInstanceName(), List.of(), portalUser, roleNameToDashboardNamesPerInstanceName);
                } else {
                    applied = generateUserDto(userPacksForSubsystem.getInstanceName(), subsystemUser.getRoles(), portalUser, roleNameToDashboardNamesPerInstanceName);
                }
                subsystemUserDtos.add(applied);
            }

            DashboardUsersResultDto apply = new DashboardUsersResultDto(
                    contextKeyToNameMap.get(userPacksForSubsystem.getContextKey()),
                    userPacksForSubsystem.getInstanceName(),
                    subsystemUserDtos
            );
            list.add(apply);
        }
        return list;
    }

    private SubsystemUserDto generateUserDto(String usersInstanceName, List<SubsystemRole> subsystemUserRoles, UserDto portalUser,
                                             Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName) {
        List<RoleToDashboardName> roleToDashboardNameList = roleNameToDashboardNamesPerInstanceName.get(usersInstanceName);
        if (portalUser != null) {
            List<String> roles;
            if (CollectionUtils.containsAny(subsystemUserRoles.stream().map(SubsystemRole::getName).toList(), ADMIN_ROLE_NAME, BI_ADMIN_ROLE_NAME, BI_EDITOR_ROLE_NAME)) {
                // concat user roles and dashboard roles as these users have access to all dashboards in instance
                roles = Stream.concat(roleNameToDashboardNamesPerInstanceName.get(usersInstanceName).stream().map(r -> r.roleName()), subsystemUserRoles.stream().map(r -> r.getName()))
                        .filter(r -> complies(r))
                        .sorted()
                        .map(r -> mapRoleNameToDashboardName(r, roleToDashboardNameList))
                        .toList();
            } else {
                roles = subsystemUserRoles.stream()
                        .map(r -> r.getName())
                        .filter(r -> complies(r))
                        .sorted()
                        .map(r -> mapRoleNameToDashboardName(r, roleToDashboardNameList))
                        .toList();
            }
            return new SubsystemUserDto(
                    portalUser.getFirstName(),
                    portalUser.getLastName(),
                    portalUser.getEmail(),
                    portalUser.getUsername(),
                    roles,
                    usersInstanceName,
                    portalUser.getEnabled()
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
        return roleName;
    }

    private boolean complies(String r) {
        return r.equalsIgnoreCase(ADMIN_ROLE_NAME) ||
                r.startsWith(DASHBOARD_ROLE_PREFIX) ||
                r.equalsIgnoreCase(BI_ADMIN_ROLE_NAME) ||
                r.equalsIgnoreCase(BI_VIEWER_ROLE_NAME) ||
                r.equalsIgnoreCase(BI_EDITOR_ROLE_NAME);
    }

    private Map<String, List<RoleToDashboardName>> mapDashboardRoleWithDashboardNamePerInstance(List<DashboardResource> supersetDashboards) {
        // Collect mapping between 'D_*' role and dashboard name for each instance
        Map<String, List<SupersetDashboard>> instanceNamesToSupersetDashboards = supersetDashboards.stream()
                .collect(Collectors.toMap(DashboardResource::getInstanceName, DashboardResource::getData));

        return instanceNamesToSupersetDashboards.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().parallelStream()
                                .flatMap(supersetDashboard -> supersetDashboard.getRoles().stream()
                                        .filter(role -> role.getName().startsWith(DASHBOARD_ROLE_PREFIX))
                                        .map(role -> new RoleToDashboardName(role.getName(), supersetDashboard.getDashboardTitle()))
                                )
                                .toList()
                ));
    }
}
