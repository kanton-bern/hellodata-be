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

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {combineLatest, interval, Observable, Subscription, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {filter, switchMap, take} from "rxjs/operators";
import {AsyncPipe} from '@angular/common';
import {TranslocoPipe} from "@jsverse/transloco";
import {SubsystemIframeComponent} from "../../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {AppState} from "../../../store/app/app.state";
import {OpenedSubsystemsService} from "../../../shared/services/opened-subsystems.service";
import {
  selectCurrentDashboardContextKey,
  selectCurrentDashboardId,
  selectCurrentDashboardUrl,
  selectCurrentMyDashboardInfo
} from "../../../store/my-dashboards/my-dashboards.selector";
import {selectSelectedLanguage} from "../../../store/auth/auth.selector";
import {SupersetDashboard} from "../../../store/my-dashboards/my-dashboards.model";
import {naviElements} from "../../../app-navi-elements";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {loadDashboardComments, setCurrentDashboard} from "../../../store/my-dashboards/my-dashboards.action";
import {Drawer} from "primeng/drawer";
import {CommentsFeed} from "../comments-feed/comments-feed.component";

const COMMENTS_REFRESH_INTERVAL_MS = 30000; // 30 seconds

@Component({
  selector: 'app-embed-my-dashboard-mobile',
  templateUrl: 'embed-my-dashboard-mobile.component.html',
  styleUrls: ['./embed-my-dashboard-mobile.component.scss'],
  imports: [SubsystemIframeComponent, AsyncPipe, TranslocoPipe, Drawer, CommentsFeed]
})
export class EmbedMyDashboardMobileComponent extends BaseComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly openedSupersetsService = inject(OpenedSubsystemsService);

  url!: string;
  currentMyDashboardInfo$!: Observable<any>;
  isDrawerVisible = false;
  private loadedDashboardId: number | null = null;
  private commentsRefreshSubscription: Subscription | null = null;

  constructor() {
    super();
    this.currentMyDashboardInfo$ = combineLatest([
      this.store.select(selectCurrentMyDashboardInfo),
      this.store.select(selectSelectedLanguage),
    ]).pipe(
      filter(([_dashboardInfo, selectedLanguage]) => selectedLanguage !== null),
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

  openCommentsDrawer(): void {
    this.isDrawerVisible = true;
    this.loadCommentsAndStartTimer();
  }

  onDrawerHide(): void {
    this.stopCommentsRefreshTimer();
  }

  ngOnDestroy(): void {
    this.stopCommentsRefreshTimer();
  }

  private loadCommentsAndStartTimer(): void {
    combineLatest([
      this.store.select(selectCurrentDashboardId),
      this.store.select(selectCurrentDashboardContextKey),
      this.store.select(selectCurrentDashboardUrl)
    ]).pipe(
      filter(([id, contextKey, dashboardUrl]) => id !== null && contextKey !== null && dashboardUrl !== null),
      take(1)
    ).subscribe(([dashboardId, contextKey, dashboardUrl]) => {
      if (dashboardId && contextKey && dashboardUrl) {
        // Load comments immediately
        this.store.dispatch(loadDashboardComments({dashboardId, contextKey, dashboardUrl}));

        // Start refresh timer - fetch current values from store on each tick
        this.stopCommentsRefreshTimer();
        this.commentsRefreshSubscription = interval(COMMENTS_REFRESH_INTERVAL_MS).pipe(
          switchMap(() => combineLatest([
            this.store.select(selectCurrentDashboardId),
            this.store.select(selectCurrentDashboardContextKey),
            this.store.select(selectCurrentDashboardUrl)
          ]).pipe(take(1))),
          filter(([id, key, url]) => id !== null && key !== null && url !== null)
        ).subscribe(([currentDashboardId, currentContextKey, currentDashboardUrl]) => {
          if (currentDashboardId && currentContextKey && currentDashboardUrl) {
            this.store.dispatch(loadDashboardComments({
              dashboardId: currentDashboardId,
              contextKey: currentContextKey,
              dashboardUrl: currentDashboardUrl
            }));
          }
        });
      }
    });
  }

  private stopCommentsRefreshTimer(): void {
    if (this.commentsRefreshSubscription) {
      this.commentsRefreshSubscription.unsubscribe();
      this.commentsRefreshSubscription = null;
    }
  }

  private load(dashboardInfo: any, selectedLanguage: string) {
    if (dashboardInfo.appinfo && dashboardInfo.dashboard && dashboardInfo.profile) {
      const supersetUrl = dashboardInfo.appinfo?.data.url;
      const dashboardPath = 'superset/dashboard/' + dashboardInfo.dashboard?.id + '/?standalone=1';
      const supersetLogoutUrl = supersetUrl + 'logout';
      const supersetLoginUrl = supersetUrl + `login/keycloak?lang=${selectedLanguage.slice(0, 2)}${encodeURIComponent('&')}next=${supersetUrl + dashboardPath}`;
      const defaultUrl = supersetLogoutUrl + `?redirect=${supersetLoginUrl}`;

      this.url = defaultUrl;

      this.openedSupersetsService.rememberOpenedSubsystem(supersetUrl + 'logout');
      const dataDomainName = dashboardInfo.appinfo?.businessContextInfo.subContext.name;
      this.createBreadcrumbs(dataDomainName, dashboardInfo.dashboard, dashboardInfo.currentUrl);

      // Load comments for current dashboard
      const dashboardId = dashboardInfo.dashboard.id;
      const contextKey = dashboardInfo.appinfo?.businessContextInfo.subContext.key;
      const dashboardUrl = defaultUrl;
      if (dashboardId && contextKey && this.loadedDashboardId !== dashboardId) {
        this.loadedDashboardId = dashboardId;
        this.store.dispatch(setCurrentDashboard({dashboardId, contextKey, dashboardUrl}));
      }
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

  navigateToPointerUrl(pointerUrl: string): void {
    if (pointerUrl) {
      this.url = '';
      setTimeout(() => {
        this.url = pointerUrl;
      }, 0);
    }
  }
}

