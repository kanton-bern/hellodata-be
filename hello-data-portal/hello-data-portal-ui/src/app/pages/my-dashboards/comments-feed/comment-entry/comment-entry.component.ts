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

import {Component, computed, inject, input, output} from "@angular/core";
import {AsyncPipe, DatePipe, SlicePipe} from "@angular/common";
import {Tooltip} from "primeng/tooltip";
import {TranslocoPipe} from "@jsverse/transloco";
import {CommentEntry, CommentStatus} from "../../../../store/my-dashboards/my-dashboards.model";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectIsSuperuser} from "../../../../store/auth/auth.selector";
import {
  cloneCommentForEdit,
  deleteComment,
  publishComment,
  restoreCommentVersion,
  unpublishComment,
  updateComment
} from "../../../../store/my-dashboards/my-dashboards.action";
import {
  canDeleteComment,
  canEditComment,
  canPublishComment,
  canUnpublishComment,
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId,
  selectCurrentDashboardUrl
} from "../../../../store/my-dashboards/my-dashboards.selector";
import {take} from "rxjs";
import {ConfirmationService, PrimeTemplate} from "primeng/api";
import {TranslateService} from "../../../../shared/services/translate.service";
import {Dialog} from "primeng/dialog";
import {FormsModule} from "@angular/forms";
import {Textarea} from "primeng/textarea";
import {Button} from "primeng/button";

