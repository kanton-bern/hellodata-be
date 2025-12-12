package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.metainfo.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.RoleToDashboardName;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portal.metainfo.data.SubsystemUsersResultDto;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portal.user.data.UserWithBusinessRoleDto;
import ch.bedag.dap.hellodata.portal.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
import static ch.bedag.dap.hellodata.portal.base.config.RedisConfig.SUBSYSTEM_USERS_CACHE;
import static ch.bedag.dap.hellodata.portal.base.config.RedisConfig.USERS_WITH_DASHBOARD_CACHE;

@Log4j2
@Service
@AllArgsConstructor
public class MetaInfoUsersService {
    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final HdContextRepository contextRepository;

    @Cacheable(value = SUBSYSTEM_USERS_CACHE)
    @Transactional(readOnly = true)
    public List<SubsystemUsersResultDto> getAllUsersWithRoles() {
        return refreshSubsystemUsersCache();
    }

    @Cacheable(value = USERS_WITH_DASHBOARD_CACHE)
    @Transactional(readOnly = true)
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboards() {
        return refreshDashboardUsersCache();
    }

    @CachePut(value = SUBSYSTEM_USERS_CACHE)
    public List<SubsystemUsersResultDto> refreshSubsystemUsersCache() {
        List<UserWithBusinessRoleDto> allPortalUsers = userService.getAllUsersWithBusinessDomainRole();
        Map<String, UserWithBusinessRoleDto> emailToUserMap = mapUsersByEmail(allPortalUsers);
        List<HdResource> userPacks = metaInfoResourceService.findAllByKind(ModuleResourceKind.HELLO_DATA_USERS);

        List<SubsystemUsersResultDto> result = new ArrayList<>();
        for (HdResource usersPack : userPacks) {
            List<SubsystemUserDto> subsystemUserDtos = mapSubsystemUsers(usersPack, emailToUserMap);
            result.add(new SubsystemUsersResultDto(usersPack.getInstanceName(), subsystemUserDtos));
        }
        return result;
    }

    @CachePut(value = USERS_WITH_DASHBOARD_CACHE)
    public List<DashboardUsersResultDto> refreshDashboardUsersCache() {
        List<AppInfoResource> supersetAppInfos = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_APP_INFO, AppInfoResource.class);
        List<DashboardResource> supersetDashboards = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, HELLO_DATA_DASHBOARDS, DashboardResource.class);
        Map<String, List<RoleToDashboardName>> dashboardRolesMap = mapDashboardRolesToNames(supersetDashboards);
        Map<String, String> contextKeyToName = mapContextKeyToName();
        Set<String> supersetNames = extractInstanceNames(supersetAppInfos);
        List<UserWithBusinessRoleDto> allPortalUsers = userService.getAllUsersWithBusinessDomainRole();

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

    private Map<String, UserWithBusinessRoleDto> mapUsersByEmail(List<UserWithBusinessRoleDto> users) {
        return users.stream().collect(Collectors.toMap(UserDto::getEmail, u -> u));
    }

    @SuppressWarnings("unchecked")
    private List<SubsystemUserDto> mapSubsystemUsers(HdResource usersPack, Map<String, UserWithBusinessRoleDto> emailToUserMap) {
        List<SubsystemUser> subsystemUsers = (List<SubsystemUser>) usersPack.getData();
        List<SubsystemUserDto> dtos = new ArrayList<>(subsystemUsers.size());
        for (SubsystemUser u : subsystemUsers) {
            UserWithBusinessRoleDto userDto = emailToUserMap.get(u.getEmail());
            if (userDto == null) continue;
            dtos.add(createSubsystemUserDto(u, usersPack.getInstanceName(), userDto));
        }
        return dtos;
    }

    private SubsystemUserDto createSubsystemUserDto(SubsystemUser u, String instanceName, UserWithBusinessRoleDto userDto) {
        return new SubsystemUserDto(
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getUsername(),
                u.getRoles().stream().map(SubsystemRole::getName).toList(),
                instanceName,
                userDto.getBusinessDomainRole(),
                userDto.getEnabled()
        );
    }

    @SuppressWarnings("unchecked")
    private List<SubsystemUserDto> mapDashboardSubsystemUsers(MetaInfoResourceEntity userInfo, List<UserWithBusinessRoleDto> allPortalUsers,
                                                              Map<String, List<RoleToDashboardName>> dashboardRolesMap) {
        List<SubsystemUser> subsystemUsers = (List<SubsystemUser>) userInfo.getMetainfo().getData();
        List<SubsystemUserDto> dtos = new ArrayList<>();
        for (UserWithBusinessRoleDto portalUser : allPortalUsers) {
            SubsystemUser subsystemUser = subsystemUsers.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(portalUser.getEmail()))
                    .findFirst()
                    .orElse(null);
            List<SubsystemRole> roles = subsystemUser != null ? subsystemUser.getRoles() : List.of();
            dtos.add(createDashboardUserDto(userInfo.getInstanceName(), roles, portalUser, dashboardRolesMap));
        }
        return dtos;
    }

    private SubsystemUserDto createDashboardUserDto(String instanceName, List<SubsystemRole> subsystemUserRoles, UserWithBusinessRoleDto portalUser,
                                                    Map<String, List<RoleToDashboardName>> dashboardRolesMap) {
        List<RoleToDashboardName> roleToDashboardNameList = CollectionUtils.emptyIfNull(dashboardRolesMap.get(instanceName)).stream().toList();
        List<String> roles = mapRolesForUser(subsystemUserRoles, dashboardRolesMap, instanceName, roleToDashboardNameList);
        return new SubsystemUserDto(
                portalUser.getFirstName(),
                portalUser.getLastName(),
                portalUser.getEmail(),
                portalUser.getUsername(),
                roles,
                instanceName,
                portalUser.getBusinessDomainRole(),
                portalUser.getEnabled()
        );
    }

    private List<String> mapRolesForUser(List<SubsystemRole> subsystemUserRoles, Map<String, List<RoleToDashboardName>> dashboardRolesMap,
                                         String instanceName, List<RoleToDashboardName> roleToDashboardNameList) {
        List<String> subsystemRoleNames = subsystemUserRoles.stream().map(SubsystemRole::getName).toList();
        boolean isAdmin = CollectionUtils.containsAny(subsystemRoleNames, ADMIN_ROLE_NAME, BI_ADMIN_ROLE_NAME, BI_EDITOR_ROLE_NAME);

        Stream<String> rolesStream = isAdmin
                ? Stream.concat(
                dashboardRolesMap.get(instanceName).stream().map(RoleToDashboardName::roleName),
                subsystemRoleNames.stream()
        )
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

    private Map<String, String> mapContextKeyToName() {
        return contextRepository.findAll().stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName));
    }

    private Set<String> extractInstanceNames(List<AppInfoResource> appInfos) {
        return appInfos.stream().map(AppInfoResource::getInstanceName).collect(Collectors.toSet());
    }
}
