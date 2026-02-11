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
import {asyncScheduler, catchError, delay, map, scheduled, switchMap, tap, withLatestFrom} from "rxjs";
import {UsersManagementService} from "./users-management.service";
import {NotificationService} from "../../shared/services/notification.service";
import {Store} from '@ngrx/store';
import {AppState} from "../app/app.state";
import {
  selectCommentPermissionsForUser,
  selectCurrentPagination,
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
  clearSubsystemUsersCache,
  clearSubsystemUsersForDashboardsCache,
  createUser,
  createUserSuccess,
  deleteUserInStore,
  hideUserPopupAction,
  invokeActionFromUserPopup,
  loadAdminEmails,
  loadAdminEmailsSuccess,
  loadAvailableContextRoles,
  loadAvailableContextRolesSuccess,
  loadAvailableContexts,
  loadAvailableContextsSuccess,
  loadCommentPermissions,
  loadCommentPermissionsSuccess,
  loadDashboards,
  loadDashboardsSuccess,
  loadSubsystemUsers,
  loadSubsystemUsersForDashboards,
  loadSubsystemUsersForDashboardsSuccess,
  loadSubsystemUsersSuccess,
  loadSyncStatus,
  loadSyncStatusSuccess,
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
  updateUserInStore,
  updateUserRoles,
  updateUserRolesSuccess,
  userPopupActionSuccess
} from "./users-management.action";

@Injectable()
export class UsersManagementEffects {
  private readonly _router = inject(Router);
  private readonly _actions$ = inject(Actions);
  private readonly _store = inject<Store<AppState>>(Store);
  private readonly _usersManagementService = inject(UsersManagementService);
  private readonly _contextRoleService = inject(ContextRoleService);
  private readonly _notificationService = inject(NotificationService);


  loadUsers$ = createEffect(() =>
    this._actions$.pipe(
      ofType(loadUsers),
      switchMap(({page, size, sort, search}) =>
        this._usersManagementService.getUsers(page, size, sort, search).pipe(
          map((response) => loadUsersSuccess({
            users: response.content,
            totalElements: response.totalElements,
            totalPages: response.totalPages
          })),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        )
      )
    )
  );

