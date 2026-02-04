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
  selectContextNameByKey
} from '../../../store/my-dashboards/my-dashboards.selector';
import {selectCurrentUserCommentPermissions} from '../../../store/auth/auth.selector';
import {ConfirmationService, PrimeTemplate} from 'primeng/api';
import {Dialog} from "primeng/dialog";
import {Textarea} from "primeng/textarea";
import {ConfirmDialog} from "primeng/confirmdialog";
import {DashboardCommentUtilsService} from '../services/dashboard-comment-utils.service';
import {AutoComplete} from 'primeng/autocomplete';
import {loadAvailableDataDomains} from '../../../store/my-dashboards/my-dashboards.action';


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
    ConfirmDialog,
    AutoComplete
  ],
  providers: [ConfirmationService]
})
export class DomainDashboardCommentsComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly store = inject<Store<AppState>>(Store);
  private readonly domainCommentsService = inject(DomainDashboardCommentsService);
  readonly commentUtils = inject(DashboardCommentUtilsService);
  private readonly confirmationService = inject(ConfirmationService);

  protected readonly DashboardCommentStatus = DashboardCommentStatus;

  private routeSubscription?: Subscription;

  contextKey: string = '';
  contextName: string = '';
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
  editedTags: string[] = [];
  editNewTagText = '';
  editTagSuggestions: string[] = [];
  domainTags: string[] = [];

  // Decline dialog
  declineDialogVisible = false;
  decliningComment: DomainDashboardComment | null = null;
  declineReason = '';

  // Permission selectors
  canEditFn = this.store.selectSignal(canEditComment);
  canPublishFn = this.store.selectSignal(canPublishComment);
  canUnpublishFn = this.store.selectSignal(canUnpublishComment);
  canDeleteFn = this.store.selectSignal(canDeleteComment);
  canViewMetadata = this.store.selectSignal(canViewMetadataAndVersions);


  ngOnInit(): void {
    // Load available data domains to populate contextName in breadcrumb
    this.store.dispatch(loadAvailableDataDomains());

    // Subscribe to route params using ngrx selectors
    this.routeSubscription = combineLatest([
      this.store.select(selectContextKey),
      this.store.select(selectContextNameByKey),
      this.store.select(selectCurrentUserCommentPermissions)
    ]).pipe(
      filter(([contextKey]) => !!contextKey)
    ).subscribe(([contextKey, contextName, commentPermissions]) => {
      // Check if permissions are loaded (not empty object)
      const hasPermissionsLoaded = Object.keys(commentPermissions).length > 0;

      // Redirect only if permissions are loaded AND user doesn't have readComments permission
      if (hasPermissionsLoaded) {
        const perms = commentPermissions[contextKey!];
        if (!perms?.readComments) {
          this.router.navigate(['/my-dashboards']);
          return;
        }
      }

      // Only reload if contextKey changed
      if (contextKey !== this.contextKey) {
        this.contextKey = contextKey!;
        // contextName now comes from selector with built-in fallback to contextKey
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
          label: this.contextName || this.contextKey,
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
        // Map active version text to 'text' field and tags to 'tagsString' for filtering
        this.comments = comments.map(comment => ({
          ...comment,
          text: this.commentUtils.getActiveVersionData(comment)?.text || '',
          status: this.commentUtils.getActiveVersionData(comment)?.status || '',
          tagsString: (this.commentUtils.getActiveVersionData(comment)?.tags || []).join(' ')
        }));

        this.domainTags = this.extractUniqueTags(comments);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private extractUniqueTags(comments: DomainDashboardComment[]): string[] {
    const allTags = new Set<string>();
    for (const comment of comments) {
      if (comment.tags) {
        comment.tags.forEach(t => allTags.add(t));
      }
      if (comment.history) {
        for (const historyItem of comment.history) {
          if (historyItem.tags) {
            historyItem.tags.forEach(t => allTags.add(t));
          }
        }
      }
    }
    return Array.from(allTags).sort((a, b) => a.localeCompare(b));
  }

  getStatusSeverity(status: DashboardCommentStatus | string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    return this.commentUtils.getStatusSeverity(status);
  }

  getStatusLabel(status: DashboardCommentStatus | string): string {
    return this.commentUtils.getStatusLabel(status);
  }

  getActiveVersionData(comment: DomainDashboardComment): DashboardCommentVersion | undefined {
    return this.commentUtils.getActiveVersionData(comment);
  }

  getAllVersions(comment: DomainDashboardComment): (DashboardCommentVersion & { isCurrentVersion: boolean })[] {
    return this.commentUtils.getAllVersions(comment, this.canViewMetadata());
  }

  navigateToDashboard(comment: DomainDashboardComment, pointerUrl?: string): void {
    if (comment.dashboardId && comment.instanceName) {
      const queryParams: any = {};

      // Use provided pointerUrl, or fallback to active version's pointerUrl
      const targetUrl = pointerUrl || this.getActiveVersionData(comment)?.pointerUrl;

      if (targetUrl) {
        queryParams['pointerUrl'] = targetUrl;
      }

      this.router.navigate([
        'my-dashboards',
        'detail',
        comment.instanceName,
        comment.dashboardId
      ], {queryParams});
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
    this.editedPointerUrl = activeVer?.pointerUrl || '';
    this.editedTags = [...(activeVer?.tags || [])];
    this.editDialogVisible = true;
  }

  isEditedPointerUrlValid(): boolean {
    return this.commentUtils.isPointerUrlValid(this.editedPointerUrl, this.editingComment?.dashboardUrl);
  }

  getNormalizedEditedPointerUrl(): string | undefined {
    return this.commentUtils.normalizePointerUrl(this.editedPointerUrl);
  }

  isSaveEditDisabled(): boolean {
    return this.commentUtils.isSaveEditDisabled(this.editedText, this.editedPointerUrl, this.editingComment?.dashboardUrl);
  }

  clearEditedPointerUrl(): void {
    this.editedPointerUrl = '';
  }

  // Tags methods for edit dialog
  searchEditTags(event: { query: string }): void {
    const query = event.query.toLowerCase();
    this.editTagSuggestions = this.domainTags.filter(tag =>
      tag.toLowerCase().includes(query) && !this.editedTags.includes(tag)
    );
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

  saveEdit(): void {
    if (!this.editingComment) return;

    const comment = this.editingComment;
    const activeVer = this.getActiveVersionData(comment);
    const newText = this.editedText.trim();
    const normalizedPointerUrl = this.getNormalizedEditedPointerUrl();

    const textChanged = newText !== activeVer?.text;
    const pointerUrlChanged = normalizedPointerUrl !== (activeVer?.pointerUrl || undefined);

    // Check if tags changed
    const currentTags = activeVer?.tags || [];
    const tagsChanged = this.editedTags.length !== currentTags.length ||
      !this.editedTags.every(t => currentTags.includes(t));

    if (!newText || (!textChanged && !pointerUrlChanged && !tagsChanged)) {
      this.editDialogVisible = false;
      return;
    }

    if (this.editedPointerUrl.trim() && !this.isEditedPointerUrlValid()) {
      return;
    }

    const dashboardId = comment.dashboardId;
    const contextKey = comment.contextKey;

    if (dashboardId && contextKey) {
      // If status is PUBLISHED or DECLINED, clone to create new draft version
      if (activeVer?.status === DashboardCommentStatus.PUBLISHED ||
        activeVer?.status === DashboardCommentStatus.DECLINED) {
        this.commentUtils.dispatchClonePublishedComment(
          dashboardId,
          contextKey,
          comment.id,
          newText,
          normalizedPointerUrl,
          comment.entityVersion,
          this.editedTags
        );
      } else {
        // For DRAFT status, just update the existing draft
        this.commentUtils.dispatchUpdateDraftComment(
          dashboardId,
          contextKey,
          comment.id,
          newText,
          normalizedPointerUrl,
          comment.entityVersion,
          this.editedTags
        );
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
    this.editedTags = [];
    this.editNewTagText = '';
  }

  // Publish comment
  publishCommentAction(comment: DomainDashboardComment): void {
    this.commentUtils.confirmPublishComment(
      comment.dashboardId,
      comment.contextKey,
      comment.id,
      () => setTimeout(() => this.loadComments(), 500),
      this.confirmationService
    );
  }

  // Unpublish comment
  unpublishCommentAction(comment: DomainDashboardComment): void {
    this.commentUtils.confirmUnpublishComment(
      comment.dashboardId,
      comment.contextKey,
      comment.id,
      () => setTimeout(() => this.loadComments(), 500),
      this.confirmationService
    );
  }

  // Open decline dialog
  openDeclineDialog(comment: DomainDashboardComment): void {
    this.decliningComment = comment;
    this.declineReason = '';
    this.declineDialogVisible = true;
  }

  // Close decline dialog
  closeDeclineDialog(): void {
    this.declineDialogVisible = false;
    this.decliningComment = null;
    this.declineReason = '';
  }

  // Submit decline
  submitDecline(): void {
    if (!this.decliningComment || !this.declineReason || this.declineReason.trim().length === 0) {
      return;
    }

    this.commentUtils.declineComment(
      this.decliningComment.dashboardId,
      this.decliningComment.contextKey,
      this.decliningComment.id,
      this.declineReason.trim(),
      () => setTimeout(() => this.loadComments(), 500)
    );
    this.closeDeclineDialog();
  }

  // Check if decline button is disabled
  isDeclineDisabled(): boolean {
    return !this.declineReason || this.declineReason.trim().length === 0;
  }

  // Check if comment can be declined (reviewer can decline drafts)
  canDecline(comment: DomainDashboardComment): boolean {
    const activeVer = this.getActiveVersionData(comment);
    return this.canPublish(comment) && activeVer?.status === DashboardCommentStatus.DRAFT;
  }

  // Delete comment
  deleteCommentAction(comment: DomainDashboardComment): void {
    this.commentUtils.confirmDeleteComment(
      comment.dashboardId,
      comment.contextKey,
      comment.id,
      () => setTimeout(() => this.loadComments(), 500),
      this.confirmationService
    );
  }

  // Restore version
  restoreVersion(comment: DomainDashboardComment, versionNumber: number): void {
    this.commentUtils.confirmRestoreVersion(
      comment.dashboardId,
      comment.contextKey,
      comment.id,
      versionNumber,
      () => setTimeout(() => this.loadComments(), 500),
      this.confirmationService
    );
  }

  onGlobalFilter(table: Table, event: Event): void {
    table.filterGlobal((event.target as HTMLInputElement).value, 'contains');
  }

  clearFilter(table: Table): void {
    this.globalFilterValue = '';
    table.clear();
  }
}
