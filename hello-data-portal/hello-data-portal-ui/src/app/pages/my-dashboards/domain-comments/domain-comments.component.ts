///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Store} from '@ngrx/store';
import {AppState} from '../../../store/app/app.state';
import {TranslocoPipe} from '@jsverse/transloco';
import {DatePipe, SlicePipe} from '@angular/common';
import {Table, TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {createBreadcrumbs} from '../../../store/breadcrumb/breadcrumb.action';
import {naviElements} from '../../../app-navi-elements';
import {DomainDashboardComment, DomainDashboardCommentsService} from './domain-comments.service';
import {DashboardCommentStatus, DashboardCommentVersion} from '../../../store/my-dashboards/my-dashboards.model';
import {InputText} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {combineLatest, filter, Subscription} from 'rxjs';
import {
  canDeleteComment,
  canEditComment,
  canPublishComment,
  canUnpublishComment,
  canViewMetadataAndVersions,
  selectContextKey,
  selectContextName
} from '../../../store/my-dashboards/my-dashboards.selector';
import {ConfirmationService, PrimeTemplate} from 'primeng/api';
import {TranslateService} from '../../../shared/services/translate.service';
import {
  cloneCommentForEdit,
  deleteComment,
  publishComment,
  restoreCommentVersion,
  unpublishComment,
  updateComment
} from '../../../store/my-dashboards/my-dashboards.action';
import {Dialog} from "primeng/dialog";
import {Textarea} from "primeng/textarea";
import {ConfirmDialog} from "primeng/confirmdialog";


@Component({
  selector: 'app-domain-comments',
  templateUrl: './domain-comments.component.html',
  styleUrls: ['./domain-comments.component.scss'],
  imports: [
    TranslocoPipe,
    DatePipe,
    SlicePipe,
    TableModule,
    Button,
    Tag,
    Tooltip,
    InputText,
    FormsModule,
    IconField,
    InputIcon,
    PrimeTemplate,
    Dialog,
    Textarea,
    ConfirmDialog
  ],
  providers: [ConfirmationService]
})
export class DomainDashboardCommentsComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly store = inject<Store<AppState>>(Store);
  private readonly domainCommentsService = inject(DomainDashboardCommentsService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);

  protected readonly DashboardCommentStatus = DashboardCommentStatus;

  private routeSubscription?: Subscription;

  contextKey: string = '';
  contextName: string | string[] = '';
  comments: DomainDashboardComment[] = [];
  loading = true;

  // For filtering
  globalFilterValue: string = '';

  // For expanded rows (info panel)
  expandedComments: Set<string> = new Set();

  // Edit dialog
  editDialogVisible = false;
  editingComment: DomainDashboardComment | null = null;
  editedText = '';
  editedPointerUrl = '';

  // Permission selectors
  canEditFn = this.store.selectSignal(canEditComment);
  canPublishFn = this.store.selectSignal(canPublishComment);
  canUnpublishFn = this.store.selectSignal(canUnpublishComment);
  canDeleteFn = this.store.selectSignal(canDeleteComment);
  canViewMetadata = this.store.selectSignal(canViewMetadataAndVersions);


  ngOnInit(): void {
    // Subscribe to route params using ngrx selectors
    this.routeSubscription = combineLatest([
      this.store.select(selectContextKey),
      this.store.select(selectContextName)
    ]).pipe(
      filter(([contextKey]) => !!contextKey)
    ).subscribe(([contextKey, contextName]) => {
      // Only reload if contextKey changed
      if (contextKey !== this.contextKey) {
        this.contextKey = contextKey!;
        this.contextName = contextName || contextKey!;
        this.globalFilterValue = '';
        this.createBreadcrumbs();
        this.loadComments();
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.myDashboards.label,
          routerLink: naviElements.myDashboards.path
        },
        {
          label: this.contextName,
          routerLink: naviElements.myDashboards.path
        },
        {
          label: '@Domain Comments'
        }
      ]
    }));
  }

  private loadComments(): void {
    this.loading = true;
    this.domainCommentsService.getCommentsForDomain(this.contextKey).subscribe({
      next: (comments: DomainDashboardComment[]) => {
        this.comments = comments;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getStatusSeverity(status: DashboardCommentStatus | string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (status) {
      case DashboardCommentStatus.PUBLISHED:
      case 'PUBLISHED':
        return 'success';
      case DashboardCommentStatus.DRAFT:
      case 'DRAFT':
        return 'warn';
      default:
        return 'info';
    }
  }

  getStatusLabel(status: DashboardCommentStatus | string): string {
    return status === DashboardCommentStatus.PUBLISHED || status === 'PUBLISHED' ? '@Published' : '@DRAFT';
  }

  getActiveVersionData(comment: DomainDashboardComment): DashboardCommentVersion | undefined {
    if (!comment.history || comment.history.length === 0) {
      return undefined;
    }
    return comment.history.find(v => v.version === comment.activeVersion);
  }

  getAllVersions(comment: DomainDashboardComment): (DashboardCommentVersion & { isCurrentVersion: boolean })[] {
    const canViewMetadataValue = this.canViewMetadata();
    return comment.history
      .filter(h => {
        if (h.deleted) return false;
        return canViewMetadataValue || h.status === DashboardCommentStatus.PUBLISHED;
      })
      .map(h => ({
        ...h,
        isCurrentVersion: h.version === comment.activeVersion
      }))
      .sort((a, b) => a.version - b.version);
  }

  navigateToDashboard(comment: DomainDashboardComment): void {
    if (comment.dashboardId && comment.instanceName) {
      this.router.navigate([
        'my-dashboards',
        'detail',
        comment.instanceName,
        comment.dashboardId
      ]);
    }
  }

  // Permission checks
  canEdit(comment: DomainDashboardComment): boolean {
    return this.canEditFn()(comment);
  }

  canPublish(comment: DomainDashboardComment): boolean {
    return this.canPublishFn()(comment);
  }

  canUnpublish(comment: DomainDashboardComment): boolean {
    return this.canUnpublishFn()(comment);
  }

  canDelete(comment: DomainDashboardComment): boolean {
    return this.canDeleteFn()(comment);
  }

  // Toggle expanded state
  isExpanded(comment: DomainDashboardComment): boolean {
    return this.expandedComments.has(comment.id);
  }

  toggleDetails(comment: DomainDashboardComment): void {
    if (this.expandedComments.has(comment.id)) {
      this.expandedComments.delete(comment.id);
    } else {
      this.expandedComments.add(comment.id);
    }
  }

  // Edit comment
  editComment(comment: DomainDashboardComment): void {
    this.editingComment = comment;
    const activeVer = this.getActiveVersionData(comment);
    this.editedText = activeVer?.text || '';
    this.editedPointerUrl = comment.pointerUrl || '';
    this.editDialogVisible = true;
  }

  isEditedPointerUrlValid(): boolean {
    const trimmedUrl = this.editedPointerUrl.trim();
    if (!trimmedUrl) return true;

    if (trimmedUrl.startsWith('/')) return true;

    if (!this.editingComment?.dashboardUrl) {
      return !trimmedUrl;
    }

    let normalizedUrl = trimmedUrl;
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      normalizedUrl = 'https://' + trimmedUrl;
    }

    try {
      const dashboardUrlObj = new URL(this.editingComment.dashboardUrl);
      const pointerUrlObj = new URL(normalizedUrl);
      return dashboardUrlObj.host === pointerUrlObj.host;
    } catch {
      return false;
    }
  }

  getNormalizedEditedPointerUrl(): string | undefined {
    const trimmedUrl = this.editedPointerUrl.trim();
    if (!trimmedUrl) return undefined;
    if (trimmedUrl.startsWith('/')) return trimmedUrl;
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      return 'https://' + trimmedUrl;
    }
    return trimmedUrl;
  }

  isSaveEditDisabled(): boolean {
    if (!this.editedText?.trim()) return true;
    const trimmedPointerUrl = this.editedPointerUrl?.trim();
    if (trimmedPointerUrl && trimmedPointerUrl.length > 0) {
      return !this.isEditedPointerUrlValid();
    }
    return false;
  }

  saveEdit(): void {
    if (!this.editingComment) return;

    const comment = this.editingComment;
    const activeVer = this.getActiveVersionData(comment);
    const newText = this.editedText.trim();
    const normalizedPointerUrl = this.getNormalizedEditedPointerUrl();

    const textChanged = newText !== activeVer?.text;
    const pointerUrlChanged = normalizedPointerUrl !== (comment.pointerUrl || undefined);

    if (!newText || (!textChanged && !pointerUrlChanged)) {
      this.editDialogVisible = false;
      return;
    }

    if (this.editedPointerUrl.trim() && !this.isEditedPointerUrlValid()) {
      return;
    }

    const dashboardId = comment.dashboardId;
    const contextKey = comment.contextKey;

    if (dashboardId && contextKey) {
      if (activeVer?.status === DashboardCommentStatus.PUBLISHED) {
        this.store.dispatch(cloneCommentForEdit({
          dashboardId,
          contextKey,
          commentId: comment.id,
          newText,
          newPointerUrl: normalizedPointerUrl,
          entityVersion: comment.entityVersion
        }));
      } else {
        this.store.dispatch(updateComment({
          dashboardId,
          contextKey,
          commentId: comment.id,
          text: newText,
          pointerUrl: normalizedPointerUrl,
          entityVersion: comment.entityVersion
        }));
      }
    }

    this.editDialogVisible = false;
    this.editingComment = null;

    // Reload comments after action
    setTimeout(() => this.loadComments(), 500);
  }

  cancelEdit(): void {
    this.editDialogVisible = false;
    this.editingComment = null;
    this.editedText = '';
    this.editedPointerUrl = '';
  }

  // Publish comment
  publishCommentAction(comment: DomainDashboardComment): void {
    const message = this.translateService.translate('@Publish comment question');
    this.confirmationService.confirm({
      key: 'publishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      accept: () => {
        this.store.dispatch(publishComment({
          dashboardId: comment.dashboardId,
          contextKey: comment.contextKey,
          commentId: comment.id
        }));
        setTimeout(() => this.loadComments(), 500);
      }
    });
  }

  // Unpublish comment
  unpublishCommentAction(comment: DomainDashboardComment): void {
    const message = this.translateService.translate('@Unpublish comment question');
    this.confirmationService.confirm({
      key: 'unpublishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      accept: () => {
        this.store.dispatch(unpublishComment({
          dashboardId: comment.dashboardId,
          contextKey: comment.contextKey,
          commentId: comment.id
        }));
        setTimeout(() => this.loadComments(), 500);
      }
    });
  }

  // Delete comment
  deleteCommentAction(comment: DomainDashboardComment): void {
    const message = this.translateService.translate('@Delete comment question');
    this.confirmationService.confirm({
      key: 'deleteComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      accept: () => {
        this.store.dispatch(deleteComment({
          dashboardId: comment.dashboardId,
          contextKey: comment.contextKey,
          commentId: comment.id
        }));
        setTimeout(() => this.loadComments(), 500);
      }
    });
  }

  // Restore version
  restoreVersion(comment: DomainDashboardComment, versionNumber: number): void {
    const message = this.translateService.translate('@Restore version question');
    this.confirmationService.confirm({
      key: 'restoreVersion',
      message: message,
      icon: 'fas fa-rotate-left',
      accept: () => {
        this.store.dispatch(restoreCommentVersion({
          dashboardId: comment.dashboardId,
          contextKey: comment.contextKey,
          commentId: comment.id,
          versionNumber
        }));
        setTimeout(() => this.loadComments(), 500);
      }
    });
  }

  onGlobalFilter(table: Table, event: Event): void {
    table.filterGlobal((event.target as HTMLInputElement).value, 'contains');
  }

  clearFilter(table: Table): void {
    this.globalFilterValue = '';
    table.clear();
  }
}

