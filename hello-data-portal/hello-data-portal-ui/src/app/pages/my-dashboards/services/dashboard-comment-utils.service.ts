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

import {inject, Injectable} from '@angular/core';
import {DashboardCommentStatus, DashboardCommentVersion} from '../../../store/my-dashboards/my-dashboards.model';
import {Store} from '@ngrx/store';
import {AppState} from '../../../store/app/app.state';
import {ConfirmationService} from 'primeng/api';
import {TranslateService} from '../../../shared/services/translate.service';
import {
  cloneCommentForEdit,
  deleteComment,
  publishComment,
  restoreCommentVersion,
  unpublishComment,
  updateComment
} from '../../../store/my-dashboards/my-dashboards.action';

/**
 * Common interface for comment-like objects used in both comment-entry and domain-comments
 */
export interface CommentLike {
  id: string;
  activeVersion: number;
  history: DashboardCommentVersion[];
  pointerUrl?: string;
  dashboardUrl?: string;
  entityVersion: number;
}

/**
 * Service providing common utilities for dashboard comments
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardCommentUtilsService {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);

  /**
   * Gets the active version data from a comment
   */
  getActiveVersionData(comment: CommentLike): DashboardCommentVersion | undefined {
    if (!comment?.history || comment.history.length === 0) {
      return undefined;
    }
    return comment.history.find(v => v.version === comment.activeVersion);
  }

  /**
   * Gets all visible versions for a comment
   * - Admins can see all non-deleted versions (both PUBLISHED and DRAFT)
   * - Non-admins can see only non-deleted PUBLISHED versions
   */
  getAllVersions(comment: CommentLike, canViewMetadata: boolean): (DashboardCommentVersion & {
    isCurrentVersion: boolean
  })[] {
    if (!comment?.history) {
      return [];
    }

    return comment.history
      .filter(h => {
        if (h.deleted) return false;
        return canViewMetadata || h.status === DashboardCommentStatus.PUBLISHED;
      })
      .map(h => ({
        ...h,
        isCurrentVersion: h.version === comment.activeVersion
      }))
      .sort((a, b) => a.version - b.version);
  }

  /**
   * Validates that pointerUrl is a valid Superset link (same domain as dashboard)
   */
  isPointerUrlValid(pointerUrl: string, dashboardUrl: string | undefined): boolean {
    const trimmedUrl = pointerUrl?.trim();
    if (!trimmedUrl) return true; // Empty is valid (optional field)

    // Relative paths are always valid
    if (trimmedUrl.startsWith('/')) {
      return true;
    }

    // If dashboardUrl is not available, only empty pointerUrl is valid
    if (!dashboardUrl) {
      return !trimmedUrl;
    }

    // Normalize URL - add https:// if no protocol
    let normalizedUrl = trimmedUrl;
    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      normalizedUrl = 'https://' + trimmedUrl;
    }

    try {
      const dashboardUrlObj = new URL(dashboardUrl);
      const pointerUrlObj = new URL(normalizedUrl);

      // Check same host (Superset instance)
      return dashboardUrlObj.host === pointerUrlObj.host;
    } catch {
      return false;
    }
  }

  /**
   * Normalizes pointerUrl by adding protocol if missing
   */
  normalizePointerUrl(pointerUrl: string): string | undefined {
    const trimmedUrl = pointerUrl?.trim();
    if (!trimmedUrl) return undefined;

    if (trimmedUrl.startsWith('/')) {
      return trimmedUrl;
    }

    if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
      return 'https://' + trimmedUrl;
    }

    return trimmedUrl;
  }

  /**
   * Checks if save edit should be disabled
   */
  isSaveEditDisabled(editedText: string, editedPointerUrl: string, dashboardUrl: string | undefined): boolean {
    // Disabled if text is empty
    if (!editedText?.trim()) {
      return true;
    }

    // Disabled only if pointerUrl is non-empty AND invalid
    const trimmedPointerUrl = editedPointerUrl?.trim();
    if (trimmedPointerUrl && trimmedPointerUrl.length > 0) {
      return !this.isPointerUrlValid(trimmedPointerUrl, dashboardUrl);
    }

    // Otherwise enabled
    return false;
  }

  /**
   * Gets status severity for PrimeNG Tag component
   */
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

  /**
   * Gets status label for translation
   */
  getStatusLabel(status: DashboardCommentStatus | string): string {
    return status === DashboardCommentStatus.PUBLISHED || status === 'PUBLISHED' ? '@Published' : '@DRAFT';
  }

  /**
   * Dispatches publish comment action with confirmation dialog
   */
  confirmPublishComment(dashboardId: number, contextKey: string, commentId: string, onSuccess?: () => void, confirmationService?: ConfirmationService): void {
    const message = this.translateService.translate('@Publish comment question');
    const service = confirmationService || this.confirmationService;
    service.confirm({
      key: 'publishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        this.store.dispatch(publishComment({dashboardId, contextKey, commentId}));
        onSuccess?.();
      }
    });
  }

  /**
   * Dispatches unpublish comment action with confirmation dialog
   */
  confirmUnpublishComment(dashboardId: number, contextKey: string, commentId: string, onSuccess?: () => void, confirmationService?: ConfirmationService): void {
    const message = this.translateService.translate('@Unpublish comment question');
    const service = confirmationService || this.confirmationService;
    service.confirm({
      key: 'unpublishComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        this.store.dispatch(unpublishComment({dashboardId, contextKey, commentId}));
        onSuccess?.();
      }
    });
  }

  /**
   * Dispatches delete comment action with confirmation dialog
   */
  confirmDeleteComment(dashboardId: number, contextKey: string, commentId: string, onSuccess?: () => void, confirmationService?: ConfirmationService): void {
    const message = this.translateService.translate('@Delete comment question');
    const service = confirmationService || this.confirmationService;
    service.confirm({
      key: 'deleteComment',
      message: message,
      icon: 'fas fa-triangle-exclamation',
      closeOnEscape: false,
      accept: () => {
        this.store.dispatch(deleteComment({dashboardId, contextKey, commentId}));
        onSuccess?.();
      }
    });
  }

  /**
   * Dispatches restore version action with confirmation dialog
   */
  confirmRestoreVersion(dashboardId: number, contextKey: string, commentId: string, versionNumber: number, onSuccess?: () => void, confirmationService?: ConfirmationService): void {
    const message = this.translateService.translate('@Restore version question');
    const service = confirmationService || this.confirmationService;
    service.confirm({
      key: 'restoreVersion',
      message: message,
      icon: 'fas fa-rotate-left',
      closeOnEscape: false,
      accept: () => {
        this.store.dispatch(restoreCommentVersion({dashboardId, contextKey, commentId, versionNumber}));
        onSuccess?.();
      }
    });
  }

  /**
   * Dispatches update or clone comment action based on status
   */
  dispatchSaveEdit(
    dashboardId: number,
    contextKey: string,
    commentId: string,
    newText: string,
    newPointerUrl: string | undefined,
    entityVersion: number,
    isPublished: boolean
  ): void {
    if (isPublished) {
      this.store.dispatch(cloneCommentForEdit({
        dashboardId,
        contextKey,
        commentId,
        newText,
        newPointerUrl,
        entityVersion
      }));
    } else {
      this.store.dispatch(updateComment({
        dashboardId,
        contextKey,
        commentId,
        text: newText,
        pointerUrl: newPointerUrl,
        entityVersion
      }));
    }
  }
}

