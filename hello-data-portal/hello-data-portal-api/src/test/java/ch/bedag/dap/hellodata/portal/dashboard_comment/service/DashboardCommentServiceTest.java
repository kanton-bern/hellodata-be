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
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.DashboardResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard.response.superset.SupersetDashboard;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.*;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentVersionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.mapper.DashboardCommentMapper;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentRepository;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardCommentServiceTest {

    /**
     * Comment Permission System:
     * - READ: Can view published comments only
     * - WRITE: Can create/edit own comments (drafts), view published + own drafts
     * - REVIEW: Can view all comments including all drafts from all users
     * - ADMIN (superuser/data_domain_admin/business_domain_admin): Full access to all operations
     * <p>
     * Tests are organized to verify each permission level and operation.
     */

    @Mock
    private UserRepository userRepository;

    @Mock
    private DashboardCommentRepository commentRepository;

    @Mock
    private DashboardCommentPermissionRepository commentPermissionRepository;

    @Mock
    private DashboardCommentMapper commentMapper;

    @Mock
    private MetaInfoResourceService metaInfoResourceService;

    private DashboardCommentService commentService;

    // In-memory storage for tests
    private final Map<String, DashboardCommentEntity> commentStore = new HashMap<>();

    private static final String TEST_CONTEXT_KEY = "test-context";
    private static final int TEST_DASHBOARD_ID = 123;
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_NAME = "Test User";
    private static final String SUPERUSER_EMAIL = "admin@example.com";
    private static final String SUPERUSER_NAME = "Admin User";
    private static final String REVIEWER_EMAIL = "reviewer@example.com";
    private static final String REVIEWER_NAME = "Reviewer User";
    private static final String TEST_DASHBOARD_URL = "https://example.com/superset/dashboard/123/?standalone=1";
    private static final String TEST_POINTER_URL = "https://example.com/dashboard/123?tab=1";
    private static final String TEST_INSTANCE_NAME = "example.com";

    @BeforeEach
    void setUp() {
        commentStore.clear();
        commentService = new DashboardCommentService(commentRepository, commentPermissionRepository, commentMapper, metaInfoResourceService, userRepository);

        // Setup default mocking behavior for repository save
        lenient().when(commentRepository.save(any(DashboardCommentEntity.class)))
                .thenAnswer(invocation -> {
                    DashboardCommentEntity entity = invocation.getArgument(0);

                    // If entity doesn't have ID, assign one (simulating database auto-generation for INSERT)
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID().toString());
                    }


                    // Store in memory
                    commentStore.put(entity.getId(), entity);
                    return entity;
                });

        // Setup mocking for findByIdWithHistory
        lenient().when(commentRepository.findByIdWithHistory(any(String.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    return Optional.ofNullable(commentStore.get(id));
                });

        // Setup mocking for findByIdWithHistoryForUpdate
        lenient().when(commentRepository.findByIdWithHistoryForUpdate(any(String.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    return Optional.ofNullable(commentStore.get(id));
                });

        // Setup mocking for findByContextKeyAndDashboardIdOrderByCreatedDateAsc
        lenient().when(commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(any(String.class), any(Integer.class)))
                .thenAnswer(invocation -> {
                    String contextKey = invocation.getArgument(0);
                    Integer dashboardId = invocation.getArgument(1);
                    return commentStore.values().stream()
                            .filter(c -> c.getContextKey().equals(contextKey) && c.getDashboardId().equals(dashboardId))
                            .sorted(Comparator.comparing(DashboardCommentEntity::getCreatedDate))
                            .collect(Collectors.toList());
                });

        // Setup default mocking for mapper
        lenient().when(commentMapper.toDto(any(DashboardCommentEntity.class)))
                .thenAnswer(invocation -> {
                    DashboardCommentEntity entity = invocation.getArgument(0);
                    return convertEntityToDto(entity);
                });

        // Setup mocking for tagsToString (used when creating/updating version entities)
        lenient().when(commentMapper.tagsToString(any()))
                .thenAnswer(invocation -> {
                    List<String> tags = invocation.getArgument(0);
                    if (tags == null || tags.isEmpty()) {
                        return null;
                    }
                    return String.join(",", tags);
                });
    }

    // Helper method to convert entity to DTO for testing (mirrors real mapper logic)
    private DashboardCommentDto convertEntityToDto(DashboardCommentEntity entity) {
        List<DashboardCommentVersionDto> historyDto = entity.getHistory().stream()
                .map(v -> DashboardCommentVersionDto.builder()
                        .version(v.getVersion())
                        .text(v.getText())
                        .status(v.getStatus())
                        .editedDate(v.getEditedDate())
                        .editedBy(v.getEditedBy())
                        .publishedDate(v.getPublishedDate())
                        .publishedBy(v.getPublishedBy())
                        .deleted(v.isDeleted())
                        .pointerUrl(v.getPointerUrl())
                        .tags(parseTagsFromString(v.getTags()))
                        .deletionReason(v.getDeletionReason())
                        .declineReason(v.getDeclineReason())
                        .build())
                .collect(Collectors.toList());

        // Derive pointerUrl and tags from active version (same as real mapper)
        DashboardCommentVersionDto activeVersion = historyDto.stream()
                .filter(v -> v.getVersion() == entity.getActiveVersion())
                .findFirst()
                .orElse(null);

        return DashboardCommentDto.builder()
                .id(entity.getId())
                .dashboardId(entity.getDashboardId())
                .dashboardUrl(entity.getDashboardUrl())
                .contextKey(entity.getContextKey())
                .pointerUrl(activeVersion != null ? activeVersion.getPointerUrl() : null)
                .author(entity.getAuthor())
                .authorEmail(entity.getAuthorEmail())
                .createdDate(entity.getCreatedDate())
                .deleted(entity.isDeleted())
                .deletedDate(entity.getDeletedDate())
                .deletedBy(entity.getDeletedBy())
                .deletionReason(entity.getDeletionReason())
                .activeVersion(entity.getActiveVersion())
                .hasActiveDraft(entity.isHasActiveDraft())
                .entityVersion(entity.getEntityVersion())
                .history(historyDto)
                .tags(activeVersion != null && activeVersion.getTags() != null ? activeVersion.getTags() : Collections.emptyList())
                .build();
    }

    private List<String> parseTagsFromString(String tagsString) {
        if (tagsString == null || tagsString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Helper methods for creating permissions
    private DashboardCommentPermissionEntity createReadOnlyPermission() {
        DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
        permission.setReadComments(true);
        permission.setWriteComments(false);
        permission.setReviewComments(false);
        return permission;
    }

    private DashboardCommentPermissionEntity createWritePermission() {
        DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
        permission.setReadComments(true);
        permission.setWriteComments(true);
        permission.setReviewComments(false);
        return permission;
    }

    private DashboardCommentPermissionEntity createReviewPermission() {
        DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
        permission.setReadComments(true);
        permission.setWriteComments(true);
        permission.setReviewComments(true);
        return permission;
    }

    private void mockPermissionForUser(UUID userId, DashboardCommentPermissionEntity permission) {
        when(commentPermissionRepository.findByUserIdAndContextKey(userId, TEST_CONTEXT_KEY))
                .thenReturn(Optional.of(permission));
    }

    private void mockSuperUser(MockedStatic<SecurityUtils> securityUtils) {
        securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(SUPERUSER_EMAIL);
        securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(SUPERUSER_NAME);
        UUID superUserId = UUID.randomUUID();
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(superUserId);
        securityUtils.when(SecurityUtils::isSuperuser).thenReturn(true);
        mockPermissionForUser(superUserId, createReviewPermission());
    }

    @Test
    void getComments_shouldReturnEmptyListWhenUserHasNoReadPermission() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock no permission found
            when(commentPermissionRepository.findByUserIdAndContextKey(testUserId, TEST_CONTEXT_KEY))
                    .thenReturn(Optional.empty());

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // Without read permission, user should see no comments
            assertThat(comments).isEmpty();
        }
    }

    @Test
    void getComments_shouldReturnEmptyListWhenNoComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user with read permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock permission entity with read access
            DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
            permission.setReadComments(true);
            permission.setWriteComments(false);
            permission.setReviewComments(false);
            when(commentPermissionRepository.findByUserIdAndContextKey(any(), eq(TEST_CONTEXT_KEY)))
                    .thenReturn(Optional.of(permission));

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            assertThat(comments).isEmpty();
        }
    }

    @Test
    void createComment_shouldFailWhenUserHasNoWritePermission() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock permission with only read access, no write
            DashboardCommentPermissionEntity permission = createReadOnlyPermission();
            mockPermissionForUser(testUserId, permission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();

            assertThatThrownBy(() ->
                    commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No write permission for comments");
        }
    }

    @Test
    void createComment_shouldCreateCommentWithDraftStatus() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user without admin privileges
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock permission entity with write access
            DashboardCommentPermissionEntity permission = new DashboardCommentPermissionEntity();
            permission.setWriteComments(true);
            when(commentPermissionRepository.findByUserIdAndContextKey(any(), eq(TEST_CONTEXT_KEY)))
                    .thenReturn(Optional.of(permission));

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto result = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

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

            DashboardCommentVersionDto version = result.getHistory().get(0);
            assertThat(version.getVersion()).isEqualTo(1);
            assertThat(version.getText()).isEqualTo("Test comment");
            assertThat(version.getStatus()).isEqualTo(DashboardCommentStatus.DRAFT);
            assertThat(version.getEditedBy()).isEqualTo(TEST_USER_NAME);
            assertThat(version.isDeleted()).isFalse();
        }
    }

    @Test
    void getComments_shouldReturnOnlyPublishedCommentsForRegularUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // User with write permission can create drafts
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create draft comment
            DashboardCommentCreateDto draftDto = DashboardCommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, draftDto);

            // Create and publish another comment as superuser
            mockSuperUser(securityUtils);

            DashboardCommentCreateDto publishedDto = DashboardCommentCreateDto.builder()
                    .text("Published comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto publishedComment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, publishedDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, publishedComment.getId());

            // Now check as different user with only read permission
            UUID otherUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn("Other User");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(otherUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity otherUser = new UserEntity();
            otherUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherUser));

            // Mock read-only permission for other user
            DashboardCommentPermissionEntity readPermission = createReadOnlyPermission();
            mockPermissionForUser(otherUserId, readPermission);

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // Should only see the published comment, not the draft
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(publishedComment.getId());
        }
    }

    @Test
    void getComments_shouldReturnDraftCommentsForAuthor() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock write permission for user
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("My draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // User with WRITE permission should see their own drafts
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(comment.getId());
        }
    }

    @Test
    void getComments_shouldNotReturnDraftsForReviewerIfNotAuthor() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create draft as regular user with write permission
            UUID authorUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity authorUser = new UserEntity();
            authorUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(authorUser));

            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("User draft")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Check as user with review permission (not author, not superuser)
            UUID reviewerUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("reviewer@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn("Reviewer");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity reviewerUser = new UserEntity();
            reviewerUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("reviewer@example.com")).thenReturn(Optional.of(reviewerUser));

            // Mock review permission
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerUserId, reviewPermission);

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // Reviewer should NOT see other users' DRAFT comments (only READY_FOR_REVIEW)
            // Since the draft has no published version, reviewer should not see it at all
            assertThat(comments).isEmpty();
        }
    }

    @Test
    void getComments_reviewerSeesReadyForReviewButNotDraft() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create two comments as author
            UUID authorUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity authorUser = new UserEntity();
            authorUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(authorUser));

            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorUserId, writePermission);

            // Create first comment and send for review
            DashboardCommentCreateDto createDto1 = DashboardCommentCreateDto.builder()
                    .text("Comment for review")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment1 = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto1);
            commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment1.getId());

            // Switch to reviewer user
            mockSuperUser(securityUtils);

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // Reviewer should see only the READY_FOR_REVIEW comment, not the DRAFT
            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(comment1.getId());
            assertThat(comments.get(0).getHistory().stream()
                    .filter(v -> v.getVersion() == comments.get(0).getActiveVersion())
                    .findFirst()
                    .get()
                    .getStatus()).isEqualTo(DashboardCommentStatus.READY_FOR_REVIEW);
        }
    }

    @Test
    void updateComment_shouldUpdateDraftComment() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            // Mock write permission
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
                    .text("Updated text")
                    .pointerUrl("https://example.com/dashboard/123?tab=2")
                    .entityVersion(comment.getEntityVersion()) // Use current entityVersion
                    .build();

            DashboardCommentDto updated = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), updateDto);

            assertThat(updated.getId()).isEqualTo(comment.getId());
            assertThat(updated.getPointerUrl()).isEqualTo("https://example.com/dashboard/123?tab=2");
            assertThat(updated.getHistory().get(0).getText()).isEqualTo("Updated text");
        }
    }

    @Test
    void updateComment_shouldThrowExceptionWhenNotAuthorOrSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create comment as one user
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to update as different user
            UUID otherUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(otherUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock other user without admin privileges but with write permission
            UserEntity otherUser = new UserEntity();
            otherUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherUser));

            // Mock write permission for other user
            DashboardCommentPermissionEntity otherWritePermission = createWritePermission();
            mockPermissionForUser(otherUserId, otherWritePermission);

            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
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
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission for creating comment
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Send for review as author
            DashboardCommentDto readyForReview = commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Verify status is READY_FOR_REVIEW
            DashboardCommentVersionDto reviewVersion = readyForReview.getHistory().stream()
                    .filter(v -> v.getVersion() == readyForReview.getActiveVersion())
                    .findFirst()
                    .orElse(null);
            assertThat(reviewVersion).isNotNull();
            assertThat(reviewVersion.getStatus()).isEqualTo(DashboardCommentStatus.READY_FOR_REVIEW);

            // Publish as superuser
            mockSuperUser(securityUtils);

            DashboardCommentDto published = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            assertThat(published.isHasActiveDraft()).isFalse();
            DashboardCommentVersionDto activeVersion = published.getHistory().stream()
                    .filter(v -> v.getVersion() == published.getActiveVersion())
                    .findFirst()
                    .orElse(null);
            assertThat(activeVersion).isNotNull();
            assertThat(activeVersion.getStatus()).isEqualTo(DashboardCommentStatus.PUBLISHED);
            assertThat(activeVersion.getPublishedBy()).isEqualTo(SUPERUSER_NAME);
            assertThat(activeVersion.getPublishedDate()).isNotNull();
        }
    }

    @Test
    void publishComment_shouldThrowExceptionWhenNotSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission for creating comment
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Send for review as author
            commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Try to publish without review permissions
            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No review permission for comments");
        }
    }

    @Test
    void sendForReview_shouldChangeStatusFromDraftToReadyForReview() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create draft comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Send for review
            DashboardCommentDto sentForReview = commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            DashboardCommentVersionDto activeVersion = sentForReview.getHistory().stream()
                    .filter(v -> v.getVersion() == sentForReview.getActiveVersion())
                    .findFirst()
                    .orElse(null);
            assertThat(activeVersion).isNotNull();
            assertThat(activeVersion.getStatus()).isEqualTo(DashboardCommentStatus.READY_FOR_REVIEW);
        }
    }

    @Test
    void sendForReview_shouldThrowExceptionWhenNotAuthorAndNotReviewer() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create comment as author
            UUID authorId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity author = new UserEntity();
            author.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(author));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to send for review as different user with only READ permission (not author, not reviewer)
            UUID otherUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(otherUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity otherUser = new UserEntity();
            otherUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherUser));
            // Give WRITE permission (to pass checkHasAccessToComment) but not REVIEW (so not reviewer)
            DashboardCommentPermissionEntity otherWritePermission = createWritePermission();
            mockPermissionForUser(otherUserId, otherWritePermission);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Not authorized");
        }
    }

    @Test
    void sendForReview_shouldAllowReviewerToSendForReview() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create comment as author
            UUID authorId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity author = new UserEntity();
            author.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(author));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Publish it
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(REVIEWER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Reviewer edits comment (creates new draft)
            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
                    .text("Updated by reviewer")
                    .entityVersion(1)
                    .build();
            DashboardCommentDto updated = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), updateDto);

            // Reviewer should be able to send for review
            DashboardCommentDto sentForReview = commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, updated.getId());

            assertThat(sentForReview).isNotNull();
            DashboardCommentVersionDto activeVersion = sentForReview.getHistory().stream()
                    .filter(v -> v.getVersion() == sentForReview.getActiveVersion())
                    .findFirst()
                    .orElseThrow();
            assertThat(activeVersion.getStatus()).isEqualTo(DashboardCommentStatus.READY_FOR_REVIEW);
        }
    }

    @Test
    void deleteComment_shouldSoftDeleteDraftWithNoPublishedVersions() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            DashboardCommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), false, null);

            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.getDeletedDate()).isNotNull();
            assertThat(deleted.getDeletedBy()).isEqualTo(TEST_USER_NAME);
        }
    }

    @Test
    void deleteComment_shouldRestoreToLastPublishedVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            DashboardCommentDto publishedComment = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Create new version with entityVersion
            DashboardCommentUpdateDto editDto = DashboardCommentUpdateDto.builder()
                    .text("Version 2")
                    .entityVersion(publishedComment.getEntityVersion())
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), editDto);

            // Delete current version (should restore to version 1)
            DashboardCommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), false, null);

            assertThat(deleted.isDeleted()).isFalse();
            assertThat(deleted.getActiveVersion()).isEqualTo(1);
            assertThat(deleted.isHasActiveDraft()).isFalse();
        }
    }

    @Test
    void deleteComment_shouldThrowExceptionWhenNotAuthorOrSuperuser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Try to delete as different user
            UUID otherUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("other@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(otherUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock other user without admin privileges but with write permission
            UserEntity otherUser = new UserEntity();
            otherUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherUser));

            // Mock write permission for other user
            DashboardCommentPermissionEntity otherWritePermission = createWritePermission();
            mockPermissionForUser(otherUserId, otherWritePermission);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, false, null)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Not authorized to delete this comment");
        }
    }

    @Test
    void deleteComment_shouldRequireDeletionReasonForReviewer() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Setup author to create the comment
            UUID authorId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity author = new UserEntity();
            author.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(author));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorId, writePermission);

            // Create comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Switch to reviewer
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("reviewer@example.com");
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn("Reviewer");
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase("reviewer@example.com")).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            // Try to delete without reason - should fail
            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, false, null)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Deletion reason is required");

            // Try to delete with empty reason - should fail
            assertThatThrownBy(() ->
                    commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, false, "  ")
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Deletion reason is required");
        }
    }

    @Test
    void deleteComment_shouldStoreDeletionReason() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Delete with reason
            String deletionReason = "This comment violates policy";
            DashboardCommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID,
                    comment.getId(), false, deletionReason);

            // Verify deletion reason is stored on the deleted version
            DashboardCommentVersionDto deletedVersion = deleted.getHistory().stream()
                    .filter(v -> v.getStatus() == DashboardCommentStatus.DELETED)
                    .findFirst()
                    .orElse(null);

            assertThat(deletedVersion).isNotNull();
            assertThat(deletedVersion.getDeletionReason()).isEqualTo(deletionReason);
        }
    }

    @Test
    void deleteComment_shouldStoreDeletionReasonOnEntityWhenDeleteEntire() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Delete entire comment with reason
            String deletionReason = "This comment is no longer relevant";
            DashboardCommentDto deleted = commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID,
                    comment.getId(), true, deletionReason);

            // Verify deletion reason is stored on the main entity
            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.getDeletedBy()).isEqualTo(SUPERUSER_NAME);
            assertThat(deleted.getDeletionReason()).isEqualTo(deletionReason);
        }
    }

    @Test
    void cloneCommentForEdit_shouldCreateNewDraftVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            DashboardCommentDto publishedComment = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Clone for editing with entityVersion
            DashboardCommentUpdateDto editDto = DashboardCommentUpdateDto.builder()
                    .text("Version 2")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(publishedComment.getEntityVersion())
                    .build();
            DashboardCommentDto cloned = commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID,
                    comment.getId(), editDto);

            assertThat(cloned.getHistory()).hasSize(2);
            assertThat(cloned.getActiveVersion()).isEqualTo(2);
            assertThat(cloned.isHasActiveDraft()).isTrue();
            assertThat(cloned.getPointerUrl()).isEqualTo(TEST_POINTER_URL);

            DashboardCommentVersionDto newVersion = cloned.getHistory().stream()
                    .filter(v -> v.getVersion() == 2)
                    .findFirst()
                    .orElse(null);
            assertThat(newVersion).isNotNull();
            assertThat(newVersion.getText()).isEqualTo("Version 2");
            assertThat(newVersion.getStatus()).isEqualTo(DashboardCommentStatus.DRAFT);
        }
    }

    @Test
    void cloneCommentForEdit_shouldThrowExceptionWhenNotPublished() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            String commentId = comment.getId();
            DashboardCommentUpdateDto editDto = DashboardCommentUpdateDto.builder()
                    .text("New text")
                    .entityVersion(comment.getEntityVersion())
                    .build();
            assertThatThrownBy(() ->
                    commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, editDto)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("must be published or declined to create a new edit version");
        }
    }

    @Test
    void restoreVersion_shouldRestoreSpecificVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Setup author
            UUID authorId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity author = new UserEntity();
            author.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(author));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorId, writePermission);

            // Create comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Switch to reviewer to publish
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(REVIEWER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            DashboardCommentDto published1 = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Back to author for edit
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);

            DashboardCommentUpdateDto editDto2 = DashboardCommentUpdateDto.builder()
                    .text("Version 2")
                    .entityVersion(published1.getEntityVersion())
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), editDto2);

            // Reviewer publishes version 2
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);
            DashboardCommentDto published2 = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Author creates version 3
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorId);

            DashboardCommentUpdateDto editDto3 = DashboardCommentUpdateDto.builder()
                    .text("Version 3")
                    .entityVersion(published2.getEntityVersion())
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), editDto3);

            // Reviewer restores to version 1
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            DashboardCommentDto restored = commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), 1);

            assertThat(restored.getActiveVersion()).isEqualTo(1);
            assertThat(restored.isHasActiveDraft()).isFalse();
        }
    }

    @Test
    void restoreVersion_shouldThrowExceptionWhenNotReviewer() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission (but no review permission)
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Publish as reviewer
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Try to restore as author (who has write but not review permission)
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            String commentId = comment.getId();
            assertThatThrownBy(() ->
                    commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, 1)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only reviewers can restore comment versions");
        }
    }

    @Test
    void restoreVersion_shouldAllowReviewerToRestoreVersion() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Publish as reviewer
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(REVIEWER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Edit as author (creates v2)
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
                    .text("Test comment version 2")
                    .entityVersion(1)
                    .build();
            commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), updateDto);

            // Publish v2 as reviewer
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Reviewer should be able to restore version 1
            DashboardCommentDto restored = commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), 1);

            assertThat(restored.getActiveVersion()).isEqualTo(1);
            DashboardCommentVersionDto activeVersion = restored.getHistory().stream()
                    .filter(v -> v.getVersion() == restored.getActiveVersion())
                    .findFirst()
                    .orElseThrow();
            assertThat(activeVersion.getText()).isEqualTo("Test comment version 1");
        }
    }

    @Test
    void restoreVersion_shouldThrowExceptionWhenCommentIsReadyForReview() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create comment and publish it
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Publish the comment (as reviewer)
            UUID reviewerId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(REVIEWER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity reviewer = new UserEntity();
            reviewer.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewer));
            DashboardCommentPermissionEntity reviewPermission = createReviewPermission();
            mockPermissionForUser(reviewerId, reviewPermission);

            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Edit comment as author (creates draft v2)
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
                    .text("Updated comment")
                    .entityVersion(1)
                    .build();
            DashboardCommentDto updatedComment = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), updateDto);

            // Send for review
            DashboardCommentDto readyForReview = commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, updatedComment.getId());

            // Switch back to reviewer and try to restore version 1 while version 2 is READY_FOR_REVIEW
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(REVIEWER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(reviewerId);

            String commentId = readyForReview.getId();
            assertThatThrownBy(() ->
                    commentService.restoreVersion(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, 1)
            ).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Cannot restore version while comment is waiting for review");
        }
    }

    @Test
    void getComments_shouldNotReturnDeletedComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), false, null);


            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            assertThat(comments).isEmpty();
        }
    }

    @Test
    void getComments_shouldHideDeletedCommentsByDefault() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create and delete a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Test comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), true, "Reason");

            // Fetch comments with default (false) includeDeleted
            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            assertThat(comments).isEmpty();
        }
    }

    @Test
    void getComments_shouldIncludeDeletedCommentsWhenRequestedByAuthor() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create and delete a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Deleted comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), true, "Reason");

            // Fetch comments with includeDeleted = true
            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, true);

            assertThat(comments).hasSize(1);
            assertThat(comments.get(0).getId()).isEqualTo(comment.getId());
            assertThat(comments.get(0).isDeleted()).isTrue();
        }
    }

    @Test
    void getComments_shouldShowLastPublishedVersionToNonAdminWhenActiveVersionIsSomeoneElsesDraft() {
        // Scenario: showcase-viewer creates a comment, admin publishes it, then showcase-viewer edits it (creating a draft)
        // Other users (showcase-editor) should still see the last published version

        final String AUTHOR_EMAIL = "showcase-viewer@example.com";
        final String AUTHOR_NAME = "Showcase Viewer";
        final String OTHER_USER_EMAIL = "showcase-editor@example.com";
        final String OTHER_USER_NAME = "Showcase Editor";

        // Step 1: Author creates a comment
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID authorUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(AUTHOR_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(AUTHOR_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission for author
            UserEntity authorUser = new UserEntity();
            authorUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(AUTHOR_EMAIL)).thenReturn(Optional.of(authorUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(authorUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original comment text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();

            // Step 2: Admin publishes the comment
            mockSuperUser(securityUtils);

            DashboardCommentDto publishedComment = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Step 3: Author edits the published comment (creates a new draft version)
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(AUTHOR_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(AUTHOR_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(authorUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            DashboardCommentUpdateDto editDto = DashboardCommentUpdateDto.builder()
                    .text("Edited comment text - this is a draft")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(publishedComment.getEntityVersion())
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, editDto);

            // Step 4: Other non-admin user (showcase-editor) requests comments
            UUID otherUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(OTHER_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(OTHER_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(otherUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity otherUser = new UserEntity();
            otherUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(OTHER_USER_EMAIL)).thenReturn(Optional.of(otherUser));

            // Mock read-only permission for other user
            DashboardCommentPermissionEntity readPermission = createReadOnlyPermission();
            mockPermissionForUser(otherUserId, readPermission);

            List<DashboardCommentDto> comments = commentService.getComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, false);

            // Verify: Other user should see the comment with the last published version (v1), not the draft (v2)
            assertThat(comments).hasSize(1);
            DashboardCommentDto visibleComment = comments.get(0);
            assertThat(visibleComment.getActiveVersion()).isEqualTo(1); // Should show v1 (published)

            // Find the active version in history
            DashboardCommentVersionDto visibleVersion = visibleComment.getHistory().stream()
                    .filter(v -> v.getVersion() == visibleComment.getActiveVersion())
                    .findFirst()
                    .orElseThrow();

            assertThat(visibleVersion.getText()).isEqualTo("Original comment text");
            assertThat(visibleVersion.getStatus()).isEqualTo(DashboardCommentStatus.PUBLISHED);
        }
    }

    @Test
    void updateComment_shouldThrowConflictWhenEntityVersionMismatch() {
        // simulate two users editing the same comment concurrently

        final String USER_EMAIL = "user@example.com";
        final String USER_NAME = "Test User";

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Step 1: Create a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();
            long initialVersion = comment.getEntityVersion();

            assertThat(initialVersion).isZero(); // Initial version should be 0

            // Step 2: User A retrieves the comment (entityVersion = 0)
            DashboardCommentUpdateDto userAUpdate = DashboardCommentUpdateDto.builder()
                    .text("User A's changes")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(0) // User A has version 0
                    .build();

            // Step 3: User B also retrieves the comment (entityVersion = 0) and updates it first
            DashboardCommentUpdateDto userBUpdate = DashboardCommentUpdateDto.builder()
                    .text("User B's changes")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(0) // User B also has version 0
                    .build();

            // User B updates successfully
            DashboardCommentDto updatedByB = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, userBUpdate);
            assertThat(updatedByB.getEntityVersion()).isEqualTo(1); // Version incremented to 1

            // Step 4: User A tries to update with stale version (0) - should fail with CONFLICT
            assertThatThrownBy(() ->
                    commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, userAUpdate)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("modified by another user")
                    .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 409);
        }
    }

    @Test
    void updateComment_shouldIncrementEntityVersionOnSuccessfulUpdate() {
        // Test that entityVersion is properly incremented on each update

        final String USER_EMAIL = "user@example.com";
        final String USER_NAME = "Test User";

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();
            long initialEntityVersion = comment.getEntityVersion();

            assertThat(initialEntityVersion).isZero();

            // First update
            DashboardCommentUpdateDto update1 = DashboardCommentUpdateDto.builder()
                    .text("First update")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(initialEntityVersion)
                    .build();

            DashboardCommentDto updated1 = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, update1);
            assertThat(updated1.getEntityVersion()).isEqualTo(1);

            // Second update with correct version
            DashboardCommentUpdateDto update2 = DashboardCommentUpdateDto.builder()
                    .text("Second update")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(1)
                    .build();

            DashboardCommentDto updated2 = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, update2);
            assertThat(updated2.getEntityVersion()).isEqualTo(2);

            // Third update with correct version
            DashboardCommentUpdateDto update3 = DashboardCommentUpdateDto.builder()
                    .text("Third update")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(2)
                    .build();

            DashboardCommentDto updated3 = commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, update3);
            assertThat(updated3.getEntityVersion()).isEqualTo(3);
        }
    }

    @Test
    void cloneCommentForEdit_shouldThrowConflictWhenVersionMismatch() {
        // Test that when one user edits a published comment, another user with stale version gets CONFLICT
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Step 1: Create and publish a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original published text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();
            DashboardCommentDto publishedComment = commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            long publishedVersion = publishedComment.getEntityVersion();

            // Step 2: User B creates edit first (version increments)
            DashboardCommentUpdateDto userBEdit = DashboardCommentUpdateDto.builder()
                    .text("User B's edit")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(publishedVersion)
                    .build();

            DashboardCommentDto editedByB = commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, userBEdit);
            assertThat(editedByB.getEntityVersion()).isEqualTo(publishedVersion + 1);

            // Step 3: User A tries to create edit with stale version - should fail with CONFLICT
            DashboardCommentUpdateDto userAEdit = DashboardCommentUpdateDto.builder()
                    .text("User A's edit")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(publishedVersion) // User A has stale version
                    .build();

            assertThatThrownBy(() ->
                    commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, userAEdit)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("modified by another user")
                    .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 409);
        }
    }

    @Test
    void updateComment_shouldRejectAndLogSuspiciousEntityVersion() {
        // Test that extremely high entity version is rejected and logged as suspicious

        final String USER_EMAIL = "user@example.com";
        final String USER_NAME = "Test User";

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .pointerUrl(TEST_POINTER_URL)
                    .build();

            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();
            long initialEntityVersion = comment.getEntityVersion();

            assertThat(initialEntityVersion).isZero();

            // Attempt to update with suspiciously high version (potential attack)
            DashboardCommentUpdateDto maliciousUpdate = DashboardCommentUpdateDto.builder()
                    .text("Hacked text")
                    .pointerUrl(TEST_POINTER_URL)
                    .entityVersion(999) // Way too high!
                    .build();

            // Should be rejected with CONFLICT
            assertThatThrownBy(() ->
                    commentService.updateComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, maliciousUpdate)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("modified by another user")
                    .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 409);
        }
    }

    // ========== EXPORT TESTS ==========

    @Test
    void exportComments_shouldFailForNonAdminUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user repository for admin role check (user is not admin)
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() ->
                    commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No review permission for comments");
        }
    }

    @Test
    void exportComments_shouldExportDraftComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // First create a draft as regular user
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user and write permission
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            // Create a draft comment (not published)
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);

            // Now export as admin
            mockSuperUser(securityUtils);

            // Export should include the draft comment (all non-deleted comments are exported)
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData).isNotNull();
            assertThat(exportData.getExportVersion()).isEqualTo("1.0");
            assertThat(exportData.getContextKey()).isEqualTo(TEST_CONTEXT_KEY);
            assertThat(exportData.getDashboardId()).isEqualTo(TEST_DASHBOARD_ID);
            assertThat(exportData.getExportDate()).isPositive();
            assertThat(exportData.getComments()).hasSize(1);
            assertThat(exportData.getComments().get(0).getStatus()).isEqualTo("DRAFT");
        }
    }

    @Test
    void exportComments_shouldExportAllNonDeletedComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Published comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .tags(List.of("tag1", "tag2"))
                    .build();
            DashboardCommentDto publishedComment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, publishedComment.getId());

            // Create another draft comment (should also be exported now)
            DashboardCommentCreateDto draftDto = DashboardCommentCreateDto.builder()
                    .text("Draft comment")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, draftDto);

            // Export - should include both comments
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(2);

            // Find the published comment in export
            CommentExportItemDto exportedPublished = exportData.getComments().stream()
                    .filter(c -> c.getId().equals(publishedComment.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(exportedPublished.getText()).isEqualTo("Published comment");
            assertThat(exportedPublished.getAuthor()).isEqualTo(SUPERUSER_NAME);
            assertThat(exportedPublished.getAuthorEmail()).isEqualTo(SUPERUSER_EMAIL);
            assertThat(exportedPublished.getStatus()).isEqualTo("PUBLISHED");
            assertThat(exportedPublished.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
        }
    }

    @Test
    void exportComments_shouldIncludeAllVersionsInHistory() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create, publish, edit and publish again
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();

            // Publish version 1
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Clone for edit (creates version 2)
            DashboardCommentUpdateDto updateDto = DashboardCommentUpdateDto.builder()
                    .text("Version 2")
                    .entityVersion(comment.getEntityVersion() + 1)
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, updateDto);

            // Publish version 2
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Export
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(1);
            CommentExportItemDto exportedItem = exportData.getComments().get(0);
            // History should contain all non-deleted versions
            assertThat(exportedItem.getHistory()).hasSize(2);
            assertThat(exportedItem.getHistory().get(0).getVersion()).isEqualTo(1);
            assertThat(exportedItem.getHistory().get(0).getText()).isEqualTo("Version 1");
            assertThat(exportedItem.getHistory().get(1).getVersion()).isEqualTo(2);
            assertThat(exportedItem.getHistory().get(1).getText()).isEqualTo("Version 2");
            // Check that activeVersion is correctly exported
            assertThat(exportedItem.getActiveVersion()).isEqualTo(2);
        }
    }

    @Test
    void exportComments_shouldIncludeDeletedComments() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish a comment
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("To be deleted")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId());

            // Delete it
            commentService.deleteComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment.getId(), false, "Deletion reason");

            // Export should include the deleted comment
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(1);
            assertThat(exportData.getComments().get(0).getStatus()).isEqualTo("DELETED");
            assertThat(exportData.getComments().get(0).getHistory().get(0).getDeletionReason()).isEqualTo("Deletion reason");
        }
    }

    // ========== IMPORT TESTS ==========

    @Test
    void importComments_shouldFailForNonAdminUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Mock user repository for admin role check (user is not admin)
            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(CommentExportItemDto.builder()
                            .text("Imported comment")
                            .build()))
                    .build();

            assertThatThrownBy(() ->
                    commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No review permission for comments");
        }
    }

    @Test
    void importComments_shouldFailWithInvalidImportData() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Null import data
            assertThatThrownBy(() ->
                    commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, null)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid import data");

            // Import data with null comments list
            CommentExportDto importDataWithNullComments = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(null)
                    .build();

            assertThatThrownBy(() ->
                    commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importDataWithNullComments)
            )
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid import data");
        }
    }

    @Test
    void importComments_shouldPreserveStatusFromImportData() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Imported comment 1")
                                    .author("Original Author")
                                    .authorEmail("original@example.com")
                                    .status("PUBLISHED")
                                    .createdDate(1700000000000L)
                                    .tags(List.of("imported", "test"))
                                    .history(List.of(
                                            CommentVersionExportDto.builder()
                                                    .version(1)
                                                    .text("Imported comment 1")
                                                    .status("PUBLISHED")
                                                    .editedDate(1700000000000L)
                                                    .editedBy("Original Author")
                                                    .editedByEmail("original@example.com")
                                                    .publishedDate(1700000001000L)
                                                    .publishedBy("Admin")
                                                    .publishedByEmail("admin@example.com")
                                                    .build()
                                    ))
                                    .activeVersion(1)
                                    .build(),
                            CommentExportItemDto.builder()
                                    .text("Imported comment 2")
                                    .status("DRAFT")
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            assertThat(result.getImported()).isEqualTo(2);
            assertThat(result.getUpdated()).isZero();
            assertThat(result.getSkipped()).isZero();

            // Verify comments are stored with correct statuses preserved
            assertThat(commentStore).hasSize(2);
        }
    }

    @Test
    void importComments_shouldSkipInvalidItems() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            // Valid item
                            CommentExportItemDto.builder()
                                    .text("Valid comment")
                                    .build(),
                            // Invalid - empty text
                            CommentExportItemDto.builder()
                                    .text("")
                                    .build(),
                            // Invalid - null text
                            CommentExportItemDto.builder()
                                    .text(null)
                                    .build(),
                            // Invalid - whitespace only
                            CommentExportItemDto.builder()
                                    .text("   ")
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            assertThat(result.getImported()).isEqualTo(1);
            assertThat(result.getUpdated()).isZero();
            assertThat(result.getSkipped()).isEqualTo(3);
        }
    }

    @Test
    void importComments_shouldUpdateExistingCommentIfIdMatches() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create an existing comment first
            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Original text")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto existingComment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String existingId = existingComment.getId();

            // Mock findById for existing comment
            when(commentRepository.findById(existingId)).thenReturn(Optional.of(commentStore.get(existingId)));

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            // Import with same ID should update existing comment
            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .id(existingId) // Same ID - should update
                                    .text("Updated via import")
                                    .tags(List.of("newtag"))
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            // Should update existing comment, not create new one
            assertThat(result.getImported()).isZero();
            assertThat(result.getUpdated()).isEqualTo(1);
            assertThat(result.getSkipped()).isZero();

            // Verify we still have only 1 comment (updated, not duplicated)
            assertThat(commentStore).hasSize(1);

            // Comment should be updated with new text
            DashboardCommentEntity updated = commentStore.get(existingId);
            assertThat(updated).isNotNull();
            assertThat(updated.getHistory().get(0).getText()).isEqualTo("Updated via import");
        }
    }

    @Test
    void importComments_shouldReplaceDashboardUrlWithCurrentUrl() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval to return current URL
            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Comment from different environment")
                                    .build()
                    ))
                    .build();

            commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            // Verify dashboardUrl is set to current URL, not from import file
            DashboardCommentEntity imported = commentStore.values().iterator().next();
            assertThat(imported.getDashboardUrl()).isEqualTo(TEST_DASHBOARD_URL);
        }
    }

    @Test
    void importComments_shouldNotImportPointerUrl() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            // pointerUrl in import data should be ignored
            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Comment with pointer")
                                    .build()
                    ))
                    .build();

            commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            // Verify pointerUrl is null on the version (no pointerUrl in import data)
            DashboardCommentEntity imported = commentStore.values().iterator().next();
            DashboardCommentVersionEntity activeVersion = imported.getHistory().stream()
                    .filter(v -> v.getVersion() == imported.getActiveVersion())
                    .findFirst()
                    .orElseThrow();
            assertThat(activeVersion.getPointerUrl()).isNull();
        }
    }

    @Test
    void importComments_shouldPreserveOriginalAuthorInfo() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            String originalAuthor = "Original Author";
            String originalEmail = "original@example.com";
            long originalCreatedDate = 1700000000000L;

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Imported comment")
                                    .author(originalAuthor)
                                    .authorEmail(originalEmail)
                                    .createdDate(originalCreatedDate)
                                    .build()
                    ))
                    .build();

            commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            DashboardCommentEntity imported = commentStore.values().iterator().next();
            assertThat(imported.getAuthor()).isEqualTo(originalAuthor);
            assertThat(imported.getAuthorEmail()).isEqualTo(originalEmail);
            assertThat(imported.getCreatedDate()).isEqualTo(originalCreatedDate);
        }
    }

    @Test
    void importComments_shouldUseCurrentUserWhenAuthorNotProvided() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Imported comment without author")
                                    // No author or authorEmail provided
                                    .build()
                    ))
                    .build();

            commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            DashboardCommentEntity imported = commentStore.values().iterator().next();
            assertThat(imported.getAuthor()).isEqualTo(SUPERUSER_NAME);
            assertThat(imported.getAuthorEmail()).isEqualTo(SUPERUSER_EMAIL);
        }
    }

    @Test
    void importComments_shouldNormalizeTagsDuringImport() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Comment with tags")
                                    .tags(List.of("TAG1", "  Tag2  ", "verylongtagnamethatexceedslimit"))
                                    .build()
                    ))
                    .build();

            commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            DashboardCommentEntity imported = commentStore.values().iterator().next();
            // Tags are now stored on the version entity, not the comment entity
            DashboardCommentVersionEntity activeVersion = imported.getHistory().stream()
                    .filter(v -> v.getVersion() == imported.getActiveVersion())
                    .findFirst()
                    .orElseThrow();
            String versionTags = activeVersion.getTags();
            assertThat(versionTags).isNotNull();
            // Tags from import item are stored as-is (trimmed) on the version entity
            List<String> tagList = Arrays.stream(versionTags.split(","))
                    .map(String::trim)
                    .toList();
            assertThat(tagList).contains("TAG1", "Tag2", "verylongtagnamethatexceedslimit");
        }
    }

    // ========== EXPORT/IMPORT ROUND-TRIP TEST ==========

    @Test
    void exportImport_shouldMaintainCommentDataIntegrity() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Create and publish some comments
            DashboardCommentCreateDto createDto1 = DashboardCommentCreateDto.builder()
                    .text("Comment 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .tags(List.of("important"))
                    .build();
            DashboardCommentDto comment1 = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto1);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment1.getId());

            DashboardCommentCreateDto createDto2 = DashboardCommentCreateDto.builder()
                    .text("Comment 2")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .tags(List.of("review"))
                    .build();
            DashboardCommentDto comment2 = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto2);
            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, comment2.getId());

            // Export comments
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(2);
            assertThat(exportData.getExportVersion()).isEqualTo("1.0");
            assertThat(exportData.getContextKey()).isEqualTo(TEST_CONTEXT_KEY);
            assertThat(exportData.getDashboardId()).isEqualTo(TEST_DASHBOARD_ID);

            // Verify exported data maintains original info
            List<String> exportedTexts = exportData.getComments().stream()
                    .map(CommentExportItemDto::getText)
                    .toList();
            assertThat(exportedTexts).containsExactlyInAnyOrder("Comment 1", "Comment 2");

            List<String> exportedTags = exportData.getComments().stream()
                    .flatMap(c -> c.getTags().stream())
                    .toList();
            assertThat(exportedTags).containsExactlyInAnyOrder("important", "review");
        }
    }

    @Test
    void importComments_shouldPreserveFullHistoryWithStatuses() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            // Create import data with full history
            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Version 3")
                                    .author("Original Author")
                                    .authorEmail("author@example.com")
                                    .status("PUBLISHED")
                                    .activeVersion(3)
                                    .createdDate(1700000000000L)
                                    .tags(List.of("imported"))
                                    .history(List.of(
                                            CommentVersionExportDto.builder()
                                                    .version(1)
                                                    .text("Version 1")
                                                    .status("PUBLISHED")
                                                    .editedDate(1700000000000L)
                                                    .editedBy("Original Author")
                                                    .editedByEmail("author@example.com")
                                                    .publishedDate(1700000001000L)
                                                    .publishedBy("Admin")
                                                    .publishedByEmail("admin@example.com")
                                                    .tags(List.of("v1tag"))
                                                    .build(),
                                            CommentVersionExportDto.builder()
                                                    .version(2)
                                                    .text("Version 2")
                                                    .status("PUBLISHED")
                                                    .editedDate(1700001000000L)
                                                    .editedBy("Editor")
                                                    .editedByEmail("editor@example.com")
                                                    .publishedDate(1700001001000L)
                                                    .publishedBy("Admin")
                                                    .publishedByEmail("admin@example.com")
                                                    .tags(List.of("v2tag"))
                                                    .build(),
                                            CommentVersionExportDto.builder()
                                                    .version(3)
                                                    .text("Version 3")
                                                    .status("PUBLISHED")
                                                    .editedDate(1700002000000L)
                                                    .editedBy("Editor")
                                                    .editedByEmail("editor@example.com")
                                                    .publishedDate(1700002001000L)
                                                    .publishedBy("Admin")
                                                    .publishedByEmail("admin@example.com")
                                                    .tags(List.of("v3tag"))
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            assertThat(result.getImported()).isEqualTo(1);
            assertThat(result.getSkipped()).isZero();

            // Verify imported comment has full history
            DashboardCommentEntity imported = commentStore.values().iterator().next();
            assertThat(imported.getHistory()).hasSize(3);
            assertThat(imported.getActiveVersion()).isEqualTo(3);
            assertThat(imported.getAuthor()).isEqualTo("Original Author");
            assertThat(imported.getAuthorEmail()).isEqualTo("author@example.com");

            // Verify version statuses are preserved
            assertThat(imported.getHistory().stream()
                    .allMatch(v -> v.getStatus() == DashboardCommentStatus.PUBLISHED)).isTrue();
        }
    }

    @Test
    void importComments_shouldNotDuplicateOnReimportFromOtherDashboard() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            // Mock dashboard URL retrieval
            mockDashboardUrlRetrieval();

            String originalCommentId = "original-comment-from-dashboard-a";

            // First import - should create new comment with importedFromId set
            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .id(originalCommentId) // ID from another dashboard
                                    .text("Imported comment")
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result1 = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);
            assertThat(result1.getImported()).isEqualTo(1);
            assertThat(commentStore).hasSize(1);

            // Get the newly created comment
            DashboardCommentEntity importedComment = commentStore.values().iterator().next();
            String newCommentId = importedComment.getId();
            assertThat(importedComment.getImportedFromId()).isEqualTo(originalCommentId);

            // Mock findByImportedFromId for re-import detection
            when(commentRepository.findByImportedFromIdAndContextKeyAndDashboardId(
                    originalCommentId, TEST_CONTEXT_KEY, TEST_DASHBOARD_ID))
                    .thenReturn(Optional.of(importedComment));

            // Second import with same original ID - should update, not duplicate
            CommentExportDto importData2 = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .id(originalCommentId) // Same original ID
                                    .text("Updated imported comment")
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result2 = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData2);

            // Should update existing, not create new
            assertThat(result2.getImported()).isZero();
            assertThat(result2.getUpdated()).isEqualTo(1);

            // Still only one comment
            assertThat(commentStore).hasSize(1);

            // Should be the same comment (same ID) with updated text
            DashboardCommentEntity updated = commentStore.get(newCommentId);
            assertThat(updated).isNotNull();
            assertThat(updated.getHistory().get(0).getText()).isEqualTo("Updated imported comment");
        }
    }

    @Test
    void exportComments_shouldExportDeclinedCommentWithDeclineReason() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create a comment as regular user
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Comment to decline")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();
            commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Decline as superuser
            mockSuperUser(securityUtils);

            DashboardCommentDeclineDto declineDto = DashboardCommentDeclineDto.builder()
                    .declineReason("Needs improvement")
                    .build();
            commentService.declineComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, declineDto);

            // Export
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(1);
            CommentExportItemDto exported = exportData.getComments().get(0);
            assertThat(exported.getStatus()).isEqualTo("DECLINED");

            // Verify history contains the declined version with decline reason
            assertThat(exported.getHistory()).hasSize(1);
            CommentVersionExportDto declinedVersion = exported.getHistory().get(0);
            assertThat(declinedVersion.getStatus()).isEqualTo("DECLINED");
            assertThat(declinedVersion.getDeclineReason()).isEqualTo("Needs improvement");
        }
    }

    @Test
    void importComments_shouldPreserveDeclinedStatusAndDeclineReason() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            mockSuperUser(securityUtils);

            mockDashboardUrlRetrieval();

            CommentExportDto importData = CommentExportDto.builder()
                    .exportVersion("1.0")
                    .contextKey(TEST_CONTEXT_KEY)
                    .dashboardId(TEST_DASHBOARD_ID)
                    .comments(List.of(
                            CommentExportItemDto.builder()
                                    .text("Declined comment")
                                    .author("Author")
                                    .authorEmail("author@example.com")
                                    .status("DECLINED")
                                    .createdDate(1700000000000L)
                                    .history(List.of(
                                            CommentVersionExportDto.builder()
                                                    .version(1)
                                                    .text("Declined comment")
                                                    .status("DECLINED")
                                                    .editedDate(1700000000000L)
                                                    .editedBy("Author")
                                                    .editedByEmail("author@example.com")
                                                    .publishedBy("Reviewer")
                                                    .publishedByEmail("reviewer@example.com")
                                                    .publishedDate(1700000001000L)
                                                    .declineReason("Content not appropriate")
                                                    .build()
                                    ))
                                    .activeVersion(1)
                                    .build()
                    ))
                    .build();

            CommentImportResultDto result = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, importData);

            assertThat(result.getImported()).isEqualTo(1);
            assertThat(result.getSkipped()).isZero();

            DashboardCommentEntity imported = commentStore.values().iterator().next();
            assertThat(imported.getHistory()).hasSize(1);

            DashboardCommentVersionEntity version = imported.getHistory().get(0);
            assertThat(version.getStatus()).isEqualTo(DashboardCommentStatus.DECLINED);
            assertThat(version.getDeclineReason()).isEqualTo("Content not appropriate");
        }
    }

    @Test
    void exportImport_shouldRoundTripDeclinedCommentWithFullHistory() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            // Create and publish a comment as regular user
            UUID testUserId = UUID.randomUUID();
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            UserEntity regularUser = new UserEntity();
            regularUser.setContextRoles(Collections.emptySet());
            when(userRepository.findUserEntityByEmailIgnoreCase(TEST_USER_EMAIL)).thenReturn(Optional.of(regularUser));
            DashboardCommentPermissionEntity writePermission = createWritePermission();
            mockPermissionForUser(testUserId, writePermission);

            DashboardCommentCreateDto createDto = DashboardCommentCreateDto.builder()
                    .text("Version 1")
                    .dashboardUrl(TEST_DASHBOARD_URL)
                    .build();
            DashboardCommentDto comment = commentService.createComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, createDto);
            String commentId = comment.getId();

            // Publish as superuser, then clone and decline
            mockSuperUser(securityUtils);

            commentService.publishComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Re-mock author to send v2 for review
            securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(TEST_USER_EMAIL);
            securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(TEST_USER_NAME);
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);
            securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

            // Clone for edit (creates v2 draft)
            DashboardCommentUpdateDto editDto = DashboardCommentUpdateDto.builder()
                    .text("Version 2 - needs review")
                    .entityVersion(comment.getEntityVersion() + 1)
                    .build();
            commentService.cloneCommentForEdit(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, editDto);
            commentService.sendForReview(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId);

            // Decline v2 as reviewer
            mockSuperUser(securityUtils);
            DashboardCommentDeclineDto declineDto = DashboardCommentDeclineDto.builder()
                    .declineReason("Missing data sources")
                    .build();
            commentService.declineComment(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, commentId, declineDto);

            // Export
            CommentExportDto exportData = commentService.exportComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID);

            assertThat(exportData.getComments()).hasSize(1);
            CommentExportItemDto exported = exportData.getComments().get(0);
            assertThat(exported.getStatus()).isEqualTo("DECLINED");
            assertThat(exported.getHistory()).hasSize(2);

            // v1 = PUBLISHED, v2 = DECLINED with reason
            CommentVersionExportDto v1 = exported.getHistory().stream()
                    .filter(v -> v.getVersion() == 1).findFirst().orElseThrow();
            CommentVersionExportDto v2 = exported.getHistory().stream()
                    .filter(v -> v.getVersion() == 2).findFirst().orElseThrow();
            assertThat(v1.getStatus()).isEqualTo("PUBLISHED");
            assertThat(v2.getStatus()).isEqualTo("DECLINED");
            assertThat(v2.getDeclineReason()).isEqualTo("Missing data sources");

            // Clear store and reimport
            commentStore.clear();
            mockDashboardUrlRetrieval();

            CommentImportResultDto importResult = commentService.importComments(TEST_CONTEXT_KEY, TEST_DASHBOARD_ID, exportData);

            assertThat(importResult.getImported()).isEqualTo(1);

            DashboardCommentEntity reimported = commentStore.values().iterator().next();
            assertThat(reimported.getHistory()).hasSize(2);
            assertThat(reimported.getActiveVersion()).isEqualTo(2);

            // Verify statuses and decline reason survived round-trip
            DashboardCommentVersionEntity reimportedV1 = reimported.getHistory().stream()
                    .filter(v -> v.getVersion() == 1).findFirst().orElseThrow();
            DashboardCommentVersionEntity reimportedV2 = reimported.getHistory().stream()
                    .filter(v -> v.getVersion() == 2).findFirst().orElseThrow();

            assertThat(reimportedV1.getStatus()).isEqualTo(DashboardCommentStatus.PUBLISHED);
            assertThat(reimportedV2.getStatus()).isEqualTo(DashboardCommentStatus.DECLINED);
            assertThat(reimportedV2.getDeclineReason()).isEqualTo("Missing data sources");
        }
    }

    // Helper method to mock dashboard URL retrieval
    private void mockDashboardUrlRetrieval() {
        // Create mock SupersetDashboard
        SupersetDashboard mockDashboard = new SupersetDashboard();
        mockDashboard.setId(TEST_DASHBOARD_ID);
        mockDashboard.setDashboardTitle("Test Dashboard");

        // Create mock DashboardResource
        DashboardResource mockResource = new DashboardResource(TEST_INSTANCE_NAME, List.of(mockDashboard));

        // Mock metaInfoResourceService to return the mock resource
        when(metaInfoResourceService.findAllByModuleTypeAndKindAndContextKey(
                ModuleType.SUPERSET,
                ModuleResourceKind.HELLO_DATA_DASHBOARDS,
                TEST_CONTEXT_KEY,
                DashboardResource.class
        )).thenReturn(mockResource);
    }
}

