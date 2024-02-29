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
import {FileSelectEvent} from "primeng/fileupload";
import {AuthService} from "../../../shared/services";
import {loadMyDashboards} from "../../../store/my-dashboards/my-dashboards.action";

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

  constructor(private store: Store<AppState>, private dynamicComponentContainer: ViewContainerRef, readonly authService: AuthService) {
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
    console.log('on select', $event.files)
  }

  async onFileUploadClicked($event: { files: Blob[] }, accessToken: string) {
    console.log('on upload clicked', $event.files)

    const csrfToken = await this.getCsrfToken(accessToken);

    if (!csrfToken) {
      console.error('Failed to obtain CSRF token.');
      return;
    }
    const fileUpload: Blob = $event.files[0];
    const formData = new FormData();
    formData.append('formData', fileUpload);
    // formData.append('overwrite', 'true'); // Set overwrite to true
    formData.append('passwords', '{}'); // Set passwords (if required)

    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8089/api/v1/dashboard/import/', true);
    xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`);
    xhr.setRequestHeader('X-Csrftoken', `${csrfToken}`);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader('Type', 'application/x-zip-compressed');
    xhr.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable) {
        const percentDone = Math.round((event.loaded / event.total) * 100);
        console.log(`File is ${percentDone}% uploaded.`);
      }
    });

    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          console.log('File uploaded successfully!', xhr.responseText);
          // Do something with the response if needed
        } else {
          console.error('Error occurred while uploading file:', xhr.statusText);
        }
      }
    };

    xhr.send(formData);
  }

  async getCsrfToken(bearerToken: string): Promise<string | null> {
    const headers = new Headers();
    headers.append('Authorization', `Bearer ${bearerToken}`);
    headers.append('Content-Type', 'application/json');
    const response = await fetch('http://localhost:8089/api/v1/security/csrf_token/', {
      method: 'GET',
      headers: headers
    });
    if (response.ok) {
      const data = await response.json();
      return data.csrf_token;
    } else {
      console.error('Failed to fetch CSRF token:', response.statusText);
      return null;
    }
  }

  onSelectionChange(dashboards: SupersetDashboard[], contextKey: string) {
    console.log('on selection change - context key', contextKey);
    console.log('on selection change - dashboards', dashboards);
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
    console.log('Export dashboards', contextKey)
    const dashboards = this.selectedDashboardsMap.get(contextKey);
    if (dashboards) {
      const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
      const instance = componentRef.instance;
      instance.style = {"display": 'none'};
      instance.delay = 600;
      const idsString = dashboards.map(dashboard => dashboard.id).join(',');
      console.debug('ids?', idsString)
      const instanceUrl = dashboards[0].instanceUrl;
      instance.url = `${instanceUrl}login/keycloak?next=${instanceUrl}api/v1/dashboard/export?q=!(${idsString})`;
    }
  }

  exportDashboard(dashboard: SupersetDashboard) {
    console.log(dashboard.instanceUrl);

    const componentRef = this.dynamicComponentContainer.createComponent(SubsystemIframeComponent);
    const instance = componentRef.instance;
    instance.style = {"display": 'none'};
    instance.url = `${dashboard.instanceUrl}/api/v1/dashboard/export?q=!(${dashboard.id})`;
  }

  showImport(contextKey: string) {
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
}
