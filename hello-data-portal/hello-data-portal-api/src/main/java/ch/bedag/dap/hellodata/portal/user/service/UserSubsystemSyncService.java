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
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.ModuleRoleNames;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Transactional(readOnly = true)
    public void synchronizeUserWithDashboards(UUID userId, String contextKey) {
        UserEntity userEntity = getUserEntity(userId);

        // Build dashboards for the given context
        Map<String, List<DashboardForUserDto>> dashboardsPerContext = buildDashboardsForContext(userId, contextKey);

        synchronizeUser(userEntity, false, new HashMap<>(), dashboardsPerContext);
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
