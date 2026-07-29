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
import {VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY} from "../my-dashboards/embed-my-dashboard/embed-my-dashboard.component";
import {SubsystemIframeComponent} from "../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {logout} from "../../store/auth/auth.action";
import {TranslocoPipe} from '@jsverse/transloco';
import {selectAppInfoByModuleType} from "../../store/metainfo-resource/metainfo-resource.selector";
import {take} from "rxjs";

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
  styleUrls: ['./logout.component.scss'],
  imports: [TranslocoPipe]
})
export class LogoutComponent implements AfterViewInit {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly dynamicComponentContainer = inject(ViewContainerRef);

  constructor() {
    // check if superset was opened in an iframe, if so call for logout there as well
    const openedSubsystems = sessionStorage.getItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY);
    if (openedSubsystems) {
      const storedSetArray: string[] = JSON.parse(openedSubsystems);
      storedSetArray.forEach(url => {
        const componentRefSupersetIframe = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        componentRefSupersetIframe.setInput('url', url);
      })
    }

    this.logoutAirflow();
    this.logoutAirflow3();
    this.logoutFilebrowser();
  }

  private logoutFilebrowser() {
    this.store.select(selectAppInfoByModuleType('SFTPGO')).pipe(take(1)).subscribe(sftpgoInfos => {
      if (sftpgoInfos.length > 0) {
        const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        componentRef.setInput('url', sftpgoInfos[0].data.url + '/web/client/logout');
      }
    });
  }

  private logoutAirflow() {
    this.store.select(selectAppInfoByModuleType('AIRFLOW')).pipe(take(1)).subscribe(airflowInfos => {
      if (airflowInfos.length > 0) {
        const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        componentRef.setInput('url', airflowInfos[0].data.url + '/logout');
      }
    });
  }

  private logoutAirflow3() {
    this.store.select(selectAppInfoByModuleType('AIRFLOW3')).pipe(take(1)).subscribe(airflow3Infos => {
      if (airflow3Infos.length > 0) {
        const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
        componentRef.setInput('url', airflow3Infos[0].data.url + '/logout');
      }
    });
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
