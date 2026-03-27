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

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.request.DashboardForUserDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentPermissionDto;
import ch.bedag.dap.hellodata.portal.dashboard_group.repository.DashboardGroupRepository;
import ch.bedag.dap.hellodata.portal.dashboard_group.service.DashboardGroupService;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portal.role.data.RoleDto;
import ch.bedag.dap.hellodata.portal.role.service.RoleService;
import ch.bedag.dap.hellodata.portal.user.data.BulkAssignmentRequestDto;
import ch.bedag.dap.hellodata.portal.user.data.BulkAssignmentResultDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextDto;
import ch.bedag.dap.hellodata.portal.user.data.ContextsDto;
import ch.bedag.dap.hellodata.portal.user.data.DashboardGroupMembershipDto;
import ch.bedag.dap.hellodata.portal.user.data.UpdateContextRolesForUserDto;
import ch.bedag.dap.hellodata.portal.user.data.UserContextRoleDto;
import ch.bedag.dap.hellodata.portal.user.data.UserDto;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class BulkAssignmentService {

    private static final Map<HdRoleName, boolean[]> ROLE_COMMENT_PERMISSIONS = Map.of(
            HdRoleName.DATA_DOMAIN_ADMIN, new boolean[]{true, true, true},
            HdRoleName.DATA_DOMAIN_EDITOR, new boolean[]{true, true, false},
            HdRoleName.DATA_DOMAIN_VIEWER, new boolean[]{true, false, false},
            HdRoleName.DATA_DOMAIN_BUSINESS_SPECIALIST, new boolean[]{true, false, false},
            HdRoleName.NONE, new boolean[]{false, false, false}
    );

    private final UserService userService;
    private final RoleService roleService;
    private final DashboardGroupRepository dashboardGroupRepository;
    private final DashboardGroupService dashboardGroupService;
    private final UserSelectedDashboardService userSelectedDashboardService;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public BulkAssignmentResultDto executeBulkAssignment(BulkAssignmentRequestDto request) {
        BulkAssignmentResultDto result = new BulkAssignmentResultDto();

        List<RoleDto> allRoles = roleService.getAll();
        ContextsDto availableContexts = userService.getAvailableContexts();
        Map<String, ContextDto> contextsByKey = availableContexts.getContexts().stream()
                .collect(Collectors.toMap(ContextDto::getContextKey, c -> c, (a, b) -> a));

        Map<String, BulkAssignmentRequestDto.DomainAssignment> assignmentsByKey = request.getDomainAssignments().stream()
                .collect(Collectors.toMap(BulkAssignmentRequestDto.DomainAssignment::getContextKey, a -> a, (a, b) -> a));

        validateReferencedGroupsExist(assignmentsByKey);

        List<UUID> userIds = request.getUserIds();
        for (int i = 0; i < userIds.size(); i++) {
            UUID userId = userIds.get(i);
            boolean isLast = (i == userIds.size() - 1);
            UserDto user = userService.getUserById(userId.toString());
            processUser(userId, user, assignmentsByKey, contextsByKey, allRoles, isLast, result);
        }

        sendAuditNotification(request, result);

        return result;
    }

    private void processUser(UUID userId,
                             UserDto user,
                             Map<String, BulkAssignmentRequestDto.DomainAssignment> assignmentsByKey,
                             Map<String, ContextDto> contextsByKey,
                             List<RoleDto> allRoles,
                             boolean isLastUser,
                             BulkAssignmentResultDto result) {
        String email = user != null ? user.getEmail() : userId.toString();
        String firstName = user != null ? user.getFirstName() : "";
        String lastName = user != null ? user.getLastName() : "";
        try {
            List<UserContextRoleDto> existingRoles = userService.getContextRolesForUser(userId);
            Map<String, List<String>> existingGroupIds = loadExistingDashboardGroupIds(userId, contextsByKey.keySet());
            Map<String, Set<Integer>> existingDashboardIds = loadExistingDashboardIds(userId, contextsByKey.keySet());

            if (isAlreadyUpToDate(existingRoles, assignmentsByKey, existingGroupIds, existingDashboardIds)) {
                log.debug("Skipping user {} — assignments already match", userId);
                result.addSkipped(email, firstName, lastName, "Assignments already match");
                return;
            }

            UpdateContextRolesForUserDto updateDto = buildUpdateDto(
                    existingRoles, assignmentsByKey, contextsByKey, allRoles, existingGroupIds);

            userService.updateContextRolesForUser(userId, updateDto, isLastUser);
            result.addUpdated(email, firstName, lastName);
        } catch (Exception e) {
            log.error("Failed to process bulk assignment for user {}", userId, e);
            result.addFailed(email, firstName, lastName, e.getMessage());
        }
    }

    private boolean isAlreadyUpToDate(List<UserContextRoleDto> existingRoles,
                                      Map<String, BulkAssignmentRequestDto.DomainAssignment> assignmentsByKey,
                                      Map<String, List<String>> existingGroupIds,
                                      Map<String, Set<Integer>> existingDashboardIds) {
        for (var entry : assignmentsByKey.entrySet()) {
            String contextKey = entry.getKey();
            BulkAssignmentRequestDto.DomainAssignment assignment = entry.getValue();

            // Check role match
            String existingRoleName = existingRoles.stream()
                    .filter(r -> r.getContext() != null && contextKey.equalsIgnoreCase(r.getContext().getContextKey()))
                    .map(r -> r.getRole().getName())
                    .findFirst()
                    .orElse(HdRoleName.NONE.name());

            if (!existingRoleName.equalsIgnoreCase(assignment.getRoleName())) {
                return false;
            }

            // Check dashboard group membership match
            List<String> existingGroups = existingGroupIds.getOrDefault(contextKey, List.of());
            List<String> requestedGroups = assignment.getDashboardGroupIds() != null
                    ? assignment.getDashboardGroupIds() : List.of();
            if (!Set.copyOf(existingGroups).equals(Set.copyOf(requestedGroups))) {
                return false;
            }

            // Check individual dashboard selection match
            Set<Integer> existingDashboards = existingDashboardIds.getOrDefault(contextKey, Set.of());
            Set<Integer> requestedDashboards = assignment.getDashboards() != null
                    ? assignment.getDashboards().stream()
                        .map(BulkAssignmentRequestDto.DashboardInfo::getId)
                        .collect(Collectors.toSet())
                    : Set.of();
            if (!existingDashboards.equals(requestedDashboards)) {
                return false;
            }
        }
        return true;
    }

    UpdateContextRolesForUserDto buildUpdateDto(
            List<UserContextRoleDto> existingRoles,
            Map<String, BulkAssignmentRequestDto.DomainAssignment> assignmentsByKey,
            Map<String, ContextDto> contextsByKey,
            List<RoleDto> allRoles,
            Map<String, List<String>> existingGroupIds) {

        UpdateContextRolesForUserDto dto = new UpdateContextRolesForUserDto();

        // Business domain role: keep existing
        RoleDto businessRole = findExistingBusinessRole(existingRoles, allRoles);
        dto.setBusinessDomainRole(businessRole);

        // Build data domain roles: merge new assignments with existing state
        List<UserContextRoleDto> dataDomainRoles = new ArrayList<>();
        Map<String, List<DashboardForUserDto>> selectedDashboards = new HashMap<>();
        Map<String, List<String>> selectedGroupIds = new HashMap<>();
        List<DashboardCommentPermissionDto> commentPermissions = new ArrayList<>();

        List<ContextDto> dataDomainContexts = contextsByKey.values().stream()
                .filter(c -> c.getType() == HdContextType.DATA_DOMAIN)
                .toList();

        for (ContextDto context : dataDomainContexts) {
            String contextKey = context.getContextKey();
            BulkAssignmentRequestDto.DomainAssignment assignment = assignmentsByKey.get(contextKey);

            UserContextRoleDto roleDto = new UserContextRoleDto();
            roleDto.setContext(context);

            if (assignment != null) {
                // This domain is part of the bulk assignment — use the new role
                RoleDto role = findRoleByName(allRoles, assignment.getRoleName());
                roleDto.setRole(role);
                buildDashboardSelections(assignment, contextKey, selectedDashboards);
                // Ensure context key always has an entry — empty list clears old selections
                selectedDashboards.putIfAbsent(contextKey, List.of());
                selectedGroupIds.put(contextKey, assignment.getDashboardGroupIds() != null
                        ? assignment.getDashboardGroupIds() : List.of());
                commentPermissions.add(buildCommentPermission(contextKey, role));
            } else {
                // This domain is NOT in the bulk assignment — preserve existing state
                RoleDto existingRole = findExistingDataDomainRole(existingRoles, contextKey, allRoles);
                roleDto.setRole(existingRole);
                selectedGroupIds.put(contextKey, existingGroupIds.getOrDefault(contextKey, List.of()));
                commentPermissions.add(buildCommentPermission(contextKey, existingRole));
            }

            dataDomainRoles.add(roleDto);
        }

        dto.setDataDomainRoles(dataDomainRoles);
        dto.setSelectedDashboardsForUser(selectedDashboards);
        dto.setSelectedDashboardGroupIdsForUser(selectedGroupIds);
        dto.setCommentPermissions(commentPermissions);

        return dto;
    }

    private RoleDto findExistingBusinessRole(List<UserContextRoleDto> existingRoles, List<RoleDto> allRoles) {
        return existingRoles.stream()
                .filter(r -> r.getContext() != null && r.getContext().getType() == HdContextType.BUSINESS_DOMAIN)
                .map(UserContextRoleDto::getRole)
                .findFirst()
                .orElseGet(() -> findRoleByName(allRoles, HdRoleName.NONE.name()));
    }

    private RoleDto findExistingDataDomainRole(List<UserContextRoleDto> existingRoles, String contextKey, List<RoleDto> allRoles) {
        return existingRoles.stream()
                .filter(r -> r.getContext() != null && contextKey.equalsIgnoreCase(r.getContext().getContextKey()))
                .map(UserContextRoleDto::getRole)
                .findFirst()
                .orElseGet(() -> findRoleByName(allRoles, HdRoleName.NONE.name()));
    }

    private RoleDto findRoleByName(List<RoleDto> allRoles, String roleName) {
        return allRoles.stream()
                .filter(r -> r.getName().equalsIgnoreCase(roleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
    }

    private void buildDashboardSelections(BulkAssignmentRequestDto.DomainAssignment assignment,
                                          String contextKey,
                                          Map<String, List<DashboardForUserDto>> selectedDashboards) {
        if (assignment.getDashboards() != null && !assignment.getDashboards().isEmpty()) {
            long skippedCount = assignment.getDashboards().stream()
                    .filter(info -> info.getTitle() == null || info.getTitle().isBlank())
                    .count();
            if (skippedCount > 0) {
                log.warn("Skipped {} dashboard(s) with null/blank title in context '{}'", skippedCount, contextKey);
            }
            List<DashboardForUserDto> dashboards = assignment.getDashboards().stream()
                    .filter(info -> info.getTitle() != null && !info.getTitle().isBlank())
                    .map(info -> {
                        DashboardForUserDto d = new DashboardForUserDto();
                        d.setId(info.getId());
                        d.setTitle(info.getTitle());
                        d.setInstanceName(info.getInstanceName());
                        d.setViewer(true);
                        d.setContextKey(contextKey);
                        return d;
                    })
                    .toList();
            if (!dashboards.isEmpty()) {
                selectedDashboards.put(contextKey, dashboards);
            }
        }
    }

    private DashboardCommentPermissionDto buildCommentPermission(String contextKey, RoleDto role) {
        DashboardCommentPermissionDto perm = new DashboardCommentPermissionDto();
        perm.setContextKey(contextKey);
        HdRoleName roleName;
        try {
            roleName = HdRoleName.valueOf(role.getName());
        } catch (IllegalArgumentException e) {
            roleName = HdRoleName.NONE;
        }
        boolean[] perms = ROLE_COMMENT_PERMISSIONS.getOrDefault(roleName, new boolean[]{false, false, false});
        perm.setReadComments(perms[0]);
        perm.setWriteComments(perms[1]);
        perm.setReviewComments(perms[2]);
        return perm;
    }

    private void sendAuditNotification(BulkAssignmentRequestDto request, BulkAssignmentResultDto result) {
        try {
            String performedBy = SecurityUtils.getCurrentUserFullName();
            List<String> adminEmails = userService.findHelloDataAdminUsers().stream()
                    .map(UserEntity::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
            emailNotificationService.notifyAdminsAboutBulkAssignment(performedBy, request, result, adminEmails);
        } catch (Exception e) {
            log.error("Failed to send bulk assignment audit notification", e);
        }
    }

    private Map<String, Set<Integer>> loadExistingDashboardIds(UUID userId, Set<String> contextKeys) {
        Map<String, Set<Integer>> result = new HashMap<>();
        for (String contextKey : contextKeys) {
            Set<Integer> dashboardIds = userSelectedDashboardService.getSelectedDashboardIds(userId, contextKey);
            if (!dashboardIds.isEmpty()) {
                result.put(contextKey, dashboardIds);
            }
        }
        return result;
    }

    private Map<String, List<String>> loadExistingDashboardGroupIds(UUID userId, Set<String> contextKeys) {
        Map<String, List<String>> result = new HashMap<>();
        for (String contextKey : contextKeys) {
            List<DashboardGroupMembershipDto> memberships = dashboardGroupService.getDashboardGroupMembership(userId, contextKey);
            List<String> memberGroupIds = memberships.stream()
                    .filter(DashboardGroupMembershipDto::isMember)
                    .map(DashboardGroupMembershipDto::getGroupId)
                    .toList();
            if (!memberGroupIds.isEmpty()) {
                result.put(contextKey, memberGroupIds);
            }
        }
        return result;
    }

    private void validateReferencedGroupsExist(Map<String, BulkAssignmentRequestDto.DomainAssignment> assignmentsByKey) {
        List<String> missingGroups = new ArrayList<>();
        for (var entry : assignmentsByKey.entrySet()) {
            BulkAssignmentRequestDto.DomainAssignment assignment = entry.getValue();
            if (assignment.getDashboardGroupIds() != null) {
                for (String groupId : assignment.getDashboardGroupIds()) {
                    try {
                        if (!dashboardGroupRepository.existsById(UUID.fromString(groupId))) {
                            missingGroups.add(groupId);
                        }
                    } catch (IllegalArgumentException e) {
                        missingGroups.add(groupId);
                    }
                }
            }
        }
        if (!missingGroups.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The following dashboard groups no longer exist: " + String.join(", ", missingGroups) +
                    ". Please go back and update your selection.");
        }
    }
}
