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

import {Component, OnInit, ViewChild} from '@angular/core';
import {Observable} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {ConfirmationService} from "primeng/api";
import {TranslateService} from "../../../shared/services/translate.service";
import {selectCurrentUserPermissions} from "../../../store/auth/auth.selector";
import {ExternalDashboard} from "../../../store/external-dashboards/external-dashboards.model";
import {selectExternalDashboards} from "../../../store/external-dashboards/external-dashboards.selector";
import {DeleteExternalDashboard, LoadExternalDashboards, OpenExternalDashboardEdition} from "../../../store/external-dashboards/external-dasboards.action";
import {Table} from "primeng/table";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";

@Component({
  selector: 'app-external-dashboards',
  templateUrl: './external-dashboards.component.html',
  styleUrls: ['./external-dashboards.component.scss']
})
export class ExternalDashboardsComponent extends BaseComponent implements OnInit {
  @ViewChild('dt') dt!: Table | undefined;
  externalDashboards$: Observable<ExternalDashboard[]>;
  currentUserPermissions$: Observable<string[]>;

  constructor(private route: ActivatedRoute, private store: Store<AppState>, private confirmationService: ConfirmationService,
              private translateService: TranslateService) {
    super();
    this.externalDashboards$ = this.store.select(selectExternalDashboards);
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.externalDashboards.label,
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.store.dispatch(new LoadExternalDashboards());
  }

  openNew() {
    this.store.dispatch(new OpenExternalDashboardEdition());
  }

  editExternalDashboard(externalDashboard: ExternalDashboard) {
    this.store.dispatch(new OpenExternalDashboardEdition(externalDashboard));
  }

  deleteExternalDashboard(externalDashboard: ExternalDashboard) {
    this.confirmationService.confirm({
      message: this.translateService.translate('@Delete external dashboard question'),
      header: 'Confirm',
      icon: 'fas fa-triangle-exclamation',
      accept: () => {
        this.store.dispatch(new DeleteExternalDashboard(externalDashboard));
      }
    });
  }

  createExternalUrl(url: string): string {
    if (url) {
      if (url.startsWith('http')) {
        return url;
      }
      return 'https://' + url;
    }
    return url;
  }

  applyFilterGlobal($event: any, stringVal: string) {
    if (this.dt) {
      this.dt.filterGlobal(($event.target as HTMLInputElement).value, stringVal);
    }
  }

}
