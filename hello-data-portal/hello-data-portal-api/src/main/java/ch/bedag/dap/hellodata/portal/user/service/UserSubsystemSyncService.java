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

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.AllUsersContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.service.DashboardCommentPermissionService;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserSubsystemSyncService {

    private final NatsSenderService natsSenderService;
    private final RoleService roleService;
    private final HdContextRepository contextRepository;
    private final UserRepository userRepository;
    private final MetaInfoResourceService metaInfoResourceService;
    private final UserSelectedDashboardService userSelectedDashboardService;
    private final DashboardGroupService dashboardGroupService;
    private final DashboardCommentPermissionService dashboardCommentPermissionService;

    public void synchronizeUser(UserEntity userEntity, boolean sendBackUsersList,
                                Map<String, List<ModuleRoleNames>> extraModuleRoles,
                                Map<String, List<DashboardForUserDto>> dashboardsPerContext) {
        UserContextRoleUpdate userContextRoleUpdate = buildUserContextRoleUpdate(userEntity, sendBackUsersList);
        userContextRoleUpdate.setExtraModuleRoles(extraModuleRoles);
        userContextRoleUpdate.setDashboardsPerContext(dashboardsPerContext);
        natsSenderService.publishMessageToJetStream(HDEvent.UPDATE_USER_CONTEXT_ROLE, userContextRoleUpdate);
    }

    public void synchronizeContextRoles(UserEntity userEntity, boolean sendBackUsersList,
                                        Map<String, List<ModuleRoleNames>> extraModuleRoles) {
        synchronizeUser(userEntity, sendBackUsersList, extraModuleRoles, null);
    }

    /**
     * Fetches all enabled users, prepares them (ensures context roles and comment permissions),
     * and synchronizes them with subsystems including dashboard permissions.
     */
    @Transactional
    public void syncAllUsers() {
        List<UserEntity> allUsers = userRepository.getUserEntitiesByEnabled(true);
        log.info("[syncAllUsers] Found {} enabled users to sync", allUsers.size());

        // Prepare users: ensure they have context roles and comment permissions
        for (UserEntity userEntity : allUsers) {
            if (CollectionUtils.isEmpty(userEntity.getContextRoles())) {
                roleService.createNoneContextRoles(userEntity);
            }
            dashboardCommentPermissionService.syncDefaultPermissionsForUser(userEntity);
        }

        // Sync all users with their dashboards
        syncAllUsersWithDashboards(allUsers);
    }

    /**
     * Synchronizes all enabled users with subsystems, including their dashboard permissions.
     * This method builds UserContextRoleUpdate for each user with their dashboard selections
     * (both direct and from groups) and publishes them in batches via JetStream.
     */
    private void syncAllUsersWithDashboards(List<UserEntity> users) {
        log.info("[syncAllUsersWithDashboards] Synchronizing {} users with dashboards", users.size());

        List<UserContextRoleUpdate> updates = new ArrayList<>();
        int total = users.size();
        int counter = 0;

        for (UserEntity userEntity : users) {
            counter++;
            boolean isLast = (counter == total);

            UserContextRoleUpdate update = buildUserContextRoleUpdate(userEntity, isLast);

            // Build dashboards for all contexts
            Map<String, List<DashboardForUserDto>> dashboards = buildDashboardsForAllContexts(userEntity.getId());
            update.setDashboardsPerContext(dashboards);

            updates.add(update);
        }

        // Partition and send in batches
        List<List<UserContextRoleUpdate>> batches = partitionToBatches(updates);
        for (List<UserContextRoleUpdate> batch : batches) {
            AllUsersContextRoleUpdate batchUpdate = new AllUsersContextRoleUpdate();
            batchUpdate.setUserContextRoleUpdates(batch);
            natsSenderService.publishMessageToJetStream(HDEvent.SYNC_USERS, batchUpdate);
            log.info("[syncAllUsersWithDashboards] Published batch of {} users", batch.size());
        }

        log.info("[syncAllUsersWithDashboards] Completed synchronization of {} users", total);
    }

    /**
     * Partition list to batches, leaving the last one as the biggest one
     * because it will trigger users list pushback.
     */
    private List<List<UserContextRoleUpdate>> partitionToBatches(List<UserContextRoleUpdate> list) {
        List<List<UserContextRoleUpdate>> partition = new ArrayList<>(ListUtils.partition(list, 25));

        if (partition.size() >= 3) {
            List<UserContextRoleUpdate> lastPartition = partition.remove(partition.size() - 1);
            List<UserContextRoleUpdate> secondLastPartition = partition.remove(partition.size() - 1);
            List<UserContextRoleUpdate> thirdLastPartition = partition.remove(partition.size() - 1);

            List<UserContextRoleUpdate> mergedPartition = new ArrayList<>();
            mergedPartition.addAll(thirdLastPartition);
            mergedPartition.addAll(secondLastPartition);
            mergedPartition.addAll(lastPartition);

            partition.add(mergedPartition);
        }
        return partition;
    }

    @Transactional(readOnly = true)
    public void synchronizeUserWithDashboards(UUID userId, String contextKey) {
        UserEntity userEntity = getUserEntity(userId);

        // Build dashboards for the given context
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = buildDashboardsForContext(userId, contextKey);

        synchronizeUser(userEntity, true, new HashMap<>(), dashboardsPerContext);
        log.info("Synchronized user {} with dashboards for context {}", userId, contextKey);
    }

    private Map<String, List<DashboardForUserDto>> buildDashboardsForContext(UUID userId, String contextKey) {
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = new HashMap<>();

        List<MetaInfoResourceEntity> allDashboards = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);
        List<MetaInfoResourceEntity> contextDashboards = allDashboards.stream()
                .filter(d -> contextKey.equals(d.getContextKey()))
                .toList();

        if (contextDashboards.isEmpty()) {
            log.debug("No dashboards found for context {} - skipping dashboard build for user {}", contextKey, userId);
            return dashboardsPerContext;
        }

        // Collect dashboards from direct selections
        Set<Integer> directDashboardIds = userSelectedDashboardService.getSelectedDashboardIds(userId, contextKey);

        // Collect dashboards from groups
        Set<Integer> groupDashboardIds = new HashSet<>();
        List<DashboardGroupEntity> userGroups = dashboardGroupService.findGroupsByContextKeyAndUserId(contextKey, userId.toString());
        for (DashboardGroupEntity group : userGroups) {
            if (group.getEntries() != null) {
                group.getEntries().forEach(entry -> groupDashboardIds.add(entry.getDashboardId()));
            }
        }

        // Merge both sets
        Set<Integer> allSelectedDashboardIds = new HashSet<>();
        allSelectedDashboardIds.addAll(directDashboardIds);
        allSelectedDashboardIds.addAll(groupDashboardIds);

        // Build dashboard DTOs
        List<DashboardForUserDto> dashboardDtos = new ArrayList<>();
        for (MetaInfoResourceEntity dashboardEntity : contextDashboards) {
            if (dashboardEntity.getMetainfo() instanceof DashboardResource dashboardResource) {
                for (SupersetDashboard dashboard : dashboardResource.getData()) {
                    if (dashboard.isPublished()) {
                        boolean isViewer = allSelectedDashboardIds.contains(dashboard.getId());
                        DashboardForUserDto dto = new DashboardForUserDto();
                        dto.setId(dashboard.getId());
                        dto.setTitle(dashboard.getDashboardTitle());
                        dto.setViewer(isViewer);
                        dto.setInstanceName(dashboardResource.getInstanceName());
                        dashboardDtos.add(dto);
                    }
                }
            }
        }

        dashboardsPerContext.put(contextKey, dashboardDtos);
        log.debug("Built {} dashboard DTOs for context {} (direct: {}, from groups: {})",
                dashboardDtos.size(), contextKey, directDashboardIds.size(), groupDashboardIds.size());
        return dashboardsPerContext;
    }

    /**
     * Builds dashboard permissions for all contexts where the user has roles.
     * Used during batch synchronization of all users.
     */
    private Map<String, List<DashboardForUserDto>> buildDashboardsForAllContexts(UUID userId) {
        Map<String, List<DashboardForUserDto>> allDashboards = new HashMap<>();

        List<MetaInfoResourceEntity> dashboardResources = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);

        // Get unique context keys from dashboard resources
        Set<String> contextKeys = dashboardResources.stream()
                .map(MetaInfoResourceEntity::getContextKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String contextKey : contextKeys) {
            Map<String, List<DashboardForUserDto>> contextDashboards = buildDashboardsForContext(userId, contextKey);
            allDashboards.putAll(contextDashboards);
        }

        return allDashboards;
    }

    private UserContextRoleUpdate buildUserContextRoleUpdate(UserEntity userEntity, boolean sendBackUsersList) {
        UserContextRoleUpdate userContextRoleUpdate = new UserContextRoleUpdate();
        userContextRoleUpdate.setEmail(userEntity.getEmail());
        userContextRoleUpdate.setUsername(userEntity.getEmail());
        userContextRoleUpdate.setFirstName(userEntity.getFirstName());
        userContextRoleUpdate.setLastName(userEntity.getLastName());
        userContextRoleUpdate.setActive(userEntity.isEnabled());
        List<UserContextRoleEntity> allContextRolesForUser = roleService.getAllContextRolesForUser(userEntity);
        List<UserContextRoleUpdate.ContextRole> contextRoles = new ArrayList<>();
        allContextRolesForUser.forEach(contextRoleForUser -> {
            UserContextRoleUpdate.ContextRole contextRole = new UserContextRoleUpdate.ContextRole();
            contextRole.setContextKey(contextRoleForUser.getContextKey());
            contextRole.setRoleName(contextRoleForUser.getRole().getName());
            Optional<HdContextEntity> byContextKey = contextRepository.getByContextKey(contextRoleForUser.getContextKey());
            byContextKey.ifPresent(contextEntity -> contextRole.setParentContextKey(contextRole.getParentContextKey()));
            contextRoles.add(contextRole);
        });
        userContextRoleUpdate.setContextRoles(contextRoles);
        userContextRoleUpdate.setSendBackUsersList(sendBackUsersList);
        return userContextRoleUpdate;
    }

    private UserEntity getUserEntity(UUID userId) {
        UserEntity userEntity = userRepository.getByIdOrAuthId(userId.toString());
        if (userEntity == null) {
            throw new NotFoundException(String.format("User %s not found in the DB", userId));
        }
        return userEntity;
    }
}
