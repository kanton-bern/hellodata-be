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
import {catchError, EMPTY, map, of, switchMap, tap, withLatestFrom} from "rxjs";
import {
  authError,
  checkAuth,
  checkAuthComplete,
  checkProfile,
  fetchContextRoles,
  fetchContextRolesSuccess,
  fetchPermissionSuccess,
  login,
  loginComplete,
  logout,
  prolongCBSession,
  setActiveTranslocoLanguage,
  setAvailableLanguages,
  setDefaultLanguage,
  setSelectedLanguage,
  userForbidden
} from "./auth.action";
import {AuthService} from "../../shared/services";
import {UsersManagementService} from "../users-management/users-management.service";
import {navigate, showError} from "../app/app.action";
import {loadMyLineageDocs} from "../lineage-docs/lineage-docs.action";
import {loadAppInfoResources} from "../metainfo-resource/metainfo-resource.action";
import {loadAvailableDataDomains, loadMyDashboards} from "../my-dashboards/my-dashboards.action";
import {loadDocumentation, loadPipelines, loadStorageSize} from "../summary/summary.actions";
import {TranslateService} from "../../shared/services/translate.service";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectProfile, selectSelectedLanguage, selectSupportedLanguages} from "./auth.selector";
import {CloudbeaverService} from "./cloudbeaver.service";
import {CloudbeaverSessionService} from "../../shared/services/cloudbeaver-session.service";
import {WindowManagementService} from "../../shared/services/window-management.service";

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
      tap(() => {
        this._cloudbeaverSessionService.destroyTimerCookie();
        this._windowManagementService.closeAllWindows();
      }),
      switchMap(() => this._authService.signOut()),
      catchError(e => of(authError(e)))
    )
  });

  checkAuth$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(checkAuth),
      switchMap(() => {
          return this._authService.checkAuth().pipe(
            map(authResult => checkAuthComplete({
              isLoggedIn: authResult.isAuthenticated,
              accessToken: authResult.accessToken,
              profile: authResult.userData
            })),
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
                  return loginComplete({
                    profile: userData,
                    isLoggedIn: action.isLoggedIn,
                    accessToken: action.accessToken
                  })
                })
              );
            }
            return of(loginComplete({
              profile: action.profile,
              isLoggedIn: action.isLoggedIn,
              accessToken: action.accessToken
            }));
          }
          return of(navigate({url: 'home'}));
        }
      ),
      catchError(e => of(authError(e)))
    )
  });

  checkProfile$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(checkProfile),
      switchMap(() => this._usersManagementService.getCurrentAuthData()),
      switchMap((currentUserAuthData) => {
        console.debug("Check profile, current data", currentUserAuthData)
        if (currentUserAuthData && !currentUserAuthData.userDisabled) {
          return of(fetchPermissionSuccess({currentUserAuthData}))
        }
        console.error("!!! User disabled !!!");
        return of(userForbidden());
      }),
      catchError(e => of(authError(e)))
    )
  })

  loginComplete$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loginComplete),
      switchMap(() => of(checkProfile())),
      catchError(e => of(authError(e)))
    )
  });

  fetchPermissionSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchPermissionSuccess),
      switchMap((action) => {
        const defaultLanguage = this._translateService.getDefaultLanguage();
        const availableLangs = this._translateService.getAvailableLangs();
        const permissions = action.currentUserAuthData.permissions;
        if (!permissions || permissions.length === 0 || !permissions.includes('DASHBOARDS')) {
          return of(
            setAvailableLanguages({langs: availableLangs}),
            setDefaultLanguage({lang: defaultLanguage}),
            loadAvailableDataDomains(),
            loadAppInfoResources(),
            loadDocumentation(),
            loadMyLineageDocs(),
            fetchContextRoles(),
            loadPipelines(),
            loadStorageSize());
        }
        return of(
          setAvailableLanguages({langs: availableLangs}),
          setDefaultLanguage({lang: defaultLanguage}),
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

  setSelectedLanguage$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setSelectedLanguage),
      withLatestFrom(
        this._store.select(selectProfile)
      ),
      switchMap(([action, profile]) => {
        this._translateService.setActiveLang(action.lang);
        return this._usersManagementService.editSelectedLanguageForUser(profile.sub, action.lang).pipe(
          switchMap(() => {
            // Reuse action.lang here
            return this._cloudbeaverService.updateUserPreferences(action.lang);
          })
        );
      }),
      switchMap(() => {
        return EMPTY;
      }),
      catchError(e => of(showError({error: e})))
    )
  });

  prolongCloudBeaverSession$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(prolongCBSession),
      switchMap(() => this._cloudbeaverService.renewSession().pipe(
        catchError(e => of(showError({error: e}))))
      ),
      switchMap(() => {
        return EMPTY;
      }),
      catchError(e => of(showError({error: e})))
    )
  });

  setAvailableLanguages$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setAvailableLanguages),
      switchMap(() => {
        return of(setActiveTranslocoLanguage());
      }),
      catchError(e => of(showError({error: e})))
    )
  });


  setActiveTranslocoLanguage$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setActiveTranslocoLanguage),
      withLatestFrom(
        this._store.select(selectSelectedLanguage),
        this._store.select(selectSupportedLanguages)
      ),
      tap(([_, selectedLanguage, supportedLanguages]) => {
        if (selectedLanguage && !supportedLanguages.includes(selectedLanguage.code as string)) {
          const lang = supportedLanguages.find(lang => lang.startsWith(selectedLanguage.code as string));
          if (lang) {
            this._translateService.setActiveLang(lang as string);
          } else {
            this._translateService.setActiveLang(this._translateService.getDefaultLanguage());
          }
        } else if (selectedLanguage) {
          this._translateService.setActiveLang(selectedLanguage.code as string);
        }
      }),
      switchMap(() => {
        return EMPTY;
      }),
      catchError(e => of(showError({error: e})))
    )
  });

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _authService: AuthService,
    private _usersManagementService: UsersManagementService,
    private _translateService: TranslateService,
    private _cloudbeaverService: CloudbeaverService,
    private _cloudbeaverSessionService: CloudbeaverSessionService,
    private _windowManagementService: WindowManagementService
  ) {
  }
}