@Component({
  selector: 'app-comment-entry',
  standalone: true,
  templateUrl: './comment-entry.component.html',
  imports: [
    DatePipe,
    SlicePipe,
    Tooltip,
    TranslocoPipe,
    AsyncPipe,
    Dialog,
    FormsModule,
    Textarea,
    Button,
    PrimeTemplate
  ],
  styleUrls: ['./comment-entry.component.scss']
})
export class CommentEntryComponent {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);

  comment = input.required<CommentEntry>();
  pointerUrlClick = output<string>();

  expanded = false;

  // Edit dialog
  editDialogVisible = false;
  editedText = '';
  editedPointerUrl = '';

  isSuperuser$ = this.store.select(selectIsSuperuser);
  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);
  currentDashboardUrl$ = this.store.select(selectCurrentDashboardUrl);

  private currentDashboardUrl: string | undefined;

  // Computed properties for permissions
  canEditFn = this.store.selectSignal(canEditComment);
  canPublishFn = this.store.selectSignal(canPublishComment);
  canUnpublishFn = this.store.selectSignal(canUnpublishComment);
  canDeleteFn = this.store.selectSignal(canDeleteComment);

  canEdit = computed(() => this.canEditFn()(this.comment()));
  canPublish = computed(() => this.canPublishFn()(this.comment()));
  canUnpublish = computed(() => this.canUnpublishFn()(this.comment()));
  canDelete = computed(() => this.canDeleteFn()(this.comment()));

  // Get active version from comment
  activeVersion = computed(() => {
    const comment = this.comment();
    return comment.history.find(v => v.version === comment.activeVersion);
  });

  // All visible versions: only non-deleted PUBLISHED from history
  allVersions = computed(() => {
    const comment = this.comment();
    return comment.history
      .filter(h => h.status === CommentStatus.PUBLISHED && !h.deleted)
      .map(h => ({
        ...h,
        isCurrentVersion: h.version === comment.activeVersion
      }))
      .sort((a, b) => a.version - b.version);
  });

  protected readonly CommentStatus = CommentStatus;

  constructor() {
    this.currentDashboardUrl$.subscribe(url => this.currentDashboardUrl = url);
  }

  /**
   * Validates that editedPointerUrl is a valid Superset link (same domain as current dashboard)
   */
  isEditedPointerUrlValid(): boolean {
    const trimmedUrl = this.editedPointerUrl.trim();
    if (!trimmedUrl) return true; // Empty is valid (optional field)

    // Relative paths are always valid
    if (trimmedUrl.startsWith('/')) {
      return true;
    }

    // If currentDashboardUrl is not loaded yet, allow empty pointerUrl
    if (!this.currentDashboardUrl) {
      return !trimmedUrl; // Only valid if empty
    }

    // Normalize URL - add https:// if no protocol
    let normalizedUrl = trimmedUrl;
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      normalizedUrl = 'https://' + trimmedUrl;
    }


    try {
      const dashboardUrlObj = new URL(this.currentDashboardUrl);
      const pointerUrlObj = new URL(normalizedUrl);

      // Check same host (Superset instance)
      return dashboardUrlObj.host === pointerUrlObj.host;
    } catch {
      return false;
    }
  }

  /**
   * Returns normalized editedPointerUrl with protocol
   */
  getNormalizedEditedPointerUrl(): string | undefined {
    const trimmedUrl = this.editedPointerUrl.trim();
    if (!trimmedUrl) return undefined;

    if (trimmedUrl.startsWith('/')) {
      return trimmedUrl;
    }

    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      return 'https://' + trimmedUrl;
    }

    return trimmedUrl;
  }

  toggleDetails(): void {
    this.expanded = !this.expanded;
  }

  navigateToPointerUrl(): void {
    const url = this.comment().pointerUrl;
    if (url) {
      this.pointerUrlClick.emit(url);
    }
  }

  onKeyDown(event: KeyboardEvent, action: () => void): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      action();
    }
  }

  editComment(): void {
    const comment = this.comment();
    const activeVer = this.activeVersion();
    this.editedText = activeVer?.text || '';
    this.editedPointerUrl = comment.pointerUrl || '';
    this.editDialogVisible = true;
  }

  saveEdit(): void {
    const comment = this.comment();
    const activeVer = this.activeVersion();
    const newText = this.editedText.trim();
    const normalizedPointerUrl = this.getNormalizedEditedPointerUrl();

    // Check if anything changed
    const textChanged = newText !== activeVer?.text;
    const pointerUrlChanged = normalizedPointerUrl !== (comment.pointerUrl || undefined);

    if (!newText || (!textChanged && !pointerUrlChanged)) {
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
          if (activeVer?.status === CommentStatus.PUBLISHED) {
            // For published comments, clone with new text
            this.store.dispatch(cloneCommentForEdit({
              dashboardId,
              contextKey,
              commentId: comment.id,
              newText,
              newPointerUrl: normalizedPointerUrl
            }));
          } else {
            // For draft comments, update directly
            this.store.dispatch(updateComment({
              dashboardId,
              contextKey,
              commentId: comment.id,
              text: newText,
              pointerUrl: normalizedPointerUrl
            }));
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
  }

  /**
   * Checks if Save button should be disabled in edit dialog
   */
  isSaveEditDisabled(): boolean {
    // Disabled if text is empty
    if (!this.editedText?.trim()) {
      return true;
    }

    // Disabled only if pointerUrl is non-empty AND invalid
    const trimmedPointerUrl = this.editedPointerUrl?.trim();
    if (trimmedPointerUrl && trimmedPointerUrl.length > 0) {
      return !this.isEditedPointerUrlValid();
    }

    // Otherwise enabled
    return false;
  }

  publishComment(): void {
    const message = this.translateService.translate('@Publish comment question');
    this.confirmationService.confirm({
      key: 'publishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        const comment = this.comment();
        this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
          this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
            if (dashboardId && contextKey) {
              this.store.dispatch(publishComment({
                dashboardId,
                contextKey,
                commentId: comment.id
              }));
            }
          });
        });
      }
    });
  }

  unpublishComment(): void {
    const message = this.translateService.translate('@Unpublish comment question');
    this.confirmationService.confirm({
      key: 'unpublishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        const comment = this.comment();
        this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
          this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
            if (dashboardId && contextKey) {
              this.store.dispatch(unpublishComment({
                dashboardId,
                contextKey,
                commentId: comment.id
              }));
            }
          });
        });
      }
    });
  }

  deleteComment(): void {
    const message = this.translateService.translate('@Delete comment question');
    this.confirmationService.confirm({
      key: 'deleteComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        const comment = this.comment();
        this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
          this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
            if (dashboardId && contextKey) {
              this.store.dispatch(deleteComment({
                dashboardId,
                contextKey,
                commentId: comment.id
              }));
            }
          });
        });
      }
    });
  }

  restoreVersion(versionNumber: number): void {
    const message = this.translateService.translate('@Restore version question');
    this.confirmationService.confirm({
      key: 'restoreVersion',
      message: message,
      icon: 'fas fa-rotate-left',
      closeOnEscape: false,
      accept: () => {
        const comment = this.comment();
        this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
          this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
            if (dashboardId && contextKey) {
              this.store.dispatch(restoreCommentVersion({
                dashboardId,
                contextKey,
                commentId: comment.id,
                versionNumber
              }));
            }
          });
        });
      }
    });
  }
}
