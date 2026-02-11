/**
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

import {Component, computed, DestroyRef, inject, input, output} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {DatePipe, SlicePipe} from "@angular/common";
import {Tooltip} from "primeng/tooltip";
import {TranslocoPipe} from "@jsverse/transloco";
import {DashboardCommentEntry, DashboardCommentStatus} from "../../../../store/my-dashboards/my-dashboards.model";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {
  canDeleteComment,
  canEditComment,
  canPublishComment,
  canViewCommentMetadata,
  canViewMetadataAndVersions,
  selectAvailableTags,
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId,
  selectCurrentDashboardUrl
} from "../../../../store/my-dashboards/my-dashboards.selector";
import {take} from "rxjs";
import {PrimeTemplate} from "primeng/api";
import {Dialog} from "primeng/dialog";
import {FormsModule} from "@angular/forms";
import {Textarea} from "primeng/textarea";
import {Button} from "primeng/button";
import {DashboardCommentUtilsService} from "../../services/dashboard-comment-utils.service";
import {AutoComplete} from "primeng/autocomplete";

@Component({
  selector: 'app-comment-entry',
  standalone: true,
  templateUrl: './comment-entry.component.html',
  imports: [
    DatePipe,
    SlicePipe,
    Tooltip,
    TranslocoPipe,
    Dialog,
    FormsModule,
    Textarea,
    Button,
    PrimeTemplate,
    AutoComplete
  ],
  styleUrls: ['./comment-entry.component.scss']
})
export class CommentEntryComponent {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly commentUtils = inject(DashboardCommentUtilsService);
  private readonly destroyRef = inject(DestroyRef);

  // Expose enum for template
  protected readonly DashboardCommentStatus = DashboardCommentStatus;

  comment = input.required<DashboardCommentEntry>();
  pointerUrlClick = output<string>();

  expanded = false;

  // Edit dialog
  editDialogVisible = false;
  editedText = '';
  editedPointerUrl = '';
  editedTags: string[] = [];
  editNewTagText = '';
  editTagSuggestions: string[] = [];

  // Decline dialog
  declineDialogVisible = false;
  declineReason = '';

  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);
  currentDashboardUrl$ = this.store.select(selectCurrentDashboardUrl);
  availableTags$ = this.store.select(selectAvailableTags);

  private currentDashboardUrl: string | undefined;

  // Computed properties for permissions
  canEditFn = this.store.selectSignal(canEditComment);
  canPublishFn = this.store.selectSignal(canPublishComment);
  canDeleteFn = this.store.selectSignal(canDeleteComment);
  canViewMetadataFn = this.store.selectSignal(canViewCommentMetadata);
  userHasReviewPermission = this.store.selectSignal(canViewMetadataAndVersions);

  canEdit = computed(() => {
    const activeVer = this.activeVersion();
    // Cannot edit READY_FOR_REVIEW status - it's locked for review
    if (activeVer?.status === DashboardCommentStatus.READY_FOR_REVIEW) {
      return false;
    }
    return this.canEditFn()(this.comment());
  });

  canPublish = computed(() => this.canPublishFn()(this.comment()));
  canDelete = computed(() => this.canDeleteFn()(this.comment()));
  canViewMetadata = computed(() => this.canViewMetadataFn()(this.comment()));

  // Author can send DRAFT for review
  canSendForReview = computed(() => {
    const activeVer = this.activeVersion();
    return this.canEdit() && activeVer?.status === DashboardCommentStatus.DRAFT;
  });

  // Reviewer can decline READY_FOR_REVIEW comments
  canDecline = computed(() => {
    const activeVer = this.activeVersion();
    return this.canPublish() && activeVer?.status === DashboardCommentStatus.READY_FOR_REVIEW;
  });

  // Get active version from comment
  activeVersion = computed(() => {
    return this.commentUtils.getActiveVersionData(this.comment());
  });

  // All visible versions:
  // - Admins/Reviewers can see all non-deleted versions (both PUBLISHED and DRAFT)
  // - Author can see all their versions (including deleted/declined)
  // - Non-admins see only non-deleted PUBLISHED versions
  allVersions = computed(() => {
    return this.commentUtils.getAllVersions(this.comment(), this.canViewMetadata());
  });

  getStatusLabel(status: DashboardCommentStatus | string): string {
    return this.commentUtils.getStatusLabel(status);
  }

  constructor() {
    this.currentDashboardUrl$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(url => this.currentDashboardUrl = url);
  }

  /**
   * Validates that editedPointerUrl is a valid Superset link (same domain as current dashboard)
   */
  isEditedPointerUrlValid(): boolean {
    return this.commentUtils.isPointerUrlValid(this.editedPointerUrl, this.currentDashboardUrl);
  }

  /**
   * Returns normalized editedPointerUrl with protocol
   */
  getNormalizedEditedPointerUrl(): string | undefined {
    return this.commentUtils.normalizePointerUrl(this.editedPointerUrl);
  }

  toggleDetails(): void {
    this.expanded = !this.expanded;
  }

  navigateToPointerUrl(): void {
    const url = this.activeVersion()?.pointerUrl;
    if (url) {
      this.pointerUrlClick.emit(url);
    }
  }

  navigateToUrl(url: string): void {
    this.pointerUrlClick.emit(url);
  }

  onKeyDown(event: KeyboardEvent, action: () => void): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      action();
    }
  }

  editComment(): void {
    const activeVer = this.activeVersion();
    this.editedText = activeVer?.text || '';
    this.editedPointerUrl = activeVer?.pointerUrl || '';
    this.editedTags = activeVer?.tags ? [...activeVer.tags] : [];
    this.editNewTagText = '';
    this.editDialogVisible = true;
  }

  saveEdit(): void {
    const comment = this.comment();
    const activeVer = this.activeVersion();
    const newText = this.editedText.trim();
    const normalizedPointerUrl = this.getNormalizedEditedPointerUrl();

    // Check if anything changed
    const textChanged = newText !== activeVer?.text;
    const pointerUrlChanged = normalizedPointerUrl !== (activeVer?.pointerUrl || undefined);
    const sortedEditedTags = [...this.editedTags].sort((a: string, b: string) => a.localeCompare(b));
    const sortedCommentTags = [...(activeVer?.tags || [])].sort((a: string, b: string) => a.localeCompare(b));
    const tagsChanged = JSON.stringify(sortedEditedTags) !== JSON.stringify(sortedCommentTags);

    if (!newText || (!textChanged && !pointerUrlChanged && !tagsChanged)) {
      this.editDialogVisible = false;
      return;
    }

    // Block if pointerUrl is invalid
    if (this.editedPointerUrl.trim() && !this.isEditedPointerUrlValid()) {
      return;
    }

    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          const tagsToSave = this.editedTags.length > 0 ? this.editedTags : undefined;
          // Send empty string to clear pointerUrl, or normalized URL
          const pointerUrlToSave = normalizedPointerUrl ?? '';
          // If status is PUBLISHED or DECLINED, clone to create new draft version
          if (activeVer?.status === DashboardCommentStatus.PUBLISHED || activeVer?.status === DashboardCommentStatus.DECLINED) {
            this.commentUtils.dispatchClonePublishedComment(
              dashboardId,
              contextKey,
              comment.id,
              newText,
              pointerUrlToSave,
              comment.entityVersion,
              tagsToSave
            );
          } else {
            // For DRAFT status, just update the existing draft
            this.commentUtils.dispatchUpdateDraftComment(
              dashboardId,
              contextKey,
              comment.id,
              newText,
              pointerUrlToSave,
              comment.entityVersion,
              tagsToSave
            );
          }
        }
      });
    });

    this.editDialogVisible = false;
  }

  cancelEdit(): void {
    this.editDialogVisible = false;
    this.editedText = '';
    this.editedPointerUrl = '';
    this.editedTags = [];
    this.editNewTagText = '';
  }

  clearEditedPointerUrl(): void {
    this.editedPointerUrl = '';
  }

  sendForReview(): void {
    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          this.commentUtils.confirmSendForReview(dashboardId, contextKey, this.comment().id);
        }
      });
    });
  }

  // Tag methods for edit dialog
  searchEditTags(event: { query: string }): void {
    this.availableTags$.pipe(take(1)).subscribe(tags => {
      const query = event.query.toLowerCase();
      this.editTagSuggestions = tags.filter(tag =>
        tag.toLowerCase().includes(query) && !this.editedTags.includes(tag)
      );
    });
  }

  addEditTag(): void {
    const tag = this.editNewTagText.trim().toLowerCase().substring(0, 10);
    if (tag && !this.editedTags.includes(tag)) {
      this.editedTags = [...this.editedTags, tag];
    }
    this.editNewTagText = '';
  }

  removeEditTag(tag: string): void {
    this.editedTags = this.editedTags.filter(t => t !== tag);
  }

  onEditTagSelect(event: { value: string }): void {
    const tag = event.value.toLowerCase().substring(0, 10);
    if (tag && !this.editedTags.includes(tag)) {
      this.editedTags = [...this.editedTags, tag];
    }
    this.editNewTagText = '';
  }

  /**
   * Checks if Save button should be disabled in edit dialog
   */
  isSaveEditDisabled(): boolean {
    return this.commentUtils.isSaveEditDisabled(this.editedText, this.editedPointerUrl, this.currentDashboardUrl);
  }

  publishComment(): void {
    const comment = this.comment();
    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          this.commentUtils.confirmPublishComment(dashboardId, contextKey, comment.id);
        }
      });
    });
  }


  openDeclineDialog(): void {
    this.declineReason = '';
    this.declineDialogVisible = true;
  }

  closeDeclineDialog(): void {
    this.declineDialogVisible = false;
    this.declineReason = '';
  }

  submitDecline(): void {
    if (!this.declineReason || this.declineReason.trim().length === 0) {
      return;
    }

    const comment = this.comment();
    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          this.commentUtils.declineComment(dashboardId, contextKey, comment.id, this.declineReason.trim());
          this.closeDeclineDialog();
        }
      });
    });
  }

  isDeclineDisabled(): boolean {
    return !this.declineReason || this.declineReason.trim().length === 0;
  }

  deleteComment(): void {
    const comment = this.comment();
    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          this.commentUtils.openDeleteDialog(dashboardId, contextKey, comment.id);
        }
      });
    });
  }

  restoreVersion(versionNumber: number): void {
    const comment = this.comment();
    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          this.commentUtils.confirmRestoreVersion(dashboardId, contextKey, comment.id, versionNumber);
        }
      });
    });
  }

  isCurrentVersionReadyForReview(): boolean {
    const comment = this.comment();
    const currentVersion = comment.history.find(v => v.version === comment.activeVersion);
    return currentVersion?.status === DashboardCommentStatus.READY_FOR_REVIEW;
  }
}
