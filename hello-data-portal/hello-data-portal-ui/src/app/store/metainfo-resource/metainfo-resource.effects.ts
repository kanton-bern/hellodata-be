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
import {Actions, createEffect, ofType} from '@ngrx/effects';
import {catchError, of, switchMap, withLatestFrom} from 'rxjs';
import {MetaInfoResourceService} from "./metainfo-resource.service";
import {
  loadAppInfoResources,
  loadAppInfoResourcesSuccess,
  loadPermissionResources,
  loadPermissionResourcesSuccess,
  loadRoleResources,
  loadRoleResourcesSuccess,
  loadSelectedAppInfoResources,
  loadSelectedAppInfoResourcesSuccess
} from "./metainfo-resource.action";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectSelectedAppInfoResourcesParams} from "./metainfo-resource.selector";
import {showError} from "../app/app.action";

@Injectable()
export class MetaInfoResourceEffects {

  loadAppInfoResources$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAppInfoResources),
      switchMap(() => this._metaInfoResourceService.getAppInfoResources()),
      switchMap(result => of(loadAppInfoResourcesSuccess({result: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadRoleResources$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadRoleResources),
      switchMap(() => this._metaInfoResourceService.getRoleResources()),
      switchMap(result => of(loadRoleResourcesSuccess({result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadPermissionResources$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadPermissionResources),
      switchMap(() => this._metaInfoResourceService.getPermissionResources()),
      switchMap(result => of(loadPermissionResourcesSuccess({result: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadSelectedAppInfoResources$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSelectedAppInfoResources),
      withLatestFrom(this._store.select(selectSelectedAppInfoResourcesParams)),
      switchMap(([action, params]) => this._metaInfoResourceService.getResourcesFilteredByAppInfo(params.apiVersion as string, params.instanceName as string, params.moduleType as string)),
      switchMap(result => of(loadSelectedAppInfoResourcesSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _metaInfoResourceService: MetaInfoResourceService,
  ) {
  }
}
