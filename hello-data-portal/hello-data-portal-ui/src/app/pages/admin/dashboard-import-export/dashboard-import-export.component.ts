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

import {Component, inject} from '@angular/core';
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
import {FileSelectEvent, FileUpload, FileUploadHandlerEvent} from "primeng/fileupload";
import {
  loadMyDashboards,
  uploadDashboards
} from "../../../store/my-dashboards/my-dashboards.action";

import {AsyncPipe} from '@angular/common';
import {TableModule} from 'primeng/table';
import {ConfirmationService, PrimeTemplate} from 'primeng/api';
import {Button} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {Tooltip} from 'primeng/tooltip';
import {Card} from 'primeng/card';
import {SilentLoginComponent} from '../../../shared/components/silent-login/silent-login.component';
import {TranslocoPipe} from '@jsverse/transloco';
import {NgArrayPipesModule} from 'ngx-pipes';
import {ICON_REGISTRY} from '../../../shared/icons';
import {Checkbox} from 'primeng/checkbox';
import {Message} from 'primeng/message';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {TranslateService} from "../../../shared/services/translate.service";
import {NotificationService} from "../../../shared/services/notification.service";
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-dashboard-import-export',
  templateUrl: './dashboard-import-export.component.html',
  styleUrl: './dashboard-import-export.component.scss',
  imports: [TableModule, PrimeTemplate, Button, Ripple, Tooltip, FileUpload, SilentLoginComponent, AsyncPipe, TranslocoPipe, NgArrayPipesModule, Card, Checkbox, Message, ConfirmDialog, FormsModule]
})
export class DashboardImportExportComponent extends BaseComponent {
  protected readonly icons = ICON_REGISTRY;
  supersetInfos$: Observable<MetaInfoResource[]>;
  dashboards$: Observable<SupersetDashboard[]>;
  availableDataDomains$: Observable<any>;
  selectedDashboardsMap = new Map<string, SupersetDashboard[]>();
  showUploadForContextMap = new Map<string, boolean>();
  pruneMap: { [key: string]: boolean } = {};
  private readonly store = inject<Store<AppState>>(Store);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    super();
    this.supersetInfos$ = this.store.select(selectAppInfoByModuleType('SUPERSET'));
    this.dashboards$ = this.store.select(selectMyDashboards);
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
    console.debug('dashboards selected?', dashboards);

    if (!dashboards || dashboards.length === 0) {
      return;
    }

    const idsString = dashboards.map(d => d.id).join(',');
    const instanceUrl = dashboards[0].instanceUrl;
    const exportApiUrl = `${instanceUrl}api/v1/dashboard/export?q=!(${idsString})`;

    // Dispatch loader start
    console.debug("Exporting dashboards from URL:", exportApiUrl);
    window.open(exportApiUrl, '_blank', 'noopener');
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

  onUpload(event: FileUploadHandlerEvent, contextKey: string, fileUpload: FileUpload) {
    const file = event.files?.[0];
    if (!file) {
      return;
    }
    const prune = !!this.pruneMap[contextKey];
    this.notificationService.info('@Dashboard upload started');
    this.store.dispatch(uploadDashboards({file, contextKey, prune}));
    fileUpload.clear();
  }

  onPruneCheckboxChange(event: any, contextKey: string) {
    if (event.checked) {
      this.confirmationService.confirm({
        message: this.translateService.translate('@Replace dashboard warning message'),
        header: this.translateService.translate('@Replace dashboard warning header'),
        icon: 'pi pi-exclamation-triangle',
        accept: () => {
          this.pruneMap[contextKey] = true;
        },
        reject: () => {
          this.pruneMap[contextKey] = false;
        }
      });
    } else {
      this.pruneMap[contextKey] = false;
    }
  }
}
