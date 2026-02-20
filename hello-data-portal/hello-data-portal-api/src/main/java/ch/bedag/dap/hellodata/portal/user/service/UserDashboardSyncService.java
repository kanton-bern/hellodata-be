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
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.events.RequestReplySubject;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.SupersetDashboardsForUserUpdate;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserDashboardSyncService {

    private final UserSelectedDashboardService userSelectedDashboardService;
    private final DashboardGroupService dashboardGroupService;
    private final MetaInfoResourceService metaInfoResourceService;
    private final UserRepository userRepository;
    private final HdContextRepository contextRepository;
    private final Connection connection;
    private final ObjectMapper objectMapper;
    @org.springframework.context.annotation.Lazy
    private final UserDashboardSyncService self;

    /**
     * Synchronizes user's dashboards (direct selections + group memberships) to Superset sidecar.
     *
     * @param userId     the user ID
     * @param contextKey the context key (data domain)
     */
    @Transactional
    public void syncUserDashboardsToSuperset(UUID userId, String contextKey) {
        // Get all dashboards for this context
        List<MetaInfoResourceEntity> allDashboards = metaInfoResourceService.findAllByKindWithContext(ModuleResourceKind.HELLO_DATA_DASHBOARDS);
        List<MetaInfoResourceEntity> contextDashboards = allDashboards.stream()
                .filter(d -> contextKey.equals(d.getContextKey()))
                .toList();

        if (contextDashboards.isEmpty()) {
            log.debug("No dashboards found for context {} - skipping sync for user {}", contextKey, userId);
            return;
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

        // Build dashboard DTOs for synchronization
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

        // Sync to Superset
        String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
        self.updateDashboardRoleForUser(userId, dashboardDtos, supersetInstanceName);
        log.info("Synchronized {} dashboards for user {} in context {} (direct: {}, from groups: {})",
                dashboardDtos.size(), userId, contextKey, directDashboardIds.size(), groupDashboardIds.size());
    }

    /**
     * Merges direct dashboard selections with dashboards from groups
     * Returns a combined map ready for Superset synchronization
     */
    public Map<String, List<DashboardForUserDto>> mergeDashboardSelectionsWithGroups(UUID userId, Map<String, List<DashboardForUserDto>> directSelections) {
        Map<String, List<DashboardForUserDto>> result = initializeResultWithDirectSelections(directSelections);

        // Get all context keys from user's roles that are eligible for dashboard groups
        String userIdStr = userId.toString();
        List<HdContextEntity> allDataDomains = contextRepository.findAllByTypeIn(List.of(HdContextType.DATA_DOMAIN));

        for (HdContextEntity domain : allDataDomains) {
            processDomainDashboardGroups(userIdStr, domain.getContextKey(), result);
        }

        return result;
    }

    private Map<String, List<DashboardForUserDto>> initializeResultWithDirectSelections(Map<String, List<DashboardForUserDto>> directSelections) {
        Map<String, List<DashboardForUserDto>> result = new HashMap<>();
        if (directSelections != null) {
            for (Map.Entry<String, List<DashboardForUserDto>> entry : directSelections.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return result;
    }

    private void processDomainDashboardGroups(String userIdStr, String contextKey, Map<String, List<DashboardForUserDto>> result) {
        // Get dashboards from groups for this user in this context
        Map<Integer, DashboardForUserDto> groupDashboards = collectGroupDashboardsForUser(userIdStr, contextKey);

        if (groupDashboards.isEmpty()) {
            return;
        }

        List<DashboardForUserDto> contextDashboards = result.computeIfAbsent(contextKey, k -> new ArrayList<>());
        mergeGroupDashboardsIntoContext(contextDashboards, groupDashboards);
    }

    private Map<Integer, DashboardForUserDto> collectGroupDashboardsForUser(String userIdStr, String contextKey) {
        Map<Integer, DashboardForUserDto> dashboardIdToDto = new HashMap<>();
        List<DashboardGroupEntity> groups = dashboardGroupService.findAllGroupsByContextKey(contextKey);

        for (DashboardGroupEntity group : groups) {
            boolean isMember = group.getUsers() != null && group.getUsers().stream().anyMatch(u -> userIdStr.equals(u.getId()));
            if (isMember && group.getEntries() != null) {
                for (var entry : group.getEntries()) {
                    dashboardIdToDto.putIfAbsent(entry.getDashboardId(), createDashboardForUserDto(entry, contextKey));
                }
            }
        }
        return dashboardIdToDto;
    }

    private DashboardForUserDto createDashboardForUserDto(DashboardGroupEntry entry, String contextKey) {
        DashboardForUserDto dto = new DashboardForUserDto();
        dto.setId(entry.getDashboardId());
        dto.setTitle(entry.getDashboardTitle());
        dto.setInstanceName(entry.getInstanceName());
        dto.setViewer(true);
        dto.setContextKey(contextKey);
        dto.setCompositeId(entry.getInstanceName() + "_" + entry.getDashboardId());
        return dto;
    }

    private void mergeGroupDashboardsIntoContext(List<DashboardForUserDto> contextDashboards, Map<Integer, DashboardForUserDto> groupDashboards) {
        Set<Integer> existingIds = new HashSet<>();
        for (DashboardForUserDto dto : contextDashboards) {
            if (groupDashboards.containsKey(dto.getId())) {
                dto.setViewer(true);
            }
            existingIds.add(dto.getId());
        }

        for (Map.Entry<Integer, DashboardForUserDto> groupEntry : groupDashboards.entrySet()) {
            if (!existingIds.contains(groupEntry.getKey())) {
                contextDashboards.add(groupEntry.getValue());
            }
        }
    }

    /**
     * Synchronizes the provided dashboards with Superset.
     *
     * @param userId                     the user ID
     * @param selectedDashboardsForUser mapping of context key to list of dashboard DTOs
     */
    @Transactional
    public void synchronizeDashboardsForUser(UUID userId, Map<String, List<DashboardForUserDto>> selectedDashboardsForUser) {
        for (Map.Entry<String, List<DashboardForUserDto>> entry : selectedDashboardsForUser.entrySet()) {
            String contextKey = entry.getKey();
            String supersetInstanceName = metaInfoResourceService.findSupersetInstanceNameByContextKey(contextKey);
            self.updateDashboardRoleForUser(userId, entry.getValue(), supersetInstanceName);
        }
    }

    private void updateDashboardRoleForUser(UUID userId, List<DashboardForUserDto> dashboardForUserDtoList, String supersetInstanceName) {
        try {
            SupersetDashboardsForUserUpdate supersetDashboardsForUserUpdate = new SupersetDashboardsForUserUpdate();
            UserEntity userEntity = getUserEntity(userId);
            supersetDashboardsForUserUpdate.setSupersetUserName(userEntity.getEmail());
            supersetDashboardsForUserUpdate.setSupersetUserEmail(userEntity.getEmail());
            supersetDashboardsForUserUpdate.setDashboards(dashboardForUserDtoList);
            supersetDashboardsForUserUpdate.setSupersetFirstName(userEntity.getFirstName());
            supersetDashboardsForUserUpdate.setSupersetLastName(userEntity.getLastName());
            supersetDashboardsForUserUpdate.setActive(userEntity.isEnabled());
            String subject = SlugifyUtil.slugify(supersetInstanceName + RequestReplySubject.UPDATE_DASHBOARD_ROLES_FOR_USER.getSubject());
            log.info("[updateDashboardRoleForUser] Sending request to subject: {}", subject);
            Message reply =
                    connection.request(subject, objectMapper.writeValueAsString(supersetDashboardsForUserUpdate).getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(10));
            if (reply == null) {
                log.warn("Reply is null, please verify superset sidecar or nats connection");
            } else {
                reply.ack();
                log.info("[updateDashboardRoleForUser] Response received: {}", new String(reply.getData()));
            }
        } catch (Exception e) {
            log.error("Error updating dashboard role for user", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Re-interrupt the thread
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating user", e);
        }
    }

    private UserEntity getUserEntity(UUID userId) {
        UserEntity userEntity = userRepository.getByIdOrAuthId(userId.toString());
        if (userEntity == null) {
            throw new NotFoundException(String.format("User %s not found in the DB", userId));
        }
        return userEntity;
    }
}
