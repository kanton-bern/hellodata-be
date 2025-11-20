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
import {asyncScheduler, catchError, scheduled, switchMap, withLatestFrom} from "rxjs";
import {MyDashboardsService} from "./my-dashboards.service";
import {navigate, navigateToList, showError, showSuccess, trackEvent} from "../app/app.action";
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
import {ScreenService} from "../../shared/services";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectCurrentUserPermissions} from "../auth/auth.selector";

@Injectable()
export class MyDashboardsEffects {
  private _actions$ = inject(Actions);
  private _myDashboardsService = inject(MyDashboardsService);
  private _notificationService = inject(NotificationService);
  private _translateService = inject(TranslateService);
  private _screenService = inject(ScreenService);
  private _store = inject<Store<AppState>>(Store);


  loadMyDashboards$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyDashboards),
      withLatestFrom(this._store.select(selectCurrentUserPermissions)),
      switchMap(([action, currentUserPermissions]) => {
        const dashboardsPermission = currentUserPermissions.find(permission => permission === 'DASHBOARDS');
        if (currentUserPermissions && dashboardsPermission && dashboardsPermission.length > 0) {
          return this._myDashboardsService.getMyDashboards(); //load dashboards only if user has DASHBOARDS permission
        }
        return scheduled([[]], asyncScheduler); //empty array if no permission
      }),
      switchMap(result => scheduled([loadMyDashboardsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  setSelectedDataDomain$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(setSelectedDataDomain),
      withLatestFrom(this._screenService.isMobile),
      switchMap(([action, isMobile]) => {
          const successMsg = {
            message: '@Data domain changed',
            interpolateParams: {'dataDomainName': this._translateService.translate(action.dataDomain.name)}
          };
          if (isMobile) {
            return scheduled([
              trackEvent({
                eventCategory: 'Mobile',
                eventAction: '[Click] - Data Domain changed to ' + action.dataDomain.name
              }),
              showSuccess(successMsg),
              navigate({url: 'home'})
            ], asyncScheduler);
          }
          return scheduled([
            trackEvent({
              eventCategory: 'Data Domain',
              eventAction: '[Click] - Data Domain changed to ' + action.dataDomain.name
            }),
            showSuccess(successMsg),
            navigateToList()
          ], asyncScheduler);
        }
      ),
    )
  });

  loadAvailableDataDomains$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAvailableDataDomains),
      switchMap(() => this._myDashboardsService.getAvailableDataDomains()),
      switchMap(result => scheduled([loadAvailableDataDomainsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  uploadDashboardsFileSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsSuccess),
      switchMap(() => {
        this._notificationService.success('@Dashboards uploaded successfully');
        return scheduled([navigate({url: 'redirect/dashboard-import-export'})], asyncScheduler)
      })
    )
  });

  uploadDashboardsFileError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(uploadDashboardsError),
      switchMap((payload) => {
        return scheduled([showError({error: payload.error}), navigate({url: 'redirect/dashboard-import-export'})], asyncScheduler)
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });
}
