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

import {Injectable} from '@angular/core';
import {Actions, createEffect, ofType, concatLatestFrom} from '@ngrx/effects';
import {catchError, of, switchMap, withLatestFrom} from 'rxjs';
import {MetaInfoResourceService} from "./metainfo-resource.service";
import {
  LoadAppInfoResources,
  LoadAppInfoResourcesSuccess,
  LoadPermissionResources,
  LoadPermissionResourcesSuccess,
  LoadRoleResources,
  LoadRoleResourcesSuccess,
  LoadSelectedAppInfoResources,
  LoadSelectedAppInfoResourcesSuccess,
  MetaInfoResourceActionType
} from "./metainfo-resource.action";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectSelectedAppInfoResourcesParams} from "./metainfo-resource.selector";
import {showError} from "../app/app.action";

@Injectable()
export class MetaInfoResourceEffects {

  loadAppInfos$ = createEffect(() => { return this._actions$.pipe(
    ofType<LoadAppInfoResources>(MetaInfoResourceActionType.LOAD_APP_INFOS),
    switchMap(() => this._metaInfoResourceService.getAppInfoResources()),
    switchMap(result => of(new LoadAppInfoResourcesSuccess(result))),
    catchError(e => of(showError(e)))
  ) });

  loadRoles$ = createEffect(() => { return this._actions$.pipe(
    ofType<LoadRoleResources>(MetaInfoResourceActionType.LOAD_ROLES_SUCCESS),
    switchMap(() => this._metaInfoResourceService.getRoleResources()),
    switchMap(result => of(new LoadRoleResourcesSuccess(result))),
    catchError(e => of(showError(e)))
  ) });

  loadPermissions$ = createEffect(() => { return this._actions$.pipe(
    ofType<LoadPermissionResources>(MetaInfoResourceActionType.LOAD_PERMISSIONS),
    switchMap(() => this._metaInfoResourceService.getPermissionResources()),
    switchMap(result => of(new LoadPermissionResourcesSuccess(result))),
    catchError(e => of(showError(e)))
  ) });

  loadSelectedAppInfoResources$ = createEffect(() => { return this._actions$.pipe(
    ofType<LoadSelectedAppInfoResources>(MetaInfoResourceActionType.LOAD_SELECTED_APP_INFO_RESOURCES),
    concatLatestFrom(() => this._store.select(selectSelectedAppInfoResourcesParams)),
    switchMap(([action, params]) => this._metaInfoResourceService.getResourcesFilteredByAppInfo(params.apiVersion as string, params.instanceName as string, params.moduleType as string)),
    switchMap(result => of(new LoadSelectedAppInfoResourcesSuccess(result))),
    catchError(e => of(showError(e)))
  ) });

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _metaInfoResourceService: MetaInfoResourceService,
  ) {
  }
}
