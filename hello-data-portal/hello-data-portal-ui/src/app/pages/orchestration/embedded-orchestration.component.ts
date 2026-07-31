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

import { Component, OnInit, inject } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {combineLatest, Observable, tap} from "rxjs";
import {naviElements} from "../../app-navi-elements";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {selectCurrentPipelineInfo} from "../../store/orchestration/orchestration.selector";
import {BaseComponent} from "../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import { AsyncPipe } from '@angular/common';
import { SubsystemIframeComponent } from '../../shared/components/subsystem-iframe/subsystem-iframe.component';
import {selectAppInfoByModuleType} from "../../store/metainfo-resource/metainfo-resource.selector";
import {TranslocoPipe} from '@jsverse/transloco';
import {Button} from 'primeng/button';

export const LOGGED_IN_AIRFLOW_USER = 'logged_in_airflow_user';

@Component({
    templateUrl: 'embedded-orchestration.component.html',
    styleUrls: ['./embedded-orchestration.component.scss'],
    imports: [SubsystemIframeComponent, AsyncPipe, TranslocoPipe, Button]
})
export class EmbeddedOrchestrationComponent extends BaseComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private store = inject<Store<AppState>>(Store);

  // Which orchestrator this embedded view targets. Airflow 2 ('AIRFLOW') and the side-by-side
  // Airflow 3 ('AIRFLOW3') coexist; the module type is provided via route data (default 'AIRFLOW').
  private readonly moduleType: string = this.route.snapshot.data['moduleType'] ?? 'AIRFLOW';

  protected readonly isAirflow3 = this.moduleType === 'AIRFLOW3';

  // Width (px) of the Airflow 3 native left nav we crop off when hidden. Must match the actual
  // sidebar width: too large clips the page content, too small leaves a sliver of the nav.
  // Tuned slightly below the measured width to avoid clipping page content.
  private readonly AIRFLOW3_SIDEBAR_WIDTH_PX = 68;

  // Airflow 3 renders its own left nav inside the (cross-origin) iframe. The portal already
  // provides navigation, so default to hiding it, with a toggle to show it on demand. The crop
  // is pure portal-side CSS on the iframe element (SubsystemIframeComponent.cropLeftPx), so
  // toggling needs no iframe reload and never touches the cross-origin iframe DOM.
  protected airflowSidebarHidden = true;

  protected get cropLeftPx(): number {
    return this.isAirflow3 && this.airflowSidebarHidden ? this.AIRFLOW3_SIDEBAR_WIDTH_PX : 0;
  }

  protected toggleAirflowSidebar(): void {
    this.airflowSidebarHidden = !this.airflowSidebarHidden;
  }

  url!: string;

  currentPipelineInfo$: Observable<any>;

  constructor() {
    super();
    this.currentPipelineInfo$ = combineLatest([
      this.store.select(selectCurrentPipelineInfo),
      this.store.select(selectAppInfoByModuleType(this.moduleType))
    ]).pipe(tap(([pipelineInfo, airflowInfos]) => {
      if (!airflowInfos || airflowInfos.length === 0) {
        return;
      }
      const pipelineId = pipelineInfo.pipelineId;
      const airflowBaseUrl = airflowInfos[0].data.url;

      if (this.moduleType === 'AIRFLOW3') {
        // Logout-first, mirroring the Superset embed. Open the FAB logout endpoint, which tears
        // down the current Airflow session AND the UI _token JWT, then redirects to /auth/login/.
        // That cookie-SSO login (webserver_config) re-runs auth_user_oauth on every open, so it
        // re-syncs FAB roles from the current portal token — portal role changes (added/removed
        // DD_<domain>) take effect the next time the orchestration is opened, and the login mints
        // a fresh _token so the UI API (/ui/config, /api/v2/*) stays authenticated.
        // NB: landing straight on /dags reuses a still-valid _token and never re-authenticates,
        // so role changes wouldn't show until that token expired. /auth/logout/ and /auth/login/
        // are server-side FAB routes (not SPA routes), so they don't 404.
        const base = airflowBaseUrl.replace(/\/+$/, '');
        const target = pipelineId ? `/dags/${pipelineId}` : '/dags';
        this.url = `${base}/auth/logout/?next=${encodeURIComponent(target)}`;
      } else {
        // Airflow 2: force a re-login as the current user via the FAB logout->login redirect.
        const airflowLogoutUrl = `${airflowBaseUrl}/logout?redirect=${airflowBaseUrl}`;
        const loggedInAirflowUser = sessionStorage.getItem(this.loggedInUserKey());
        let airflowLoginUrl;
        if (!loggedInAirflowUser || loggedInAirflowUser !== pipelineInfo.profile.email) {
          airflowLoginUrl = `${airflowLogoutUrl}/login/keycloak?next=${airflowBaseUrl}`;
        } else {
          airflowLoginUrl = `${airflowBaseUrl}/login/keycloak?next=${airflowBaseUrl}`;
        }
        this.url = pipelineId ? `${airflowLoginUrl}/dags/${pipelineId}` : `${airflowLoginUrl}/home`;
      }
      sessionStorage.setItem(this.loggedInUserKey(), pipelineInfo.profile.email);
    }));

    this.createBreadcrumbs();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  private loggedInUserKey(): string {
    return this.moduleType === 'AIRFLOW3' ? `${LOGGED_IN_AIRFLOW_USER}_airflow3` : LOGGED_IN_AIRFLOW_USER;
  }

  private createBreadcrumbs() {
    const naviElement = this.moduleType === 'AIRFLOW3'
      ? naviElements.embeddedOrchestrationAirflow3
      : naviElements.embeddedOrchestration;
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElement.label,
          routerLink: 'redirect/' + naviElement.path
        }
      ]
    }));
  }

}
