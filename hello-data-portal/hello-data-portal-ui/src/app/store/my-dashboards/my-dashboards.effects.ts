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

import {inject, Injectable} from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {asyncScheduler, catchError, scheduled, switchMap, take, withLatestFrom} from "rxjs";
import {MyDashboardsService} from "./my-dashboards.service";
import {navigate, navigateToList, showError, showSuccess, trackEvent} from "../app/app.action";
import {
  addComment,
  addCommentSuccess,
  cloneCommentForEdit,
  cloneCommentForEditSuccess,
  deleteComment,
  deleteCommentSuccess,
  loadAvailableDataDomains,
  loadAvailableDataDomainsSuccess,
  loadDashboardComments,
  loadDashboardCommentsSuccess,
  loadMyDashboards,
  loadMyDashboardsSuccess,
  publishComment,
  publishCommentSuccess,
  restoreCommentVersion,
  restoreCommentVersionSuccess,
  setSelectedDataDomain,
  unpublishComment,
  unpublishCommentSuccess,
  updateComment,
  updateCommentError,
  updateCommentSuccess,
  uploadDashboardsError,
  uploadDashboardsSuccess
} from "./my-dashboards.action";
import {CommentEntry, CommentStatus, CommentVersion} from "./my-dashboards.model";
import {NotificationService} from "../../shared/services/notification.service";
import {TranslateService} from "../../shared/services/translate.service";
import {ScreenService} from "../../shared/services";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectCurrentUserPermissions, selectProfile} from "../auth/auth.selector";

@Injectable()
export class MyDashboardsEffects {
  private readonly _actions$ = inject(Actions);
  private readonly _myDashboardsService = inject(MyDashboardsService);
  private readonly _notificationService = inject(NotificationService);
  private readonly _translateService = inject(TranslateService);
  private readonly _screenService = inject(ScreenService);
  private readonly _store = inject<Store<AppState>>(Store);


  loadMyDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyDashboards),
      withLatestFrom(this._store.select(selectCurrentUserPermissions)),
      switchMap(([action, currentUserPermissions]) => {
        const dashboardsPermission = currentUserPermissions.find(permission => permission === 'DASHBOARDS');
        if (currentUserPermissions && dashboardsPermission && dashboardsPermission.length > 0) {
          return this._myDashboardsService.getMyDashboards(); //load dashboards only if user has DASHBOARDS permission
        }
        return scheduled([[]], asyncScheduler); //empty array if no permission
      }),
      switchMap(result => scheduled([loadMyDashboardsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  setSelectedDataDomain$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setSelectedDataDomain),
      withLatestFrom(this._screenService.isMobile),
      switchMap(([action, isMobile]) => {
          const successMsg = {
            message: '@Data domain changed',
            interpolateParams: {'dataDomainName': this._translateService.translate(action.dataDomain.name)}
          };
          if (isMobile) {
            return scheduled([
              trackEvent({
                eventCategory: 'Mobile',
                eventAction: '[Click] - Data Domain changed to ' + action.dataDomain.name
              }),
              showSuccess(successMsg),
              navigate({url: 'home'})
            ], asyncScheduler);
          }
          return scheduled([
            trackEvent({
              eventCategory: 'Data Domain',
              eventAction: '[Click] - Data Domain changed to ' + action.dataDomain.name
            }),
            showSuccess(successMsg),
            navigateToList()
          ], asyncScheduler);
        }
      ),
    )
  });

  loadAvailableDataDomains$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableDataDomains),
      switchMap(() => this._myDashboardsService.getAvailableDataDomains()),
      switchMap(result => scheduled([loadAvailableDataDomainsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  uploadDashboardsFileSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsSuccess),
      switchMap(() => {
        this._notificationService.success('@Dashboards uploaded successfully');
        return scheduled([navigate({url: 'redirect/dashboard-import-export'})], asyncScheduler)
      })
    )
  });

  uploadDashboardsFileError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsError),
      switchMap((payload) => {
        return scheduled([showError({error: payload.error}), navigate({url: 'redirect/dashboard-import-export'})], asyncScheduler)
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  // Comments effects
  loadDashboardComments$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadDashboardComments),
      switchMap(({dashboardId, contextKey, dashboardUrl}) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.getDashboardComments(contextKey, dashboardId).pipe(
        //   switchMap(comments => scheduled([loadDashboardCommentsSuccess({comments})], asyncScheduler)),
        //   catchError(e => scheduled([loadDashboardCommentsError({error: e})], asyncScheduler))
        // )

        // Temporary mock data for testing
        const mockComments = [
          {
            id: '1',
            dashboardId: dashboardId,
            dashboardUrl: dashboardUrl,
            contextKey: contextKey,
            text: 'First test comment.',
            author: 'John Doe',
            authorEmail: 'john.doe@example.com',
            status: CommentStatus.PUBLISHED,
            createdDate: new Date('2024-06-01T09:30:00').getTime(),
            publishedDate: new Date('2024-06-01T09:30:00').getTime(),
            publishedBy: 'Admin',
            deleted: false,
            version: 1,
            history: [] as CommentVersion[],
          },
          {
            id: '2',
            dashboardId: dashboardId,
            dashboardUrl: dashboardUrl,
            contextKey: contextKey,
            text: 'Great data, thanks for sharing!',
            author: 'Anne Smith',
            authorEmail: 'anne.smith@example.com',
            status: CommentStatus.PUBLISHED,
            createdDate: new Date('2024-06-02T14:15:00').getTime(),
            publishedDate: new Date('2024-06-02T14:15:00').getTime(),
            publishedBy: 'Admin',
            deleted: false,
            version: 1,
            history: [] as CommentVersion[],
          },
        ];
        return scheduled([loadDashboardCommentsSuccess({comments: mockComments})], asyncScheduler);
      })
    )
  });

  addComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(addComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, dashboardUrl, text}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.addComment(contextKey, dashboardId, text).pipe(
        //   switchMap(comment => scheduled([
        //     addCommentSuccess({comment}),
        //     showSuccess({message: '@Comment added successfully'})
        //   ], asyncScheduler)),
        //   catchError(e => scheduled([addCommentError({error: e}), showError({error: e})], asyncScheduler))
        // )

        // Temporary mock - simulating backend response
        // Backend generates: id, author (createdBy), authorEmail, status (DRAFT), deleted, version
        const authorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';
        const authorEmail = profile?.email || 'unknown@example.com';
        const mockComment = {
          id: crypto.randomUUID(), // Backend generates UUID
          dashboardId: dashboardId,
          dashboardUrl: dashboardUrl,
          contextKey: contextKey,
          text,
          author: authorName, // Backend sets from authenticated user
          authorEmail: authorEmail, // Backend sets from authenticated user
          status: CommentStatus.DRAFT, // Backend sets initial status as DRAFT
          createdDate: Date.now(),
          deleted: false, // Backend sets default as false
          version: 1, // Initial version
          history: [] as CommentVersion[], // Empty history for new comment
        };
        return scheduled([
          addCommentSuccess({comment: mockComment}),
          showSuccess({message: '@Comment added successfully'})
        ], asyncScheduler);
      })
    )
  });

  updateComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId, text}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.updateComment(contextKey, dashboardId, commentId, text).pipe(
        //   switchMap(comment => scheduled([
        //     updateCommentSuccess({comment}),
        //     showSuccess({message: '@Comment updated successfully'})
        //   ], asyncScheduler)),
        //   catchError(e => scheduled([updateCommentError({error: e}), showError({error: e})], asyncScheduler))
        // )

        // Temporary mock - simulating backend response for DRAFT comment update
        // For DRAFT comments, we only update the text and edit metadata - no versioning needed
        const editorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              // For DRAFT comments - just update text and edit metadata, no version increment
              const updatedComment = {
                ...existingComment,
                text,
                lastEditedDate: Date.now(),
                lastEditedBy: editorName,
              };
              return scheduled([
                updateCommentSuccess({comment: updatedComment}),
                showSuccess({message: '@Comment updated successfully'})
              ], asyncScheduler);
            }
            return scheduled([
              updateCommentError({error: 'Comment not found'}),
              showError({error: 'Comment not found'})
            ], asyncScheduler);
          })
        );
      })
    )
  });

  deleteComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId}, profile]) => {
        // TODO: Replace with actual API call when backend is ready

        // Temporary mock - simulating backend response (soft delete)
        const deleterName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        // Get existing comment and original comment (if draft) to check for history
        return this._store.select(state => ({
          comment: state.myDashboards.currentDashboardComments.find(c => c.id === commentId),
          allComments: state.myDashboards.currentDashboardComments
        })).pipe(
          take(1),
          switchMap(({comment: existingComment, allComments}) => {
            let restoredComment: CommentEntry | undefined;

            console.log('DELETE COMMENT DEBUG:', {
              commentId,
              existingComment,
              status: existingComment?.status,
              history: existingComment?.history,
              historyLength: existingComment?.history?.length,
              previousVersionId: existingComment?.previousVersionId
            });

            // Try to restore last published version from history
            if (existingComment?.history && existingComment.history.length > 0) {
              // Find the last published version in history (different from current version)
              const lastPublishedVersion = [...existingComment.history]
                .reverse()
                .find(h => h.status === CommentStatus.PUBLISHED && h.version !== existingComment.version);

              console.log('DELETE COMMENT DEBUG - looking for published version:', {
                historyStatuses: existingComment.history.map(h => ({version: h.version, status: h.status})),
                currentVersion: existingComment.version,
                lastPublishedVersion
              });

              if (lastPublishedVersion) {
                // If comment has previousVersionId, restore the original comment
                if (existingComment.previousVersionId) {
                  const originalComment = allComments.find(c => c.id === existingComment.previousVersionId);
                  if (originalComment) {
                    restoredComment = {
                      ...originalComment,
                      text: lastPublishedVersion.text,
                      status: CommentStatus.PUBLISHED,
                      version: lastPublishedVersion.version,
                      publishedDate: lastPublishedVersion.publishedDate,
                      publishedBy: lastPublishedVersion.publishedBy,
                      lastEditedDate: lastPublishedVersion.editedDate,
                      lastEditedBy: lastPublishedVersion.editedBy,
                      hasActiveDraft: false,
                      deleted: false,
                      deletedDate: undefined,
                      deletedBy: undefined,
                      history: existingComment.history,
                    };
                  }
                } else {
                  // No previousVersionId - restore in place
                  restoredComment = {
                    ...existingComment,
                    text: lastPublishedVersion.text,
                    status: CommentStatus.PUBLISHED,
                    version: lastPublishedVersion.version,
                    publishedDate: lastPublishedVersion.publishedDate,
                    publishedBy: lastPublishedVersion.publishedBy,
                    lastEditedDate: lastPublishedVersion.editedDate,
                    lastEditedBy: lastPublishedVersion.editedBy,
                    deleted: false,
                    history: existingComment.history,
                  };
                }
              }
            }

            console.log('DELETE COMMENT DEBUG - restoredComment:', restoredComment);

            return scheduled([
              deleteCommentSuccess({
                commentId,
                deletedDate: Date.now(),
                deletedBy: deleterName,
                previousVersionId: existingComment?.previousVersionId,
                restoredComment
              }),
              showSuccess({message: restoredComment ? '@Comment version restored' : '@Comment deleted successfully'})
            ], asyncScheduler);
          })
        );
      })
    )
  });

  publishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(publishComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.publishComment(contextKey, dashboardId, commentId).pipe(...)

        // Temporary mock - simulating backend response
        const publisherName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        // Get existing comment data from store and merge with published data
        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              const updatedComment = {
                ...existingComment,
                status: CommentStatus.PUBLISHED,
                publishedDate: Date.now(),
                publishedBy: publisherName,
              };
              return scheduled([
                publishCommentSuccess({comment: updatedComment}),
                showSuccess({message: '@Comment published successfully'})
              ], asyncScheduler);
            }
            return scheduled([showError({error: 'Comment not found'})], asyncScheduler);
          })
        );
      })
    )
  });

  unpublishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(unpublishComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.unpublishComment(contextKey, dashboardId, commentId).pipe(...)

        // Temporary mock - simulating backend response
        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              // Save current published version to history before unpublishing
              let updatedHistory = [...(existingComment.history || [])];
              const currentVersionInHistory = updatedHistory.some(h => h.version === existingComment.version);

              if (!currentVersionInHistory && existingComment.status === CommentStatus.PUBLISHED) {
                // Add current published version to history
                updatedHistory.push({
                  version: existingComment.version,
                  text: existingComment.text,
                  status: existingComment.status,
                  editedDate: existingComment.lastEditedDate || existingComment.createdDate,
                  editedBy: existingComment.lastEditedBy || existingComment.author,
                  publishedDate: existingComment.publishedDate,
                  publishedBy: existingComment.publishedBy,
                });
                // Sort by version number
                updatedHistory = updatedHistory.sort((a, b) => a.version - b.version);
              }

              const updatedComment: CommentEntry = {
                ...existingComment,
                status: CommentStatus.DRAFT,
                publishedDate: undefined,
                publishedBy: undefined,
                history: updatedHistory,
              };
              return scheduled([
                unpublishCommentSuccess({comment: updatedComment}),
                showSuccess({message: '@Comment unpublished successfully'})
              ], asyncScheduler);
            }
            return scheduled([showError({error: 'Comment not found'})], asyncScheduler);
          })
        );
      })
    )
  });

  cloneCommentForEdit$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(cloneCommentForEdit),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId, newText}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.cloneCommentForEdit(contextKey, dashboardId, commentId, newText).pipe(...)

        // Temporary mock - simulating backend response
        const editorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment && existingComment.status === CommentStatus.PUBLISHED) {
              // Clone the published comment as a draft for editing with new text
              // Keep original createdDate and author - only edit metadata changes
              const clonedComment: CommentEntry = {
                ...existingComment,
                id: crypto.randomUUID(), // New ID for the clone
                text: newText, // Use new text from edit dialog
                status: CommentStatus.DRAFT,
                version: existingComment.version + 1,
                previousVersionId: existingComment.id, // Reference to original
                // createdDate and author remain unchanged from existingComment
                publishedDate: undefined,
                publishedBy: undefined,
                lastEditedDate: Date.now(),
                lastEditedBy: editorName,
                hasActiveDraft: false, // New draft doesn't have active draft
                history: [
                  ...(existingComment.history || []),
                  {
                    version: existingComment.version,
                    text: existingComment.text,
                    status: existingComment.status,
                    editedDate: existingComment.lastEditedDate || existingComment.createdDate,
                    editedBy: existingComment.lastEditedBy || existingComment.author,
                    publishedDate: existingComment.publishedDate,
                    publishedBy: existingComment.publishedBy,
                  }
                ],
              };
              return scheduled([
                cloneCommentForEditSuccess({clonedComment, originalCommentId: existingComment.id}),
                showSuccess({message: '@Comment edited successfully'})
              ], asyncScheduler);
            }
            return scheduled([showError({error: 'Comment not found or not published'})], asyncScheduler);
          })
        );
      })
    )
  });

  restoreCommentVersion$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(restoreCommentVersion),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId, versionNumber}, profile]) => {
        // TODO: Replace with actual API call when backend is ready

        const publisherName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment && existingComment.history) {
              const versionToRestore = existingComment.history.find(h => h.version === versionNumber);
              if (versionToRestore) {
                // First, add current version to history if not already there
                let updatedHistory = [...existingComment.history];
                const currentVersionInHistory = updatedHistory.some(h => h.version === existingComment.version);

                if (!currentVersionInHistory) {
                  // Add current version to history before restoring
                  updatedHistory.push({
                    version: existingComment.version,
                    text: existingComment.text,
                    status: existingComment.status,
                    editedDate: existingComment.lastEditedDate || existingComment.createdDate,
                    editedBy: existingComment.lastEditedBy || existingComment.author,
                    publishedDate: existingComment.publishedDate,
                    publishedBy: existingComment.publishedBy,
                  });
                  // Sort by version number
                  updatedHistory = updatedHistory.sort((a, b) => a.version - b.version);
                }

                // Restore the selected version
                const restoredComment: CommentEntry = {
                  ...existingComment,
                  text: versionToRestore.text,
                  status: CommentStatus.PUBLISHED,
                  version: versionToRestore.version,
                  publishedDate: versionToRestore.publishedDate || Date.now(),
                  publishedBy: versionToRestore.publishedBy || publisherName,
                  lastEditedDate: versionToRestore.editedDate,
                  lastEditedBy: versionToRestore.editedBy,
                  // Keep entire history including the just-saved current version
                  history: updatedHistory,
                };
                return scheduled([
                  restoreCommentVersionSuccess({comment: restoredComment}),
                  showSuccess({message: '@Comment version restored successfully'})
                ], asyncScheduler);
              }
            }
            return scheduled([showError({error: 'Version not found'})], asyncScheduler);
          })
        );
      })
    )
  });
}
