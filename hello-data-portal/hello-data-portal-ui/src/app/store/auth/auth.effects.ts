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
import {authError, checkAuth, checkAuthComplete, fetchContextRoles, fetchContextRolesSuccess, fetchPermissionSuccess, login, loginComplete, logout} from "./auth.action";
import {AuthService} from "../../shared/services";
import {UsersManagementService} from "../users-management/users-management.service";
import {navigate, showError} from "../app/app.action";
import {loadMyLineageDocs} from "../lineage-docs/lineage-docs.action";
import {loadAppInfoResources} from "../metainfo-resource/metainfo-resource.action";
import {loadAvailableDataDomains, loadMyDashboards} from "../my-dashboards/my-dashboards.action";
import {loadDocumentation, loadPipelines, loadStorageSize} from "../summary/summary.actions";

@Injectable()
export class AuthEffects {
  login$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(login),
      tap(() => this._authService.doLogin()),
      catchError(e => of(authError(e)))
    )
  }, {dispatch: false});

  logout$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(logout),
      switchMap(() => this._authService.signOut()),
      catchError(e => of(authError(e)))
    )
  });

  checkAuth$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(checkAuth),
      switchMap(() => {
          return this._authService.checkAuth().pipe(
            map(authResult => checkAuthComplete({isLoggedIn: authResult.isAuthenticated, accessToken: authResult.accessToken, profile: authResult.userData})),
            catchError(e => of(authError(e)))
          )
        }
      ),
      catchError(e => of(authError(e)))
    )
  });

  checkAuthComplete$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(checkAuthComplete),
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
                  return loginComplete({profile: userData, isLoggedIn: action.isLoggedIn, accessToken: action.accessToken})
                })
              );
            }
            return of(loginComplete({profile: action.profile, isLoggedIn: action.isLoggedIn, accessToken: action.accessToken}));
          }
          return of(navigate({url: 'home'}));
        }
      ),
      catchError(e => of(authError(e)))
    )
  });

  loginComplete$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loginComplete),
      switchMap(() => this._usersManagementService.getCurrentAuthData()),
      switchMap((currentUserAuthData) => of(fetchPermissionSuccess({currentUserAuthData}))),
      catchError(e => of(authError(e)))
    )
  });

  fetchPermissionSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchPermissionSuccess),
      switchMap((action) => {
        const permissions = action.currentUserAuthData.permissions;
        if (!permissions || permissions.length === 0 || !permissions.includes('DASHBOARDS')) {
          return of(
            loadAvailableDataDomains(),
            loadAppInfoResources(),
            loadDocumentation(),
            loadMyLineageDocs(),
            fetchContextRoles(),
            loadPipelines(),
            loadStorageSize());
        }
        return of(
          loadAvailableDataDomains(),
          loadAppInfoResources(),
          loadMyDashboards(),
          loadDocumentation(),
          loadMyLineageDocs(),
          fetchContextRoles(),
          loadPipelines(),
          loadStorageSize());
      }),
    )
  });

  authError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(authError),
      switchMap(action => of(showError({error: action.error})))
    )
  }, {dispatch: false});

  fetchContextRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchContextRoles),
      switchMap(() => this._usersManagementService.getCurrentContextRoles()),
      switchMap(result => of(fetchContextRolesSuccess({contextRoles: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  constructor(
    private _actions$: Actions,
    private _authService: AuthService,
    private _usersManagementService: UsersManagementService,
  ) {
  }
}
