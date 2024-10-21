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
import {catchError, map, of, switchMap, tap, withLatestFrom} from "rxjs";
import {UsersManagementService} from "./users-management.service";
import {NotificationService} from "../../shared/services/notification.service";
import {Store} from '@ngrx/store';
import {AppState} from "../app/app.state";
import {
  selectDashboardsForUser,
  selectParamUserId,
  selectSelectedRolesForUser,
  selectUserForPopup
} from "./users-management.selector";
import {Router} from "@angular/router";
import {UserAction, UserActionForPopup} from "./users-management.model";
import {CURRENT_EDITED_USER_ID} from "./users-management.reducer";
import {ContextRoleService} from "./context-role.service";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError, showSuccess} from "../app/app.action";
import {
  createUser,
  createUserSuccess,
  hideUserPopupAction,
  invokeActionFromUserPopup,
  loadAdminEmails,
  loadAdminEmailsSuccess,
  loadAvailableContextRoles,
  loadAvailableContextRolesSuccess,
  loadAvailableContexts,
  loadAvailableContextsSuccess,
  loadDashboards,
  loadDashboardsSuccess,
  loadSubsystemUsers,
  loadSubsystemUsersForDashboardsSuccess,
  loadSubsystemUsersSuccess,
  loadUserById,
  loadUserByIdSuccess,
  loadUserContextRoles,
  loadUserContextRolesSuccess,
  loadUsers,
  loadUsersSuccess,
  navigateToUserEdition,
  navigateToUsersManagement,
  syncUsers,
  syncUsersSuccess,
  updateUserRoles,
  updateUserRolesSuccess,
  userPopupActionSuccess
} from "./users-management.action";

@Injectable()
export class UsersManagementEffects {

  loadUsers$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadUsers),
      switchMap(() => this._usersManagementService.getUsers()),
      switchMap(result => of(loadUsersSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadSubsystemUsers$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSubsystemUsers),
      switchMap(() => this._usersManagementService.getSubsystemUsers()),
      switchMap(result => of(loadSubsystemUsersSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadSubsystemUsersForDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSubsystemUsers),
      switchMap(() => this._usersManagementService.getAllUsersWithRolesForDashboards()),
      switchMap(result => of(loadSubsystemUsersForDashboardsSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });


  userPopupAction$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(invokeActionFromUserPopup),
      concatLatestFrom(() => this._store.select(selectUserForPopup)),
      switchMap(([action, userActionForPopup]) => {
          switch (userActionForPopup!.action) {
            case (UserAction.DISABLE):
              return this._usersManagementService.disableUser(userActionForPopup!.user).pipe(
                map(() => userPopupActionSuccess({
                  email: userActionForPopup!.user.email,
                  userActionForPopup: userActionForPopup as UserActionForPopup
                })),
                catchError(e => of(showError({error: e})))
              );
            case (UserAction.ENABLE):
              return this._usersManagementService.enableUser(userActionForPopup!.user).pipe(
                map(() => userPopupActionSuccess({
                  email: userActionForPopup!.user.email,
                  userActionForPopup: userActionForPopup as UserActionForPopup
                })),
                catchError(e => of(showError({error: e})))
              );
            default:
              return this._usersManagementService.deleteUser(userActionForPopup!.user).pipe(
                map(() => userPopupActionSuccess({
                  email: userActionForPopup!.user.email,
                  userActionForPopup: userActionForPopup as UserActionForPopup
                })),
                catchError(e => of(showError({error: e})))
              );
          }
        }
      ))
  });

  userPopupActionSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(userPopupActionSuccess),
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
          return of(hideUserPopupAction(), navigate({url: '/user-management'}));
        } else if (action.userActionForPopup.actionFromUsersEdition) {
          return of(loadUserById(), hideUserPopupAction());
        }
        return of(loadUsers(), hideUserPopupAction());
      })
    )
  });

  createUser$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createUser),
      switchMap(action => {
          return this._usersManagementService.createUser(action.createUserForm).pipe(
            map(response => createUserSuccess({email: action.createUserForm.user.email, userId: response.userId})),
            catchError(e => of(showError({error: e})))
          )
        }
      ))
  });

  createUserSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createUserSuccess),
      tap(action => this._notificationService.success('@User created successfully', {email: action.email})),
      switchMap(action => of(navigateToUserEdition({userId: action.userId})))
    )
  });

  navigateToUserEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(navigateToUserEdition),
      tap((action) => this._router.navigate([`user-management/edit/${action.userId}`])),
    )
  }, {dispatch: false});

  loadUserById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadUserById),
      concatLatestFrom(() => this._store.select(selectParamUserId)),
      tap(([action, userId]) => sessionStorage.setItem(CURRENT_EDITED_USER_ID, userId as string)),
      switchMap(([action, userId]) => this._usersManagementService.getUserById(userId as string)),
      switchMap(result => of(loadUserByIdSuccess({user: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  navigateToUsersManagement$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(navigateToUsersManagement),
      tap((action) => this._router.navigate(['user-management'])),
    )
  }, {dispatch: false});

  loadAllDashboardsWithMarkedUser$ = createEffect(() => {
      return this._actions$.pipe(
        ofType(loadDashboards),
        concatLatestFrom(() => this._store.select(selectParamUserId)),
        switchMap(([action, userId]) => {
            return this._usersManagementService.getDashboardsWithMarkedUser(userId as string).pipe(
              map((result) => loadDashboardsSuccess({dashboards: result.dashboards})),
              catchError(e => of(showError({error: e})))
            )
          }
        ))
    }
  );

  syncUsers$ = createEffect(() => {
      return this._actions$.pipe(
        ofType(syncUsers),
        switchMap(action => {
            return this._usersManagementService.syncUsers().pipe(
              map((result) => syncUsersSuccess()),
              catchError(e => of(showError({error: e})))
            )
          }
        ))
    }
  );

  syncUsersSuccess$ = createEffect(() => {
      return this._actions$.pipe(
        ofType(syncUsersSuccess),
        tap(action => this._notificationService.success('@Sync Started'))
      )
    }, {dispatch: false}
  );

  loadAvailableContextRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableContextRoles),
      switchMap(() => this._contextRoleService.getRoles()),
      switchMap(result => of(loadAvailableContextRolesSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadAvailableContexts$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableContexts),
      switchMap(() => this._usersManagementService.getAvailableContexts()),
      switchMap(result => of(loadAvailableContextsSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadUserContextRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadUserContextRoles),
      concatLatestFrom(() => this._store.select(selectParamUserId)),
      switchMap(([action, userId]) => this._usersManagementService.getUserContextRoles(userId as string)),
      switchMap(result => of(loadUserContextRolesSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  updateUserRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateUserRoles),
      withLatestFrom(
        this._store.select(selectSelectedRolesForUser),
        this._store.select(selectDashboardsForUser)
      ),
      switchMap(([action, selectedRoles, selectedDashboards]) => this._usersManagementService.updateUserRoles(selectedRoles, selectedDashboards)),
      switchMap(() => of(updateUserRolesSuccess(), clearUnsavedChanges())),
      catchError(e => of(showError({error: e})))
    )
  });

  updateUserRolesUserSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateUserRolesSuccess),
      switchMap((action) => of(showSuccess({message: '@User roles updated'}))),
    )
  });

  loadAdminEmails$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAdminEmails),
      switchMap(() => this._usersManagementService.getAdminEmails()),
      switchMap(result => of(loadAdminEmailsSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

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
