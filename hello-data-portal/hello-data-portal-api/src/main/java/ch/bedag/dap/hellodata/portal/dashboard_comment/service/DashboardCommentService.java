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
package ch.bedag.dap.hellodata.portal.dashboard_comment.service;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.*;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentTagEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentVersionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.mapper.DashboardCommentMapper;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentRepository;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing dashboard comments with database persistence.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class DashboardCommentService {

    private static final String COMMENT_NOT_FOUND_ERROR = "DashboardCommentEntity not found";

    private final UserRepository userRepository;
    private final DashboardCommentRepository commentRepository;
    private final DashboardCommentMapper commentMapper;
    private final MetaInfoResourceService metaInfoResourceService;

    /**
     * Get all comments for a dashboard. Filters based on user permissions.
     * - All users can see published comments
     * - Users can see their own drafts
     * - Admins (superuser, business_domain_admin, data_domain_admin) can see all drafts
     * - If active version is a draft by someone else, non-admins see the last published version instead
     */
    @Transactional(readOnly = true)
    public List<DashboardCommentDto> getComments(String contextKey, int dashboardId) {
        List<DashboardCommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);

        // Convert to DTOs, filter and transform comments based on user permissions
        return comments.stream()
                .map(commentMapper::toDto)
                .filter(c -> !c.isDeleted())
                .map(c -> filterCommentByPermissions(c, currentUserEmail, currentUserFullName, isAdmin))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Filter a single comment based on user permissions.
     * Returns the comment if visible, or null if should be hidden.
     */
    private DashboardCommentDto filterCommentByPermissions(DashboardCommentDto comment, String userEmail, String userFullName, boolean isAdmin) {
        DashboardCommentVersionDto activeVersion = getActiveVersion(comment);
        if (activeVersion == null || activeVersion.isDeleted()) {
            return null;
        }

        // Published comments are visible to everyone
        if (activeVersion.getStatus() == DashboardCommentStatus.PUBLISHED) {
            return comment;
        }

        // Handle draft comments
        if (activeVersion.getStatus() == DashboardCommentStatus.DRAFT) {
            return handleDraftComment(comment, activeVersion, userEmail, userFullName, isAdmin);
        }

        return null;
    }

    /**
     * Handle visibility logic for draft comments.
     */
    private DashboardCommentDto handleDraftComment(DashboardCommentDto comment, DashboardCommentVersionDto activeVersion,
                                                   String userEmail, String userFullName, boolean isAdmin) {
        // Admins can see all drafts
        if (isAdmin) {
            return comment;
        }

        // Check if this is user's own draft
        if (isOwnDraft(comment, activeVersion, userEmail, userFullName)) {
            return comment;
        }

        // For drafts by others, show the last published version if it exists
        return showLastPublishedVersion(comment);
    }

    /**
     * Check if the draft belongs to the current user.
     */
    private boolean isOwnDraft(DashboardCommentDto comment, DashboardCommentVersionDto activeVersion, String userEmail, String userFullName) {
        return (userEmail != null && userEmail.equals(comment.getAuthorEmail())) ||
                (userFullName != null && userFullName.equals(activeVersion.getEditedBy()));
    }

    /**
     * Create a copy of the comment showing the last published version, or null if none exists.
     */
    private DashboardCommentDto showLastPublishedVersion(DashboardCommentDto comment) {
        DashboardCommentVersionDto lastPublishedVersion = findLastPublishedVersion(comment);
        if (lastPublishedVersion != null) {
            return comment.toBuilder()
                    .activeVersion(lastPublishedVersion.getVersion())
                    .build();
        }
        return null;
    }

    /**
     * Check if current user has admin privileges (business_domain_admin or data_domain_admin for specific context).
     * Fetches UserEntity once to check both roles.
     */
    private boolean isAdminForContext(String userEmail, String contextKey) {
        if (userEmail == null) return false;

        return userRepository.findUserEntityByEmailIgnoreCase(userEmail)
                .map(user -> {
                    // Check if user is business domain admin
                    if (Boolean.TRUE.equals(user.isBusinessDomainAdmin())) {
                        return true;
                    }

                    // Check if user is data domain admin for this specific context
                    Set<UserContextRoleEntity> contextRoles = user.getContextRoles();
                    if (contextRoles == null || contextKey == null) return false;

                    return contextRoles.stream()
                            .anyMatch(role ->
                                    role.getContextKey() != null &&
                                            contextKey.equals(role.getContextKey()) &&
                                            role.getRole() != null &&
                                            HdRoleName.DATA_DOMAIN_ADMIN.equals(role.getRole().getName())
                            );
                })
                .orElse(false);
    }

    /**
     * Create a new comment.
     */
    public DashboardCommentDto createComment(String contextKey, int dashboardId, DashboardCommentCreateDto createDto) {
        String authorFullName = SecurityUtils.getCurrentUserFullName();
        String authorEmail = SecurityUtils.getCurrentUserEmail();
        long now = System.currentTimeMillis();

        // Create comment entity
        DashboardCommentEntity comment = DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(dashboardId)
                .dashboardUrl(createDto.getDashboardUrl())
                .contextKey(contextKey)
                .pointerUrl(createDto.getPointerUrl())
                .author(authorFullName)
                .authorEmail(authorEmail)
                .createdDate(now)
                .deleted(false)
                .activeVersion(1)
                .hasActiveDraft(false)
                .build();

        // Create first version
        DashboardCommentVersionEntity version = DashboardCommentVersionEntity.builder()
                .version(1)
                .text(createDto.getText())
                .status(DashboardCommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(authorFullName)
                .deleted(false)
                .build();

        comment.addVersion(version);

        // Add tags if provided
        List<String> normalizedTags = validateAndNormalizeTags(createDto.getTags());
        for (String tagValue : normalizedTags) {
            DashboardCommentTagEntity tag = DashboardCommentTagEntity.builder()
                    .tag(tagValue)
                    .build();
            comment.addTag(tag);
        }

        // Save to database
        DashboardCommentEntity savedComment = commentRepository.save(comment);

        log.info("Created comment {} for dashboard {}/{}", savedComment.getId(), contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Update an existing comment (for DRAFT status).
     */
    public DashboardCommentDto updateComment(String contextKey, int dashboardId, String commentId, DashboardCommentUpdateDto updateDto) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or admin can update
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);
        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this comment");
        }

        // verify entityVersion matches
        checkEntityVersion(comment, updateDto.getEntityVersion(), currentUserEmail);

        // Update the active version in history
        String editorName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> {
                    v.setText(updateDto.getText());
                    v.setEditedDate(now);
                    v.setEditedBy(editorName);
                });

        if (updateDto.getPointerUrl() != null) {
            comment.setPointerUrl(updateDto.getPointerUrl());
        }

        // Update tags if provided
        if (updateDto.getTags() != null) {
            // Clear existing tags
            comment.getTags().clear();
            // Add new normalized tags
            List<String> normalizedTags = validateAndNormalizeTags(updateDto.getTags());
            for (String tagValue : normalizedTags) {
                DashboardCommentTagEntity tag = DashboardCommentTagEntity.builder()
                        .tag(tagValue)
                        .build();
                comment.addTag(tag);
            }
        }

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);

        log.info("Updated comment {} for dashboard {}/{}, new entityVersion: {}",
                commentId, contextKey, dashboardId, savedComment.getEntityVersion());
        return commentMapper.toDto(savedComment);
    }

    /**
     * Delete a comment (soft delete).
     */
    public DashboardCommentDto deleteComment(String contextKey, int dashboardId, String commentId) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or admin can delete
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);
        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }

        String deleterName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        // Mark current active version as deleted
        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> v.setDeleted(true));

        // Try to find last non-deleted PUBLISHED version
        Optional<DashboardCommentVersionEntity> lastPublished = comment.getHistory().stream()
                .filter(v -> v.getStatus() == DashboardCommentStatus.PUBLISHED && !v.isDeleted())
                .reduce((first, second) -> second); // Get last one

        if (lastPublished.isPresent()) {
            // Restore to last published version
            comment.setActiveVersion(lastPublished.get().getVersion());
            comment.setHasActiveDraft(false);
            log.info("Restored comment {} to version {} for dashboard {}/{}",
                    commentId, lastPublished.get().getVersion(), contextKey, dashboardId);
        } else {
            // No published versions left - soft delete entire comment
            comment.setDeleted(true);
            comment.setDeletedDate(now);
            comment.setDeletedBy(deleterName);
            log.info("Soft deleted comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        }

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Publish a comment (change status from DRAFT to PUBLISHED).
     */
    public DashboardCommentDto publishComment(String contextKey, int dashboardId, String commentId) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Only admins (superuser, business_domain_admin, data_domain_admin) can publish
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!SecurityUtils.isSuperuser() && !isAdminForContext(currentUserEmail, contextKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can publish comments");
        }

        String publisherName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(DashboardCommentStatus.PUBLISHED);
                    v.setPublishedDate(now);
                    v.setPublishedBy(publisherName);
                });

        comment.setHasActiveDraft(false);

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        log.info("Published comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Unpublish a comment (change status from PUBLISHED to DRAFT).
     */
    public DashboardCommentDto unpublishComment(String contextKey, int dashboardId, String commentId) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Only admins (superuser, business_domain_admin, data_domain_admin) can unpublish
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!SecurityUtils.isSuperuser() && !isAdminForContext(currentUserEmail, contextKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can unpublish comments");
        }

        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(DashboardCommentStatus.DRAFT);
                    v.setPublishedDate(null);
                    v.setPublishedBy(null);
                });

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        log.info("Unpublished comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }


    /**
     * Clone a published comment for editing (creates a new DRAFT version) - with optimistic locking support.
     */
    public DashboardCommentDto cloneCommentForEdit(String contextKey, int dashboardId, String commentId,
                                                   DashboardCommentUpdateDto updateDto) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or admin can edit
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);
        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this comment");
        }

        // verify entityVersion matches
        checkEntityVersion(comment, updateDto.getEntityVersion(), currentUserEmail);

        // Check business validation
        DashboardCommentVersionDto activeVersion = getActiveVersion(commentMapper.toDto(comment));
        if (activeVersion == null || activeVersion.getStatus() != DashboardCommentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DashboardCommentEntity must be published to create a new edit version");
        }

        String editorName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        int newVersionNumber = comment.getHistory().stream()
                .mapToInt(DashboardCommentVersionEntity::getVersion)
                .max()
                .orElse(0) + 1;

        DashboardCommentVersionEntity newVersion = DashboardCommentVersionEntity.builder()
                .version(newVersionNumber)
                .text(updateDto.getText())
                .status(DashboardCommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(editorName)
                .deleted(false)
                .build();

        comment.addVersion(newVersion);
        comment.setActiveVersion(newVersionNumber);
        comment.setHasActiveDraft(true);
        if (updateDto.getPointerUrl() != null) {
            comment.setPointerUrl(updateDto.getPointerUrl());
        }

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        log.info("Created new version {} for comment {} on dashboard {}/{}",
                newVersionNumber, commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Restore a specific version of a comment.
     */
    public DashboardCommentDto restoreVersion(String contextKey, int dashboardId, String commentId, int versionNumber) {
        DashboardCommentEntity comment = commentRepository.findByIdWithHistoryForUpdate(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or admin can restore
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);
        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to restore this comment version");
        }

        comment.setActiveVersion(versionNumber);
        comment.setHasActiveDraft(false);

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        log.info("Restored comment {} to version {} for dashboard {}/{}",
                commentId, versionNumber, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Find the last published (non-deleted) version in the comment's history.
     */
    private DashboardCommentVersionDto findLastPublishedVersion(DashboardCommentDto comment) {
        return comment.getHistory().stream()
                .filter(v -> v.getStatus() == DashboardCommentStatus.PUBLISHED && !v.isDeleted())
                .max(Comparator.comparingInt(DashboardCommentVersionDto::getVersion))
                .orElse(null);
    }

    private DashboardCommentVersionDto getActiveVersion(DashboardCommentDto comment) {
        return comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if the provided entity version matches the current version in database.
     * Throws CONFLICT (409) if versions don't match.
     */
    private void checkEntityVersion(DashboardCommentEntity comment, long providedVersion, String userEmail) {
        long currentVersion = comment.getEntityVersion();

        if (currentVersion != providedVersion) {
            log.warn("Entity version mismatch for comment {}. Current: {}, Provided: {}, User: {}",
                    comment.getId(), currentVersion, providedVersion, userEmail);

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Comment was modified by another user. Please refresh and try again.");
        }
    }

    /**
     * Get all comments for a data domain (contextKey) across all dashboards.
     * Admins see all comments, viewers only see comments for dashboards they have access to.
     */
    @Transactional(readOnly = true)
    public List<DomainDashboardCommentDto> getCommentsForDomain(String contextKey) {
        List<DashboardCommentEntity> comments = commentRepository.findByContextKeyOrderByCreatedDateAsc(contextKey);

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);

        // Get dashboard info map (title and instanceName)
        Map<Integer, DashboardInfo> dashboardInfoMap = getDashboardInfoMap(contextKey);

        // For non-admin users (viewers), get the set of dashboard IDs they have access to
        Set<Integer> accessibleDashboardIds = isAdmin ? null : getAccessibleDashboardIdsForUser(contextKey, currentUserEmail);

        // Convert to DTOs, filter and transform comments based on user permissions
        return comments.stream()
                .filter(entity -> isAdmin || accessibleDashboardIds == null || accessibleDashboardIds.contains(entity.getDashboardId()))
                .map(entity -> toDomainDashboardCommentDto(entity, dashboardInfoMap))
                .filter(c -> !c.isDeleted())
                .map(c -> filterDomainDashboardCommentByPermissions(c, currentUserEmail, currentUserFullName, isAdmin))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Get the set of dashboard IDs that the current user has access to for a given context.
     * Uses RBAC roles from Superset to determine access.
     */
    private Set<Integer> getAccessibleDashboardIdsForUser(String contextKey, String userEmail) {
        Set<Integer> accessibleDashboardIds = new HashSet<>();
        try {
            DashboardResource dashboardResource = metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(
                    ModuleType.SUPERSET,
                    ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                    contextKey,
                    DashboardResource.class
            );
            if (dashboardResource == null || dashboardResource.getData() == null) {
                return accessibleDashboardIds;
            }

            String instanceName = dashboardResource.getInstanceName();

            // Get user's roles from Superset
            UserResource userResource = metaInfoResourceService.findByModuleTypeInstanceNameAndKind(
                    ModuleType.SUPERSET, instanceName, ModuleResourceKind.HELLO_DATA_USERS, UserResource.class);

            Optional<SubsystemUser> subsystemUserFound = CollectionUtils.emptyIfNull(userResource.getData())
                    .stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(userEmail))
                    .findFirst();

            if (subsystemUserFound.isEmpty()) {
                log.debug("User {} not found in Superset instance {}", userEmail, instanceName);
                return accessibleDashboardIds;
            }

            SubsystemUser assignedUser = subsystemUserFound.get();
            List<SubsystemRole> userRoles = assignedUser.getRoles();
            Set<String> rolesOfCurrentUser = userRoles.stream().map(SubsystemRole::getName).collect(Collectors.toSet());

            boolean isSupersetAdmin = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_ADMIN_ROLE_NAME));
            boolean isSupersetEditor = userRoles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_EDITOR_ROLE_NAME));

            // Check each dashboard for RBAC access
            List<SupersetDashboard> publishedDashboards = CollectionUtils.emptyIfNull(dashboardResource.getData())
                    .stream()
                    .filter(SupersetDashboard::isPublished)
                    .toList();

            for (SupersetDashboard dashboard : publishedDashboards) {
                Set<String> dashboardRoles = dashboard.getRoles().stream().map(SubsystemRole::getName).collect(Collectors.toSet());

                // Admin and Editor have access to all dashboards
                if (isSupersetAdmin || isSupersetEditor) {
                    accessibleDashboardIds.add(dashboard.getId());
                    continue;
                }

                // Check if user has any of the dashboard's RBAC roles
                boolean hasAccess = dashboardRoles.stream().anyMatch(rolesOfCurrentUser::contains);
                if (hasAccess) {
                    accessibleDashboardIds.add(dashboard.getId());
                }
            }

            log.debug("User {} has access to {} dashboards in context {}", userEmail, accessibleDashboardIds.size(), contextKey);
        } catch (Exception e) {
            log.warn("Could not fetch accessible dashboards for user {} in context {}: {}", userEmail, contextKey, e.getMessage());
        }
        return accessibleDashboardIds;
    }

    /**
     * Helper record to store dashboard info (title and instanceName).
     */
    private record DashboardInfo(String title, String instanceName) {
    }

    /**
     * Get a map of dashboard ID to dashboard info (title and instanceName) for a given context.
     */
    private Map<Integer, DashboardInfo> getDashboardInfoMap(String contextKey) {
        Map<Integer, DashboardInfo> infoMap = new HashMap<>();
        try {
            DashboardResource dashboardResource = metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(
                    ModuleType.SUPERSET,
                    ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                    contextKey,
                    DashboardResource.class
            );
            if (dashboardResource != null && dashboardResource.getData() != null) {
                String instanceName = dashboardResource.getInstanceName();
                for (SupersetDashboard dashboard : dashboardResource.getData()) {
                    infoMap.put(dashboard.getId(), new DashboardInfo(dashboard.getDashboardTitle(), instanceName));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch dashboard info for context {}: {}", contextKey, e.getMessage());
        }
        return infoMap;
    }

    /**
     * Convert entity to DomainDashboardCommentDto with dashboard title and instanceName.
     */
    private DomainDashboardCommentDto toDomainDashboardCommentDto(DashboardCommentEntity entity, Map<Integer, DashboardInfo> dashboardInfoMap) {
        DashboardCommentDto baseDto = commentMapper.toDto(entity);
        DomainDashboardCommentDto domainDto = new DomainDashboardCommentDto();

        // Copy all properties from base DTO
        domainDto.setId(baseDto.getId());
        domainDto.setDashboardId(baseDto.getDashboardId());
        domainDto.setDashboardUrl(baseDto.getDashboardUrl());
        domainDto.setContextKey(baseDto.getContextKey());
        domainDto.setPointerUrl(baseDto.getPointerUrl());
        domainDto.setAuthor(baseDto.getAuthor());
        domainDto.setAuthorEmail(baseDto.getAuthorEmail());
        domainDto.setCreatedDate(baseDto.getCreatedDate());
        domainDto.setDeleted(baseDto.isDeleted());
        domainDto.setActiveVersion(baseDto.getActiveVersion());
        domainDto.setHistory(baseDto.getHistory());
        domainDto.setHasActiveDraft(baseDto.isHasActiveDraft());
        domainDto.setEntityVersion(baseDto.getEntityVersion());
        domainDto.setTags(baseDto.getTags());

        // Set dashboard title and instanceName from map or fallback
        DashboardInfo dashboardInfo = dashboardInfoMap.get(entity.getDashboardId());
        if (dashboardInfo != null) {
            domainDto.setDashboardTitle(dashboardInfo.title() != null ? dashboardInfo.title() : "Dashboard " + entity.getDashboardId());
            domainDto.setInstanceName(dashboardInfo.instanceName());
        } else {
            domainDto.setDashboardTitle("Dashboard " + entity.getDashboardId());
        }

        return domainDto;
    }

    /**
     * Filter domain dashboard comment by permissions (similar to filterCommentByPermissions but for DomainDashboardCommentDto).
     */
    private DomainDashboardCommentDto filterDomainDashboardCommentByPermissions(DomainDashboardCommentDto comment, String userEmail, String userFullName, boolean isAdmin) {
        DashboardCommentVersionDto activeVersion = getDomainDashboardActiveVersion(comment);
        if (activeVersion == null || activeVersion.isDeleted()) {
            return null;
        }

        // Published comments are visible to everyone
        if (activeVersion.getStatus() == DashboardCommentStatus.PUBLISHED) {
            return comment;
        }

        // Handle draft comments
        if (activeVersion.getStatus() == DashboardCommentStatus.DRAFT) {
            // Admins can see all drafts
            if (isAdmin) {
                return comment;
            }

            // Check if this is user's own draft
            boolean isOwnDraft = (userEmail != null && userEmail.equals(comment.getAuthorEmail())) ||
                    (userFullName != null && userFullName.equals(activeVersion.getEditedBy()));
            if (isOwnDraft) {
                return comment;
            }

            // For drafts by others, show the last published version if it exists
            DashboardCommentVersionDto lastPublished = findLastPublishedVersionInDomainDashboardComment(comment);
            if (lastPublished != null) {
                comment.setActiveVersion(lastPublished.getVersion());
                return comment;
            }
            return null;
        }

        return null;
    }

    /**
     * Get active version from DomainDashboardCommentDto.
     */
    private DashboardCommentVersionDto getDomainDashboardActiveVersion(DomainDashboardCommentDto comment) {
        if (comment.getHistory() == null || comment.getHistory().isEmpty()) {
            return null;
        }
        return comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .orElse(null);
    }

    /**
     * Find the last published version in domain dashboard comment history.
     */
    private DashboardCommentVersionDto findLastPublishedVersionInDomainDashboardComment(DomainDashboardCommentDto comment) {
        if (comment.getHistory() == null || comment.getHistory().isEmpty()) {
            return null;
        }
        return comment.getHistory().stream()
                .filter(v -> v.getStatus() == DashboardCommentStatus.PUBLISHED && !v.isDeleted())
                .max(Comparator.comparingInt(DashboardCommentVersionDto::getVersion))
                .orElse(null);
    }

    /**
     * Validate and normalize tags.
     * Each tag must be max 10 characters and will be trimmed and lowercased.
     */
    private List<String> validateAndNormalizeTags(List<String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim().toLowerCase())
                .map(tag -> tag.length() > 10 ? tag.substring(0, 10) : tag)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get all unique tags used in comments for a specific dashboard.
     * Returns tags from all non-deleted comments.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableTags(String contextKey, int dashboardId) {
        List<DashboardCommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);

        return comments.stream()
                .filter(c -> !c.isDeleted())
                .flatMap(c -> c.getTags().stream())
                .map(tag -> tag.getTag())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Export comments for a dashboard to JSON format.
     * Only exports published comments.
     */
    @Transactional(readOnly = true)
    public CommentExportDto exportComments(String contextKey, int dashboardId) {
        List<DashboardCommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);

        List<CommentExportItemDto> exportItems = comments.stream()
                .filter(c -> !c.isDeleted())
                .filter(this::hasPublishedVersion)
                .map(this::toExportItem)
                .toList();

        return CommentExportDto.builder()
                .exportVersion("1.0")
                .contextKey(contextKey)
                .dashboardId(dashboardId)
                .exportDate(System.currentTimeMillis())
                .comments(exportItems)
                .build();
    }

    private boolean hasPublishedVersion(DashboardCommentEntity comment) {
        return comment.getHistory().stream()
                .anyMatch(v -> !v.isDeleted() && v.getStatus() == DashboardCommentStatus.PUBLISHED);
    }

    private CommentExportItemDto toExportItem(DashboardCommentEntity entity) {
        // Get current active version
        DashboardCommentVersionEntity activeVersion = entity.getHistory().stream()
                .filter(v -> Objects.equals(v.getVersion(), entity.getActiveVersion()))
                .findFirst()
                .orElse(null);

        List<CommentVersionExportDto> historyExport = entity.getHistory().stream()
                .filter(v -> !v.isDeleted() && v.getStatus() == DashboardCommentStatus.PUBLISHED)
                .map(v -> CommentVersionExportDto.builder()
                        .version(v.getVersion())
                        .text(v.getText())
                        .status(v.getStatus().name())
                        .editedDate(v.getEditedDate())
                        .editedBy(v.getEditedBy())
                        .publishedDate(v.getPublishedDate())
                        .publishedBy(v.getPublishedBy())
                        .build())
                .sorted(Comparator.comparingInt(CommentVersionExportDto::getVersion))
                .toList();

        return CommentExportItemDto.builder()
                .text(activeVersion != null ? activeVersion.getText() : "")
                .author(entity.getAuthor())
                .authorEmail(entity.getAuthorEmail())
                .createdDate(entity.getCreatedDate())
                .status(activeVersion != null ? activeVersion.getStatus().name() : DashboardCommentStatus.DRAFT.name())
                .tags(entity.getTags().stream().map(DashboardCommentTagEntity::getTag).toList())
                .history(historyExport)
                .build();
    }

    /**
     * Import comments from JSON format.
     * Creates new comments with DRAFT status, pointerUrl is ignored.
     */
    @Transactional
    public CommentImportResultDto importComments(String contextKey, int dashboardId, CommentExportDto importData) {
        // Validate import data
        if (importData == null || importData.getComments() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid import data");
        }

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can import comments");
        }

        int imported = 0;
        int skipped = 0;

        for (CommentExportItemDto item : importData.getComments()) {
            if (!isValidImportItem(item)) {
                skipped++;
                continue;
            }

            DashboardCommentEntity entity = createImportedComment(contextKey, dashboardId, item, currentUserFullName, currentUserEmail);
            commentRepository.save(entity);
            imported++;
        }

        return CommentImportResultDto.builder()
                .imported(imported)
                .skipped(skipped)
                .message(String.format("Imported %d comments, skipped %d", imported, skipped))
                .build();
    }

    private boolean isValidImportItem(CommentExportItemDto item) {
        return item != null && item.getText() != null && !item.getText().trim().isEmpty();
    }

    private DashboardCommentEntity createImportedComment(String contextKey, int dashboardId,
                                                         CommentExportItemDto item, String importedBy, String importedByEmail) {

        String commentId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        // Create initial version as DRAFT
        DashboardCommentVersionEntity version = DashboardCommentVersionEntity.builder()
                .version(1)
                .text(item.getText())
                .status(DashboardCommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(importedBy)
                .deleted(false)
                .build();

        // Create tags
        List<DashboardCommentTagEntity> tags = new ArrayList<>();
        if (item.getTags() != null) {
            for (String tagText : item.getTags()) {
                if (tagText != null && !tagText.trim().isEmpty()) {
                    tags.add(DashboardCommentTagEntity.builder()
                            .tag(tagText.trim().toLowerCase().substring(0, Math.min(tagText.trim().length(), 10)))
                            .build());
                }
            }
        }

        // Create entity - note: pointerUrl is intentionally NOT imported
        DashboardCommentEntity entity = DashboardCommentEntity.builder()
                .id(commentId)
                .contextKey(contextKey)
                .dashboardId(dashboardId)
                .author(item.getAuthor() != null ? item.getAuthor() : importedBy)
                .authorEmail(item.getAuthorEmail() != null ? item.getAuthorEmail() : importedByEmail)
                .createdDate(item.getCreatedDate() > 0 ? item.getCreatedDate() : now)
                .deleted(false)
                .activeVersion(1)
                .hasActiveDraft(true)
                .entityVersion(0L)
                .history(new ArrayList<>(List.of(version)))
                .tags(tags)
                .build();

        // Set back-references
        version.setComment(entity);
        tags.forEach(t -> t.setComment(entity));

        return entity;
    }

}

