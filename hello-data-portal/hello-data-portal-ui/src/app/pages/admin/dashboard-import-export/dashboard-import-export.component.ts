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
import {Observable} from "rxjs";
import {MetaInfoResource} from "../../../store/metainfo-resource/metainfo-resource.model";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectAppInfoByModuleType} from "../../../store/metainfo-resource/metainfo-resource.selector";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {SupersetDashboard} from "../../../store/my-dashboards/my-dashboards.model";
import {selectAvailableDataDomainItems, selectMyDashboards} from "../../../store/my-dashboards/my-dashboards.selector";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {SubsystemIframeComponent} from "../../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {FileSelectEvent, FileUploadErrorEvent, FileUploadEvent} from "primeng/fileupload";
import {
  loadMyDashboards,
  uploadDashboardsError,
  uploadDashboardsSuccess
} from "../../../store/my-dashboards/my-dashboards.action";
import {environment} from "../../../../environments/environment";
import {showInfo} from "../../../store/app/app.action";

@Component({
  selector: 'app-dashboard-import-export',
  templateUrl: './dashboard-import-export.component.html',
  styleUrl: './dashboard-import-export.component.scss'
})
export class DashboardImportExportComponent extends BaseComponent {
  supersetInfos$: Observable<MetaInfoResource[]>;
  dashboards$: Observable<SupersetDashboard[]>;
  availableDataDomains$: Observable<any>;

  selectedDashboardsMap = new Map<string, SupersetDashboard[]>();
  showUploadForContextMap = new Map<string, boolean>();

  baseUrl = `${environment.portalApi}/superset/upload-dashboards/`;

  constructor(private store: Store<AppState>, private dynamicComponentContainer: ViewContainerRef) {
    super();
    this.supersetInfos$ = this.store.select(selectAppInfoByModuleType('SUPERSET'));
    this.dashboards$ = this.store.select(selectMyDashboards);
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomainItems);
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardCopy.label,
          routerLink: naviElements.dashboardCopy.path
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.store.dispatch(loadMyDashboards());
  }

  filterDashboardsByContext(dashboards: SupersetDashboard[], contextKey: string): SupersetDashboard[] {
    return dashboards.filter(dashboard => dashboard.contextKey === contextKey);
  }

  onSelect($event: FileSelectEvent) {
    console.debug('on dashboard select', $event.files)
  }

  onSelectionChange(dashboards: SupersetDashboard[], contextKey: string) {
    console.debug('on selection change - context key', contextKey);
    console.debug('on selection change - dashboards', dashboards);
    this.selectedDashboardsMap.set(contextKey, dashboards);
  }

  getSelectedDashboards(contextKey: string): SupersetDashboard[] {
    const dashboards = this.selectedDashboardsMap.get(contextKey);
    if (dashboards) {
      return dashboards;
    }
    return [];
  }

  exportDashboards(contextKey: string) {
    console.debug('Export dashboards', contextKey)
    const dashboards = this.selectedDashboardsMap.get(contextKey);
    console.debug('dashboards selected?', dashboards)
    if (dashboards) {
      const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
      const instance = componentRef.instance;
      instance.style = {"display": 'none', 'visibility': 'hidden'};
      instance.delay = 600;
      const idsString = dashboards.map(dashboard => dashboard.id).join(',');
      console.debug('ids?', idsString);
      const instanceUrl = dashboards[0].instanceUrl;
      instance.url = `${instanceUrl}login/keycloak?next=${instanceUrl}api/v1/dashboard/export?q=!(${idsString})`;
      this.store.dispatch(showInfo({message: '@Dashboard export started'}));
    }
  }

  toggleImportVisible(contextKey: string) {
    const visible = this.showUploadForContextMap.get(contextKey);
    if (visible) {
      this.showUploadForContextMap.set(contextKey, false);
    } else {
      this.showUploadForContextMap.set(contextKey, true);
    }
  }

  uploadVisible(contextKey: string): boolean | undefined {
    return this.showUploadForContextMap.get(contextKey);
  }

  onUploadCompleted($event: FileUploadEvent) {
    console.debug("upload completed", $event);
    this.store.dispatch(uploadDashboardsSuccess());
  }

  onError($event: FileUploadErrorEvent) {
    this.store.dispatch(uploadDashboardsError({error: $event.error}));
  }
}
