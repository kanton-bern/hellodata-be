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
        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        List<DashboardResource> supersetDashboards = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName = mapDashboardRoleWithDashboardNamePerInstance(supersetDashboards);
        Map<String, String> contextKeyToNameMap = contextRepository.findAll().stream().collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName));
        Set<String> supersetsNames = supersetAppInfos.stream().map(AppInfoResource::getInstanceName).collect(Collectors.toSet());
        List<UserDto> allPortalUsers = userService.getAllUsers();
        List<String> allPortalUsersEmails = allPortalUsers.stream().map(UserDto::getEmail).toList();
        Map<String, UserDto> emailToPortalUserDtoMap = allPortalUsers.stream().collect(Collectors.toMap(UserDto::getEmail, u -> u));

        List<MetaInfoResourceEntity> userPacksForSubsystems = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_USERS)
                .stream().filter(uPack -> supersetsNames.contains(uPack.getInstanceName())).toList();

        return userPacksForSubsystems.parallelStream()
                .map(usersPack -> {
                    List<SubsystemUserDto> subsystemUserDtos = ((List<SubsystemUser>) usersPack.getMetainfo().getData())
                            .stream()
                            .filter(subsystemUser -> allPortalUsersEmails.contains(subsystemUser.getEmail()))
                            .map(subsystemUser -> generateUserDto(usersPack, subsystemUser, emailToPortalUserDtoMap.get(subsystemUser.getEmail()), roleNameToDashboardNamesPerInstanceName))
                            .toList();

                    return new DashboardUsersResultDto(contextKeyToNameMap.get(usersPack.getContextKey()), usersPack.getInstanceName(), subsystemUserDtos);
                })
                .toList();
    }

    private SubsystemUserDto generateUserDto(MetaInfoResourceEntity usersPack, SubsystemUser subsystemUser, UserDto portalUser,
                                             Map<String, List<RoleToDashboardName>> roleNameToDashboardNamesPerInstanceName) {
        String usersInstanceName = usersPack.getInstanceName();
        List<RoleToDashboardName> roleToDashboardNameList = roleNameToDashboardNamesPerInstanceName.get(usersInstanceName);
        if (portalUser != null) {
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
