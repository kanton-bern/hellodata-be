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
import {NotificationService} from "../../shared/services/notification.service";
import {Store} from '@ngrx/store';
import {AppState} from "../app/app.state";
import {Router} from "@angular/router";
import {MenuService} from "./menu.service";
import {catchError, of, switchMap} from "rxjs";
import {MenuActionType, ProcessNavigation, ProcessNavigationSuccess} from "./menu.action";
import {ShowError} from "../app/app.action";

@Injectable()
export class MenuEffects {

  processNavigation$ = createEffect(() => this._actions$.pipe(
    ofType<ProcessNavigation>(MenuActionType.PROCESS_NAVIGATION),
    switchMap((action) =>
      this._menuService.processNavigation(action.compactMode)),
    switchMap(result => of(new ProcessNavigationSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  constructor(
    private _router: Router,
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _menuService: MenuService,
    private _notificationService: NotificationService
  ) {
  }
}
