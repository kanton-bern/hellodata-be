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
package ch.bedag.dap.hellodata.portal.comment.service;

import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portal.comment.data.*;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing dashboard comments.
 * Currently uses in-memory mock storage.
 * TODO: Implement communication with superset sidecar via NATS.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String COMMENT_NOT_FOUND_ERROR = "Comment not found";

    private final UserRepository userRepository;

    // In-memory mock storage - key is "contextKey:dashboardId"
    private final Map<String, List<CommentDto>> commentsStore = new ConcurrentHashMap<>();

    // Initialize with some mock data
    {
        initMockData();
    }

    private void initMockData() {
        // Initial mock comments will be created dynamically when first dashboard is accessed
    }

    private String buildKey(String contextKey, int dashboardId) {
        return contextKey + ":" + dashboardId;
    }

    /**
     * Get all comments for a dashboard. Filters based on user permissions.
     * - All users can see published comments
     * - Users can see their own drafts
     * - Admins (superuser, business_domain_admin, data_domain_admin) can see all drafts
     * - If active version is a draft by someone else, non-admins see the last published version instead
     */
    public List<CommentDto> getComments(String contextKey, int dashboardId) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.getOrDefault(key, new ArrayList<>());

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        String currentUserFullName = SecurityUtils.getCurrentUserFullName();
        boolean isAdmin = SecurityUtils.isSuperuser() || isAdminForContext(currentUserEmail, contextKey);

        // Filter and transform comments based on user permissions
        return comments.stream()
                .filter(c -> !c.isDeleted())
                .map(c -> filterCommentByPermissions(c, currentUserEmail, currentUserFullName, isAdmin))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Filter a single comment based on user permissions.
     * Returns the comment if visible, or null if should be hidden.
     */
    private CommentDto filterCommentByPermissions(CommentDto comment, String userEmail, String userFullName, boolean isAdmin) {
        CommentVersionDto activeVersion = getActiveVersion(comment);
        if (activeVersion == null || activeVersion.isDeleted()) {
            return null;
        }

        // Published comments are visible to everyone
        if (activeVersion.getStatus() == CommentStatus.PUBLISHED) {
            return comment;
        }

        // Handle draft comments
        if (activeVersion.getStatus() == CommentStatus.DRAFT) {
            return handleDraftComment(comment, activeVersion, userEmail, userFullName, isAdmin);
        }

        return null;
    }

    /**
     * Handle visibility logic for draft comments.
     */
    private CommentDto handleDraftComment(CommentDto comment, CommentVersionDto activeVersion,
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
    private boolean isOwnDraft(CommentDto comment, CommentVersionDto activeVersion, String userEmail, String userFullName) {
        return (userEmail != null && userEmail.equals(comment.getAuthorEmail())) ||
                (userFullName != null && userFullName.equals(activeVersion.getEditedBy()));
    }

    /**
     * Create a copy of the comment showing the last published version, or null if none exists.
     */
    private CommentDto showLastPublishedVersion(CommentDto comment) {
        CommentVersionDto lastPublishedVersion = findLastPublishedVersion(comment);
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
    public CommentDto createComment(String contextKey, int dashboardId, CommentCreateDto createDto) {
        String key = buildKey(contextKey, dashboardId);

        String authorFullName = SecurityUtils.getCurrentUserFullName();
        String authorEmail = SecurityUtils.getCurrentUserEmail();
        long now = System.currentTimeMillis();

        CommentVersionDto version = CommentVersionDto.builder()
                .version(1)
                .text(createDto.getText())
                .status(CommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(authorFullName)
                .deleted(false)
                .build();

        CommentDto comment = CommentDto.builder()
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
                .history(new ArrayList<>(List.of(version)))
                .entityVersion(0) // Initial version
                .build();

        commentsStore.computeIfAbsent(key, k -> new ArrayList<>()).add(comment);

        log.info("Created comment {} for dashboard {}/{}", comment.getId(), contextKey, dashboardId);
        return comment;
    }

    /**
     * Update an existing comment (for DRAFT status).
     */
    public CommentDto updateComment(String contextKey, int dashboardId, String commentId, CommentUpdateDto updateDto) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can update
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this comment");
        }

        // Optimistic locking check
        if (comment.getEntityVersion() != updateDto.getEntityVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Comment was modified by another user. Please refresh and try again.");
        }

        // Update the active version in history
        String editorName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .ifPresent(v -> {
                    v.setText(updateDto.getText());
                    v.setEditedDate(now);
                    v.setEditedBy(editorName);
                });

        comment.setPointerUrl(updateDto.getPointerUrl());
        comment.setEntityVersion(comment.getEntityVersion() + 1); // Increment version

        log.info("Updated comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return comment;
    }

    /**
     * Delete a comment (soft delete).
     */
    public CommentDto deleteComment(String contextKey, int dashboardId, String commentId) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can delete
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }

        String deleterName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        // Mark current active version as deleted
        comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .ifPresent(v -> v.setDeleted(true));

        // Try to find last non-deleted PUBLISHED version
        Optional<CommentVersionDto> lastPublished = comment.getHistory().stream()
                .filter(v -> v.getStatus() == CommentStatus.PUBLISHED && !v.isDeleted())
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

        return comment;
    }

    /**
     * Publish a comment (change status from DRAFT to PUBLISHED).
     */
    public CommentDto publishComment(String contextKey, int dashboardId, String commentId) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        // Only superuser can publish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can publish comments");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        String publisherName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(CommentStatus.PUBLISHED);
                    v.setPublishedDate(now);
                    v.setPublishedBy(publisherName);
                });

        comment.setHasActiveDraft(false);
        comment.setEntityVersion(comment.getEntityVersion() + 1); // Increment version

        log.info("Published comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return comment;
    }

    /**
     * Unpublish a comment (change status from PUBLISHED to DRAFT).
     */
    public CommentDto unpublishComment(String contextKey, int dashboardId, String commentId) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        // Only superuser can unpublish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can unpublish comments");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(CommentStatus.DRAFT);
                    v.setPublishedDate(null);
                    v.setPublishedBy(null);
                });

        comment.setEntityVersion(comment.getEntityVersion() + 1); // Increment version

        log.info("Unpublished comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return comment;
    }

    /**
     * Clone a published comment for editing (creates a new DRAFT version).
     */
    public CommentDto cloneCommentForEdit(String contextKey, int dashboardId, String commentId,
                                          String newText, String newPointerUrl) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can edit
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this comment");
        }

        CommentVersionDto activeVersion = getActiveVersion(comment);
        if (activeVersion == null || activeVersion.getStatus() != CommentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be published to create a new edit version");
        }

        String editorName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        int newVersionNumber = comment.getHistory().stream()
                .mapToInt(CommentVersionDto::getVersion)
                .max()
                .orElse(0) + 1;

        CommentVersionDto newVersion = CommentVersionDto.builder()
                .version(newVersionNumber)
                .text(newText)
                .status(CommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(editorName)
                .deleted(false)
                .build();

        comment.getHistory().add(newVersion);
        comment.setActiveVersion(newVersionNumber);
        comment.setHasActiveDraft(true);
        if (newPointerUrl != null) {
            comment.setPointerUrl(newPointerUrl);
        }
        comment.setEntityVersion(comment.getEntityVersion() + 1); // Increment version

        log.info("Created new version {} for comment {} on dashboard {}/{}",
                newVersionNumber, commentId, contextKey, dashboardId);
        return comment;
    }

    /**
     * Restore a specific version of a comment.
     */
    public CommentDto restoreVersion(String contextKey, int dashboardId, String commentId, int versionNumber) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.get(key);

        if (comments == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR);
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can restore
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to restore this comment version");
        }


        comment.setActiveVersion(versionNumber);
        comment.setHasActiveDraft(false);
        comment.setEntityVersion(comment.getEntityVersion() + 1); // Increment version

        log.info("Restored comment {} to version {} for dashboard {}/{}",
                commentId, versionNumber, contextKey, dashboardId);
        return comment;
    }

    /**
     * Find the last published (non-deleted) version in the comment's history.
     */
    private CommentVersionDto findLastPublishedVersion(CommentDto comment) {
        return comment.getHistory().stream()
                .filter(v -> v.getStatus() == CommentStatus.PUBLISHED && !v.isDeleted())
                .max((v1, v2) -> Integer.compare(v1.getVersion(), v2.getVersion()))
                .orElse(null);
    }

    private CommentVersionDto getActiveVersion(CommentDto comment) {
        return comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .orElse(null);
    }

}

