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

import {Injectable, Type} from "@angular/core";
import {FunctionalEffect} from "@ngrx/effects/src/models";
import {MetaInfoResourceEffects} from "../metainfo-resource/metainfo-resource.effects";
import {UsersManagementEffects} from "../users-management/users-management.effects";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {tap, withLatestFrom} from "rxjs";
import {NotificationService} from "../../shared/services/notification.service";
import {navigate, navigateToList, showError, showInfo, showSuccess} from "./app.action";
import {AuthEffects} from "../auth/auth.effects";
import {Router} from "@angular/router";
import {PortalRolesManagementEffects} from "../portal-roles-management/portal-roles-management.effects";
import {RouterEffects} from "../router/router.effects";
import {AnnouncementEffects} from "../announcement/announcement.effects";
import {MyDashboardsEffects} from "../my-dashboards/my-dashboards.effects";
import {MenuEffects} from "../menu/menu.effects";
import {StartPageEffects} from "../start-page/start-page.effects";
import {FaqEffects} from "../faq/faq.effects";
import {SummaryEffects} from "../summary/summary.effects";
import {ExternalDashboardsEffects} from "../external-dashboards/external-dashboards.effects";
import {LineageDocsEffects} from "../lineage-docs/lineage-docs-effects.service";
import {UnsavedChangesEffects} from "../unsaved-changes/unsaved-changes.effects";
import {naviElements} from "../../app-navi-elements";
import {Store} from "@ngrx/store";
import {AppState} from "./app.state";
import {selectSelectedDataDomain} from "../my-dashboards/my-dashboards.selector";
import {ALL_DATA_DOMAINS} from "./app.constants";

@Injectable()
export class AppEffects {
  showError$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(showError),
      tap(action => {
        console.error(action);
        if (action.error.error.message) {
          this._notificationService.error(action.error.error.message);
        } else if (action.error.message) {
          this._notificationService.error(action.error.message);
        } else {
          this._notificationService.error('@Unexpected error occurred');
        }
      })
    )
  }, {dispatch: false});

  showInfo$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(showInfo),
      tap(action => this._notificationService.info(action.message, action.interpolateParams))
    )
  }, {dispatch: false});

  showSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(showSuccess),
      tap(action => this._notificationService.success(action.message, action.interpolateParams))
    )
  }, {dispatch: false});

  navigate$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(navigate),
      tap((action) => this._router.navigate([action.url], action.extras)),
    )
  }, {dispatch: false});

  navigateToList$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(navigateToList),
      withLatestFrom(this._store.select(selectSelectedDataDomain)),
      tap(([action, selectedDD]) => {
        const currentUrl = this._router.url;
        if (selectedDD && selectedDD.name !== ALL_DATA_DOMAINS && !decodeURIComponent(currentUrl).includes(selectedDD!.name) && !decodeURIComponent(currentUrl).includes(selectedDD!.key)) {
          if (currentUrl.includes(naviElements.myDashboards.path)) {
            this._router.navigate([naviElements.myDashboards.path]);
          }
          if (currentUrl.includes(naviElements.lineageDocs.path)) {
            this._router.navigate([naviElements.lineageDocs.path + '/' + naviElements.lineageDocsList.path]);
          }
        }
      }),
    )
  }, {dispatch: false});

  constructor(
    private _store: Store<AppState>,
    private _router: Router,
    private _actions$: Actions,
    private _notificationService: NotificationService
  ) {
  }

}

export const appEffects: Array<Type<unknown> | Record<string, FunctionalEffect>> =
  [AppEffects, AuthEffects, RouterEffects, MetaInfoResourceEffects, UsersManagementEffects, PortalRolesManagementEffects, AnnouncementEffects,
    MyDashboardsEffects, MenuEffects, StartPageEffects, FaqEffects, SummaryEffects, ExternalDashboardsEffects, LineageDocsEffects, UnsavedChangesEffects]
