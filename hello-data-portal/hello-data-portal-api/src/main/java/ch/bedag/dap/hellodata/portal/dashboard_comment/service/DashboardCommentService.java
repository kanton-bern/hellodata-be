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
    private static final String DASHBOARD_TITLE_PREFIX = "Dashboard ";

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

        // Get dashboard URL - use provided URL or generate from dashboard info
        String dashboardUrl = createDto.getDashboardUrl();
        if (dashboardUrl == null || dashboardUrl.isBlank()) {
            dashboardUrl = getDashboardUrl(contextKey, dashboardId);
        }
        if (dashboardUrl == null || dashboardUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dashboard URL is required and could not be determined");
        }

        // Validate and normalize tags
        List<String> normalizedTags = validateAndNormalizeTags(createDto.getTags());

        // Create comment entity
        DashboardCommentEntity comment = DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(dashboardId)
                .dashboardUrl(dashboardUrl)
                .contextKey(contextKey)
                .pointerUrl(createDto.getPointerUrl())
                .author(authorFullName)
                .authorEmail(authorEmail)
                .createdDate(now)
                .deleted(false)
                .activeVersion(1)
                .hasActiveDraft(false)
                .build();

        // Create first version with tags snapshot
        DashboardCommentVersionEntity version = DashboardCommentVersionEntity.builder()
                .version(1)
                .text(createDto.getText())
                .status(DashboardCommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(authorFullName)
                .deleted(false)
                .tags(commentMapper.tagsToString(normalizedTags))
                .pointerUrl(createDto.getPointerUrl())
                .build();

        comment.addVersion(version);

        // Add tags to comment entity (current tags)
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

        // Normalize tags for version snapshot
        List<String> normalizedTags = updateDto.getTags() != null
                ? validateAndNormalizeTags(updateDto.getTags())
                : getCurrentTags(comment);

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
                    v.setTags(commentMapper.tagsToString(normalizedTags));
                    // Update pointerUrl - allow clearing by setting to null/empty
                    String newPointerUrl = updateDto.getPointerUrl();
                    v.setPointerUrl(newPointerUrl != null && !newPointerUrl.trim().isEmpty() ? newPointerUrl : null);
                });

        // Update pointerUrl on comment entity - allow clearing
        String newPointerUrl = updateDto.getPointerUrl();
        comment.setPointerUrl(newPointerUrl != null && !newPointerUrl.trim().isEmpty() ? newPointerUrl : null);

        // Update tags on comment entity if provided
        if (updateDto.getTags() != null) {
            // Clear existing tags
            comment.getTags().clear();
            // Add new normalized tags
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
     * Get current tags from comment entity.
     */
    private List<String> getCurrentTags(DashboardCommentEntity comment) {
        return comment.getTags().stream()
                .map(DashboardCommentTagEntity::getTag)
                .collect(Collectors.toList());
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

        // Get tags for new version - from updateDto or current comment tags
        List<String> tagsForNewVersion = updateDto.getTags() != null
                ? validateAndNormalizeTags(updateDto.getTags())
                : getCurrentTags(comment);

        int newVersionNumber = comment.getHistory().stream()
                .mapToInt(DashboardCommentVersionEntity::getVersion)
                .max()
                .orElse(0) + 1;

        // Determine pointerUrl for new version - allow clearing
        String pointerUrlForVersion = updateDto.getPointerUrl();
        if (pointerUrlForVersion == null || pointerUrlForVersion.trim().isEmpty()) {
            pointerUrlForVersion = null; // Explicitly clear if empty
        }

        DashboardCommentVersionEntity newVersion = DashboardCommentVersionEntity.builder()
                .version(newVersionNumber)
                .text(updateDto.getText())
                .status(DashboardCommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(editorName)
                .deleted(false)
                .tags(commentMapper.tagsToString(tagsForNewVersion))
                .pointerUrl(pointerUrlForVersion)
                .build();

        comment.addVersion(newVersion);
        comment.setActiveVersion(newVersionNumber);
        comment.setHasActiveDraft(true);
        // Update pointerUrl on comment entity - allow clearing
        comment.setPointerUrl(pointerUrlForVersion);

        updateTagsIfProvided(updateDto, comment);

        comment.setEntityVersion(comment.getEntityVersion() + 1);

        DashboardCommentEntity savedComment = commentRepository.save(comment);
        log.info("Created new version {} for comment {} on dashboard {}/{}",
                newVersionNumber, commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    private void updateTagsIfProvided(DashboardCommentUpdateDto updateDto, DashboardCommentEntity comment) {
        if (updateDto.getTags() != null) {
            comment.getTags().clear();
            for (String tagText : updateDto.getTags()) {
                if (tagText != null && !tagText.trim().isEmpty()) {
                    DashboardCommentTagEntity tag = DashboardCommentTagEntity.builder()
                            .tag(tagText.trim().toLowerCase().substring(0, Math.min(tagText.trim().length(), 10)))
                            .build();
                    comment.addTag(tag);
                }
            }
        }
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
            domainDto.setDashboardTitle(dashboardInfo.title() != null ? dashboardInfo.title() : DASHBOARD_TITLE_PREFIX + entity.getDashboardId());
            domainDto.setInstanceName(dashboardInfo.instanceName());
        } else {
            domainDto.setDashboardTitle(DASHBOARD_TITLE_PREFIX + entity.getDashboardId());
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
                .map(DashboardCommentTagEntity::getTag)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Export comments for a dashboard to JSON format.
     * Exports all non-deleted comments with their full history (all versions).
     * Only admins (superuser, business_domain_admin, data_domain_admin) can export.
     */
    @Transactional(readOnly = true)
    public CommentExportDto exportComments(String contextKey, int dashboardId) {
        // Validate that user is admin
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can export comments");
        }

        List<DashboardCommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);

        List<CommentExportItemDto> exportItems = comments.stream()
                .filter(c -> !c.isDeleted())
                .map(this::toExportItem)
                .toList();

        // Get dashboard title
        String dashboardTitle = getDashboardTitle(contextKey, dashboardId);

        return CommentExportDto.builder()
                .exportVersion("1.0")
                .contextKey(contextKey)
                .dashboardId(dashboardId)
                .dashboardTitle(dashboardTitle)
                .exportDate(System.currentTimeMillis())
                .comments(exportItems)
                .build();
    }

    /**
     * Get dashboard title for a specific dashboard.
     */
    private String getDashboardTitle(String contextKey, int dashboardId) {
        Map<Integer, DashboardInfo> infoMap = getDashboardInfoMap(contextKey);
        DashboardInfo info = infoMap.get(dashboardId);
        return info != null && info.title() != null ? info.title() : DASHBOARD_TITLE_PREFIX + dashboardId;
    }


    private CommentExportItemDto toExportItem(DashboardCommentEntity entity) {
        // Get current active version
        DashboardCommentVersionEntity activeVersion = entity.getHistory().stream()
                .filter(v -> Objects.equals(v.getVersion(), entity.getActiveVersion()))
                .findFirst()
                .orElse(null);

        // Export all non-deleted versions (preserve full history)
        List<CommentVersionExportDto> historyExport = entity.getHistory().stream()
                .filter(v -> !v.isDeleted())
                .map(v -> CommentVersionExportDto.builder()
                        .version(v.getVersion())
                        .text(v.getText())
                        .status(v.getStatus().name())
                        .editedDate(v.getEditedDate())
                        .editedBy(v.getEditedBy())
                        .publishedDate(v.getPublishedDate())
                        .publishedBy(v.getPublishedBy())
                        .tags(parseTagsFromString(v.getTags()))
                        .pointerUrl(v.getPointerUrl())
                        .build())
                .sorted(Comparator.comparingInt(CommentVersionExportDto::getVersion))
                .toList();

        return CommentExportItemDto.builder()
                .id(entity.getId())
                .text(activeVersion != null ? activeVersion.getText() : "")
                .author(entity.getAuthor())
                .authorEmail(entity.getAuthorEmail())
                .createdDate(entity.getCreatedDate())
                .status(activeVersion != null ? activeVersion.getStatus().name() : DashboardCommentStatus.DRAFT.name())
                .activeVersion(entity.getActiveVersion())
                .tags(entity.getTags().stream().map(DashboardCommentTagEntity::getTag).toList())
                .history(historyExport)
                .build();
    }

    private List<String> parseTagsFromString(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String convertTagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    /**
     * Import comments from JSON format.
     * If a comment with the same ID exists in the same dashboard, it will be updated.
     * New comments are created with new IDs if no ID provided or ID not found.
     * Comments preserve their full history and statuses from import data.
     * Only admins (superuser, business_domain_admin, data_domain_admin) can import.
     */
    @Transactional
    public CommentImportResultDto importComments(String contextKey, int dashboardId, CommentExportDto importData) {
        validateImportData(importData);
        validateImportPermissions(contextKey);

        String dashboardUrl = getRequiredDashboardUrl(contextKey, dashboardId);
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();

        int[] counters = {0, 0, 0}; // imported, updated, skipped

        for (CommentExportItemDto item : importData.getComments()) {
            processImportItem(item, contextKey, dashboardId, dashboardUrl, currentUserFullName, currentUserEmail, counters);
        }

        return CommentImportResultDto.builder()
                .imported(counters[0])
                .updated(counters[1])
                .skipped(counters[2])
                .message(String.format("Imported %d new, updated %d existing, skipped %d", counters[0], counters[1], counters[2]))
                .build();
    }

    private void validateImportData(CommentExportDto importData) {
        if (importData == null || importData.getComments() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid import data");
        }
    }

    private void validateImportPermissions(String contextKey) {
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can import comments");
        }
    }

    private String getRequiredDashboardUrl(String contextKey, int dashboardId) {
        String dashboardUrl = getDashboardUrl(contextKey, dashboardId);
        if (dashboardUrl == null || dashboardUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not determine dashboard URL for import");
        }
        return dashboardUrl;
    }

    private void processImportItem(CommentExportItemDto item, String contextKey, int dashboardId,
                                   String dashboardUrl, String userFullName, String userEmail, int[] counters) {
        if (!isValidImportItem(item)) {
            counters[2]++; // skipped
            return;
        }

        String originalCommentId = item.getId();

        // CASE 1: Check if this is a re-import of the same dashboard (same ID, same dashboard)
        if (originalCommentId != null && !originalCommentId.isBlank()) {
            Optional<DashboardCommentEntity> sameIdOpt = commentRepository.findById(originalCommentId);
            if (sameIdOpt.isPresent()) {
                DashboardCommentEntity existing = sameIdOpt.get();
                // Only update if it belongs to the SAME dashboard (re-import scenario)
                if (existing.getContextKey().equals(contextKey) && existing.getDashboardId() == dashboardId) {
                    updateImportedComment(existing, item, dashboardUrl, userFullName);
                    commentRepository.save(existing);
                    counters[1]++; // updated
                    return;
                }
            }

            // CASE 2: Check if comment was previously imported from another dashboard (importedFromId matches)
            Optional<DashboardCommentEntity> importedOpt = commentRepository
                    .findByImportedFromIdAndContextKeyAndDashboardId(originalCommentId, contextKey, dashboardId);
            if (importedOpt.isPresent()) {
                // Update previously imported comment
                DashboardCommentEntity existing = importedOpt.get();
                updateImportedComment(existing, item, dashboardUrl, userFullName);
                commentRepository.save(existing);
                counters[1]++; // updated
                return;
            }
        }

        // CASE 3: Create new comment (first import from another dashboard or no ID provided)
        DashboardCommentEntity entity = createImportedComment(contextKey, dashboardId, dashboardUrl, item, userFullName, userEmail);
        commentRepository.save(entity);
        counters[0]++; // imported
    }

    /**
     * Update an existing comment from import data.
     * Updates existing versions or adds new ones from imported history.
     */
    private void updateImportedComment(DashboardCommentEntity entity, CommentExportItemDto item,
                                       String dashboardUrl, String updatedBy) {
        entity.setDashboardUrl(dashboardUrl);

        Map<Integer, DashboardCommentVersionEntity> existingVersionsMap = buildExistingVersionsMap(entity);
        ImportVersionResult result = processImportVersions(entity, item, existingVersionsMap, updatedBy);

        updateEntityMetadata(entity, item, result);
        updateEntityTags(entity, item.getTags());
    }

    private Map<Integer, DashboardCommentVersionEntity> buildExistingVersionsMap(DashboardCommentEntity entity) {
        return entity.getHistory().stream()
                .collect(Collectors.toMap(DashboardCommentVersionEntity::getVersion, v -> v, (v1, v2) -> v1));
    }

    private ImportVersionResult processImportVersions(DashboardCommentEntity entity, CommentExportItemDto item,
                                                      Map<Integer, DashboardCommentVersionEntity> existingVersionsMap,
                                                      String updatedBy) {
        long now = System.currentTimeMillis();

        if (item.getHistory() != null && !item.getHistory().isEmpty()) {
            return processHistoryVersions(entity, item.getHistory(), existingVersionsMap, updatedBy, now);
        } else {
            return processSingleVersion(entity, item, existingVersionsMap, updatedBy, now);
        }
    }

    private ImportVersionResult processHistoryVersions(DashboardCommentEntity entity,
                                                       List<CommentVersionExportDto> history,
                                                       Map<Integer, DashboardCommentVersionEntity> existingVersionsMap,
                                                       String updatedBy, long now) {
        Set<Integer> importedVersionNumbers = new HashSet<>();
        boolean hasActiveDraft = false;
        int maxVersion = 0;

        for (CommentVersionExportDto historyItem : history) {
            DashboardCommentStatus status = parseStatus(historyItem.getStatus());
            hasActiveDraft = hasActiveDraft || (status == DashboardCommentStatus.DRAFT);
            importedVersionNumbers.add(historyItem.getVersion());

            processVersionItem(entity, historyItem, existingVersionsMap, status, updatedBy, now);
            maxVersion = Math.max(maxVersion, historyItem.getVersion());
        }

        markDeletedVersions(existingVersionsMap, importedVersionNumbers);
        return new ImportVersionResult(hasActiveDraft, maxVersion);
    }

    private void processVersionItem(DashboardCommentEntity entity, CommentVersionExportDto historyItem,
                                    Map<Integer, DashboardCommentVersionEntity> existingVersionsMap,
                                    DashboardCommentStatus status, String updatedBy, long now) {
        DashboardCommentVersionEntity existingVersion = existingVersionsMap.get(historyItem.getVersion());

        if (existingVersion != null) {
            updateExistingVersion(existingVersion, historyItem, status, updatedBy, now);
        } else {
            createAndAddNewVersion(entity, historyItem, status, updatedBy, now);
        }
    }

    private void updateExistingVersion(DashboardCommentVersionEntity existingVersion,
                                       CommentVersionExportDto historyItem,
                                       DashboardCommentStatus status, String updatedBy, long now) {
        existingVersion.setText(historyItem.getText());
        existingVersion.setStatus(status);
        existingVersion.setEditedDate(historyItem.getEditedDate() > 0 ? historyItem.getEditedDate() : now);
        existingVersion.setEditedBy(historyItem.getEditedBy() != null ? historyItem.getEditedBy() : updatedBy);
        existingVersion.setPublishedDate(historyItem.getPublishedDate());
        existingVersion.setPublishedBy(historyItem.getPublishedBy());
        existingVersion.setDeleted(false);
        existingVersion.setTags(convertTagsToString(historyItem.getTags()));
        existingVersion.setPointerUrl(historyItem.getPointerUrl());
    }

    private void createAndAddNewVersion(DashboardCommentEntity entity, CommentVersionExportDto historyItem,
                                        DashboardCommentStatus status, String updatedBy, long now) {
        DashboardCommentVersionEntity version = DashboardCommentVersionEntity.builder()
                .version(historyItem.getVersion())
                .text(historyItem.getText())
                .status(status)
                .editedDate(historyItem.getEditedDate() > 0 ? historyItem.getEditedDate() : now)
                .editedBy(historyItem.getEditedBy() != null ? historyItem.getEditedBy() : updatedBy)
                .publishedDate(historyItem.getPublishedDate())
                .publishedBy(historyItem.getPublishedBy())
                .deleted(false)
                .tags(convertTagsToString(historyItem.getTags()))
                .pointerUrl(historyItem.getPointerUrl())
                .comment(entity)
                .build();
        entity.getHistory().add(version);
    }

    private ImportVersionResult processSingleVersion(DashboardCommentEntity entity, CommentExportItemDto item,
                                                     Map<Integer, DashboardCommentVersionEntity> existingVersionsMap,
                                                     String updatedBy, long now) {
        DashboardCommentStatus status = parseStatus(item.getStatus());
        boolean hasActiveDraft = (status == DashboardCommentStatus.DRAFT);

        DashboardCommentVersionEntity existingVersion = existingVersionsMap.get(1);
        if (existingVersion != null) {
            updateExistingVersionFromItem(existingVersion, item, status, updatedBy, now);
        } else {
            createAndAddVersionFromItem(entity, item, status, updatedBy, now);
        }

        markOtherVersionsAsDeleted(existingVersionsMap);
        return new ImportVersionResult(hasActiveDraft, 1);
    }

    private void updateExistingVersionFromItem(DashboardCommentVersionEntity existingVersion,
                                               CommentExportItemDto item,
                                               DashboardCommentStatus status, String updatedBy, long now) {
        existingVersion.setText(item.getText());
        existingVersion.setStatus(status);
        existingVersion.setEditedDate(now);
        existingVersion.setEditedBy(updatedBy);
        existingVersion.setDeleted(false);
    }

    private void createAndAddVersionFromItem(DashboardCommentEntity entity, CommentExportItemDto item,
                                             DashboardCommentStatus status, String updatedBy, long now) {
        DashboardCommentVersionEntity version = DashboardCommentVersionEntity.builder()
                .version(1)
                .text(item.getText())
                .status(status)
                .editedDate(now)
                .editedBy(updatedBy)
                .deleted(false)
                .comment(entity)
                .build();
        entity.getHistory().add(version);
    }

    private void markDeletedVersions(Map<Integer, DashboardCommentVersionEntity> existingVersionsMap,
                                     Set<Integer> importedVersionNumbers) {
        existingVersionsMap.values().stream()
                .filter(v -> !importedVersionNumbers.contains(v.getVersion()))
                .forEach(v -> v.setDeleted(true));
    }

    private void markOtherVersionsAsDeleted(Map<Integer, DashboardCommentVersionEntity> existingVersionsMap) {
        existingVersionsMap.values().stream()
                .filter(v -> v.getVersion() != 1)
                .forEach(v -> v.setDeleted(true));
    }

    private void updateEntityMetadata(DashboardCommentEntity entity, CommentExportItemDto item,
                                      ImportVersionResult result) {
        int activeVersion = item.getActiveVersion() > 0 ? item.getActiveVersion() : result.maxVersion();
        entity.setActiveVersion(activeVersion);
        entity.setHasActiveDraft(result.hasActiveDraft());
        entity.setEntityVersion(entity.getEntityVersion() + 1);
    }

    private void updateEntityTags(DashboardCommentEntity entity, List<String> tagNames) {
        entity.getTags().clear();
        if (tagNames == null) {
            return;
        }
        for (String tagText : tagNames) {
            if (tagText != null && !tagText.trim().isEmpty()) {
                DashboardCommentTagEntity tag = DashboardCommentTagEntity.builder()
                        .tag(normalizeTag(tagText))
                        .comment(entity)
                        .build();
                entity.getTags().add(tag);
            }
        }
    }

    private String normalizeTag(String tagText) {
        String trimmed = tagText.trim().toLowerCase();
        return trimmed.substring(0, Math.min(trimmed.length(), 10));
    }


    /**
     * Get dashboard URL for a specific dashboard.
     */
    private String getDashboardUrl(String contextKey, int dashboardId) {
        Map<Integer, DashboardInfo> infoMap = getDashboardInfoMap(contextKey);
        DashboardInfo info = infoMap.get(dashboardId);
        if (info != null && info.instanceName() != null) {
            return String.format("https://%s/superset/dashboard/%d/?standalone=1", info.instanceName(), dashboardId);
        }
        return null;
    }

    private boolean isValidImportItem(CommentExportItemDto item) {
        return item != null && item.getText() != null && !item.getText().trim().isEmpty();
    }

    private DashboardCommentEntity createImportedComment(String contextKey, int dashboardId, String dashboardUrl,
                                                         CommentExportItemDto item, String importedBy, String importedByEmail) {
        long now = System.currentTimeMillis();
        ImportContext ctx = new ImportContext(contextKey, dashboardId, dashboardUrl, importedBy, importedByEmail, now);

        ImportVersionResult versionResult = createVersionsFromImport(item, importedBy, now);
        List<DashboardCommentTagEntity> tags = createTagsFromImport(item.getTags());

        DashboardCommentEntity entity = buildImportedCommentEntity(ctx, item, versionResult, tags);
        setBackReferences(entity, versionResult.versions(), tags);
        return entity;
    }

    /**
     * Context for import operation containing common parameters.
     */
    private record ImportContext(String contextKey, int dashboardId, String dashboardUrl,
                                 String importedBy, String importedByEmail, long now) {
    }

    private ImportVersionResult createVersionsFromImport(CommentExportItemDto item, String importedBy, long now) {
        List<DashboardCommentVersionEntity> versions = new ArrayList<>();
        int maxVersion = 0;
        boolean hasActiveDraft = false;

        if (item.getHistory() != null && !item.getHistory().isEmpty()) {
            for (CommentVersionExportDto historyItem : item.getHistory()) {
                DashboardCommentStatus status = parseStatus(historyItem.getStatus());
                hasActiveDraft = hasActiveDraft || (status == DashboardCommentStatus.DRAFT);

                versions.add(buildVersionFromHistory(historyItem, status, importedBy, now));
                maxVersion = Math.max(maxVersion, historyItem.getVersion());
            }
        } else {
            DashboardCommentStatus status = parseStatus(item.getStatus());
            hasActiveDraft = (status == DashboardCommentStatus.DRAFT);
            versions.add(buildInitialVersion(item.getText(), status, importedBy, now));
            maxVersion = 1;
        }

        return new ImportVersionResult(hasActiveDraft, maxVersion, versions);
    }

    private DashboardCommentVersionEntity buildVersionFromHistory(CommentVersionExportDto historyItem,
                                                                  DashboardCommentStatus status,
                                                                  String importedBy, long now) {
        return DashboardCommentVersionEntity.builder()
                .version(historyItem.getVersion())
                .text(historyItem.getText())
                .status(status)
                .editedDate(historyItem.getEditedDate() > 0 ? historyItem.getEditedDate() : now)
                .editedBy(historyItem.getEditedBy() != null ? historyItem.getEditedBy() : importedBy)
                .publishedDate(historyItem.getPublishedDate())
                .publishedBy(historyItem.getPublishedBy())
                .deleted(false)
                .tags(convertTagsToString(historyItem.getTags()))
                .pointerUrl(historyItem.getPointerUrl())
                .build();
    }

    private DashboardCommentVersionEntity buildInitialVersion(String text, DashboardCommentStatus status,
                                                              String editedBy, long now) {
        return DashboardCommentVersionEntity.builder()
                .version(1)
                .text(text)
                .status(status)
                .editedDate(now)
                .editedBy(editedBy)
                .deleted(false)
                .build();
    }

    private List<DashboardCommentTagEntity> createTagsFromImport(List<String> tagNames) {
        List<DashboardCommentTagEntity> tags = new ArrayList<>();
        if (tagNames == null) {
            return tags;
        }
        for (String tagText : tagNames) {
            if (tagText != null && !tagText.trim().isEmpty()) {
                tags.add(DashboardCommentTagEntity.builder()
                        .tag(normalizeTag(tagText))
                        .build());
            }
        }
        return tags;
    }

    private DashboardCommentEntity buildImportedCommentEntity(ImportContext ctx, CommentExportItemDto item,
                                                              ImportVersionResult versionResult,
                                                              List<DashboardCommentTagEntity> tags) {
        int activeVersion = item.getActiveVersion() > 0 ? item.getActiveVersion() : versionResult.maxVersion();

        return DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .contextKey(ctx.contextKey())
                .dashboardId(ctx.dashboardId())
                .dashboardUrl(ctx.dashboardUrl())
                .importedFromId(item.getId())
                .author(item.getAuthor() != null ? item.getAuthor() : ctx.importedBy())
                .authorEmail(item.getAuthorEmail() != null ? item.getAuthorEmail() : ctx.importedByEmail())
                .createdDate(item.getCreatedDate() > 0 ? item.getCreatedDate() : ctx.now())
                .deleted(false)
                .activeVersion(activeVersion)
                .hasActiveDraft(versionResult.hasActiveDraft())
                .entityVersion(0L)
                .history(versionResult.versions())
                .tags(tags)
                .build();
    }

    private void setBackReferences(DashboardCommentEntity entity,
                                   List<DashboardCommentVersionEntity> versions,
                                   List<DashboardCommentTagEntity> tags) {
        versions.forEach(v -> v.setComment(entity));
        tags.forEach(t -> t.setComment(entity));
    }

    /**
     * Extended result including versions list for createImportedComment.
     */
    private record ImportVersionResult(boolean hasActiveDraft, int maxVersion,
                                       List<DashboardCommentVersionEntity> versions) {
        ImportVersionResult(boolean hasActiveDraft, int maxVersion) {
            this(hasActiveDraft, maxVersion, null);
        }
    }

    private DashboardCommentStatus parseStatus(String status) {
        if (status == null) {
            return DashboardCommentStatus.DRAFT;
        }
        try {
            return DashboardCommentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DashboardCommentStatus.DRAFT;
        }
    }

}

