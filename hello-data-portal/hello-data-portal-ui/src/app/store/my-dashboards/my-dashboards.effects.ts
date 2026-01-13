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
import {NotificationService} from "../../shared/services/notification.service";
import {TranslateService} from "../../shared/services/translate.service";
import {ScreenService} from "../../shared/services";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectCurrentUserPermissions} from "../auth/auth.selector";

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
      switchMap(({dashboardId, contextKey, dashboardUrl, text, pointerUrl}) => {
        return this._myDashboardsService.createComment(contextKey, dashboardId, {
          dashboardUrl,
          pointerUrl,
          text
        }).pipe(
          switchMap(comment => scheduled([
            addCommentSuccess({comment}),
            showSuccess({message: '@Comment added successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  updateComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateComment),
      switchMap(({dashboardId, contextKey, commentId, text, pointerUrl}) => {
        return this._myDashboardsService.updateComment(contextKey, dashboardId, commentId, {
          text,
          pointerUrl
        }).pipe(
          switchMap(comment => scheduled([
            updateCommentSuccess({comment}),
            showSuccess({message: '@Comment updated successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([
            updateCommentError({error: e}),
            showError({error: e})
          ], asyncScheduler))
        );
      })
    )
  });

  deleteComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteComment),
      switchMap(({dashboardId, contextKey, commentId}) => {
        return this._myDashboardsService.deleteComment(contextKey, dashboardId, commentId).pipe(
          switchMap(restoredComment => {
            // Check if comment was soft deleted or restored to previous version
            if (restoredComment.deleted) {
              return scheduled([
                deleteCommentSuccess({commentId, restoredComment}),
                showSuccess({message: '@Comment deleted successfully'})
              ], asyncScheduler);
            } else {
              return scheduled([
                deleteCommentSuccess({commentId, restoredComment}),
                showSuccess({message: '@Comment version restored'})
              ], asyncScheduler);
            }
          }),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  publishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(publishComment),
      switchMap(({dashboardId, contextKey, commentId}) => {
        return this._myDashboardsService.publishComment(contextKey, dashboardId, commentId).pipe(
          switchMap(comment => scheduled([
            publishCommentSuccess({comment}),
            showSuccess({message: '@Comment published successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  unpublishComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(unpublishComment),
      switchMap(({dashboardId, contextKey, commentId}) => {
        return this._myDashboardsService.unpublishComment(contextKey, dashboardId, commentId).pipe(
          switchMap(comment => scheduled([
            unpublishCommentSuccess({comment}),
            showSuccess({message: '@Comment unpublished successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  cloneCommentForEdit$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(cloneCommentForEdit),
      switchMap(({dashboardId, contextKey, commentId, newText, newPointerUrl}) => {
        return this._myDashboardsService.cloneCommentForEdit(contextKey, dashboardId, commentId, {
          text: newText,
          pointerUrl: newPointerUrl
        }).pipe(
          switchMap(clonedComment => scheduled([
            cloneCommentForEditSuccess({clonedComment, originalCommentId: commentId}),
            showSuccess({message: '@Comment edited successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });

  restoreCommentVersion$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(restoreCommentVersion),
      switchMap(({dashboardId, contextKey, commentId, versionNumber}) => {
        return this._myDashboardsService.restoreVersion(contextKey, dashboardId, commentId, versionNumber).pipe(
          switchMap(comment => scheduled([
            restoreCommentVersionSuccess({comment}),
            showSuccess({message: '@Comment version restored successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        );
      })
    )
  });
}
