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
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {MyDashboardsService} from "../my-dashboards/my-dashboards.service";
import {catchError, map, of, switchMap, withLatestFrom} from "rxjs";
import {Navigate, ShowError, ShowSuccess} from "../app/app.action";
import {
  CreateExternalDashboard,
  CreateExternalDashboardSuccess,
  DeleteExternalDashboard,
  DeleteExternalDashboardSuccess,
  ExternalDashboardsActionType,
  LoadExternalDashboardById,
  LoadExternalDashboardByIdSuccess,
  LoadExternalDashboards,
  LoadExternalDashboardsSuccess,
  OpenExternalDashboardEdition,
  UpdateExternalDashboard,
  UpdateExternalDashboardSuccess
} from "./external-dasboards.action";
import {ExternalDashboardsService} from "./external-dashboards.service";
import {select, Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectParamExternalDashboardId} from "./external-dashboards.selector";
import {ClearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";

@Injectable()
export class ExternalDashboardsEffects {


  loadExternalDashboards$ = createEffect(() => this._actions$.pipe(
    ofType<LoadExternalDashboards>(ExternalDashboardsActionType.LOAD_EXTERNAL_DASHBOARDS),
    switchMap(() => this._externalDashboardsService.getExternalDashboards()),
    switchMap(result => of(new LoadExternalDashboardsSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  createExternalDashboard$ = createEffect(() => this._actions$.pipe(
    ofType<CreateExternalDashboard>(ExternalDashboardsActionType.CREATE_EXTERNAL_DASHBOARD),
    switchMap(action => this._externalDashboardsService.createExternalDashboard(action.dashboard).pipe(map(() => new CreateExternalDashboardSuccess(action.dashboard)))),
    catchError(e => of(new ShowError(e)))
  ));

  createExternalDashboardSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<CreateExternalDashboardSuccess>(ExternalDashboardsActionType.CREATE_EXTERNAL_DASHBOARD_SUCCESS),
    switchMap(action => of(new ClearUnsavedChanges(), new LoadExternalDashboards(), new ShowSuccess('@External dashboard created', {title: action.dashboard.title}), new Navigate('external-dashboards'))),
    catchError(e => of(new ShowError(e)))
  ));

  updateExternalDashboard$ = createEffect(() => this._actions$.pipe(
    ofType<UpdateExternalDashboard>(ExternalDashboardsActionType.UPDATE_EXTERNAL_DASHBOARD),
    switchMap(action => this._externalDashboardsService.updateExternalDashboard(action.dashboard).pipe(map(() => new UpdateExternalDashboardSuccess(action.dashboard)))),
    catchError(e => of(new ShowError(e)))
  ));

  updateExternalDashboardSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<UpdateExternalDashboardSuccess>(ExternalDashboardsActionType.UPDATE_EXTERNAL_DASHBOARD_SUCCESS),
    switchMap(action => of(new ClearUnsavedChanges(), new LoadExternalDashboards(), new ShowSuccess('@External dashboard updated', {title: action.dashboard.title}), new Navigate('external-dashboards'))),
    catchError(e => of(new ShowError(e)))
  ));

  deleteExternalDashboard$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteExternalDashboard>(ExternalDashboardsActionType.DELETE_EXTERNAL_DASHBOARD),
    switchMap(action => this._externalDashboardsService.deleteExternalDashboard(action.dashboard).pipe(map(() => new DeleteExternalDashboardSuccess(action.dashboard)))),
    catchError(e => of(new ShowError(e)))
  ));

  deleteExternalDashboardSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteExternalDashboardSuccess>(ExternalDashboardsActionType.DELETE_EXTERNAL_DASHBOARD_SUCCESS),
    switchMap(action => of(new LoadExternalDashboards(), new ShowSuccess('@External dashboard deleted', {title: action.dashboard.title}), new Navigate('external-dashboards'))),
    catchError(e => of(new ShowError(e)))
  ));

  openExternalDashboardEdition$ = createEffect(() => this._actions$.pipe(
    ofType<OpenExternalDashboardEdition>(ExternalDashboardsActionType.OPEN_EXTERNAL_DASHBOARD_EDITION),
    switchMap(action => {
      if (action.dashboard) {
        return of(new Navigate(`external-dashboards/edit/${action.dashboard.id}`));
      }
      return of(new Navigate('external-dashboards/create'));
    }),
    catchError(e => of(new ShowError(e)))
  ));


  loadExternalDashboardById$ = createEffect(() => this._actions$.pipe(
    ofType<LoadExternalDashboardById>(ExternalDashboardsActionType.LOAD_EXTERNAL_DASHBOARD_BY_ID),
    withLatestFrom(this._store.pipe(select(selectParamExternalDashboardId))),
    switchMap(([action, externalDashboardId]) => this._externalDashboardsService.getExternalDashboardById(externalDashboardId as string)),
    switchMap(result => of(new LoadExternalDashboardByIdSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _externalDashboardsService: ExternalDashboardsService,
    private _myDashboardsService: MyDashboardsService,
  ) {
  }
}
