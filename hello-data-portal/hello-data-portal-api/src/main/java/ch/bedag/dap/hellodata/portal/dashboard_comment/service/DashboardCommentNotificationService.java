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

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portal.email.service.EmailNotificationService;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Sends comment-related email notifications asynchronously so that the triggering CRUD request
 * (send for review, publish, decline, delete, edit by reviewer) returns immediately instead of
 * blocking on the synchronous SMTP send.
 * <p>
 * All entry points receive plain values captured on the request thread: no JPA entities (to avoid
 * lazy-loading outside the original session) and no reliance on the {@code SecurityContext} (which
 * is not propagated to the async thread).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardCommentNotificationService {

    private static final String DASHBOARD_TITLE_PREFIX = "Dashboard ";

    private final MetaInfoResourceService metaInfoResourceService;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final DashboardCommentPermissionRepository commentPermissionRepository;

    /**
     * Notify all reviewers of a context that a comment was sent for review.
     */
    @Async
    public void notifySentForReview(String contextKey, int dashboardId, String commentId, String commentText,
                                    String senderEmail, String senderFullName) {
        try {
            String dashboardName = getDashboardTitle(contextKey, dashboardId);
            List<DashboardCommentPermissionEntity> reviewerPermissions =
                    commentPermissionRepository.findByContextKeyAndReviewCommentsTrue(contextKey);
            for (DashboardCommentPermissionEntity reviewerPerm : reviewerPermissions) {
                notifySingleReviewer(reviewerPerm, commentId, senderEmail, commentText, dashboardName, senderFullName);
            }
        } catch (Exception e) {
            log.warn("Failed to send sent-for-review notifications for comment {}: {}", commentId, e.getMessage());
        }
    }

    private void notifySingleReviewer(DashboardCommentPermissionEntity reviewerPerm, String commentId,
                                      String senderEmail, String commentText, String dashboardName, String senderFullName) {
        try {
            Optional<UserEntity> reviewerUser = userRepository.findById(reviewerPerm.getUserId());
            if (reviewerUser.isEmpty()) {
                return;
            }
            UserEntity reviewer = reviewerUser.get();
            if (!reviewer.isEnabled()) {
                log.debug("Reviewer {} is disabled, skipping notification for comment {}", reviewer.getEmail(), commentId);
                return;
            }
            String reviewerEmail = reviewer.getEmail();
            boolean isSelf = reviewerEmail != null && reviewerEmail.equalsIgnoreCase(senderEmail);
            if (isSelf) {
                log.debug("Skipping self-notification for reviewer {} on comment {}", reviewerEmail, commentId);
                return;
            }
            String reviewerFirstName = reviewer.getFirstName() != null ? reviewer.getFirstName() : reviewerEmail;
            Locale reviewerLocale = reviewer.getSelectedLanguage();
            emailNotificationService.notifyAboutCommentSentForReview(
                    reviewerFirstName, reviewerEmail, commentText, dashboardName, senderFullName, reviewerLocale);
        } catch (Exception e) {
            log.warn("Failed to send sent-for-review notification to reviewer {}: {}", reviewerPerm.getUserId(), e.getMessage());
        }
    }

    /**
     * Notify the comment author that their comment was published or declined.
     */
    @Async
    public void notifyStatusChange(String contextKey, int dashboardId, String commentId, String authorEmail,
                                   String authorNameFallback, String commentText, String actorEmail,
                                   String reviewerFullName, boolean published, String declineReason) {
        try {
            ResolvedAuthor author = resolveAuthor(commentId, authorEmail, authorNameFallback, actorEmail);
            if (author == null) {
                return;
            }
            String dashboardName = getDashboardTitle(contextKey, dashboardId);
            if (published) {
                emailNotificationService.notifyAboutCommentPublished(
                        author.firstName(), authorEmail, commentText, dashboardName, reviewerFullName, author.locale());
            } else {
                emailNotificationService.notifyAboutCommentDeclined(
                        author.firstName(), authorEmail, commentText, dashboardName, declineReason, reviewerFullName, author.locale());
            }
        } catch (Exception e) {
            log.warn("Failed to send status-change notification for comment {}: {}", commentId, e.getMessage());
        }
    }

    /**
     * Notify the comment author that their comment was deleted by another user.
     */
    @Async
    public void notifyDeleted(String contextKey, int dashboardId, String commentId, String authorEmail,
                              String authorNameFallback, String commentText, String deleterEmail,
                              String deleterFullName, String deletionReason) {
        try {
            ResolvedAuthor author = resolveAuthor(commentId, authorEmail, authorNameFallback, deleterEmail);
            if (author == null) {
                return;
            }
            String dashboardName = getDashboardTitle(contextKey, dashboardId);
            emailNotificationService.notifyAboutCommentDeleted(
                    author.firstName(), authorEmail, commentText, dashboardName, deletionReason, deleterFullName, author.locale());
        } catch (Exception e) {
            log.warn("Failed to send deletion notification for comment {}: {}", commentId, e.getMessage());
        }
    }

    /**
     * Notify the comment author that their comment was edited by a reviewer.
     */
    @Async
    public void notifyEditedByReviewer(String contextKey, int dashboardId, String commentId, String authorEmail,
                                       String authorNameFallback, String newText, String editorEmail, String reviewerFullName) {
        try {
            ResolvedAuthor author = resolveAuthor(commentId, authorEmail, authorNameFallback, editorEmail);
            if (author == null) {
                return;
            }
            String dashboardName = getDashboardTitle(contextKey, dashboardId);
            emailNotificationService.notifyAboutCommentEditedByReviewer(
                    author.firstName(), authorEmail, newText, dashboardName, reviewerFullName, author.locale());
        } catch (Exception e) {
            log.warn("Failed to send edited-by-reviewer notification for comment {}: {}", commentId, e.getMessage());
        }
    }

    /**
     * Resolves the author's display name and locale, applying the shared skip rules (missing email,
     * self-notification, disabled author). Returns {@code null} when no notification should be sent.
     */
    private ResolvedAuthor resolveAuthor(String commentId, String authorEmail, String authorNameFallback, String actorEmail) {
        if (authorEmail == null || authorEmail.isBlank()) {
            log.debug("No author email for comment {}, skipping notification", commentId);
            return null;
        }
        if (actorEmail != null && actorEmail.equalsIgnoreCase(authorEmail)) {
            log.debug("Actor is the author, skipping self-notification for comment {}", commentId);
            return null;
        }
        String authorFirstName = authorNameFallback;
        Locale authorLocale = null;
        Optional<UserEntity> authorUser = userRepository.findUserEntityByEmailIgnoreCase(authorEmail);
        if (authorUser.isPresent()) {
            UserEntity author = authorUser.get();
            if (!author.isEnabled()) {
                log.debug("Author {} is disabled, skipping notification for comment {}", authorEmail, commentId);
                return null;
            }
            if (author.getFirstName() != null) {
                authorFirstName = author.getFirstName();
            }
            authorLocale = author.getSelectedLanguage();
        }
        return new ResolvedAuthor(authorFirstName, authorLocale);
    }

    private String getDashboardTitle(String contextKey, int dashboardId) {
        try {
            DashboardResource dashboardResource = metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(
                    ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_DASHBOARDS, contextKey, DashboardResource.class);
            if (dashboardResource != null && dashboardResource.getData() != null) {
                return dashboardResource.getData().stream()
                        .filter(dashboard -> dashboard.getId() == dashboardId)
                        .map(SupersetDashboard::getDashboardTitle)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(DASHBOARD_TITLE_PREFIX + dashboardId);
            }
        } catch (Exception e) {
            log.warn("Could not fetch dashboard title for context {}: {}", contextKey, e.getMessage());
        }
        return DASHBOARD_TITLE_PREFIX + dashboardId;
    }

    private record ResolvedAuthor(String firstName, Locale locale) {
    }
}
