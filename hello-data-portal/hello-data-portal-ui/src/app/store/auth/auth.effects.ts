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
import {catchError, map, of, switchMap, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {
  AuthActionType,
  AuthError,
  CheckAuth,
  CheckAuthComplete,
  FetchContextRoles,
  FetchContextRolesSuccess,
  FetchPermissionSuccess,
  Login,
  LoginComplete,
  Logout
} from "./auth.action";
import {AuthService} from "../../shared/services";
import {Router} from "@angular/router";
import {UsersManagementService} from "../users-management/users-management.service";
import {LoadAvailableDataDomains, LoadMyDashboards} from "../my-dashboards/my-dashboards.action";
import {Navigate, ShowError} from "../app/app.action";
import {LoadDocumentation, LoadPipelines, LoadStorageSize} from "../summary/summary.actions";
import {LoadAppInfoResources} from "../metainfo-resource/metainfo-resource.action";
import {LoadMyLineageDocs} from "../lineage-docs/lineage-docs.action";

@Injectable()
export class AuthEffects {
  login$ = createEffect(() => this._actions$.pipe(
    ofType<Login>(AuthActionType.LOGIN),
    tap(() => this._authService.doLogin()),
    catchError(e => of(new AuthError(e)))
  ), {dispatch: false});

  logout$ = createEffect(() => this._actions$.pipe(
    ofType<Logout>(AuthActionType.LOGOUT),
    switchMap(() => this._authService.signOut()),
    catchError(e => of(new AuthError(e)))
  ));

  checkAuth$ = createEffect(() => this._actions$.pipe(
    ofType<CheckAuth>(AuthActionType.CHECK_AUTH),
    switchMap(() => {
        return this._authService.checkAuth().pipe(
          map(authResult => new CheckAuthComplete(authResult.isAuthenticated, authResult.accessToken, authResult.userData)),
          catchError(e => of(new AuthError(e)))
        )
      }
    ),
    catchError(e => of(new AuthError(e)))
  ));

  checkAuthComplete$ = createEffect(() => this._actions$.pipe(
    ofType<CheckAuthComplete>(AuthActionType.CHECK_AUTH_COMPLETE),
    switchMap((action) => {
        if (action.isLoggedIn) {
          if (!action.profile) {//sometimes the userinfo is not fetched properly from the auth server, so we use an already obtained access_token
            return this._authService.payloadFromAccessToken.pipe(
              map(accessTokenPayload => {
                const userData = {
                  sub: accessTokenPayload['sub'],
                  name: accessTokenPayload['name'],
                  family_name: accessTokenPayload['family_name'],
                  given_name: accessTokenPayload['given_name'],
                  email: accessTokenPayload['email'],
                }
                return new LoginComplete(userData, action.isLoggedIn, action.accessToken);
              })
            );
          }
          return of(new LoginComplete(action.profile, action.isLoggedIn, action.accessToken));
        }
        return of(new Navigate('home'));
      }
    ),
    catchError(e => of(new AuthError(e)))
  ));

  loginComplete$ = createEffect(() => this._actions$.pipe(
    ofType<LoginComplete>(AuthActionType.LOGIN_COMPLETE),
    switchMap(({profile, isLoggedIn, accessToken}) => this._usersManagementService.getCurrentAuthData()),
    switchMap((permissions) => of(new FetchPermissionSuccess(permissions))),
    catchError(e => of(new AuthError(e)))
  ));

  fetchPermissionsSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<FetchPermissionSuccess>(AuthActionType.FETCH_PERMISSIONS_SUCCESS),
    switchMap(() => of(
      new LoadAvailableDataDomains(),
      new LoadAppInfoResources(),
      new LoadMyDashboards(),
      new LoadDocumentation(),
      new LoadMyLineageDocs(),
      new FetchContextRoles(),
      new LoadPipelines(),
      new LoadStorageSize())),
  ));


  authError$ = createEffect(() => this._actions$.pipe(
    ofType<AuthError>(AuthActionType.AUTH_ERROR),
    tap(action => this._store.dispatch(new ShowError(action.error)))
  ), {dispatch: false});

  fetchContextRoles$ = createEffect(() => this._actions$.pipe(
    ofType<FetchContextRoles>(AuthActionType.FETCH_CONTEXT_ROLES),
    switchMap(() => this._usersManagementService.getCurrentContextRoles()),
    switchMap(result => of(new FetchContextRolesSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  constructor(
    private router: Router,
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _authService: AuthService,
    private _usersManagementService: UsersManagementService,
  ) {
  }
}
