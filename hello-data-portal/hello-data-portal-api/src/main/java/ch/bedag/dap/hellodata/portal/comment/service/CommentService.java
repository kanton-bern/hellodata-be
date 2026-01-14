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
import ch.bedag.dap.hellodata.portal.comment.entity.CommentEntity;
import ch.bedag.dap.hellodata.portal.comment.entity.CommentVersionEntity;
import ch.bedag.dap.hellodata.portal.comment.mapper.CommentMapper;
import ch.bedag.dap.hellodata.portal.comment.repository.CommentRepository;
import ch.bedag.dap.hellodata.portalcommon.role.entity.relation.UserContextRoleEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Service for managing dashboard comments with database persistence.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private static final String COMMENT_NOT_FOUND_ERROR = "CommentEntity not found";

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    /**
     * Get all comments for a dashboard. Filters based on user permissions.
     * - All users can see published comments
     * - Users can see their own drafts
     * - Admins (superuser, business_domain_admin, data_domain_admin) can see all drafts
     * - If active version is a draft by someone else, non-admins see the last published version instead
     */
    @Transactional(readOnly = true)
    public List<CommentDto> getComments(String contextKey, int dashboardId) {
        List<CommentEntity> comments = commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(contextKey, dashboardId);

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
        String authorFullName = SecurityUtils.getCurrentUserFullName();
        String authorEmail = SecurityUtils.getCurrentUserEmail();
        long now = System.currentTimeMillis();

        // Create comment entity
        CommentEntity comment = CommentEntity.builder()
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
                .entityVersion(0L) // Initial version
                .build();

        // Create first version
        CommentVersionEntity version = CommentVersionEntity.builder()
                .version(1)
                .text(createDto.getText())
                .status(CommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(authorFullName)
                .deleted(false)
                .build();

        comment.addVersion(version);

        // Save to database
        CommentEntity savedComment = commentRepository.save(comment);

        log.info("Created comment {} for dashboard {}/{}", savedComment.getId(), contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Update an existing comment (for DRAFT status).
     */
    public CommentDto updateComment(String contextKey, int dashboardId, String commentId, CommentUpdateDto updateDto) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can update
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this comment");
        }

        // Optimistic locking check
        long currentVersion = comment.getEntityVersion();
        long providedVersion = updateDto.getEntityVersion();

        if (currentVersion != providedVersion) {
            // Log suspicious attempts with extremely high version numbers (potential attack)
            long versionDiff = Math.abs(currentVersion - providedVersion);
            if (versionDiff > 100) {
                log.warn("Suspicious entity version mismatch detected for comment {}. Current: {}, Provided: {}, Difference: {}, User: {}",
                        commentId, currentVersion, providedVersion, versionDiff, currentUserEmail);
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CommentEntity was modified by another user. Please refresh and try again.");
        }

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

        // Entity version will be incremented automatically by @Version annotation
        CommentEntity savedComment = commentRepository.save(comment);

        log.info("Updated comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Delete a comment (soft delete).
     */
    public CommentDto deleteComment(String contextKey, int dashboardId, String commentId) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
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
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> v.setDeleted(true));

        // Try to find last non-deleted PUBLISHED version
        Optional<CommentVersionEntity> lastPublished = comment.getHistory().stream()
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

        CommentEntity savedComment = commentRepository.save(comment);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Publish a comment (change status from DRAFT to PUBLISHED).
     */
    public CommentDto publishComment(String contextKey, int dashboardId, String commentId) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Only superuser can publish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can publish comments");
        }

        String publisherName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(CommentStatus.PUBLISHED);
                    v.setPublishedDate(now);
                    v.setPublishedBy(publisherName);
                });

        comment.setHasActiveDraft(false);

        CommentEntity savedComment = commentRepository.save(comment);
        log.info("Published comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Unpublish a comment (change status from PUBLISHED to DRAFT).
     */
    public CommentDto unpublishComment(String contextKey, int dashboardId, String commentId) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Only superuser can unpublish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can unpublish comments");
        }

        comment.getHistory().stream()
                .filter(v -> v.getVersion().equals(comment.getActiveVersion()))
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(CommentStatus.DRAFT);
                    v.setPublishedDate(null);
                    v.setPublishedBy(null);
                });

        CommentEntity savedComment = commentRepository.save(comment);
        log.info("Unpublished comment {} for dashboard {}/{}", commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Clone a published comment for editing (creates a new DRAFT version).
     * This is a convenience method for backward compatibility.
     */
    public CommentDto cloneCommentForEdit(String contextKey, int dashboardId, String commentId,
                                          String newText, String newPointerUrl) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        CommentUpdateDto updateDto = CommentUpdateDto.builder()
                .text(newText)
                .pointerUrl(newPointerUrl)
                .entityVersion(comment.getEntityVersion())
                .build();
        return cloneCommentForEdit(contextKey, dashboardId, commentId, updateDto);
    }

    /**
     * Clone a published comment for editing (creates a new DRAFT version) - with optimistic locking support.
     */
    public CommentDto cloneCommentForEdit(String contextKey, int dashboardId, String commentId,
                                          CommentUpdateDto updateDto) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can edit
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this comment");
        }

        // IMPORTANT: Check optimistic locking FIRST before business validation
        long currentVersion = comment.getEntityVersion();
        long providedVersion = updateDto.getEntityVersion();

        if (currentVersion != providedVersion) {
            long versionDiff = Math.abs(currentVersion - providedVersion);
            if (versionDiff > 100) {
                log.warn("Suspicious entity version mismatch detected for comment clone/edit {}. Current: {}, Provided: {}, Difference: {}, User: {}",
                        commentId, currentVersion, providedVersion, versionDiff, currentUserEmail);
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CommentEntity was modified by another user. Please refresh and try again.");
        }

        // Now check business validation
        CommentVersionDto activeVersion = getActiveVersion(commentMapper.toDto(comment));
        if (activeVersion == null || activeVersion.getStatus() != CommentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CommentEntity must be published to create a new edit version");
        }

        String editorName = SecurityUtils.getCurrentUserFullName();
        long now = System.currentTimeMillis();

        int newVersionNumber = comment.getHistory().stream()
                .mapToInt(CommentVersionEntity::getVersion)
                .max()
                .orElse(0) + 1;

        CommentVersionEntity newVersion = CommentVersionEntity.builder()
                .version(newVersionNumber)
                .text(updateDto.getText())
                .status(CommentStatus.DRAFT)
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

        CommentEntity savedComment = commentRepository.save(comment);
        log.info("Created new version {} for comment {} on dashboard {}/{}",
                newVersionNumber, commentId, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Restore a specific version of a comment.
     */
    public CommentDto restoreVersion(String contextKey, int dashboardId, String commentId, int versionNumber) {
        CommentEntity comment = commentRepository.findByIdWithHistory(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND_ERROR));

        // Check permissions - only author or superuser can restore
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAuthor = currentUserEmail != null && currentUserEmail.equals(comment.getAuthorEmail());
        if (!isAuthor && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to restore this comment version");
        }

        comment.setActiveVersion(versionNumber);
        comment.setHasActiveDraft(false);

        CommentEntity savedComment = commentRepository.save(comment);
        log.info("Restored comment {} to version {} for dashboard {}/{}",
                commentId, versionNumber, contextKey, dashboardId);
        return commentMapper.toDto(savedComment);
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

