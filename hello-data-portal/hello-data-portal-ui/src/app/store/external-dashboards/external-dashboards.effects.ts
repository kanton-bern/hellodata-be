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

import {Injectable} from "@angular/core";
import {Actions, concatLatestFrom, createEffect, ofType} from "@ngrx/effects";
import {catchError, map, of, switchMap} from "rxjs";
import {
  createExternalDashboard,
  createExternalDashboardSuccess,
  deleteExternalDashboard,
  deleteExternalDashboardSuccess,
  loadExternalDashboardById,
  loadExternalDashboardByIdSuccess,
  loadExternalDashboards,
  loadExternalDashboardsSuccess,
  openExternalDashboardEdition,
  updateExternalDashboard,
  updateExternalDashboardSuccess,
} from "./external-dasboards.action";
import {ExternalDashboardsService} from "./external-dashboards.service";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectParamExternalDashboardId} from "./external-dashboards.selector";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError, showSuccess} from "../app/app.action";

@Injectable()
export class ExternalDashboardsEffects {


  loadExternalDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadExternalDashboards),
      switchMap(() => this._externalDashboardsService.getExternalDashboards()),
      switchMap(result => of(loadExternalDashboardsSuccess({payload: result}))),
      catchError(e => of(showError(e)))
    )
  });

  createExternalDashboard$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createExternalDashboard),
      switchMap(action => this._externalDashboardsService.createExternalDashboard(action.dashboard).pipe(map(() => createExternalDashboardSuccess({dashboard: action.dashboard})))),
      catchError(e => of(showError(e)))
    )
  });

  createExternalDashboardSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createExternalDashboardSuccess),
      switchMap(action => of(clearUnsavedChanges(), loadExternalDashboards(), showSuccess({
        message: '@External dashboard created', interpolateParams: {
          title: action.dashboard.title
        }
      }), navigate({url: 'external-dashboards'}))),
      catchError(e => of(showError(e)))
    )
  });

  updateExternalDashboard$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateExternalDashboard),
      switchMap(action => this._externalDashboardsService.updateExternalDashboard(action.dashboard).pipe(map(() => updateExternalDashboardSuccess({dashboard: action.dashboard})))),
      catchError(e => of(showError(e)))
    )
  });

  updateExternalDashboardSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateExternalDashboardSuccess),
      switchMap(action => of(clearUnsavedChanges(), loadExternalDashboards(), showSuccess({
        message: '@External dashboard updated',
        interpolateParams: {title: action.dashboard.title}
      }), navigate({url: 'external-dashboards'}))),
      catchError(e => of(showError(e)))
    )
  });

  deleteExternalDashboard$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteExternalDashboard),
      switchMap(action => this._externalDashboardsService.deleteExternalDashboard(action.dashboard).pipe(map(() => deleteExternalDashboardSuccess({dashboard: action.dashboard})))),
      catchError(e => of(showError(e)))
    )
  });

  deleteExternalDashboardSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteExternalDashboardSuccess),
      switchMap(action => of(loadExternalDashboards(), showSuccess({
        message: '@External dashboard deleted',
        interpolateParams: {title: action.dashboard.title}
      }), navigate({url: 'external-dashboards'}))),
      catchError(e => of(showError(e)))
    )
  });

  openExternalDashboardEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(openExternalDashboardEdition),
      switchMap(action => {
        if (action.dashboard) {
          return of(navigate({
            url: `external-dashboards/edit/${action.dashboard.id}`
          }));
        }
        return of(navigate({url: 'external-dashboards/create'}));
      }),
      catchError(e => of(showError(e)))
    )
  });


  loadExternalDashboardById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadExternalDashboardById),
      concatLatestFrom(() => this._store.select(selectParamExternalDashboardId)),
      switchMap(([action, externalDashboardId]) => this._externalDashboardsService.getExternalDashboardById(externalDashboardId as string)),
      switchMap(result => of(loadExternalDashboardByIdSuccess({dashboard: result}))),
      catchError(e => of(showError(e)))
    )
  });

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _externalDashboardsService: ExternalDashboardsService,
  ) {
  }
}
