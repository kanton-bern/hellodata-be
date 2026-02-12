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

import {Actions, createEffect, ofType} from "@ngrx/effects";
import {ROUTER_NAVIGATED, ROUTER_NAVIGATION, RouterNavigationAction} from "@ngrx/router-store";
import {asyncScheduler, catchError, EMPTY, scheduled, switchMap} from "rxjs";
import {inject, Injectable} from "@angular/core";
import {loadAnnouncementById} from "../announcement/announcement.action";
import {showError} from "../app/app.action";
import {loadExternalDashboardById} from "../external-dashboards/external-dasboards.action";
import {loadFaqById} from "../faq/faq.action";
import {loadPortalRoleById} from "../portal-roles-management/portal-roles-management.action";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {loadDashboardGroupById} from "../dashboard-groups/dashboard-groups.action";

@Injectable()
export class RouterEffects {
  private readonly _actions$ = inject(Actions);


  openEditionFinished$ = createEffect(() => {
    return this._actions$.pipe(
      ofType<RouterNavigationAction>(ROUTER_NAVIGATION),
      switchMap(action => {
        const urlParts = action.payload.routerState.url.split('/');
        if (urlParts.length === 4 && urlParts[1] === 'roles-management' && urlParts[2] === 'edit') {
          return scheduled([loadPortalRoleById()], asyncScheduler);
        }
        if (urlParts.length === 4 && urlParts[1] === 'announcements-management' && urlParts[2] === 'edit') {
          return scheduled([loadAnnouncementById()], asyncScheduler);
        }
        if (urlParts.length === 4 && urlParts[1] === 'faq-management' && urlParts[2] === 'edit') {
          return scheduled([loadFaqById()], asyncScheduler);
        }
        if (urlParts.length === 4 && urlParts[1] === 'external-dashboards' && urlParts[2] === 'edit') {
          return scheduled([loadExternalDashboardById()], asyncScheduler);
        }
        if (urlParts.length === 4 && urlParts[1] === 'dashboard-groups' && urlParts[2] === 'edit') {
          return scheduled([loadDashboardGroupById()], asyncScheduler);
        }
        return EMPTY;
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });

  navigated$ = createEffect(() => {
    return this._actions$.pipe(
      ofType<RouterNavigationAction>(ROUTER_NAVIGATED),
      switchMap(action => {
        return scheduled([clearUnsavedChanges()], asyncScheduler);
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  })

}

