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

export const LOGGED_IN_AIRFLOW_USER = 'logged_in_airflow_user';

@Component({
    templateUrl: 'embedded-orchestration.component.html',
    styleUrls: ['./embedded-orchestration.component.scss'],
    imports: [SubsystemIframeComponent, AsyncPipe]
})
export class EmbeddedOrchestrationComponent extends BaseComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private store = inject<Store<AppState>>(Store);

  // Which orchestrator this embedded view targets. Airflow 2 ('AIRFLOW') and the side-by-side
  // Airflow 3 ('AIRFLOW3') coexist; the module type is provided via route data (default 'AIRFLOW').
  private readonly moduleType: string = this.route.snapshot.data['moduleType'] ?? 'AIRFLOW';

  url!: string;

  currentPipelineInfo$: Observable<any>;

  constructor() {
    super();
    // Airflow 3 dropped the legacy '/home' landing page; land on the app root instead.
    const homePath = this.moduleType === 'AIRFLOW3' ? '/' : '/home';
    this.currentPipelineInfo$ = combineLatest([
      this.store.select(selectCurrentPipelineInfo),
      this.store.select(selectAppInfoByModuleType(this.moduleType))
    ]).pipe(tap(([pipelineInfo, airflowInfos]) => {
      if (!airflowInfos || airflowInfos.length === 0) {
        return;
      }
      const pipelineId = pipelineInfo.pipelineId;
      const airflowBaseUrl = airflowInfos[0].data.url;
      const airflowLogoutUrl = `${airflowBaseUrl}/logout?redirect=${airflowBaseUrl}`;
      const loggedInAirflowUser = sessionStorage.getItem(this.loggedInUserKey());
      let airflowLoginUrl;
      if (!loggedInAirflowUser || loggedInAirflowUser !== pipelineInfo.profile.email) {
        airflowLoginUrl = `${airflowLogoutUrl}/login/keycloak?next=${airflowBaseUrl}`;
      } else {
        airflowLoginUrl = `${airflowBaseUrl}/login/keycloak?next=${airflowBaseUrl}`;
      }
      if (pipelineId) {
        this.url = `${airflowLoginUrl}/dags/${pipelineId}`;
      } else {
        this.url = `${airflowLoginUrl}${homePath}`;
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
