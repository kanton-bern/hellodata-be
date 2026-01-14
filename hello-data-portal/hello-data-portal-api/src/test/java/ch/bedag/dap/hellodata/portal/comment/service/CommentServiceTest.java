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
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private UserRepository userRepository;

    private CommentService commentService;

    private static final String TEST_CONTEXT_KEY = "test-context";
    private static final int TEST_DASHBOARD_ID = 123;
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_NAME = "Test User";
    private static final String SUPERUSER_EMAIL = "admin@example.com";
    private static final String SUPERUSER_NAME = "Admin User";
    private static final String TEST_DASHBOARD_URL = "https://example.com/dashboard/123";
    private static final String TEST_POINTER_URL = "https://example.com/dashboard/123?tab=1";

    @BeforeEach
    void setUp() {
        commentService = new CommentService(userRepository);
    }

    @Test
    void getComments_shouldReturnEmptyListWhenNoComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user repository for admin role check
            UserEntity regularUser = new UserEntity();
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            List<CommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(comments).isEmpty();
        }
    }

    @Test
    void createComment_shouldCreateCommentWithDraftStatus() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            CommentDto result = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getAuthor()).isEqualTo(TEST_USER_NAME);
            assertThat(result.getAuthorEmail()).isEqualTo(TEST_USER_EMAIL);
            assertThat(result.getDashboardId()).isEqualTo(TEST_DASHBOARD_ID);
            assertThat(result.getDashboardUrl()).isEqualTo(TEST_DASHBOARD_URL);
            assertThat(result.getPointerUrl()).isEqualTo(TEST_POINTER_URL);
            assertThat(result.getContextKey()).isEqualTo(TEST_CONTEXT_KEY);
            assertThat(result.getActiveVersion()).isEqualTo(1);
            assertThat(result.isDeleted()).isFalse();
            assertThat(result.isHasActiveDraft()).isFalse();
            assertThat(result.getHistory()).hasSize(1);

            CommentVersionDto version = result.getHistory().get(0);
            assertThat(version.getVersion()).isEqualTo(1);
            assertThat(version.getText()).isEqualTo("Test comment");
            assertThat(version.getStatus()).isEqualTo(CommentStatus.DRAFT);
            assertThat(version.getEditedBy()).isEqualTo(TEST_USER_NAME);
            assertThat(version.isDeleted()).isFalse();
        }
    }

    @Test
    void getComments_shouldReturnOnlyPublishedCommentsForRegularUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Create draft comment
            CommentCreateDto draftDto = CommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, draftDto);

            // Create and publish another comment
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);
            CommentCreateDto publishedDto = CommentCreateDto.builder()
                    .text("Published comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto publishedComment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, publishedDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, publishedComment.getId());

            // Now check as regular user (different from author)
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn("Other User");
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user repository for admin role check
            UserEntity regularUser = new UserEntity();
            when(userRepository.findUserEntityByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(regularUser));

            List<CommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(publishedComment.getId());
        }
    }

    @Test
    void getComments_shouldReturnDraftCommentsForAuthor() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("My draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Mock user repository for admin role check
            UserEntity regularUser = new UserEntity();
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            List<CommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(comment.getId());
        }
    }

    @Test
    void getComments_shouldReturnAllDraftsForSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create draft as regular user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("User draft")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Check as superuser - no need for user repository mock as isSuperuser short-circuits the check
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            List<CommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(comments).hasSize(1);
        }
    }

    @Test
    void updateComment_shouldUpdateDraftComment() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            CommentUpdateDto updateDto = CommentUpdateDto.builder()
                    .text("Updated text")
                    .pointerUrl("https://example.com/dashboard/123?tab=2")
                    .build();

            CommentDto updated = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), updateDto);

            assertThat(updated.getId()).isEqualTo(comment.getId());
            assertThat(updated.getPointerUrl()).isEqualTo("https://example.com/dashboard/123?tab=2");
            assertThat(updated.getHistory().get(0).getText()).isEqualTo("Updated text");
        }
    }

    @Test
    void updateComment_shouldThrowExceptionWhenNotAuthorOrSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create comment as one user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to update as different user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentUpdateDto updateDto = CommentUpdateDto.builder()
                    .text("Updated text")
                    .build();

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, updateDto)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Not authorized to update this comment");
        }
    }

    @Test
    void publishComment_shouldPublishDraftComment() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Publish as superuser
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            CommentDto published = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            assertThat(published.isHasActiveDraft()).isFalse();
            CommentVersionDto activeVersion = published.getHistory().stream()
                    .filter(v -> v.getVersion() == published.getActiveVersion())
                    .findFirst()
                    .orElse(null);
            assertThat(activeVersion).isNotNull();
            assertThat(activeVersion.getStatus()).isEqualTo(CommentStatus.PUBLISHED);
            assertThat(activeVersion.getPublishedBy()).isEqualTo(SUPERUSER_NAME);
            assertThat(activeVersion.getPublishedDate()).isNotNull();
        }
    }

    @Test
    void publishComment_shouldThrowExceptionWhenNotSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only superusers can publish comments");
        }
    }

    @Test
    void unpublishComment_shouldUnpublishComment() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            CommentDto unpublished = commentService.unpublishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            CommentVersionDto activeVersion = unpublished.getHistory().stream()
                    .filter(v -> v.getVersion() == unpublished.getActiveVersion())
                    .findFirst()
                    .orElse(null);
            assertThat(activeVersion).isNotNull();
            assertThat(activeVersion.getStatus()).isEqualTo(CommentStatus.DRAFT);
            assertThat(activeVersion.getPublishedBy()).isNull();
            assertThat(activeVersion.getPublishedDate()).isNull();
        }
    }

    @Test
    void unpublishComment_shouldThrowExceptionWhenNotSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Try to unpublish as regular user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.unpublishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only superusers can unpublish comments");
        }
    }

    @Test
    void deleteComment_shouldSoftDeleteDraftWithNoPublishedVersions() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            CommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.getDeletedDate()).isNotNull();
            assertThat(deleted.getDeletedBy()).isEqualTo(TEST_USER_NAME);
        }
    }

    @Test
    void deleteComment_shouldRestoreToLastPublishedVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            // Create and publish comment
            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Create new version
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID,
                    comment.getId(), "Version 2", null);

            // Delete current version (should restore to version 1)
            CommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            assertThat(deleted.isDeleted()).isFalse();
            assertThat(deleted.getActiveVersion()).isEqualTo(1);
            assertThat(deleted.isHasActiveDraft()).isFalse();
        }
    }

    @Test
    void deleteComment_shouldThrowExceptionWhenNotAuthorOrSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to delete as different user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Not authorized to delete this comment");
        }
    }

    @Test
    void cloneCommentForEdit_shouldCreateNewDraftVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            // Create and publish comment
            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Clone for editing
            CommentDto cloned = commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID,
                    comment.getId(), "Version 2", TEST_POINTER_URL);

            assertThat(cloned.getHistory()).hasSize(2);
            assertThat(cloned.getActiveVersion()).isEqualTo(2);
            assertThat(cloned.isHasActiveDraft()).isTrue();
            assertThat(cloned.getPointerUrl()).isEqualTo(TEST_POINTER_URL);

            CommentVersionDto newVersion = cloned.getHistory().stream()
                    .filter(v -> v.getVersion() == 2)
                    .findFirst()
                    .orElse(null);
            assertThat(newVersion).isNotNull();
            assertThat(newVersion.getText()).isEqualTo("Version 2");
            assertThat(newVersion.getStatus()).isEqualTo(CommentStatus.DRAFT);
        }
    }

    @Test
    void cloneCommentForEdit_shouldThrowExceptionWhenNotPublished() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, "New text", null)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Comment must be published to create a new edit version");
        }
    }

    @Test
    void restoreVersion_shouldRestoreSpecificVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);

            // Create, publish, and create multiple versions
            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), "Version 2", null);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), "Version 3", null);

            // Restore to version 1
            CommentDto restored = commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), 1);

            assertThat(restored.getActiveVersion()).isEqualTo(1);
            assertThat(restored.isHasActiveDraft()).isFalse();
        }
    }

    @Test
    void restoreVersion_shouldThrowExceptionWhenNotAuthorOrSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to restore as different user
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, 1)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Not authorized to restore this comment version");
        }
    }

    @Test
    void getComments_shouldNotReturnDeletedComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            CommentCreateDto createDto = CommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            CommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Mock user repository for admin role check
            UserEntity regularUser = new UserEntity();
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            List<CommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(comments).isEmpty();
        }
    }
}

