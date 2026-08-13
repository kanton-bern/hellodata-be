package ch.bedag.dap.hellodata.portal.dashboard_comment.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentStatus;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DashboardCommentVersionDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.data.DomainDashboardCommentDto;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentPermissionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.entity.DashboardCommentVersionEntity;
import ch.bedag.dap.hellodata.portal.dashboard_comment.mapper.DashboardCommentMapper;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentPermissionRepository;
import ch.bedag.dap.hellodata.portal.dashboard_comment.repository.DashboardCommentRepository;
import ch.bedag.dap.hellodata.portalcommon.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Reproduces the redaction gap in {@link DashboardCommentService#getComments}: when the active
 * version of a comment is DRAFT / READY_FOR_REVIEW / DECLINED and the caller has neither write nor
 * review permission, {@code showLastPublishedVersion(...)} only rewinds the {@code activeVersion}
 * pointer (plus pointerUrl/tags). The DTO's {@code history} list — which carries the full {@code text}
 * of every version, plus {@code editedBy} / {@code editedByEmail} / {@code declineReason} — is copied
 * through {@code toBuilder()} untouched and is serialized straight to the HTTP client.
 * <p>
 * Uses the real {@link DashboardCommentMapper} so the DTO under assertion is exactly what the
 * controller returns.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardCommentUnpublishedTextLeakTest {

    private static final String CONTEXT_KEY = "dd_one";
    private static final int DASHBOARD_ID = 123;

    private static final String AUTHOR_EMAIL = "author@example.org";
    private static final String AUTHOR_NAME = "Ann Author";
    private static final String READER_EMAIL = "reader@example.org";
    private static final String READER_NAME = "Rob Reader";

    private static final String PUBLISHED_TEXT = "This is the published, approved comment.";
    private static final String SECRET_DRAFT_TEXT = "INTERNAL DRAFT: the figures for Q3 are wrong, do not circulate.";
    private static final String SECRET_DECLINE_REASON = "Rejected: contains unreleased headcount numbers.";

    @Mock
    private DashboardCommentRepository commentRepository;
    @Mock
    private DashboardCommentPermissionRepository commentPermissionRepository;
    @Mock
    private MetaInfoResourceService metaInfoResourceService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DashboardCommentNotificationService notificationService;
    @Mock
    private DashboardCommentDwhSyncService dwhSyncService;
    @Mock
    private Connection connection;
    @Mock
    private ObjectMapper objectMapper;

    private DashboardCommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new DashboardCommentService(commentRepository, commentPermissionRepository,
                new DashboardCommentMapper(), metaInfoResourceService, userRepository,
                notificationService, dwhSyncService, connection, objectMapper);

        // No UserEntity -> shouldFilterByDashboardAccess() is false, so the RBAC dashboard filter is
        // out of the picture and only the comment-status redaction under test remains.
        when(userRepository.findUserEntityByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    private static DashboardCommentVersionEntity version(int number, DashboardCommentStatus status,
                                                         String text, String editedBy, String declineReason) {
        return DashboardCommentVersionEntity.builder()
                .version(number)
                .text(text)
                .status(status)
                .editedDate(1_700_000_000_000L + number)
                .editedBy(editedBy)
                .editedByEmail(AUTHOR_EMAIL)
                .declineReason(declineReason)
                .build();
    }

    /**
     * A comment that was published (v1) and has since been re-opened for editing, which is exactly what
     * {@code cloneCommentForEdit} produces: a new DRAFT version becomes the active one while the
     * published version stays in history.
     */
    private DashboardCommentEntity publishedThenRedraftedComment(DashboardCommentStatus activeStatus,
                                                                 String declineReason) {
        DashboardCommentEntity comment = DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(DASHBOARD_ID)
                .dashboardUrl("https://superset.example.org/superset/dashboard/123/")
                .contextKey(CONTEXT_KEY)
                .author(AUTHOR_NAME)
                .authorEmail(AUTHOR_EMAIL)
                .createdDate(1_700_000_000_000L)
                .activeVersion(2)
                .hasActiveDraft(true)
                .build();
        comment.addVersion(version(1, DashboardCommentStatus.PUBLISHED, PUBLISHED_TEXT, AUTHOR_NAME, null));
        comment.addVersion(version(2, activeStatus, SECRET_DRAFT_TEXT, AUTHOR_NAME, declineReason));
        return comment;
    }

    private void stubStore(DashboardCommentEntity comment) {
        when(commentRepository.findByContextKeyAndDashboardIdOrderByCreatedDateAsc(CONTEXT_KEY, DASHBOARD_ID))
                .thenReturn(List.of(comment));
    }

    private void stubCaller(MockedStatic<SecurityUtils> securityUtils, String email, String fullName,
                            boolean write, boolean review) {
        UUID userId = UUID.randomUUID();
        securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn(email);
        securityUtils.when(SecurityUtils::getCurrentUserFullName).thenReturn(fullName);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        securityUtils.when(SecurityUtils::isSuperuser).thenReturn(false);

        DashboardCommentPermissionEntity perm = new DashboardCommentPermissionEntity();
        perm.setReadComments(true);
        perm.setWriteComments(write);
        perm.setReviewComments(review);
        when(commentPermissionRepository.findByUserIdAndContextKey(any(UUID.class), any(String.class)))
                .thenReturn(Optional.of(perm));
    }

    private static List<String> textsInHistory(DashboardCommentDto dto) {
        return dto.getHistory().stream().map(DashboardCommentVersionDto::getText).toList();
    }

    /**
     * Read-only caller, active version is an unpublished DRAFT.
     * Expected: only the published text reaches the caller. Actual: the draft text ships in history.
     */
    @Test
    void readOnlyUserMustNotReceiveTheTextOfAnUnpublishedDraft() {
        stubStore(publishedThenRedraftedComment(DashboardCommentStatus.DRAFT, null));

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        DashboardCommentDto dto = result.get(0);

        // The pointer is rewound correctly -- this part of the redaction works.
        assertThat(dto.getActiveVersion())
                .as("the caller should be steered at the last published version")
                .isEqualTo(1);

        // ...but the payload still carries the unpublished text.
        assertThat(textsInHistory(dto))
                .as("a read-only caller must not receive the text of an unpublished draft version")
                .containsExactly(PUBLISHED_TEXT);
    }

    /**
     * Same shape with a DECLINED active version: the decline reason is reviewer-internal and leaks too.
     */
    @Test
    void readOnlyUserMustNotReceiveTheDeclineReasonOfADeclinedVersion() {
        stubStore(publishedThenRedraftedComment(DashboardCommentStatus.DECLINED, SECRET_DECLINE_REASON));

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        DashboardCommentDto dto = result.get(0);

        assertThat(dto.getHistory())
                .as("a read-only caller must not receive the reviewer's decline reason")
                .noneMatch(v -> SECRET_DECLINE_REASON.equals(v.getDeclineReason()));
    }

    /**
     * Control: a reviewer is entitled to a READY_FOR_REVIEW version and legitimately gets it, active
     * pointer and all. Confirms the fixture really does carry an unpublished v2 and that the tests
     * above differ from this one only in the caller's permissions.
     */
    @Test
    void reviewerLegitimatelyReceivesTheVersionAwaitingReview() {
        stubStore(publishedThenRedraftedComment(DashboardCommentStatus.READY_FOR_REVIEW, null));

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, "reviewer@example.org", "Rita Reviewer", true, true);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion()).isEqualTo(2);
        assertThat(textsInHistory(result.get(0))).contains(SECRET_DRAFT_TEXT);
    }

    /**
     * Second control, and the sharpest framing of the defect: the same READY_FOR_REVIEW version that
     * the reviewer above is entitled to is withheld from a read-only caller by the activeVersion
     * rewind -- and shipped to them anyway inside history.
     */
    @Test
    void readOnlyUserMustNotReceiveTheTextOfAVersionAwaitingReview() {
        stubStore(publishedThenRedraftedComment(DashboardCommentStatus.READY_FOR_REVIEW, null));

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion()).isEqualTo(1);
        assertThat(textsInHistory(result.get(0)))
                .as("a read-only caller must not receive the text of a version still awaiting review")
                .containsExactly(PUBLISHED_TEXT);
    }

    /**
     * The domain-wide view has the same defect through {@code fallbackToLastPublishedVersion}, and it is
     * the wider exposure: one GET returns every dashboard's comments in the data domain at once.
     */
    @Test
    void readOnlyUserMustNotReceiveUnpublishedTextInTheDomainView() {
        DashboardCommentEntity comment = publishedThenRedraftedComment(DashboardCommentStatus.DRAFT, null);
        when(commentRepository.findByContextKeyOrderByCreatedDateAsc(CONTEXT_KEY)).thenReturn(List.of(comment));

        List<DomainDashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getCommentsForDomain(CONTEXT_KEY, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion())
                .as("the caller should be steered at the last published version")
                .isEqualTo(1);
        assertThat(textsInHistory(result.get(0)))
                .as("the domain view must not carry the text of an unpublished draft either")
                .containsExactly(PUBLISHED_TEXT);
    }

    /**
     * Control, and a guard against over-pruning: redaction must hide the unpublished versions, not
     * collapse the history down to the single last published one. A comment published twice before
     * being re-drafted still owes the read-only caller both published versions -- that is what the UI
     * renders in the version-history table.
     */
    @Test
    void readOnlyUserStillReceivesEveryPublishedVersionInHistory() {
        DashboardCommentEntity comment = DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(DASHBOARD_ID)
                .contextKey(CONTEXT_KEY)
                .author(AUTHOR_NAME)
                .authorEmail(AUTHOR_EMAIL)
                .createdDate(1_700_000_000_000L)
                .activeVersion(3)
                .hasActiveDraft(true)
                .build();
        comment.addVersion(version(1, DashboardCommentStatus.PUBLISHED, "First published wording.", AUTHOR_NAME, null));
        comment.addVersion(version(2, DashboardCommentStatus.PUBLISHED, "Second published wording.", AUTHOR_NAME, null));
        comment.addVersion(version(3, DashboardCommentStatus.DRAFT, SECRET_DRAFT_TEXT, AUTHOR_NAME, null));
        stubStore(comment);

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion()).isEqualTo(2);
        assertThat(textsInHistory(result.get(0)))
                .as("redaction must drop only the unpublished versions, not the earlier published ones")
                .containsExactly("First published wording.", "Second published wording.");
    }

    /**
     * Control pinning the ownership rule: it lives in {@code handleDraftVisibility} (hasWrite &amp;&amp;
     * isOwnDraft), one level above the redaction, so the author of a draft never reaches
     * {@code showLastPublishedVersion} and keeps seeing their own work in full.
     */
    @Test
    void authorWithWritePermissionStillSeesTheirOwnDraftInFull() {
        stubStore(publishedThenRedraftedComment(DashboardCommentStatus.DRAFT, null));

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, AUTHOR_EMAIL, AUTHOR_NAME, true, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion())
                .as("the author keeps their own draft as the active version")
                .isEqualTo(2);
        assertThat(textsInHistory(result.get(0))).containsExactly(PUBLISHED_TEXT, SECRET_DRAFT_TEXT);
    }

    /**
     * A comment whose active version is PUBLISHED but whose history still carries an earlier DECLINED
     * version - exactly what {@code publishComment} (publishing a declined comment) and the
     * decline-then-re-edit-then-publish flow leave behind. The active-version rewind never runs here (the
     * active version is already published), so the redaction must instead prune the DECLINED entry from
     * history. Otherwise its secret text and reviewer decline reason ship to a read-only caller.
     */
    private DashboardCommentEntity publishedWithDeclinedInHistory() {
        DashboardCommentEntity comment = DashboardCommentEntity.builder()
                .id(UUID.randomUUID().toString())
                .dashboardId(DASHBOARD_ID)
                .contextKey(CONTEXT_KEY)
                .author(AUTHOR_NAME)
                .authorEmail(AUTHOR_EMAIL)
                .createdDate(1_700_000_000_000L)
                .activeVersion(3)
                .hasActiveDraft(false)
                .build();
        comment.addVersion(version(1, DashboardCommentStatus.PUBLISHED, PUBLISHED_TEXT, AUTHOR_NAME, null));
        comment.addVersion(version(2, DashboardCommentStatus.DECLINED, SECRET_DRAFT_TEXT, AUTHOR_NAME, SECRET_DECLINE_REASON));
        comment.addVersion(version(3, DashboardCommentStatus.PUBLISHED, "The finally approved wording.", AUTHOR_NAME, null));
        return comment;
    }

    @Test
    void readOnlyUserMustNotReceiveDeclinedHistoryWhenActiveVersionIsPublished() {
        stubStore(publishedWithDeclinedInHistory());

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        DashboardCommentDto dto = result.get(0);

        // The active version is (and stays) the published one -- nothing to rewind.
        assertThat(dto.getActiveVersion()).isEqualTo(3);

        // Only the two published versions may reach the reader; the declined one is dropped entirely.
        assertThat(textsInHistory(dto))
                .as("a read-only caller must not receive the text of a declined version kept in history")
                .containsExactly(PUBLISHED_TEXT, "The finally approved wording.");
        assertThat(dto.getHistory())
                .as("a read-only caller must not receive the reviewer's decline reason")
                .noneMatch(v -> SECRET_DECLINE_REASON.equals(v.getDeclineReason()));
    }

    /**
     * Control for the case above: a reviewer is entitled to the declined history and still receives it in
     * full, active pointer untouched. Confirms the redaction keys on permission, not on presence.
     */
    @Test
    void reviewerStillReceivesDeclinedHistoryWhenActiveVersionIsPublished() {
        stubStore(publishedWithDeclinedInHistory());

        List<DashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, "reviewer@example.org", "Rita Reviewer", true, true);
            result = commentService.getComments(CONTEXT_KEY, DASHBOARD_ID, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion()).isEqualTo(3);
        assertThat(textsInHistory(result.get(0)))
                .as("a reviewer keeps the full history, declined version included")
                .contains(SECRET_DRAFT_TEXT);
    }

    /**
     * Same leak, same shape, through the domain-wide view.
     */
    @Test
    void readOnlyUserMustNotReceiveDeclinedHistoryInTheDomainViewWhenActiveVersionIsPublished() {
        when(commentRepository.findByContextKeyOrderByCreatedDateAsc(CONTEXT_KEY))
                .thenReturn(List.of(publishedWithDeclinedInHistory()));

        List<DomainDashboardCommentDto> result;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            stubCaller(securityUtils, READER_EMAIL, READER_NAME, false, false);
            result = commentService.getCommentsForDomain(CONTEXT_KEY, false);
        }

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveVersion()).isEqualTo(3);
        assertThat(textsInHistory(result.get(0)))
                .as("the domain view must not carry the text of a declined version kept in history")
                .containsExactly(PUBLISHED_TEXT, "The finally approved wording.");
    }
}
