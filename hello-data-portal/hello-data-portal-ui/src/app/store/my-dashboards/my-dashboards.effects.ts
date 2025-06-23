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
import {catchError, of, switchMap, withLatestFrom} from "rxjs";
import {MyDashboardsService} from "./my-dashboards.service";
import {navigate, navigateToList, showError, showSuccess} from "../app/app.action";
import {processNavigation} from "../menu/menu.action";
import {
  loadAvailableDataDomains,
  loadAvailableDataDomainsSuccess,
  loadMyDashboards,
  loadMyDashboardsSuccess,
  setSelectedDataDomain,
  uploadDashboardsError,
  uploadDashboardsSuccess
} from "./my-dashboards.action";
import {NotificationService} from "../../shared/services/notification.service";
import {TranslateService} from "../../shared/services/translate.service";
import {selectParamExternalDashboardId} from "../external-dashboards/external-dashboards.selector";
import {ScreenService} from "../../shared/services";

@Injectable()
export class MyDashboardsEffects {

  loadMyDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyDashboards),
      switchMap(() => this._myDashboardsService.getMyDashboards()),
      switchMap(result => of(loadMyDashboardsSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadMyDashboardsSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyDashboardsSuccess),
      switchMap(() => of(processNavigation({compactMode: false}))),
    )
  });

  setSelectedDataDomain$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setSelectedDataDomain),
      concatLatestFrom(() => this._screenService.isMobile),
      switchMap(([action, isMobile]) => {
        const successMsg = {message: '@Data domain changed', interpolateParams: {'dataDomainName': this._translateService.translate(action.dataDomain.name)}};
        if (isMobile) {
          return of(
            showSuccess(successMsg),
            navigate({url: 'home'})
          );
        }
        return of(
          showSuccess(successMsg),
          navigateToList()
        );
        }
      ),
    )
  });

  loadAvailableDataDomains$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableDataDomains),
      switchMap(() => this._myDashboardsService.getAvailableDataDomains()),
      switchMap(result => of(loadAvailableDataDomainsSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  uploadDashboardsFileSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsSuccess),
      switchMap(() => {
        this._notificationService.success('@Dashboards uploaded successfully');
        return of(navigate({url: 'redirect/dashboard-import-export'}))
      })
    )
  });

  uploadDashboardsFileError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsError),
      switchMap((payload) => {
        return of(showError({error: payload.error}), navigate({url: 'redirect/dashboard-import-export'}))
      }),
      catchError(e => of(showError({error: e})))
    )
  });

  constructor(
    private _actions$: Actions,
    private _myDashboardsService: MyDashboardsService,
    private _notificationService: NotificationService,
    private _translateService: TranslateService,
    private _screenService: ScreenService
  ) {
  }
}
