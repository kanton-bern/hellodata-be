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

import {AfterViewInit, Component, inject, ViewContainerRef} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY} from "../my-dashboards/embed-my-dashboard.component";
import {SubsystemIframeComponent} from "../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {environment} from "../../../environments/environment";
import {logout} from "../../store/auth/auth.action";
import {TranslocoPipe} from '@jsverse/transloco';

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
  styleUrls: ['./logout.component.scss'],
  imports: [TranslocoPipe]
})
export class LogoutComponent implements AfterViewInit {
  private store = inject<Store<AppState>>(Store);
  private dynamicComponentContainer = inject(ViewContainerRef);

  constructor() {
    // check if superset was opened in an iframe, if so call for logout there as well
    const openedSubsystems = sessionStorage.getItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY);
    if (openedSubsystems) {
      const storedSetArray: string[] = JSON.parse(openedSubsystems || '[]');
      storedSetArray.forEach(url => {
        const componentRefSupersetIframe = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        componentRefSupersetIframe.setInput('url', url);
      })
    }

    this.logoutAirflow();
    this.logoutFilebrowser();
  }

  private logoutFilebrowser() {
    const protocol = environment.subSystemsConfig.filebrowser.protocol;
    const host = environment.subSystemsConfig.filebrowser.host;
    const domain = environment.subSystemsConfig.filebrowser.domain;
    const filebrowserBaseUrl = protocol + host + domain;
    const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
    componentRef.setInput('url', filebrowserBaseUrl + '/web/client/logout');
  }

  private logoutAirflow() {
    const protocol = environment.subSystemsConfig.airflow.protocol;
    const host = environment.subSystemsConfig.airflow.host;
    const domain = environment.subSystemsConfig.airflow.domain;
    const airflowBaseUrl = protocol + host + domain;
    const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
    componentRef.setInput('url', airflowBaseUrl + '/logout');
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      sessionStorage.removeItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY);
      const cookieName = 'auth.access_token';
      document.cookie = cookieName + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
      this.store.dispatch(logout());
    }, 150);

  }
}
