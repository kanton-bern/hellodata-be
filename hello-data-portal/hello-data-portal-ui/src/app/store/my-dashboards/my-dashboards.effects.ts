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
import {asyncScheduler, catchError, scheduled, switchMap, withLatestFrom} from "rxjs";
import {MyDashboardsService} from "./my-dashboards.service";
import {navigate, navigateToList, showError, showSuccess, showWarning, trackEvent} from "../app/app.action";
import {
  addComment,
  addCommentSuccess,
  cloneCommentForEdit,
  cloneCommentForEditSuccess,
  declineComment,
  declineCommentSuccess,
  deleteComment,
  deleteCommentSuccess,
  loadAvailableDataDomains,
  loadAvailableDataDomainsSuccess,
  loadAvailableTags,
  loadAvailableTagsSuccess,
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
import {NotificationService} from "../../shared/services/notification.service";
import {TranslateService} from "../../shared/services/translate.service";
import {ScreenService} from "../../shared/services";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectCurrentUserPermissions} from "../auth/auth.selector";
import {selectCurrentDashboardUrl} from "./my-dashboards.selector";

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
        return this._myDashboardsService.getDashboardComments(contextKey, dashboardId).pipe(
          switchMap(comments => scheduled([loadDashboardCommentsSuccess({comments})], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  addComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(addComment),
      switchMap(({dashboardId, contextKey, dashboardUrl, text, pointerUrl, tags}) => {
        return this._myDashboardsService.createComment(contextKey, dashboardId, {
          dashboardUrl,
          pointerUrl,
          text,
          tags
        }).pipe(
          switchMap(comment => scheduled([
            addCommentSuccess({comment}),
            showSuccess({message: '@Comment added successfully'}),
            // Reload comments to ensure proper visibility filtering
            loadDashboardComments({dashboardId, contextKey, dashboardUrl})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  updateComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateComment),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId, text, pointerUrl, entityVersion, tags}, dashboardUrl]) => {
        return this._myDashboardsService.updateComment(contextKey, dashboardId, commentId, {
          text,
          pointerUrl,
          entityVersion,
          tags
        }).pipe(
          switchMap(comment => {
            const actions: any[] = [
              updateCommentSuccess({comment}),
              showSuccess({message: '@Comment updated successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => {
            const actions: any[] = [updateCommentError({error: e})];

            // Handle optimistic locking conflict (409)
            if (e.status === 409) {
              actions.push(showWarning({message: '@Comment was modified by another user. Refreshing...'}));
              if (dashboardUrl) {
                actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
              }
            } else {
              actions.push(showError({error: e}));
            }

            return scheduled(actions, asyncScheduler);
          })
        );
      })
    )
  });

  deleteComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteComment),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId, deleteEntire}, dashboardUrl]) => {
        return this._myDashboardsService.deleteComment(contextKey, dashboardId, commentId, deleteEntire).pipe(
          switchMap(restoredComment => {
            const actions: any[] = [
              deleteCommentSuccess({commentId, restoredComment})
            ];

            // Check if comment was soft deleted or restored to previous version
            if (restoredComment.deleted) {
              actions.push(showSuccess({message: '@Comment deleted successfully'}));
            } else {
              actions.push(showSuccess({message: '@Comment version restored'}));
            }

            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }

            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  publishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(publishComment),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId}, dashboardUrl]) => {
        return this._myDashboardsService.publishComment(contextKey, dashboardId, commentId).pipe(
          switchMap(comment => {
            const actions: any[] = [
              publishCommentSuccess({comment}),
              showSuccess({message: '@Comment published successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  unpublishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(unpublishComment),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId}, dashboardUrl]) => {
        return this._myDashboardsService.unpublishComment(contextKey, dashboardId, commentId).pipe(
          switchMap(comment => {
            const actions: any[] = [
              unpublishCommentSuccess({comment}),
              showSuccess({message: '@Comment unpublished successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  declineComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(declineComment),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId, declineReason}, dashboardUrl]) => {
        return this._myDashboardsService.declineComment(contextKey, dashboardId, commentId, declineReason).pipe(
          switchMap(comment => {
            const actions: any[] = [
              declineCommentSuccess({comment}),
              showSuccess({message: '@Comment declined successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  cloneCommentForEdit$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(cloneCommentForEdit),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId, newText, newPointerUrl, entityVersion, tags}, dashboardUrl]) => {
        return this._myDashboardsService.cloneCommentForEdit(contextKey, dashboardId, commentId, {
          text: newText,
          pointerUrl: newPointerUrl,
          entityVersion,
          tags
        }).pipe(
          switchMap(clonedComment => {
            const actions: any[] = [
              cloneCommentForEditSuccess({clonedComment, originalCommentId: commentId}),
              showSuccess({message: '@Comment edited successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => {
            const actions: any[] = [];

            // Handle optimistic locking conflict (409)
            if (e.status === 409) {
              actions.push(showWarning({message: '@Comment was modified by another user. Refreshing...'}));
              if (dashboardUrl) {
                actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
              }
            } else {
              actions.push(showError({error: e}));
            }

            return scheduled(actions, asyncScheduler);
          })
        );
      })
    )
  });

  restoreCommentVersion$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(restoreCommentVersion),
      withLatestFrom(
        this._store.select(selectCurrentDashboardUrl)
      ),
      switchMap(([{dashboardId, contextKey, commentId, versionNumber}, dashboardUrl]) => {
        return this._myDashboardsService.restoreVersion(contextKey, dashboardId, commentId, versionNumber).pipe(
          switchMap(comment => {
            const actions: any[] = [
              restoreCommentVersionSuccess({comment}),
              showSuccess({message: '@Comment version restored successfully'})
            ];
            if (dashboardUrl) {
              actions.push(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));
            }
            return scheduled(actions, asyncScheduler);
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  loadAvailableTags$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableTags),
      switchMap(({dashboardId, contextKey}) => {
        return this._myDashboardsService.getAvailableTags(contextKey, dashboardId).pipe(
          switchMap(tags => scheduled([loadAvailableTagsSuccess({tags})], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  // Reload tags after adding/updating comment
  reloadTagsAfterCommentChange$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(addCommentSuccess, updateCommentSuccess, cloneCommentForEditSuccess),
      withLatestFrom(
        this._store.select(state => state.myDashboards.currentDashboardId),
        this._store.select(state => state.myDashboards.currentDashboardContextKey)
      ),
      switchMap(([action, dashboardId, contextKey]) => {
        if (dashboardId && contextKey) {
          return scheduled([loadAvailableTags({dashboardId, contextKey})], asyncScheduler);
        }
        return scheduled([], asyncScheduler);
      })
    )
  });
}
