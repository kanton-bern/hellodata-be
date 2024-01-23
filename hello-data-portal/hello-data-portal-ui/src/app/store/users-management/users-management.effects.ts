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
import {
  CreateUser,
  CreateUserSuccess,
  HideUserPopupAction,
  InvokeActionFromUserPopup,
  LoadAdminEmailsSuccess,
  LoadAvailableContextRoles,
  LoadAvailableContextRolesSuccess,
  LoadAvailableContexts,
  LoadAvailableContextsSuccess,
  LoadDashboards,
  LoadDashboardsSuccess,
  LoadUserById,
  LoadUserByIdSuccess,
  LoadUserContextRoles,
  LoadUserContextRolesSuccess,
  LoadUsers,
  LoadUsersSuccess,
  NavigateToUserEdition,
  NavigateToUsersManagement,
  SyncUsers,
  SyncUsersSuccess,
  UpdateUserRoles,
  UpdateUserRolesSuccess,
  UserPopupActionSuccess,
  UsersManagementActionType
} from "./users-management.action";
import {UsersManagementService} from "./users-management.service";
import {NotificationService} from "../../shared/services/notification.service";
import {Store} from '@ngrx/store';
import {AppState} from "../app/app.state";
import {selectDashboardsForUser, selectParamUserId, selectSelectedRolesForUser, selectUserForPopup} from "./users-management.selector";
import {Navigate, ShowError, ShowSuccess} from "../app/app.action";
import {Router} from "@angular/router";
import {UserAction, UserActionForPopup} from "./users-management.model";
import {CURRENT_EDITED_USER_ID} from "./users-management.reducer";
import {ContextRoleService} from "./context-role.service";
import {ClearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";

@Injectable()
export class UsersManagementEffects {

  loadUsers$ = createEffect(() => this._actions$.pipe(
    ofType<LoadUsers>(UsersManagementActionType.LOAD_USERS),
    switchMap(() => this._usersManagementService.getUsers()),
    switchMap(result => of(new LoadUsersSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));


  userPopupAction$ = createEffect(() => this._actions$.pipe(
    ofType<InvokeActionFromUserPopup>(UsersManagementActionType.INVOKE_ACTION_FROM_USER_POPUP),
    withLatestFrom(this._store.select(selectUserForPopup)),
    switchMap(([action, userActionForPopup]) => {
        switch (userActionForPopup!.action) {
          case (UserAction.DISABLE):
            return this._usersManagementService.disableUser(userActionForPopup!.user).pipe(
              map(() => new UserPopupActionSuccess(userActionForPopup!.user.email, userActionForPopup as UserActionForPopup)),
              catchError(e => of(new ShowError(e)))
            );
          case (UserAction.ENABLE):
            return this._usersManagementService.enableUser(userActionForPopup!.user).pipe(
              map(() => new UserPopupActionSuccess(userActionForPopup!.user.email, userActionForPopup as UserActionForPopup)),
              catchError(e => of(new ShowError(e)))
            );
          default:
            return this._usersManagementService.deleteUser(userActionForPopup!.user).pipe(
              map(() => new UserPopupActionSuccess(userActionForPopup!.user.email, userActionForPopup as UserActionForPopup)),
              catchError(e => of(new ShowError(e)))
            );
        }
      }
    )));

  userPopupActionSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<UserPopupActionSuccess>(UsersManagementActionType.USER_POPUP_ACTION_SUCCESS),
    tap(action => {
      switch (action.userActionForPopup.action) {
        case (UserAction.ENABLE):
          return this._notificationService.success('@User enabled successfully', {email: action.email});
        case (UserAction.DISABLE):
          return this._notificationService.success('@User disabled successfully', {email: action.email});
        default:
          return this._notificationService.success('@User deleted successfully', {email: action.email});
      }
    }),
    switchMap(action => {
      if (action.userActionForPopup.actionFromUsersEdition && action.userActionForPopup.action === UserAction.DELETE) {
        return of(new HideUserPopupAction(), new Navigate('/user-management'));
      } else if (action.userActionForPopup.actionFromUsersEdition) {
        return of(new LoadUserById(), new HideUserPopupAction());
      }
      return of(new LoadUsers(), new HideUserPopupAction());
    })
  ));

  createUser$ = createEffect(() => this._actions$.pipe(
    ofType<CreateUser>(UsersManagementActionType.CREATE_USER),
    switchMap(action => {
        return this._usersManagementService.createUser(action.createUserForm).pipe(
          map(response => new CreateUserSuccess(action.createUserForm.user.email, response.userId)),
          catchError(e => of(new ShowError(e)))
        )
      }
    )));

  createUserSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<CreateUserSuccess>(UsersManagementActionType.CREATE_USER_SUCCESS),
    tap(action => this._notificationService.success('@User created successfully', {email: action.email})),
    switchMap(action => of(new NavigateToUserEdition(action.userId)))
  ));

  navigateToUserEdition$ = createEffect(() => this._actions$.pipe(
    ofType<NavigateToUserEdition>(UsersManagementActionType.NAVIGATE_TO_USER_EDITION),
    tap((action) => this._router.navigate([`user-management/edit/${action.userId}`])),
  ), {dispatch: false});

  loadUserById$ = createEffect(() => this._actions$.pipe(
    ofType<LoadUserById>(UsersManagementActionType.LOAD_USER_BY_ID),
    withLatestFrom(this._store.select(selectParamUserId)),
    tap(([action, userId]) => sessionStorage.setItem(CURRENT_EDITED_USER_ID, userId as string)),
    switchMap(([action, userId]) => this._usersManagementService.getUserById(userId as string)),
    switchMap(result => of(new LoadUserByIdSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  navigateToUsersManagement$ = createEffect(() => this._actions$.pipe(
    ofType<NavigateToUsersManagement>(UsersManagementActionType.NAVIGATE_TO_USERS_MANAGEMENT),
    tap((action) => this._router.navigate(['user-management'])),
  ), {dispatch: false});

  loadAllDashboardsWithMarkedUser$ = createEffect(() => this._actions$.pipe(
    ofType<LoadDashboards>(UsersManagementActionType.LOAD_DASHBOARDS),
    withLatestFrom(this._store.select(selectParamUserId)),
    switchMap(([action, userId]) => {
        return this._usersManagementService.getDashboardsWithMarkedUser(userId as string).pipe(
          map((result) => new LoadDashboardsSuccess(result.dashboards)),
          catchError(e => of(new ShowError(e)))
        )
      }
    ))
  );

  syncUsers$ = createEffect(() => this._actions$.pipe(
    ofType<SyncUsers>(UsersManagementActionType.SYNC_ALL_USERS),
    switchMap(action => {
        return this._usersManagementService.syncUsers().pipe(
          map((result) => new SyncUsersSuccess()),
          catchError(e => of(new ShowError(e)))
        )
      }
    ))
  );

  syncUsersSuccess$ = createEffect(() => this._actions$.pipe(
      ofType<SyncUsersSuccess>(UsersManagementActionType.SYNC_ALL_USERS_SUCCESS),
      tap(action => this._notificationService.success('@Sync Started'))
    ), {dispatch: false}
  );

  loadAvailableContextRoles$ = createEffect(() => this._actions$.pipe(
    ofType<LoadAvailableContextRoles>(UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES),
    switchMap(() => this._contextRoleService.getRoles()),
    switchMap(result => of(new LoadAvailableContextRolesSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  loadAvailableContexts$ = createEffect(() => this._actions$.pipe(
    ofType<LoadAvailableContexts>(UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS),
    switchMap(() => this._usersManagementService.getAvailableContexts()),
    switchMap(result => of(new LoadAvailableContextsSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  loadUserContextRoles$ = createEffect(() => this._actions$.pipe(
    ofType<LoadUserContextRoles>(UsersManagementActionType.LOAD_USER_CONTEXT_ROLES),
    withLatestFrom(this._store.select(selectParamUserId)),
    switchMap(([action, userId]) => this._usersManagementService.getUserContextRoles(userId as string)),
    switchMap(result => of(new LoadUserContextRolesSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  updateUserRoles$ = createEffect(() => this._actions$.pipe(
    ofType<UpdateUserRoles>(UsersManagementActionType.UPDATE_USER_ROLES),
    withLatestFrom(
      this._store.select(selectSelectedRolesForUser),
      this._store.select(selectDashboardsForUser)
    ),
    switchMap(([action, selectedRoles, selectedDashboards]) => this._usersManagementService.updateUserRoles(selectedRoles, selectedDashboards)),
    switchMap(() => of(new UpdateUserRolesSuccess(), new ClearUnsavedChanges())),
    catchError(e => of(new ShowError(e)))
  ));

  updateUserRoles$UserSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<UpdateUserRolesSuccess>(UsersManagementActionType.UPDATE_USER_ROLES_SUCCESS),
    switchMap((action) => of(new ShowSuccess('@User roles updated'))),
  ));

  loadAdminEmails$ = createEffect(() => this._actions$.pipe(
    ofType<NavigateToUsersManagement>(UsersManagementActionType.LOAD_ADMIN_EMAILS),
    switchMap(() => this._usersManagementService.getAdminEmails()),
    switchMap(result => of(new LoadAdminEmailsSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  constructor(
    private _router: Router,
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _usersManagementService: UsersManagementService,
    private _contextRoleService: ContextRoleService,
    private _notificationService: NotificationService
  ) {
  }
}
