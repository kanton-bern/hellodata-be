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
import {asyncScheduler, catchError, EMPTY, ignoreElements, map, scheduled, switchMap, tap, withLatestFrom} from "rxjs";
import {
  authError,
  checkAuth,
  checkAuthComplete,
  checkProfile,
  fetchContextRoles,
  fetchContextRolesSuccess,
  fetchCurrentUserCommentPermissions,
  fetchCurrentUserCommentPermissionsSuccess,
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
import {logError, showError} from "../app/app.action";
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
import {processNavigation} from "../menu/menu.action";

@Injectable()
export class AuthEffects {
  private readonly _actions$ = inject(Actions);
  private readonly _store = inject<Store<AppState>>(Store);
  private readonly _authService = inject(AuthService);
  private readonly _usersManagementService = inject(UsersManagementService);
  private readonly _translateService = inject(TranslateService);
  private readonly _cloudbeaverService = inject(CloudbeaverService);
  private readonly _cloudbeaverSessionService = inject(CloudbeaverSessionService);
  private readonly _windowManagementService = inject(WindowManagementService);

  login$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(login),
      tap(() => this._authService.doLogin()),
      catchError(e => scheduled([authError(e)], asyncScheduler))
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
      catchError(e => scheduled([authError(e)], asyncScheduler))
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
            catchError(e => scheduled([authError(e)], asyncScheduler))
          )
        }
      ),
      catchError(e => scheduled([authError(e)], asyncScheduler))
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
            return scheduled([loginComplete({
              profile: action.profile,
              isLoggedIn: action.isLoggedIn,
              accessToken: action.accessToken
            })], asyncScheduler);
          }
          // Not logged in — trigger the OIDC authorize flow directly.
          // Do NOT navigate to a guarded route (which would cause a redirect loop).
          console.debug('[Auth] checkAuth completed but not logged in, triggering login');
          return scheduled([login()], asyncScheduler);
        }
      ),
      catchError(e => scheduled([authError(e)], asyncScheduler))
    )
  });

  checkProfile$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(checkProfile),
      switchMap(() => this._usersManagementService.getCurrentAuthData()),
      switchMap((currentUserAuthData) => {
        console.debug("Check profile, current data", currentUserAuthData)
        if (currentUserAuthData && !currentUserAuthData.userDisabled) {
          return scheduled([fetchPermissionSuccess({currentUserAuthData})], asyncScheduler)
        }
        console.error("!!! User disabled !!!");
        return scheduled([userForbidden()], asyncScheduler);
      }),
      catchError(e => scheduled([authError(e)], asyncScheduler))
    )
  })

  loginComplete$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loginComplete),
      switchMap(() => scheduled([checkProfile()], asyncScheduler)),
      catchError(e => scheduled([authError(e)], asyncScheduler))
    )
  });

  fetchPermissionSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchPermissionSuccess),
      switchMap((action) => {
        const defaultLanguage = this._translateService.getDefaultLanguage();
        const availableLangs = this._translateService.getAvailableLangs();
        return scheduled([
          setAvailableLanguages({langs: availableLangs}),
          setDefaultLanguage({lang: defaultLanguage}),
          loadAvailableDataDomains(),
          loadAppInfoResources(),
          loadMyDashboards(),
          loadDocumentation(),
          loadMyLineageDocs(),
          fetchContextRoles(),
          fetchCurrentUserCommentPermissions(),
          loadPipelines(),
          loadStorageSize(),
          processNavigation()
        ], asyncScheduler);
      }),
    )
  });

  fetchCurrentUserCommentPermissions$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchCurrentUserCommentPermissions),
      switchMap(() => this._usersManagementService.getCurrentUserCommentPermissions()),
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
        return fetchCurrentUserCommentPermissionsSuccess({commentPermissions: permissionsMap});
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  authError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(authError),
      tap(action => console.error('Auth error:', action.error))
    )
  }, {dispatch: false});

  fetchContextRoles$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(fetchContextRoles),
      switchMap(() => this._usersManagementService.getCurrentContextRoles()),
      switchMap(result => scheduled([fetchContextRolesSuccess({contextRoles: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  setSelectedLanguage$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setSelectedLanguage),
      withLatestFrom(this._store.select(selectProfile)),
      switchMap(([action, profile]) => {
        // always set transloco language immediately
        this._translateService.setActiveLang(action.lang);

        // attempt backend update
        return this._usersManagementService
          .editSelectedLanguageForUser(profile.sub, action.lang)
          .pipe(
            switchMap(() => this._cloudbeaverService.updateUserPreferences(action.lang)),

            // catch errors from backend and keep the effect alive
            catchError(e =>
              scheduled([
                logError({error: e}),
              ], asyncScheduler).pipe(
                ignoreElements() // prevents effect completion
              )
            )
          );
      }),

      // optional: catch unexpected errors in the outer stream
      catchError(e =>
        scheduled([
          logError({error: e})
        ], asyncScheduler).pipe(
          ignoreElements()
        )
      )
    );
  });

  prolongCloudBeaverSession$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(prolongCBSession),
      switchMap(() => this._cloudbeaverService.renewSession().pipe(
        catchError(e => scheduled([showError({error: e})], asyncScheduler)))
      ),
      switchMap(() => {
        return EMPTY;
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  setAvailableLanguages$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setAvailableLanguages),
      switchMap(() => {
        return scheduled([setActiveTranslocoLanguage()], asyncScheduler);
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
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
            this._translateService.setActiveLang(lang);
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
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });
}
