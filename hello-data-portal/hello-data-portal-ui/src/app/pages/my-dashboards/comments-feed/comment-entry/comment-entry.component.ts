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

import {Component, computed, inject, input} from "@angular/core";
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
  unpublishComment,
  updateComment
} from "../../../../store/my-dashboards/my-dashboards.action";
import {
  canDeleteComment,
  canEditComment,
  canPublishComment,
  canUnpublishComment,
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId
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

  expanded = false;

  // Edit dialog
  editDialogVisible = false;
  editedText = '';

  isSuperuser$ = this.store.select(selectIsSuperuser);
  currentDashboardId$ = this.store.select(selectCurrentDashboardId);
  currentDashboardContextKey$ = this.store.select(selectCurrentDashboardContextKey);

  // Computed properties for permissions
  canEditFn = this.store.selectSignal(canEditComment);
  canPublishFn = this.store.selectSignal(canPublishComment);
  canUnpublishFn = this.store.selectSignal(canUnpublishComment);
  canDeleteFn = this.store.selectSignal(canDeleteComment);

  canEdit = computed(() => this.canEditFn()(this.comment()));
  canPublish = computed(() => this.canPublishFn()(this.comment()));
  canUnpublish = computed(() => this.canUnpublishFn()(this.comment()));
  canDelete = computed(() => this.canDeleteFn()(this.comment()));

  protected readonly CommentStatus = CommentStatus;

  toggleDetails(): void {
    this.expanded = !this.expanded;
  }

  onKeyDown(event: KeyboardEvent, action: () => void): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      action();
    }
  }

  editComment(): void {
    const comment = this.comment();
    this.editedText = comment.text;
    this.editDialogVisible = true;
  }

  saveEdit(): void {
    const comment = this.comment();
    const newText = this.editedText.trim();

    if (!newText || newText === comment.text) {
      this.editDialogVisible = false;
      return;
    }

    this.currentDashboardId$.pipe(take(1)).subscribe(dashboardId => {
      this.currentDashboardContextKey$.pipe(take(1)).subscribe(contextKey => {
        if (dashboardId && contextKey) {
          if (comment.status === CommentStatus.PUBLISHED) {
            // For published comments, clone with new text
            this.store.dispatch(cloneCommentForEdit({
              dashboardId,
              contextKey,
              commentId: comment.id,
              newText
            }));
          } else {
            // For draft comments, update directly
            this.store.dispatch(updateComment({
              dashboardId,
              contextKey,
              commentId: comment.id,
              text: newText
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
}
