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

import {Component, inject, OnInit} from '@angular/core';
import {combineLatest, Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {selectCurrentMyDashboardInfo} from "../../store/my-dashboards/my-dashboards.selector";
import {SupersetDashboard} from "../../store/my-dashboards/my-dashboards.model";
import {naviElements} from "../../app-navi-elements";
import {BaseComponent} from "../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {OpenedSubsystemsService} from "../../shared/services/opened-subsystems.service";
import {selectSelectedLanguage} from "../../store/auth/auth.selector";
import {filter} from "rxjs/operators";
import {AsyncPipe} from '@angular/common';
import {SubsystemIframeComponent} from '../../shared/components/subsystem-iframe/subsystem-iframe.component';

export const VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY = 'visited_subsystems';

@Component({
  templateUrl: 'embed-my-dashboard.component.html',
  styleUrls: ['./embed-my-dashboard.component.scss'],
  imports: [SubsystemIframeComponent, AsyncPipe]
})
export class EmbedMyDashboardComponent extends BaseComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly openedSupersetsService = inject(OpenedSubsystemsService);

  url!: string;
  currentMyDashboardInfo$!: Observable<any>;
  isCommentsOpen = false;

  constructor() {
    super();
    this.currentMyDashboardInfo$ = combineLatest([
      this.store.select(selectCurrentMyDashboardInfo),
      this.store.select(selectSelectedLanguage),
    ]).pipe(
      filter(([dashboardInfo, selectedLanguage]) => selectedLanguage !== null),
      tap(([dashboardInfo, selectedLanguage]) => {
        if (dashboardInfo) {
          this.load(dashboardInfo, selectedLanguage.code as string);
        }
      }),
    )
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  toggleComments(): void {
    this.isCommentsOpen = !this.isCommentsOpen;
  }

  private load(dashboardInfo: any, selectedLanguage: string) {
    if (dashboardInfo.appinfo && dashboardInfo.dashboard && dashboardInfo.profile) {
      const supersetUrl = dashboardInfo.appinfo?.data.url;
      const dashboardPath = 'superset/dashboard/' + dashboardInfo.dashboard?.id + '/?standalone=1';
      const supersetLogoutUrl = supersetUrl + 'logout';
      const supersetLoginUrl = supersetUrl + `login/keycloak?lang=${selectedLanguage.slice(0, 2)}${encodeURIComponent('&')}next=${supersetUrl + dashboardPath}`;
      this.url = supersetLogoutUrl + `?redirect=${supersetLoginUrl}`;

      this.openedSupersetsService.rememberOpenedSubsystem(supersetUrl + 'logout');
      const dataDomainName = dashboardInfo.appinfo?.businessContextInfo.subContext.name;
      this.createBreadcrumbs(dataDomainName, dashboardInfo.dashboard, dashboardInfo.currentUrl);
    }
  }

  private createBreadcrumbs(dataDomainName: string, dashboard: SupersetDashboard | undefined, currentUrl: string) {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.myDashboards.label,
          routerLink: naviElements.myDashboards.path
        },
        {
          label: dataDomainName,
          routerLink: naviElements.myDashboards.path,
          queryParams: {
            filteredBy: dashboard?.contextId
          }
        },
        {
          label: dashboard?.dashboardTitle,
          routerLink: decodeURIComponent(currentUrl)
        }
      ]
    }));
  }

}
