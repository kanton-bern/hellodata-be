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
import {debounceTime, Observable, of} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {
  selectCurrentBusinessDomain,
  selectCurrentContextRolesFilterOffNone,
  selectCurrentUserPermissions,
  selectCurrentUserPermissionsLoaded,
  selectIsAuthenticated,
  selectProfile
} from "../../store/auth/auth.selector";
import {IUser} from "../../store/auth/auth.model";
import {map} from "rxjs/operators";
import {BaseComponent} from "../../shared/components/base/base.component";
import {selectAdminEmails} from "../../store/users-management/users-management.selector";
import {loadAdminEmails} from "../../store/users-management/users-management.action";
import {resetBreadcrumb} from "../../store/breadcrumb/breadcrumb.action";

@Component({
  templateUrl: 'home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent extends BaseComponent implements OnInit {

  userData$: Observable<IUser | undefined>;
  isAuthenticated$: Observable<boolean>;
  currentUserPermissions$: Observable<any>;
  loadedPermissions$: Observable<boolean>;
  businessDomain$: Observable<string>;
  adminEmails$: Observable<string[]>;
  currentUserContextRolesNotNone$: Observable<any>;

  @ViewChild('iframe') iframe!: ElementRef;

  constructor(private store: Store<AppState>) {
    super();
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userData$ = this.store.select(selectProfile);
    this.store.dispatch(resetBreadcrumb());
    this.store.dispatch(loadAdminEmails());
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.loadedPermissions$ = this.store.select(selectCurrentUserPermissionsLoaded);
    this.currentUserContextRolesNotNone$ = this.store.select(selectCurrentContextRolesFilterOffNone).pipe(debounceTime(100));
    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain);
    this.adminEmails$ = this.store.select(selectAdminEmails);
  }

  override ngOnInit(): void {
    super.ngOnInit();
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
