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
package ch.bedag.dap.hellodata.portal.dashboard_group.service;

import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupCreateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDomainUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.data.DashboardGroupUpdateDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntity;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.entity.DashboardGroupUserEntry;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import ch.bedag.dap.hellodata.portal.user.data.DashboardGroupMembershipDto;
import ch.bedag.dap.hellodata.portal.user.event.UserDashboardSyncEvent;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.role.repository.UserContextRoleRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardGroupService {
    private final DashboardGroupRepository dashboardGroupRepository;
    private final UserContextRoleRepository userContextRoleRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<DashboardGroupDto> getAllDashboardGroups(String contextKey, Pageable pageable, String search) {
        Page<DashboardGroupEntity> page;
        if (search != null && !search.isBlank()) {
            page = dashboardGroupRepository.searchByContextKeyAndNameOrDashboardTitle(contextKey, search, pageable);
        } else {
            page = dashboardGroupRepository.findAllByContextKey(contextKey, pageable);
        }
        return page.map(entity -> modelMapper.map(entity, DashboardGroupDto.class));
    }

    @Transactional(readOnly = true)
    public DashboardGroupDto getDashboardGroupById(UUID id) {
        Optional<DashboardGroupEntity> entity = dashboardGroupRepository.findById(id);
        if (entity.isEmpty()) {
            log.error("Dashboard group with id {} not found", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return modelMapper.map(entity.get(), DashboardGroupDto.class);
    }

    @Transactional
    public void create(DashboardGroupCreateDto createDto) {
        if (dashboardGroupRepository.existsByNameIgnoreCaseAndContextKey(createDto.getName(), createDto.getContextKey())) {
            log.error("Dashboard group with name '{}' already exists in context '{}'", createDto.getName(), createDto.getContextKey());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dashboard group with this name already exists");
        }
        DashboardGroupEntity entity = modelMapper.map(createDto, DashboardGroupEntity.class);
        dashboardGroupRepository.save(entity);
    }

    @Transactional
    public void update(DashboardGroupUpdateDto updateDto) {
        Optional<DashboardGroupEntity> entity = dashboardGroupRepository.findById(updateDto.getId());
        if (entity.isEmpty()) {
            log.error("Dashboard group with id {} not found", updateDto.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (dashboardGroupRepository.existsByNameIgnoreCaseAndContextKeyAndIdNot(updateDto.getName(), updateDto.getContextKey(), updateDto.getId())) {
            log.error("Dashboard group with name '{}' already exists in context '{}'", updateDto.getName(), updateDto.getContextKey());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dashboard group with this name already exists");
        }
        DashboardGroupEntity entityToUpdate = entity.get();

        // Find users whose membership changed
        Set<String> oldUserIds = entityToUpdate.getUsers() != null
                ? entityToUpdate.getUsers().stream().map(DashboardGroupUserEntry::getId).collect(Collectors.toSet())
                : Collections.emptySet();
        Set<String> newUserIds = updateDto.getUsers() != null
                ? updateDto.getUsers().stream().map(DashboardGroupUserEntry::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        // Users added or removed from group need sync
        Set<String> usersToSync = new HashSet<>();
        usersToSync.addAll(oldUserIds);
        usersToSync.addAll(newUserIds);
        // Remove users that didn't change (were in both old and new)
        Set<String> unchangedUsers = new HashSet<>(oldUserIds);
        unchangedUsers.retainAll(newUserIds);
        usersToSync.removeAll(unchangedUsers);

        // Also check if dashboards changed - if so, sync all current users
        boolean dashboardsChanged = haveDashboardsChanged(entityToUpdate.getEntries(), updateDto.getEntries());
        if (dashboardsChanged) {
            usersToSync.addAll(newUserIds);
        }

        modelMapper.map(updateDto, entityToUpdate);
        dashboardGroupRepository.save(entityToUpdate);

        // Sync affected users to Superset
        String contextKey = updateDto.getContextKey();
        for (String userId : usersToSync) {
            eventPublisher.publishEvent(new UserDashboardSyncEvent(UUID.fromString(userId), contextKey));
        }
    }

    private boolean haveDashboardsChanged(List<DashboardGroupEntry> oldEntries, List<DashboardGroupEntry> newEntries) {
        Set<Integer> oldIds = oldEntries != null
                ? oldEntries.stream().map(DashboardGroupEntry::getDashboardId).collect(Collectors.toSet())
                : Collections.emptySet();
        Set<Integer> newIds = newEntries != null
                ? newEntries.stream().map(DashboardGroupEntry::getDashboardId).collect(Collectors.toSet())
                : Collections.emptySet();
        return !oldIds.equals(newIds);
    }

    @Transactional
    public void delete(UUID id) {
        // Get group before deletion to sync users
        Optional<DashboardGroupEntity> entity = dashboardGroupRepository.findById(id);
        if (entity.isPresent()) {
            DashboardGroupEntity group = entity.get();
            String contextKey = group.getContextKey();
            Set<String> userIds = group.getUsers() != null
                    ? group.getUsers().stream().map(DashboardGroupUserEntry::getId).collect(Collectors.toSet())
                    : Collections.emptySet();

            dashboardGroupRepository.deleteById(id);

            // Sync affected users to Superset after deletion
            for (String userId : userIds) {
                eventPublisher.publishEvent(new UserDashboardSyncEvent(UUID.fromString(userId), contextKey));
            }
        } else {
            dashboardGroupRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardGroupMembershipDto> getDashboardGroupMembership(UUID userId, String contextKey) {
        List<DashboardGroupEntity> groups = findAllGroupsByContextKeyInternal(contextKey);
        String userIdStr = userId.toString();
        return groups.stream().map(group -> {
            boolean isMember = group.getUsers() != null &&
                    group.getUsers().stream().anyMatch(u -> userIdStr.equals(u.getId()));
            List<String> dashboardTitles = group.getEntries() != null
                    ? group.getEntries().stream().map(DashboardGroupEntry::getDashboardTitle).toList()
                    : Collections.emptyList();
            return new DashboardGroupMembershipDto(group.getId().toString(), group.getName(), isMember, dashboardTitles);
        }).toList();
    }

    /**
     * Updates dashboard group memberships for a user based on selected group IDs
     */
    @Transactional
    public void updateDashboardGroupMemberships(UUID userId, Map<String, List<String>> selectedDashboardGroupIdsForUser) {
        if (selectedDashboardGroupIdsForUser == null) {
            return;
        }
        UserEntity userEntity = userRepository.getByIdOrAuthId(userId.toString());
        if (userEntity == null) {
            log.error("User with id {} not found", userId);
            return;
        }
        String userIdStr = userId.toString();
        DashboardGroupUserEntry userEntry = createDashboardGroupUserEntry(userIdStr, userEntity);

        for (Map.Entry<String, List<String>> entry : selectedDashboardGroupIdsForUser.entrySet()) {
            processContextGroupMemberships(userIdStr, userEntry, entry.getKey(), entry.getValue());
        }
    }

    private DashboardGroupUserEntry createDashboardGroupUserEntry(String userIdStr, UserEntity userEntity) {
        DashboardGroupUserEntry userEntry = new DashboardGroupUserEntry();
        userEntry.setId(userIdStr);
        userEntry.setEmail(userEntity.getEmail());
        userEntry.setFirstName(userEntity.getFirstName());
        userEntry.setLastName(userEntity.getLastName());
        return userEntry;
    }

    private void processContextGroupMemberships(String userIdStr, DashboardGroupUserEntry userEntry, String contextKey, List<String> selectedGroupIdsList) {
        Set<String> selectedGroupIds = new HashSet<>(selectedGroupIdsList != null ? selectedGroupIdsList : Collections.emptyList());
        List<DashboardGroupEntity> allGroupsInContext = findAllGroupsByContextKeyInternal(contextKey);
        for (DashboardGroupEntity group : allGroupsInContext) {
            updateUserMembershipInGroup(userIdStr, userEntry, group, selectedGroupIds, contextKey);
        }
    }

    private void updateUserMembershipInGroup(String userIdStr, DashboardGroupUserEntry userEntry, DashboardGroupEntity group, Set<String> selectedGroupIds, String contextKey) {
        String groupId = group.getId().toString();
        boolean shouldBeMember = selectedGroupIds.contains(groupId);
        boolean isCurrentlyMember = group.getUsers() != null &&
                group.getUsers().stream().anyMatch(u -> userIdStr.equals(u.getId()));

        if (shouldBeMember && !isCurrentlyMember) {
            addUserToGroup(userIdStr, userEntry, group, contextKey);
        } else if (!shouldBeMember && isCurrentlyMember) {
            removeUserFromGroup(userIdStr, group, contextKey);
        }
    }

    private void addUserToGroup(String userIdStr, DashboardGroupUserEntry userEntry, DashboardGroupEntity group, String contextKey) {
        if (group.getUsers() == null) {
            group.setUsers(new ArrayList<>());
        }
        group.getUsers().add(userEntry);
        saveGroupInternal(group);
        log.info("Added user {} to dashboard group '{}' in context '{}'", userIdStr, group.getName(), contextKey);
    }

    private void removeUserFromGroup(String userIdStr, DashboardGroupEntity group, String contextKey) {
        group.getUsers().removeIf(u -> userIdStr.equals(u.getId()));
        saveGroupInternal(group);
        log.info("Removed user {} from dashboard group '{}' in context '{}'", userIdStr, group.getName(), contextKey);
    }

    @Transactional(readOnly = true)
    public List<DashboardGroupDomainUserDto> getEligibleUsersForDomain(String contextKey) {
        List<String> roleNames = List.of("DATA_DOMAIN_VIEWER", "DATA_DOMAIN_BUSINESS_SPECIALIST");
        List<UserContextRoleEntity> userContextRoles = userContextRoleRepository.findByContextKeyAndRoleNames(contextKey, roleNames);
        return userContextRoles.stream()
                .map(ucr -> new DashboardGroupDomainUserDto(
                        ucr.getUser().getId().toString(),
                        ucr.getUser().getEmail(),
                        ucr.getUser().getFirstName(),
                        ucr.getUser().getLastName(),
                        ucr.getRole().getName().name()
                ))
                .toList();
    }

    /**
     * Removes a user from all dashboard groups in a specific data domain.
     * This should be called when a user's role changes to something other than
     * DATA_DOMAIN_VIEWER or DATA_DOMAIN_BUSINESS_SPECIALIST.
     *
     * @param userId     the ID of the user to remove
     * @param contextKey the context key (data domain) from which to remove the user
     */
    @Transactional
    public void removeUserFromDashboardGroupsInDomain(String userId, String contextKey) {
        List<DashboardGroupEntity> groups = dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId);
        for (DashboardGroupEntity group : groups) {
            if (group.getUsers() != null) {
                group.getUsers().removeIf(user -> userId.equals(user.getId()));
                dashboardGroupRepository.save(group);
                log.info("Removed user {} from dashboard group '{}' in domain '{}'", userId, group.getName(), contextKey);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardGroupEntity> findAllGroupsByContextKey(String contextKey) {
        return findAllGroupsByContextKeyInternal(contextKey);
    }

    @Transactional(readOnly = true)
    public List<DashboardGroupEntity> findGroupsByContextKeyAndUserId(String contextKey, String userId) {
        return dashboardGroupRepository.findByContextKeyAndUserId(contextKey, userId);
    }

    @Transactional
    public void saveGroup(DashboardGroupEntity group) {
        saveGroupInternal(group);
    }

    @Transactional
    public void cleanupStaleDashboardsInGroups(String contextKey, Set<Integer> validDashboardIds) {
        List<DashboardGroupEntity> groups = dashboardGroupRepository.findAllByContextKey(contextKey);
        for (DashboardGroupEntity group : groups) {
            if (group.getEntries() != null) {
                int originalSize = group.getEntries().size();
                group.getEntries().removeIf(entry -> !validDashboardIds.contains(entry.getDashboardId()));
                if (group.getEntries().size() < originalSize) {
                    dashboardGroupRepository.save(group);
                    log.info("Cleaned up {} stale dashboard entries from group '{}' in context '{}'",
                            originalSize - group.getEntries().size(), group.getName(), contextKey);
                }
            }
        }
    }

    // ===== Internal methods (no @Transactional - use parent transaction) =====

    private List<DashboardGroupEntity> findAllGroupsByContextKeyInternal(String contextKey) {
        return dashboardGroupRepository.findAllByContextKey(contextKey);
    }

    private void saveGroupInternal(DashboardGroupEntity group) {
        dashboardGroupRepository.save(group);
    }
}
