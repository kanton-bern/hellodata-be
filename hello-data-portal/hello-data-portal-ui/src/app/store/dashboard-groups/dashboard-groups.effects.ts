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
import {asyncScheduler, catchError, map, scheduled, switchMap, tap, withLatestFrom} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {DashboardGroupsService} from "./dashboard-groups.service";
import {
  deleteDashboardGroup,
  deleteDashboardGroupSuccess,
  hideDeleteDashboardGroupPopup,
  loadDashboardGroupById,
  loadDashboardGroupByIdSuccess,
  loadDashboardGroups,
  loadDashboardGroupsSuccess,
  loadEligibleUsers,
  loadEligibleUsersSuccess,
  openDashboardGroupEdition,
  saveChangesToDashboardGroup,
  saveChangesToDashboardGroupSuccess,
} from "./dashboard-groups.action";
import {
  selectActiveContextKey,
  selectDashboardGroupForDeletion,
  selectParamDashboardGroupId
} from "./dashboard-groups.selector";
import {DashboardGroup} from "./dashboard-groups.model";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError} from "../app/app.action";
import {naviElements} from "../../app-navi-elements";

@Injectable()
export class DashboardGroupsEffects {
  private readonly _actions$ = inject(Actions);
  private readonly _store = inject<Store<AppState>>(Store);
  private readonly _dashboardGroupsService = inject(DashboardGroupsService);
  private readonly _notificationService = inject(NotificationService);

  loadDashboardGroups$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadDashboardGroups),
      switchMap(action => this._dashboardGroupsService.getDashboardGroups(action.contextKey, action.page, action.size, action.sort, action.search)),
      switchMap(result => scheduled([loadDashboardGroupsSuccess({
        payload: result.content,
        totalElements: result.totalElements
      })], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });

  openDashboardGroupEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(openDashboardGroupEdition),
      withLatestFrom(this._store.select(selectActiveContextKey)),
      switchMap(([action, contextKey]) => {
        const ctxKey = action.dashboardGroup?.contextKey || contextKey;
        if (action.dashboardGroup?.id) {
          return scheduled([navigate({url: `${naviElements.dashboardGroups.path}/${ctxKey}/edit/${action.dashboardGroup.id}`})], asyncScheduler);
        }
        return scheduled([navigate({url: `${naviElements.dashboardGroups.path}/${ctxKey}/create`})], asyncScheduler);
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });

  loadDashboardGroupById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadDashboardGroupById),
      withLatestFrom(this._store.select(selectParamDashboardGroupId)),
      switchMap(([action, groupId]) => this._dashboardGroupsService.getDashboardGroupById(groupId as string)),
      switchMap(result => scheduled([loadDashboardGroupByIdSuccess({dashboardGroup: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });

  saveChangesToDashboardGroup$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToDashboardGroup),
      switchMap(action => {
        return action.dashboardGroup.id
          ? this._dashboardGroupsService.updateDashboardGroup(action.dashboardGroup).pipe(
            tap(() => this._notificationService.success('@Dashboard group updated successfully')),
            map(() => saveChangesToDashboardGroupSuccess())
          )
          : this._dashboardGroupsService.createDashboardGroup(action.dashboardGroup).pipe(
            tap(() => this._notificationService.success('@Dashboard group created successfully')),
            map(() => saveChangesToDashboardGroupSuccess())
          );
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });

  saveChangesToDashboardGroupSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToDashboardGroupSuccess),
      withLatestFrom(this._store.select(selectActiveContextKey)),
      switchMap(([, contextKey]) => scheduled([clearUnsavedChanges(), navigate({url: `${naviElements.dashboardGroups.path}/list/${contextKey}`})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });

  deleteDashboardGroup$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteDashboardGroup),
      withLatestFrom(this._store.select(selectDashboardGroupForDeletion)),
      switchMap(([action, dashboardGroup]) => this._dashboardGroupsService.deleteDashboardGroupById((dashboardGroup as DashboardGroup).id as string).pipe(
        map(() => deleteDashboardGroupSuccess()),
        catchError(e => scheduled([showError({error: e})], asyncScheduler))
      )),
    );
  });

  deleteDashboardGroupSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteDashboardGroupSuccess),
      withLatestFrom(this._store.select(selectActiveContextKey)),
      tap(() => this._notificationService.success('@Dashboard group deleted successfully')),
      switchMap(([, contextKey]) => scheduled([hideDeleteDashboardGroupPopup(), loadDashboardGroups({
        contextKey: contextKey || '',
        page: 0,
        size: 10
      })], asyncScheduler))
    );
  });

  loadEligibleUsers$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadEligibleUsers),
      switchMap(action => this._dashboardGroupsService.getEligibleUsersForDomain(action.contextKey)),
      switchMap(result => scheduled([loadEligibleUsersSuccess({users: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    );
  });
}
