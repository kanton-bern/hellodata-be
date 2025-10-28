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

import {Component, ViewContainerRef} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectAppInfoByModuleType} from "../../../store/metainfo-resource/metainfo-resource.selector";
import {Observable} from "rxjs";
import {MetaInfoResource} from "../../../store/metainfo-resource/metainfo-resource.model";
import {SubsystemIframeComponent} from "../../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {selectProfile} from "../../../store/auth/auth.selector";
import {LOGGED_IN_AIRFLOW_USER} from "../../../pages/orchestration/embedded-orchestration.component";

@Component({
  selector: 'app-silent-login',
  templateUrl: './silent-login.component.html',
  styleUrls: ['./silent-login.component.scss'],
  standalone: false
})
export class SilentLoginComponent {
  supersetInfos$: Observable<MetaInfoResource[]>;
  airflowInfos$: Observable<MetaInfoResource[]>;
  supersetsLoggedIn = false;
  airflowsLoggedIn = false;
  profile$: Observable<any>;

  constructor(private store: Store<AppState>, private dynamicComponentContainer: ViewContainerRef) {
    this.supersetInfos$ = this.store.select(selectAppInfoByModuleType('SUPERSET'));
    this.airflowInfos$ = this.store.select(selectAppInfoByModuleType('AIRFLOW'));
    this.profile$ = this.store.select(selectProfile);
  }

  loginSupersets(supersetInfos: MetaInfoResource[]) {
    if (!this.supersetsLoggedIn) {
      supersetInfos.forEach(supersetInfo => {
        const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        const instance = componentRef.instance;
        instance.switchStyleOverflow = false;

        const supersetLogoutUrl = supersetInfo.data.url + 'logout';
        const supersetLoginUrl = supersetInfo.data.url + `login/keycloak`;

        instance.url = supersetLogoutUrl + `?redirect=${supersetLoginUrl}`;
        this.supersetsLoggedIn = true;
      });
    }
    return 'supersets-logged-in';
  }

  loginAirflows(airflowInfos: MetaInfoResource[], email: any) {
    if (!this.airflowsLoggedIn) {
      airflowInfos.forEach(airflowInfo => {
        const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        const instance = componentRef.instance;
        instance.switchStyleOverflow = false;

        const airflowLogoutUrl = airflowInfo.data.url + 'logout';
        const airflowLoginUrl = airflowInfo.data.url + `login/keycloak`;

        instance.url = airflowLogoutUrl + `?redirect=${airflowLoginUrl}`;
        sessionStorage.setItem(LOGGED_IN_AIRFLOW_USER, email);
        this.airflowsLoggedIn = true;
      });
    }
    return 'airflows-logged-in';
  }
}
