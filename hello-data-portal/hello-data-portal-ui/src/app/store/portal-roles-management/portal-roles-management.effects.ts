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
import {catchError, map, of, switchMap, tap, withLatestFrom} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {
  DeleteEditedPortalRole,
  DeleteEditedPortalRoleSuccess,
  DeletePortalRole,
  DeletePortalRoleSuccess,
  HideDeletePortalRolePopup,
  LoadPortalRoleById,
  LoadPortalRoleByIdSuccess,
  LoadPortalRoles,
  LoadPortalRolesSuccess,
  OpenPortalRoleEdition,
  RolesManagementActionType,
  SaveChangesToPortalRole,
  SaveChangesToPortalRoleSuccess
} from "./portal-roles-management.action";
import {PortalRolesManagementService} from "./portal-roles-management.service";
import {NotificationService} from "../../shared/services/notification.service";
import {selectPortalParamRoleId, selectSelectedPortalRoleForDeletion} from "./portal-roles-management.selector";
import {PortalRole} from "./portal-roles-management.model";
import {ClearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError} from "../app/app.action";

@Injectable()
export class PortalRolesManagementEffects {

  loadRoles$ = createEffect(() => this._actions$.pipe(
    ofType<LoadPortalRoles>(RolesManagementActionType.LOAD_PORTAL_ROLES),
    switchMap(action => this._portalRoleService.getPortalRoles()),
    switchMap(result => of(new LoadPortalRolesSuccess(result))),
    catchError(e => of(showError(e)))
  ));

  openRoleEdition$ = createEffect(() => this._actions$.pipe(
    ofType<OpenPortalRoleEdition>(RolesManagementActionType.OPEN_PORTAL_ROLE_EDITION),
    switchMap(action => {
      if (action.role.id) {
        return of(navigate({url: `roles-management/edit/${action.role.id}`}));
      }
      return of(navigate({url: 'roles-management/create'}));
    }),
    catchError(e => of(showError(e)))
  ));

  saveChangesToRole$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToPortalRole>(RolesManagementActionType.SAVE_CHANGES_TO_PORTAL_ROLE),
    switchMap((action: SaveChangesToPortalRole) => {
      return action.role.id
        ? this._portalRoleService.updatePortalRole({
          id: action.role.id,
          name: action.role.name as string,
          description: action.role.description as string,
          permissions: action.role.permissions as string[]
        }).pipe(
          tap(() => this._notificationService.success('@Portal role updated successfully', {role: action.role.name})),
          map(() => new SaveChangesToPortalRoleSuccess(action.role))
        )
        : this._portalRoleService.createPortalRole({
          name: action.role.name as string,
          description: action.role.description as string,
          permissions: action.role.permissions as string[]
        }).pipe(
          tap(() => this._notificationService.success('@Portal role added successfully', {role: action.role.name})),
          map(() => new SaveChangesToPortalRoleSuccess(action.role))
        )
    }),
    catchError(e => of(showError(e)))
  ));

  saveChangesToRoleSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToPortalRoleSuccess>(RolesManagementActionType.SAVE_CHANGES_TO_PORTAL_ROLE_SUCCESS),
    switchMap(action => of(navigate({url: 'roles-management'}), new ClearUnsavedChanges())),
    catchError(e => of(showError(e)))
  ));

  deleteRole$ = createEffect(() => this._actions$.pipe(
    ofType<DeletePortalRole>(RolesManagementActionType.DELETE_PORTAL_ROLE),
    withLatestFrom(this._store.select(selectSelectedPortalRoleForDeletion)),
    switchMap(([action, role]) => this._portalRoleService.deletePortalRoleById((role as PortalRole).id as string).pipe(
      map(() => new DeletePortalRoleSuccess(role as PortalRole)),
      catchError(e => of(showError(e)))
    )),
  ));

  deleteRoleSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeletePortalRoleSuccess>(RolesManagementActionType.DELETE_PORTAL_ROLE_SUCCESS),
    tap(action => this._notificationService.success('@Portal role deleted successfully', {role: action.role.name})),
    switchMap(() => of(new LoadPortalRoles(), new HideDeletePortalRolePopup()))
  ));

  deleteEditedRole$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedPortalRole>(RolesManagementActionType.DELETE_EDITED_PORTAL_ROLE),
    withLatestFrom(this._store.select(selectSelectedPortalRoleForDeletion)),
    switchMap(([action, roleToBeDeleted]) => {
        return this._portalRoleService.deletePortalRoleById((roleToBeDeleted as PortalRole).id as string).pipe(
          map(() => new DeleteEditedPortalRoleSuccess(roleToBeDeleted!.name as string)),
          catchError(e => of(showError(e)))
        )
      }
    ),
  ));

  deleteEditedRoleSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedPortalRoleSuccess>(RolesManagementActionType.DELETE_EDITED_PORTAL_ROLE_SUCCESS),
    tap(action => this._notificationService.success('@Portal role deleted successfully', {role: action.name})),
    switchMap(() => of(navigate({url: 'roles-management'}), new HideDeletePortalRolePopup()))
  ));

  loadRoleById$ = createEffect(() => this._actions$.pipe(
    ofType<LoadPortalRoleById>(RolesManagementActionType.LOAD_PORTAL_ROLE_BY_ID),
    withLatestFrom(this._store.select(selectPortalParamRoleId)),
    switchMap(([action, roleId]) => this._portalRoleService.getPortalRoleById(roleId as string)),
    switchMap(result => of(new LoadPortalRoleByIdSuccess(result))),
    catchError(e => of(showError(e)))
  ));

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _portalRoleService: PortalRolesManagementService,
    private _notificationService: NotificationService
  ) {
  }
}
