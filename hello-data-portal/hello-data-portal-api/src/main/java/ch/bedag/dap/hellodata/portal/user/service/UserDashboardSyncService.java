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
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserDashboardSyncService {

    private final HdContextRepository contextRepository;
    private final DashboardGroupService dashboardGroupServiceProvider;

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
        List<DashboardGroupEntity> groups = dashboardGroupServiceProvider.findAllGroupsByContextKey(contextKey);

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
}
