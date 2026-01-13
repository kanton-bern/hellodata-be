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
import ch.bedag.dap.hellodata.portal.comment.data.*;
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
     * Get all comments for a dashboard. Includes drafts for current user and superusers.
     */
    public List<CommentDto> getComments(String contextKey, int dashboardId) {
        String key = buildKey(contextKey, dashboardId);
        List<CommentDto> comments = commentsStore.getOrDefault(key, new ArrayList<>());

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isSuperuser = SecurityUtils.isSuperuser();

        // Filter: show non-deleted comments, but for drafts only show to author or superuser
        return comments.stream()
                .filter(c -> !c.isDeleted())
                .filter(c -> {
                    CommentVersionDto activeVersion = getActiveVersion(c);
                    if (activeVersion == null) return false;

                    // Published comments are visible to everyone
                    if (activeVersion.getStatus() == CommentStatus.PUBLISHED) {
                        return true;
                    }

                    // Drafts are only visible to author or superuser
                    return c.getAuthorEmail().equals(currentUserEmail) || isSuperuser;
                })
                .toList();
    }

    /**
     * Create a new comment.
     */
    public CommentDto createComment(String contextKey, int dashboardId, CommentCreateDto createDto) {
        String key = buildKey(contextKey, dashboardId);

        String authorName = SecurityUtils.getCurrentUsername();
        String authorEmail = SecurityUtils.getCurrentUserEmail();
        long now = System.currentTimeMillis();

        CommentVersionDto version = CommentVersionDto.builder()
                .version(1)
                .text(createDto.getText())
                .status(CommentStatus.DRAFT)
                .editedDate(now)
                .editedBy(authorName)
                .deleted(false)
                .build();

        CommentDto comment = CommentDto.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(dashboardId)
                .dashboardUrl(createDto.getDashboardUrl())
                .contextKey(contextKey)
                .pointerUrl(createDto.getPointerUrl())
                .author(authorName)
                .authorEmail(authorEmail)
                .createdDate(now)
                .deleted(false)
                .activeVersion(1)
                .hasActiveDraft(false)
                .history(new ArrayList<>(List.of(version)))
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        // Check permissions - only author or superuser can update
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!comment.getAuthorEmail().equals(currentUserEmail) && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this comment");
        }

        // Update the active version in history
        String editorName = SecurityUtils.getCurrentUsername();
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        // Check permissions - only author or superuser can delete
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!comment.getAuthorEmail().equals(currentUserEmail) && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this comment");
        }

        String deleterName = SecurityUtils.getCurrentUsername();
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        // Only superuser can publish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can publish comments");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        String publisherName = SecurityUtils.getCurrentUsername();
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        // Only superuser can unpublish
        if (!SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superusers can unpublish comments");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .ifPresent(v -> {
                    v.setStatus(CommentStatus.DRAFT);
                    v.setPublishedDate(null);
                    v.setPublishedBy(null);
                });

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        // Check permissions - only author or superuser can edit
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!comment.getAuthorEmail().equals(currentUserEmail) && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this comment");
        }

        CommentVersionDto activeVersion = getActiveVersion(comment);
        if (activeVersion == null || activeVersion.getStatus() != CommentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be published to create a new edit version");
        }

        String editorName = SecurityUtils.getCurrentUsername();
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        CommentDto comment = comments.stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        // Check permissions - only author or superuser can restore
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (!comment.getAuthorEmail().equals(currentUserEmail) && !SecurityUtils.isSuperuser()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to restore this comment version");
        }


        comment.setActiveVersion(versionNumber);
        comment.setHasActiveDraft(false);

        log.info("Restored comment {} to version {} for dashboard {}/{}",
                commentId, versionNumber, contextKey, dashboardId);
        return comment;
    }

    private CommentVersionDto getActiveVersion(CommentDto comment) {
        return comment.getHistory().stream()
                .filter(v -> v.getVersion() == comment.getActiveVersion())
                .findFirst()
                .orElse(null);
    }

}

