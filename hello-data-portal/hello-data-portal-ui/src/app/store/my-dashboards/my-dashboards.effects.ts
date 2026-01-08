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
  setSelectedDataDomain,
  unpublishComment,
  unpublishCommentSuccess,
  updateComment,
  updateCommentError,
  updateCommentSuccess,
  uploadDashboardsError,
  uploadDashboardsSuccess
} from "./my-dashboards.action";
import {CommentStatus} from "./my-dashboards.model";
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
        // Backend generates: id, author (createdBy), authorEmail, status (DRAFT), deleted
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
      switchMap(({dashboardId, contextKey, commentId, text}) =>
        this._myDashboardsService.updateComment(contextKey, dashboardId, commentId, text).pipe(
          switchMap(comment => scheduled([
            updateCommentSuccess({comment}),
            showSuccess({message: '@Comment updated successfully'})
          ], asyncScheduler)),
          catchError(e => scheduled([updateCommentError({error: e}), showError({error: e})], asyncScheduler))
        )
      )
    )
  });

  deleteComment$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteComment),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([{dashboardId, contextKey, commentId}, profile]) => {
        // TODO: Replace with actual API call when backend is ready
        // return this._myDashboardsService.deleteComment(contextKey, dashboardId, commentId).pipe(
        //   switchMap((response) => scheduled([
        //     deleteCommentSuccess({commentId, deletedDate: response.deletedDate, deletedBy: response.deletedBy}),
        //     showSuccess({message: '@Comment deleted successfully'})
        //   ], asyncScheduler)),
        //   catchError(e => scheduled([deleteCommentError({error: e}), showError({error: e})], asyncScheduler))
        // )

        // Temporary mock - simulating backend response (soft delete)
        const deleterName = profile ? `${profile.given_name} ${profile.family_name}` : 'Unknown User';
        return scheduled([
          deleteCommentSuccess({
            commentId,
            deletedDate: Date.now(),
            deletedBy: deleterName
          }),
          showSuccess({message: '@Comment deleted successfully'})
        ], asyncScheduler);
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
              const updatedComment = {
                ...existingComment,
                status: CommentStatus.DRAFT,
                publishedDate: undefined as number | undefined,
                publishedBy: undefined as string | undefined,
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
}
