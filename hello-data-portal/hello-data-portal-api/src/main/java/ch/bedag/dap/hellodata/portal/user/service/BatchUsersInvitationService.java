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
package ch.bedag.dap.hellodata.portal.user.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.RoleResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.role.superset.RolePermissions;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.csv.service.CsvParserService;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentPermissionDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.UserAlreadyExistsException;
import ch.bedag.dap.hellodata.portal.user.data.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class BatchUsersInvitationService {

    private final CsvParserService csvParserService;
    private final UserService userService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final RoleService roleService;
    private final BatchUsersCustomLogger batchUsersCustomLogger;
    private final DashboardGroupService dashboardGroupService;

    private final String batchUsersFileLocation;

    public BatchUsersInvitationService(CsvParserService csvParserService,
                                       UserService userService,
                                       MetaInfoResourceService metaInfoResourceService,
                                       RoleService roleService,
                                       BatchUsersCustomLogger batchUsersCustomLogger,
                                       DashboardGroupService dashboardGroupService,
                                       @Value("${hello-data.batch-users-file.location}") String batchUsersFileLocation) {
        this.csvParserService = csvParserService;
        this.userService = userService;
        this.batchUsersFileLocation = batchUsersFileLocation;
        this.metaInfoResourceService = metaInfoResourceService;
        this.roleService = roleService;
        this.batchUsersCustomLogger = batchUsersCustomLogger;
        this.dashboardGroupService = dashboardGroupService;
    }

    @Scheduled(fixedDelayString = "${hello-data.batch-users-file.scan-interval-seconds}", timeUnit = TimeUnit.SECONDS)
    public void inviteOrUpdateUsers() {
        try {
            ContextsDto availableContexts = userService.getAvailableContexts();
            List<BatchUpdateContextRolesForUserDto> users = fetchDataFromFile(true, availableContexts);
            String userEmails = users.stream()
                    .map(BatchUpdateContextRolesForUserDto::getEmail)
                    .collect(Collectors.joining(", "));

            if (!CollectionUtils.isEmpty(users)) {
                String importUsersMsg = "Inviting/updating %s users: %s".formatted(users.size(), userEmails);
                log.info(importUsersMsg);
                batchUsersCustomLogger.logMessage(importUsersMsg);
            } else {
                log.debug("No users were invited/updated");
                return;
            }
            List<RoleDto> allRoles = roleService.getAll();
            List<UserDto> allUsers = userService.getAllUsers();

            createOrUpdateUsers(users, allUsers, allRoles, availableContexts);
            batchUsersCustomLogger.logMessage("Processed %s users successfully%n".formatted(users.size()));
        } catch (Exception e) {
            log.error("Error inviting/updating users", e);
            batchUsersCustomLogger.logMessage("Error inviting users: %s %n".formatted(e.getMessage()));
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void createOrUpdateUsers(List<BatchUpdateContextRolesForUserDto> users, List<UserDto> allUsers, List<RoleDto> allRoles, ContextsDto availableContexts) throws InterruptedException {
        // Pre-load all dashboards for mapping supersetRoles to dashboards
        Map<String, List<SupersetDashboard>> contextToDashboards = loadDashboardsByContext();

        for (int i = 0; i < users.size(); i++) {
            BatchUpdateContextRolesForUserDto user = users.get(i);
            batchUsersCustomLogger.logMessage(String.format("Processing user %s", user.getEmail()));
            String userId = findOrCreateUser(user, allUsers);
            insertFullBusinessDomainRole(user, allRoles);
            insertFullContextRoles(user, allRoles, availableContexts);

            // Map supersetRoles to dashboard selections for VIEWER and BUSINESS_SPECIALIST roles
            Map<String, List<DashboardForUserDto>> selectedDashboards = mapSupersetRolesToDashboards(user, contextToDashboards);
            user.setSelectedDashboardsForUser(selectedDashboards);

            // Set default dashboard comment permissions based on roles
            List<DashboardCommentPermissionDto> commentPermissions = buildDefaultCommentPermissions(user, availableContexts);
            user.setCommentPermissions(commentPermissions);

            // Resolve dashboard group names from CSV to group IDs
            Map<String, List<String>> resolvedGroupIds = resolveDashboardGroupNamesToIds(user);
            user.setSelectedDashboardGroupIdsForUser(resolvedGroupIds);

            boolean isLast = i == users.size() - 1;
            userService.updateContextRolesForUserFromBatch(UUID.fromString(userId), user, isLast);
        }
    }

    private String findOrCreateUser(BatchUpdateContextRolesForUserDto user, List<UserDto> allUsers) throws InterruptedException {
        List<AdUserDto> usersSearched = this.userService.searchUser(user.getEmail());
        log.info("Found {} user(s) by email {}: {}", usersSearched.size(), user.getEmail(), usersSearched);
        Optional<AdUserDto> any = usersSearched.stream().findAny();
        log.info("Found any? {}", any.isPresent());
        Optional<AdUserDto> firstAD = usersSearched.stream().filter(adUserDto -> adUserDto.getOrigin() == AdUserOrigin.LDAP).findFirst();
        AdUserDto adUserDto = firstAD.orElseGet(() -> any.orElseThrow(() -> new NoSuchElementException("User not found: " + user.getEmail())));
        try {
            String userId = userService.createUser(adUserDto.getEmail(), adUserDto.getFirstName(), adUserDto.getLastName(), adUserDto.getOrigin());
            Thread.sleep(1000L); //wait for it to push to subsystems before proceeding to set context roles
            batchUsersCustomLogger.logMessage(String.format("Created user %s", user.getEmail()));
            return userId;
        } catch (UserAlreadyExistsException e) {
            String errMsg = "User %s already exists, updating roles...".formatted(adUserDto.getEmail());
            log.info(errMsg);
            batchUsersCustomLogger.logMessage(errMsg);
            return allUsers.stream().filter(userDto -> userDto.getEmail().equalsIgnoreCase(adUserDto.getEmail())).findFirst().get().getId();
        }
    }

    /**
     * Resolves dashboard group names from the CSV to dashboard group IDs.
     * Uses DashboardGroupService to look up groups by name and context key.
     */
    private Map<String, List<String>> resolveDashboardGroupNamesToIds(BatchUpdateContextRolesForUserDto user) {
        Map<String, List<String>> dashboardGroupNamesMap = user.getDashboardGroupNamesFromCsv();
        if (dashboardGroupNamesMap == null || dashboardGroupNamesMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<String>> resolvedGroupIds = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : dashboardGroupNamesMap.entrySet()) {
            String contextKey = entry.getKey();
            List<String> groupNames = entry.getValue();
            List<String> groupIds = resolveGroupNamesForContext(contextKey, groupNames, user.getEmail());
            if (!groupIds.isEmpty()) {
                resolvedGroupIds.put(contextKey, groupIds);
            }
        }
        return resolvedGroupIds;
    }

    private List<String> resolveGroupNamesForContext(String contextKey, List<String> groupNames, String userEmail) {
        List<DashboardGroupEntity> allGroupsInContext = dashboardGroupService.findAllGroupsByContextKey(contextKey);
        Map<String, String> groupNameToIdMap = allGroupsInContext.stream()
                .collect(Collectors.toMap(
                        g -> g.getName().toLowerCase(Locale.ROOT),
                        g -> g.getId().toString(),
                        (existing, replacement) -> existing));

        List<String> groupIds = new ArrayList<>();
        for (String groupName : groupNames) {
            String groupId = groupNameToIdMap.get(groupName.toLowerCase(Locale.ROOT));
            if (groupId == null) {
                throw new IllegalArgumentException(
                        "Dashboard group '%s' not found in context '%s' for user '%s'".formatted(groupName, contextKey, userEmail));
            }
            groupIds.add(groupId);
        }
        return groupIds;
    }

    /**
     * Loads all dashboards from metainfo resources and groups them by context key.
     * Also stores the instanceName for each dashboard.
     */
    private Map<String, List<SupersetDashboard>> loadDashboardsByContext() {
        Map<String, List<SupersetDashboard>> result = new HashMap<>();
        List<MetaInfoResourceEntity> dashboardResources = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);

        for (MetaInfoResourceEntity entity : dashboardResources) {
            if (entity.getMetainfo() instanceof DashboardResource dashboardResource) {
                String contextKey = entity.getContextKey();
                List<SupersetDashboard> dashboards = result.computeIfAbsent(contextKey, k -> new ArrayList<>());

                // Add all published dashboards
                for (SupersetDashboard dashboard : dashboardResource.getData()) {
                    if (dashboard.isPublished()) {
                        dashboards.add(dashboard);
                    }
                }
            }
        }

        log.debug("Loaded dashboards for {} contexts", result.size());
        return result;
    }

    /**
     * Maps supersetRoles from CSV (D_* roles) to dashboard selections.
     * Only applies to users with DATA_DOMAIN_VIEWER or DATA_DOMAIN_BUSINESS_SPECIALIST roles.
     * <p>
     * For each context, checks if the user's supersetRoles match any dashboard role (starting with D_).
     * If a match is found, the dashboard is added to the user's selected dashboards.
     */
    private Map<String, List<DashboardForUserDto>> mapSupersetRolesToDashboards(
            BatchUpdateContextRolesForUserDto user,
            Map<String, List<SupersetDashboard>> contextToDashboards) {

        Map<String, List<ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames>> contextToModuleRoles =
                user.getContextToModuleRoleNamesMap();

        if (contextToModuleRoles == null || contextToModuleRoles.isEmpty()) {
            log.debug("No module role names defined for user {}", user.getEmail());
            return new HashMap<>();
        }

        Map<String, String> contextToDataDomainRole = buildContextToDataDomainRoleMap(user);

        Map<String, List<DashboardForUserDto>> selectedDashboards = new HashMap<>();
        for (Map.Entry<String, List<ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames>> entry : contextToModuleRoles.entrySet()) {
            String contextKey = entry.getKey();
            List<DashboardForUserDto> matched = mapDashboardsForContext(
                    contextKey, entry.getValue(), contextToDataDomainRole, contextToDashboards, user.getEmail());
            if (!matched.isEmpty()) {
                selectedDashboards.put(contextKey, matched);
            }
        }
        return selectedDashboards;
    }

    private Map<String, String> buildContextToDataDomainRoleMap(BatchUpdateContextRolesForUserDto user) {
        return user.getDataDomainRoles().stream()
                .collect(Collectors.toMap(
                        ddRole -> ddRole.getContext().getContextKey(),
                        ddRole -> ddRole.getRole().getName(),
                        (existing, replacement) -> existing));
    }

    private boolean isEligibleForDashboardAssignment(String dataDomainRole) {
        return HdRoleName.DATA_DOMAIN_VIEWER.name().equalsIgnoreCase(dataDomainRole)
                || HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST.name().equalsIgnoreCase(dataDomainRole);
    }

    private List<DashboardForUserDto> mapDashboardsForContext(
            String contextKey,
            List<ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames> moduleRoleNamesList,
            Map<String, String> contextToDataDomainRole,
            Map<String, List<SupersetDashboard>> contextToDashboards,
            String userEmail) {

        String dataDomainRole = contextToDataDomainRole.get(contextKey);
        if (!isEligibleForDashboardAssignment(dataDomainRole)) {
            log.debug("Skipping dashboard assignment for context {} - role {} is not VIEWER or BUSINESS_SPECIALIST",
                    contextKey, dataDomainRole);
            return List.of();
        }

        Set<String> supersetRoleNames = extractDashboardRoleNames(moduleRoleNamesList);
        if (supersetRoleNames.isEmpty()) {
            log.debug("No dashboard roles (D_*) found for context {}", contextKey);
            return List.of();
        }

        List<SupersetDashboard> dashboards = contextToDashboards.getOrDefault(contextKey, List.of());
        if (dashboards.isEmpty()) {
            log.debug("No dashboards found for context {}", contextKey);
            return List.of();
        }

        List<DashboardForUserDto> matchedDashboards = dashboards.stream()
                .filter(dashboard -> hasMatchingRole(dashboard, supersetRoleNames))
                .map(dashboard -> toDashboardForUserDto(dashboard, contextKey))
                .toList();

        if (!matchedDashboards.isEmpty()) {
            log.info("Assigned {} dashboards to user {} for context {}",
                    matchedDashboards.size(), userEmail, contextKey);
        }
        return matchedDashboards;
    }

    private Set<String> extractDashboardRoleNames(
            List<ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames> moduleRoleNamesList) {
        return moduleRoleNamesList.stream()
                .filter(moduleRoleNames -> moduleRoleNames.moduleType() == ModuleType.SUPERSET)
                .flatMap(moduleRoleNames -> moduleRoleNames.roleNames().stream())
                .filter(roleName -> roleName.startsWith(SlugifyUtil.DASHBOARD_ROLE_PREFIX))
                .collect(Collectors.toSet());
    }

    private boolean hasMatchingRole(SupersetDashboard dashboard, Set<String> supersetRoleNames) {
        if (dashboard.getRoles() == null) {
            return false;
        }
        return dashboard.getRoles().stream()
                .anyMatch(role -> supersetRoleNames.contains(role.getName()));
    }

    private DashboardForUserDto toDashboardForUserDto(SupersetDashboard dashboard, String contextKey) {
        DashboardForUserDto dto = new DashboardForUserDto();
        dto.setId(dashboard.getId());
        dto.setTitle(dashboard.getDashboardTitle());
        dto.setViewer(true);
        dto.setChangedOnUtc(dashboard.getChangedOnUtc());
        dto.setContextKey(contextKey);
        return dto;
    }

    /**
     * Builds default dashboard comment permissions based on user's roles.
     * - HELLODATA_ADMIN and BUSINESS_DOMAIN_ADMIN: full access (read, write, review) in all data domains
     * - DATA_DOMAIN_ADMIN: full access in their assigned data domains
     * - DATA_DOMAIN_EDITOR: read and write access in their assigned data domains
     * - DATA_DOMAIN_VIEWER and DATA_DOMAIN_BUSINESS_SPECIALIST: read-only access in their assigned data domains
     * - NONE: no access
     */
    private List<DashboardCommentPermissionDto> buildDefaultCommentPermissions(BatchUpdateContextRolesForUserDto user, ContextsDto availableContexts) {
        String businessRoleName = user.getBusinessDomainRole().getName();

        boolean isPortalAdmin = HdRoleName.HELLODATA_ADMIN.name().equalsIgnoreCase(businessRoleName)
                || HdRoleName.BUSINESS_DOMAIN_ADMIN.name().equalsIgnoreCase(businessRoleName);

        // Get all data domain context keys
        Set<String> allDataDomainKeys = availableContexts.getContexts().stream()
                .filter(ctx -> ctx.getType() == HdContextType.DATA_DOMAIN)
                .map(ContextDto::getContextKey)
                .collect(Collectors.toSet());

        if (isPortalAdmin) {
            return allDataDomainKeys.stream()
                    .map(contextKey -> createCommentPermission(contextKey, true, true, true))
                    .toList();
        }

        // Build a map of context key -> role name from user's data domain roles
        Map<String, String> contextToRoleMap = user.getDataDomainRoles().stream()
                .collect(Collectors.toMap(
                        ddRole -> ddRole.getContext().getContextKey(),
                        ddRole -> ddRole.getRole().getName(),
                        (existing, replacement) -> existing));

        return allDataDomainKeys.stream()
                .map(contextKey -> buildCommentPermissionForRole(contextKey, contextToRoleMap.getOrDefault(contextKey, HdRoleName.NONE.name())))
                .toList();
    }

    private static final Map<String, boolean[]> ROLE_TO_COMMENT_PERMISSIONS = Map.of(
            HdRoleName.DATA_DOMAIN_ADMIN.name(), new boolean[]{true, true, true},
            HdRoleName.DATA_DOMAIN_EDITOR.name(), new boolean[]{true, true, false},
            HdRoleName.DATA_DOMAIN_VIEWER.name(), new boolean[]{true, false, false},
            HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST.name(), new boolean[]{true, false, false}
    );

    private static final boolean[] NO_PERMISSIONS = {false, false, false};

    private DashboardCommentPermissionDto buildCommentPermissionForRole(String contextKey, String roleName) {
        boolean[] perms = ROLE_TO_COMMENT_PERMISSIONS.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(roleName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(NO_PERMISSIONS);
        return createCommentPermission(contextKey, perms[0], perms[1], perms[2]);
    }

    private DashboardCommentPermissionDto createCommentPermission(String contextKey, boolean read, boolean write, boolean review) {
        DashboardCommentPermissionDto permission = new DashboardCommentPermissionDto();
        permission.setContextKey(contextKey);
        permission.setReadComments(read);
        permission.setWriteComments(write);
        permission.setReviewComments(review);
        return permission;
    }

    /**
     * Inserts full context roles to the selection from the file. The selection from the file has only role name and context key.
     *
     * @param user
     * @param allRoles
     * @param availableContexts
     */
    private void insertFullContextRoles(BatchUpdateContextRolesForUserDto user, List<RoleDto> allRoles, ContextsDto availableContexts) {
        List<UserContextRoleDto> dataDomainRoles = user.getDataDomainRoles().stream().map(dataDomainRole -> {
            RoleDto role = allRoles.stream()
                    .filter(roleDto -> roleDto.getName().equalsIgnoreCase(dataDomainRole.getRole().getName()))
                    .findAny().orElseThrow(() -> new RuntimeException("Role not found"));
            dataDomainRole.setRole(role);
            dataDomainRole.setContext(availableContexts.getContexts().stream()
                    .filter(contextDto -> contextDto.getContextKey().equalsIgnoreCase(dataDomainRole.getContext().getContextKey()))
                    .findAny().orElseThrow(() -> new RuntimeException("Context not found")));
            return dataDomainRole;
        }).collect(Collectors.toList());
        user.setDataDomainRoles(dataDomainRoles);
    }

    /**
     * Inserts full business domain role to the selection from the file. The selection from the file has only name and context.
     *
     * @param user
     * @param allRoles
     */
    private void insertFullBusinessDomainRole(BatchUpdateContextRolesForUserDto user, List<RoleDto> allRoles) {
        RoleDto businessDomainRole = allRoles.stream()
                .filter(roleDto -> roleDto.getName().equalsIgnoreCase(user.getBusinessDomainRole().getName()))
                .findAny().orElseThrow(() -> new RuntimeException("Role not found"));
        user.setBusinessDomainRole(businessDomainRole);
    }

    private void deleteFile(File file) {
        if (file.delete()) { //NOSONAR
            log.info("Batch users file successfully deleted: {}", file.getAbsolutePath());
        } else {
            log.error("Failed to delete batch users file: {}", file.getAbsolutePath());
        }
    }


    private Set<String> getAvailableDataDomainKeys(ContextsDto availableContexts) {
        return availableContexts.getContexts().stream()
                .filter(contextDto -> contextDto.getType() == HdContextType.DATA_DOMAIN)
                .map(ContextDto::getContextKey)
                .collect(Collectors.toSet());
    }

    private Map<String, List<String>> getDataDomainKeyToExistingSupersetRolesMap() {
        return metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_ROLES).stream()
                .collect(Collectors.toMap(MetaInfoResourceEntity::getContextKey,
                        metaInfoResourceEntity -> ((RoleResource) metaInfoResourceEntity.getMetainfo()).getData().stream()
                                .map(RolePermissions::name).toList()));
    }


    private static void collectSupersetRoleErrors(Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap, BatchUpdateContextRolesForUserDto user, List<String> errors) {
        user.getContextToModuleRoleNamesMap().forEach((contextKey, moduleRoleNamesList) -> {
            List<String> existingSupersetRoles = dataDomainKeyToExistingSupersetRolesMap.get(contextKey);
            if (existingSupersetRoles != null) {
                moduleRoleNamesList.stream()
                        .filter(moduleRoleNames -> moduleRoleNames.moduleType() == ModuleType.SUPERSET)
                        .findFirst()
                        .ifPresent(supersetModule -> {
                            List<String> proposedRoleNamesFromFile = supersetModule.roleNames();
                            Set<String> existingSet = new HashSet<>(existingSupersetRoles);
                            List<String> missingRoles = proposedRoleNamesFromFile.stream()
                                    .filter(role -> !existingSet.contains(role))
                                    .toList();
                            if (!missingRoles.isEmpty()) {
                                errors.add("User '%s': the following role(s) are not present in Superset for context '%s': %s"
                                        .formatted(user.getEmail(), contextKey, missingRoles));
                            }
                        });
            }
        });
    }


    /**
     * Validates all CSV data at once: context keys, superset roles, and dashboard groups.
     * Collects all errors and throws a single exception with a comprehensive error report.
     */
    private void validateCsvData(List<BatchUpdateContextRolesForUserDto> users,
                                 Set<String> availableDataDomainKeys,
                                 Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap) {
        List<String> allErrors = new ArrayList<>();

        // 1. Verify context keys
        for (BatchUpdateContextRolesForUserDto user : users) {
            for (UserContextRoleDto dataDomainRole : user.getDataDomainRoles()) {
                if (!availableDataDomainKeys.contains(dataDomainRole.getContext().getContextKey())) {
                    allErrors.add("User '%s': context key not found: '%s'"
                            .formatted(user.getEmail(), dataDomainRole.getContext().getContextKey()));
                }
            }
        }

        // 2. Verify superset roles
        for (BatchUpdateContextRolesForUserDto user : users) {
            collectSupersetRoleErrors(dataDomainKeyToExistingSupersetRolesMap, user, allErrors);
        }

        // 3. Verify dashboard group names (pre-load groups per context for efficiency)
        collectDashboardGroupErrors(users, allErrors);

        if (!allErrors.isEmpty()) {
            String errorReport = "CSV validation failed with %d error(s):%n%s"
                    .formatted(allErrors.size(), String.join(System.lineSeparator(), allErrors));
            throw new IllegalArgumentException(errorReport);
        }
    }

    private void collectDashboardGroupErrors(List<BatchUpdateContextRolesForUserDto> users, List<String> errors) {
        Set<String> contextKeysWithGroups = users.stream()
                .flatMap(u -> u.getDashboardGroupNamesFromCsv().keySet().stream())
                .collect(Collectors.toSet());

        if (contextKeysWithGroups.isEmpty()) {
            return;
        }

        Map<String, Set<String>> contextToExistingGroupNames = new HashMap<>();
        for (String contextKey : contextKeysWithGroups) {
            Set<String> groupNames = dashboardGroupService.findAllGroupsByContextKey(contextKey).stream()
                    .map(g -> g.getName().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            contextToExistingGroupNames.put(contextKey, groupNames);
        }

        for (BatchUpdateContextRolesForUserDto user : users) {
            Map<String, List<String>> groupNamesMap = user.getDashboardGroupNamesFromCsv();
            if (groupNamesMap == null || groupNamesMap.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, List<String>> entry : groupNamesMap.entrySet()) {
                String contextKey = entry.getKey();
                Set<String> existingNames = contextToExistingGroupNames.getOrDefault(contextKey, Set.of());
                List<String> missingGroups = entry.getValue().stream()
                        .filter(groupName -> !existingNames.contains(groupName.toLowerCase(Locale.ROOT)))
                        .toList();
                if (!missingGroups.isEmpty()) {
                    errors.add("User '%s': dashboard group(s) not found in context '%s': %s"
                            .formatted(user.getEmail(), contextKey, missingGroups));
                }
            }
        }
    }

    List<BatchUpdateContextRolesForUserDto> fetchDataFromFile(boolean removeFilesAfterFetch, ContextsDto availableContexts) {
        File directory = new File(batchUsersFileLocation);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("Batch users directory does not exist or is not a directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            log.debug("No .csv files found in directory: {}", batchUsersFileLocation);
            return Collections.emptyList();
        }

        Map<String, List<String>> dataDomainKeyToExistingSupersetRolesMap = getDataDomainKeyToExistingSupersetRolesMap();

        Set<String> availableDataDomainKeys = getAvailableDataDomainKeys(availableContexts);
        List<BatchUpdateContextRolesForUserDto> allUsers = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                log.debug("File {} content \n{}", file.getAbsolutePath(), FileUtils.readFileToString(file, StandardCharsets.UTF_8));
                List<BatchUpdateContextRolesForUserDto> users = csvParserService.transform(fis);
                log.debug("Batch users file successfully read: {}", users);
                validateCsvData(users, availableDataDomainKeys, dataDomainKeyToExistingSupersetRolesMap);
                allUsers.addAll(users);
                if (removeFilesAfterFetch) {
                    deleteFile(file);
                }
            } catch (IOException e) {
                log.error("Error processing batch users file: {}", file.getAbsolutePath(), e);
                throw new RuntimeException("Error processing batch users file", e); //NOSONAR
            }
        }
        return allUsers;
    }
}
