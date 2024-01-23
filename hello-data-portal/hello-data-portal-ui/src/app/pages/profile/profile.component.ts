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

import {Component} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {selectCurrentContextRoles, selectProfile} from "../../store/auth/auth.selector";
import {Observable} from "rxjs";
import {BUSINESS_DOMAIN_CONTEXT_TYPE, DATA_DOMAIN_CONTEXT_TYPE} from "../../store/users-management/users-management.model";
import {naviElements} from "../../app-navi-elements";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";

@Component({
  templateUrl: 'profile.component.html',
  styleUrls: ['./profile.component.scss']
})

export class ProfileComponent {
  userDetails$: Observable<any>;
  userContextRoles$: Observable<any[]>;
  protected readonly BUSINESS_DOMAIN_CONTEXT_TYPE = BUSINESS_DOMAIN_CONTEXT_TYPE;
  protected readonly DATA_DOMAIN_CONTEXT_TYPE = DATA_DOMAIN_CONTEXT_TYPE;

  constructor(private store: Store<AppState>) {
    this.userDetails$ = this.store.select(selectProfile);
    this.userContextRoles$ = this.store.select(selectCurrentContextRoles);
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.profile.label,
          routerLink: naviElements.profile.path
        }
      ]
    }));
  }

}
