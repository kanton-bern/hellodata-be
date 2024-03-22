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

import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {debounceTime, Observable, of, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {
  selectCurrentBusinessDomain,
  selectCurrentContextRolesFilterOffNone,
  selectCurrentUserPermissions,
  selectIsAuthenticated,
  selectProfile
} from "../../store/auth/auth.selector";
import {IUser} from "../../store/auth/auth.model";
import {map} from "rxjs/operators";
import {BaseComponent} from "../../shared/components/base/base.component";
import {selectAdminEmails} from "../../store/users-management/users-management.selector";
import {loadAdminEmails} from "../../store/users-management/users-management.action";
import {resetBreadcrumb} from "../../store/breadcrumb/breadcrumb.action";
import {selectQueryParam} from "../../store/router/router.selectors";
import {navigate} from "../../store/app/app.action";
import {Router} from "@angular/router";

@Component({
  templateUrl: 'home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent extends BaseComponent implements OnInit {

  private static readonly REDIRECT_TO_PARAM = 'redirectTo';

  userData$: Observable<IUser | undefined>;
  isAuthenticated$: Observable<boolean>;
  currentUserPermissions$: Observable<any>;
  waitForPermissionsLoaded$: Observable<any>;
  loadedPermissions$ = of(false);
  businessDomain$: Observable<string>;
  adminEmails$: Observable<string[]>;
  currentUserContextRolesNotNone$: Observable<any>;
  redirectTo$: Observable<any>;

  @ViewChild('iframe') iframe!: ElementRef;

  constructor(private store: Store<AppState>, private router: Router) {
    super();
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userData$ = this.store.select(selectProfile);
    this.store.dispatch(resetBreadcrumb());
    this.store.dispatch(loadAdminEmails());
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.currentUserContextRolesNotNone$ = this.store.select(selectCurrentContextRolesFilterOffNone);
    this.waitForPermissionsLoaded$ = this.currentUserContextRolesNotNone$.pipe(
      debounceTime(700),
      tap(() => {
        this.loadedPermissions$ = of(true);
      }));

    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain);
    this.adminEmails$ = this.store.select(selectAdminEmails);
    this.redirectTo$ = this.store.select(selectQueryParam(HomeComponent.REDIRECT_TO_PARAM)).pipe(tap(param => {
      // hack to omit /auth request done twice problem (sometimes) by lib which blocks new tab fullscreen DWH viewer
      if (param) {
        sessionStorage.setItem(HomeComponent.REDIRECT_TO_PARAM, param);
      }
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
    // hack to omit /auth request done twice problem (sometimes) by lib which blocks new tab fullscreen DWH viewer
    const clearRedirectInterval = setInterval(() => {
      const redirectToParam = sessionStorage.getItem(HomeComponent.REDIRECT_TO_PARAM);
      if (redirectToParam) {
        sessionStorage.removeItem(HomeComponent.REDIRECT_TO_PARAM);
        this.store.dispatch(navigate({url: redirectToParam as string}));
      }
    }, 500);
    setTimeout(() => clearInterval(clearRedirectInterval), 2000);
  }

  hasPermissions(requiredPermissions: string[]) {
    if (requiredPermissions) {
      return this.currentUserPermissions$.pipe(
        map(currentUserPermissions => {
          return requiredPermissions.some((requiredPermission: string) => {
              return currentUserPermissions.includes(requiredPermission);
            }
          );
        }));
    }
    return of(true);
  }
}
