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

import {Component, OnInit} from '@angular/core';
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {CreateBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {selectCurrentMyDashboardInfo} from "../../store/my-dashboards/my-dashboards.selector";
import {SupersetDashboard} from "../../store/my-dashboards/my-dashboards.model";
import {naviElements} from "../../app-navi-elements";
import {BaseComponent} from "../../shared/components/base/base.component";

export const VISITED_SUPERSETS_SESSION_STORAGE_KEY = 'visited_supersets';
export const LOGGED_IN_SUPERSET_USER = 'logged_in_superset_user';

@Component({
  templateUrl: 'embed-my-dashboard.component.html',
  styleUrls: ['./embed-my-dashboard.component.scss']
})
export class EmbedMyDashboardComponent extends BaseComponent implements OnInit {
  url!: string;
  currentMyDashboardInfo$!: Observable<any>;

  constructor(private store: Store<AppState>) {
    super();
    this.currentMyDashboardInfo$ = this.store.select(selectCurrentMyDashboardInfo).pipe(tap((dashboardInfo) => {
      if (dashboardInfo) {
        this.load(dashboardInfo);
      }
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  private load(dashboardInfo: any) {
    if (dashboardInfo.appinfo && dashboardInfo.dashboard && dashboardInfo.profile) {
      const supersetUrl = dashboardInfo.appinfo?.data.url;
      const sessionStorageKey = LOGGED_IN_SUPERSET_USER + '_[' + supersetUrl + ']';
      const dashboardPath = 'superset/dashboard/' + dashboardInfo.dashboard?.id + '/?standalone=1';
      const loggedInSupersetUser = sessionStorage.getItem(sessionStorageKey);
      if (!loggedInSupersetUser || loggedInSupersetUser !== dashboardInfo.profile.email) {
        const supersetLogoutUrl = supersetUrl + 'logout';
        const supersetLoginUrl = supersetUrl + `login/keycloak?next=${supersetUrl + dashboardPath}`;
        this.url = supersetLogoutUrl + `?redirect=${supersetLoginUrl}`;
      } else {
        this.url = supersetUrl + dashboardPath;
      }

      this.rememberOpenedSuperset(supersetUrl);
      sessionStorage.setItem(sessionStorageKey, dashboardInfo.profile.email);
      const dataDomainName = dashboardInfo.appinfo?.businessContextInfo.subContext.name;
      this.createBreadcrumbs(dataDomainName, dashboardInfo.dashboard, dashboardInfo.currentUrl);
    }
  }

  private createBreadcrumbs(dataDomainName: string, dashboard: SupersetDashboard | undefined, currentUrl: string) {
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.myDashboards.label,
        routerLink: naviElements.myDashboards.path
      },
      {
        label: dataDomainName,
      },
      {
        label: dashboard?.dashboardTitle,
        routerLink: decodeURIComponent(currentUrl)
      }
    ]));
  }

  private rememberOpenedSuperset(supersetUrl: string) {
    const openedSupersets = sessionStorage.getItem(VISITED_SUPERSETS_SESSION_STORAGE_KEY);
    if (openedSupersets) {
      const storedSetArray: string[] = JSON.parse(openedSupersets || '[]');
      storedSetArray.push(supersetUrl);
      // Convert the array back into a set to have unique urls
      const storedSet: Set<string> = new Set<string>(storedSetArray);
      const setArray: string[] = Array.from(storedSet);
      const setString: string = JSON.stringify(setArray);
      sessionStorage.setItem(VISITED_SUPERSETS_SESSION_STORAGE_KEY, setString);
    } else {
      const mySet: Set<string> = new Set<string>([supersetUrl]);
      const setArray: string[] = Array.from(mySet);
      const setString: string = JSON.stringify(setArray);
      sessionStorage.setItem(VISITED_SUPERSETS_SESSION_STORAGE_KEY, setString);
    }
  }

}
