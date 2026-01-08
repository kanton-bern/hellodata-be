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

        // Temporary mock data for testing - new model with history
        const mockComments: CommentEntry[] = [
          {
            id: '1',
            dashboardId: dashboardId,
            dashboardUrl: dashboardUrl,
            contextKey: contextKey,
            pointerUrl: 'https://superset-demo.dev.hellodatabedag.ch/superset/dashboard/5/?tab=1',
            author: 'John Doe',
            authorEmail: 'john.doe@example.com',
            createdDate: new Date('2024-06-01T09:30:00').getTime(),
            deleted: false,
            activeVersion: 1,
            history: [
              {
                version: 1,
                text: 'First test comment.',
                status: CommentStatus.PUBLISHED,
                editedDate: new Date('2024-06-01T09:30:00').getTime(),
                editedBy: 'John Doe',
                publishedDate: new Date('2024-06-01T09:30:00').getTime(),
                publishedBy: 'Admin',
                deleted: false,
              }
            ],
          },
          {
            id: '2',
            dashboardId: dashboardId,
            dashboardUrl: dashboardUrl,
            contextKey: contextKey,
            author: 'Anne Smith',
            authorEmail: 'anne.smith@example.com',
            createdDate: new Date('2024-06-02T14:15:00').getTime(),
            deleted: false,
            activeVersion: 1,
            history: [
              {
                version: 1,
                text: 'Great data, thanks for sharing!',
                status: CommentStatus.PUBLISHED,
                editedDate: new Date('2024-06-02T14:15:00').getTime(),
                editedBy: 'Anne Smith',
                publishedDate: new Date('2024-06-02T14:15:00').getTime(),
                publishedBy: 'Admin',
                deleted: false,
              }
            ],
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
      switchMap(([{dashboardId, contextKey, dashboardUrl, text, pointerUrl}, profile]) => {
        // TODO: Replace with actual API call when backend is ready

        // Temporary mock - simulating backend response
        const authorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';
        const authorEmail = profile?.email || 'unknown@example.com';
        const now = Date.now();

        // New comment starts with version 1 as DRAFT in history
        const mockComment: CommentEntry = {
          id: crypto.randomUUID(),
          dashboardId: dashboardId,
          dashboardUrl: dashboardUrl,
          contextKey: contextKey,
          pointerUrl: pointerUrl,
          author: authorName,
          authorEmail: authorEmail,
          createdDate: now,
          deleted: false,
          activeVersion: 1,
          history: [
            {
              version: 1,
              text: text,
              status: CommentStatus.DRAFT,
              editedDate: now,
              editedBy: authorName,
              deleted: false,
            }
          ],
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
      switchMap(([{dashboardId, contextKey, commentId, text, pointerUrl}, profile]) => {
        // TODO: Replace with actual API call when backend is ready

        // For DRAFT comments - update text in the active version
        const editorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              // Update the active version in history
              const updatedHistory = existingComment.history.map(v =>
                v.version === existingComment.activeVersion
                  ? {...v, text, editedDate: Date.now(), editedBy: editorName}
                  : v
              );

              const updatedComment: CommentEntry = {
                ...existingComment,
                pointerUrl: pointerUrl,
                history: updatedHistory,
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

        const deleterName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (!existingComment) {
              return scheduled([showError({error: 'Comment not found'})], asyncScheduler);
            }

            // Mark current active version as deleted in history
            const updatedHistory = existingComment.history.map(v =>
              v.version === existingComment.activeVersion ? {...v, deleted: true} : v
            );

            // Find last non-deleted PUBLISHED version to restore
            const lastPublishedVersion = [...updatedHistory]
              .reverse()
              .find(v => v.status === CommentStatus.PUBLISHED && !v.deleted);

            if (lastPublishedVersion) {
              // Restore to last published version
              const updatedComment: CommentEntry = {
                ...existingComment,
                activeVersion: lastPublishedVersion.version,
                history: updatedHistory,
              };
              return scheduled([
                deleteCommentSuccess({commentId, restoredComment: updatedComment}),
                showSuccess({message: '@Comment version restored'})
              ], asyncScheduler);
            } else {
              // Deleting a draft with no published versions - soft delete entire comment
              // Or no versions left - soft delete entire comment
              const updatedComment: CommentEntry = {
                ...existingComment,
                deleted: true,
                deletedDate: Date.now(),
                deletedBy: deleterName,
                history: updatedHistory,
              };
              return scheduled([
                deleteCommentSuccess({commentId, restoredComment: updatedComment}),
                showSuccess({message: '@Comment deleted successfully'})
              ], asyncScheduler);
            }
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

        const publisherName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';
        const now = Date.now();

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              // Update active version to PUBLISHED
              const updatedHistory = existingComment.history.map(v =>
                v.version === existingComment.activeVersion
                  ? {...v, status: CommentStatus.PUBLISHED, publishedDate: now, publishedBy: publisherName}
                  : v
              );

              const updatedComment: CommentEntry = {
                ...existingComment,
                history: updatedHistory,
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

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (existingComment) {
              // Change active version status to DRAFT
              const updatedHistory = existingComment.history.map(v =>
                v.version === existingComment.activeVersion
                  ? {...v, status: CommentStatus.DRAFT, publishedDate: undefined, publishedBy: undefined}
                  : v
              );

              const updatedComment: CommentEntry = {
                ...existingComment,
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
      switchMap(([{dashboardId, contextKey, commentId, newText, newPointerUrl}, profile]) => {
        // TODO: Replace with actual API call when backend is ready

        const editorName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';
        const now = Date.now();

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (!existingComment) {
              return scheduled([showError({error: 'Comment not found'})], asyncScheduler);
            }

            const activeVersion = existingComment.history.find(v => v.version === existingComment.activeVersion);
            if (!activeVersion || activeVersion.status !== CommentStatus.PUBLISHED) {
              return scheduled([showError({error: 'Comment must be published to edit'})], asyncScheduler);
            }

            // Create new version as DRAFT
            const newVersionNumber = Math.max(...existingComment.history.map(v => v.version)) + 1;
            const newVersion: CommentVersion = {
              version: newVersionNumber,
              text: newText,
              status: CommentStatus.DRAFT,
              editedDate: now,
              editedBy: editorName,
              deleted: false,
            };

            const updatedComment: CommentEntry = {
              ...existingComment,
              pointerUrl: newPointerUrl ?? existingComment.pointerUrl,
              activeVersion: newVersionNumber,
              hasActiveDraft: true,
              history: [...existingComment.history, newVersion],
            };

            return scheduled([
              cloneCommentForEditSuccess({clonedComment: updatedComment, originalCommentId: existingComment.id}),
              showSuccess({message: '@Comment edited successfully'})
            ], asyncScheduler);
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

        return this._store.select(state =>
          state.myDashboards.currentDashboardComments.find(c => c.id === commentId)
        ).pipe(
          take(1),
          switchMap(existingComment => {
            if (!existingComment) {
              return scheduled([showError({error: 'Comment not found'})], asyncScheduler);
            }

            const versionToRestore = existingComment.history.find(
              v => v.version === versionNumber && v.status === CommentStatus.PUBLISHED && !v.deleted
            );

            if (!versionToRestore) {
              return scheduled([showError({error: 'Version not found or not available'})], asyncScheduler);
            }

            // Simply change the active version - history stays the same
            const restoredComment: CommentEntry = {
              ...existingComment,
              activeVersion: versionNumber,
              hasActiveDraft: false,
            };

            return scheduled([
              restoreCommentVersionSuccess({comment: restoredComment}),
              showSuccess({message: '@Comment version restored successfully'})
            ], asyncScheduler);
          })
        );
      })
    )
  });
}