  loadSubsystemUsers$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSubsystemUsers),
      switchMap(() => this._usersManagementService.getSubsystemUsers()),
      switchMap(result => scheduled([loadSubsystemUsersSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadSubsystemUsersForDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSubsystemUsersForDashboards),
      switchMap(() => this._usersManagementService.getAllUsersWithRolesForDashboards()),
      switchMap(result => scheduled([loadSubsystemUsersForDashboardsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  userPopupAction$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(invokeActionFromUserPopup),
      withLatestFrom(this._store.select(selectUserForPopup), this._store.select(selectCurrentPagination)),
      switchMap(([action, userActionForPopup, currentPagination]) => {
          switch (userActionForPopup!.action) {
            case (UserAction.DISABLE):
              return this._usersManagementService.disableUser(userActionForPopup!.user).pipe(
                switchMap((user) => scheduled([
                  userPopupActionSuccess({
                    email: userActionForPopup!.user.email,
                    userActionForPopup: userActionForPopup as UserActionForPopup
                  }),
                  updateUserInStore({userChanged: user})], asyncScheduler)),
                catchError(e => scheduled([showError({error: e})], asyncScheduler))
              );
            case (UserAction.ENABLE):
              return this._usersManagementService.enableUser(userActionForPopup!.user).pipe(
                switchMap((user) => scheduled([
                  userPopupActionSuccess({
                    email: userActionForPopup!.user.email,
                    userActionForPopup: userActionForPopup as UserActionForPopup
                  }),
                  updateUserInStore({userChanged: user})], asyncScheduler)),
                catchError(e => scheduled([showError({error: e})], asyncScheduler))
              );
            default:
              return this._usersManagementService.deleteUser(userActionForPopup!.user).pipe(
                switchMap(() => scheduled([
                  userPopupActionSuccess({
                    email: userActionForPopup!.user.email,
                    userActionForPopup: userActionForPopup as UserActionForPopup
                  }),
                  deleteUserInStore({userDeleted: userActionForPopup!.user}),
                  loadUsers({
                    page: currentPagination.page,
                    size: currentPagination.size,
                    sort: currentPagination.sort,
                    search: currentPagination.search
                  })], asyncScheduler)),
                catchError(e => scheduled([showError({error: e})], asyncScheduler))
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
          return scheduled([hideUserPopupAction(), navigate({url: '/user-management'})], asyncScheduler);
        } else if (action.userActionForPopup.actionFromUsersEdition) {
          return scheduled([loadUserById(), hideUserPopupAction()], asyncScheduler);
        }
        return scheduled([hideUserPopupAction()], asyncScheduler);
      })
    )
  });

  createUser$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createUser),
      switchMap(action => {
          return this._usersManagementService.createUser(action.createUserForm).pipe(
            map(response => createUserSuccess({email: action.createUserForm.user.email, userId: response.userId})),
            catchError(e => scheduled([showError({error: e})], asyncScheduler))
          )
        }
      ))
  });

  createUserSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(createUserSuccess),
      tap(action => this._notificationService.success('@User created successfully', {email: action.email})),
      switchMap(action => scheduled([navigateToUserEdition({userId: action.userId})], asyncScheduler))
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
      withLatestFrom(this._store.select(selectParamUserId)),
      tap(([action, userId]) => sessionStorage.setItem(CURRENT_EDITED_USER_ID, userId as string)),
      switchMap(([action, userId]) => this._usersManagementService.getUserById(userId as string)),
      switchMap(result => scheduled([loadUserByIdSuccess({user: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
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
        withLatestFrom(this._store.select(selectParamUserId)),
        switchMap(([action, userId]) => {
            return this._usersManagementService.getDashboardsWithMarkedUser(userId as string).pipe(
              map((result) => loadDashboardsSuccess({dashboards: result.dashboards})),
              catchError(e => scheduled([showError({error: e})], asyncScheduler))
            )
          }
        ))
    }
  );

  syncUsers$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(syncUsers),
      switchMap(() => this._usersManagementService.syncUsers()),
      switchMap((status) => scheduled([syncUsersSuccess({status}), loadSyncStatus()], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

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
      switchMap(result => scheduled([loadAvailableContextRolesSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadAvailableContexts$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableContexts),
      switchMap(() => this._usersManagementService.getAvailableContexts()),
      switchMap(result => scheduled([loadAvailableContextsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadUserContextRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadUserContextRoles),
      withLatestFrom(this._store.select(selectParamUserId)),
      switchMap(([action, userId]) => this._usersManagementService.getUserContextRoles(userId as string)),
      switchMap(result => scheduled([loadUserContextRolesSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  updateUserRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateUserRoles),
      withLatestFrom(
        this._store.select(selectSelectedRolesForUser),
        this._store.select(selectDashboardsForUser),
        this._store.select(selectCommentPermissionsForUser)
      ),
      switchMap(([action, selectedRoles, selectedDashboards, commentPermissions]) => {
        const commentPermissionsMap = new Map(Object.entries(commentPermissions));
        return this._usersManagementService.updateUserRoles(selectedRoles, selectedDashboards, commentPermissionsMap);
      }),
      switchMap(() => scheduled([updateUserRolesSuccess(), clearUnsavedChanges()], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  updateUserRolesSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(updateUserRolesSuccess),
      delay(200),
      switchMap((action) =>
        scheduled([loadUserContextRoles(), loadUserById(), loadCommentPermissions(), showSuccess({message: '@User roles updated'})], asyncScheduler)),
    )
  });

  loadAdminEmails$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAdminEmails),
      switchMap(() => this._usersManagementService.getAdminEmails()),
      switchMap(result => scheduled([loadAdminEmailsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadSyncStatus$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadSyncStatus),
      switchMap(() => this._usersManagementService.getSyncStatus()),
      switchMap(result => scheduled([loadSyncStatusSuccess({status: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  clearSubsystemUsersCache$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(clearSubsystemUsersCache),
      switchMap(() => this._usersManagementService.clearSubsystemUsersCache()),
      switchMap(() => scheduled([loadSubsystemUsers()], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  clearSubsystemUsersForDashboardsCache$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(clearSubsystemUsersForDashboardsCache),
      switchMap(() => this._usersManagementService.clearAllUsersWithRolesForDashboardsCache()),
      switchMap(() => scheduled([loadSubsystemUsersForDashboards()], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadCommentPermissions$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadCommentPermissions),
      withLatestFrom(this._store.select(selectParamUserId)),
      switchMap(([action, userId]) => this._usersManagementService.getCommentPermissions(userId as string)),
      map(result => {
        const permissionsMap: Record<string, {
          readComments: boolean,
          writeComments: boolean,
          reviewComments: boolean
        }> = {};
        result.forEach(p => {
          permissionsMap[p.contextKey] = {
            readComments: p.readComments,
            writeComments: p.writeComments,
            reviewComments: p.reviewComments
          };
        });
        return loadCommentPermissionsSuccess({payload: permissionsMap});
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });
}
