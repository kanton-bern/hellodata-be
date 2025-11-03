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
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {Observable} from "rxjs";
import {SupersetDashboard} from "../../../store/my-dashboards/my-dashboards.model";
import {MenuService} from "../../../store/menu/menu.service";
import {SupersetDashboardWithMetadata} from "../../../store/start-page/start-page.model";
import {selectMyDashboards} from "../../../store/my-dashboards/my-dashboards.selector";
import { Table, TablePageEvent, TableModule } from "primeng/table";
import {navigate, trackEvent} from "../../../store/app/app.action";
import { NgIf, AsyncPipe } from '@angular/common';
import { PrimeTemplate } from 'primeng/api';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
    selector: 'app-dashboards',
    templateUrl: './dashboards.component.html',
    styleUrls: ['./dashboards.component.scss'],
    imports: [NgIf, TableModule, PrimeTemplate, IconField, InputIcon, FormsModule, InputText, AsyncPipe, TranslocoPipe]
})
export class DashboardsComponent implements OnInit {
  dashboards$: Observable<SupersetDashboard[]>;
  filterValue = '';
  @ViewChild('dt') dt!: Table | undefined;
  private filterTimer: any;

  constructor(private store: Store<AppState>,
              private menuService: MenuService) {
    this.dashboards$ = this.store.select(selectMyDashboards);
  }

  ngOnInit(): void {
    this.restoreDashboardSearchFilter();
  }

  private restoreDashboardSearchFilter() {
    const storageItem = sessionStorage.getItem("home-dashboards-table");
    if (storageItem) {
      const storageItemObject = JSON.parse(storageItem);
      const filterValue = storageItemObject.filters?.global?.value;
      if (filterValue) {
        this.filterValue = filterValue;
      }
    }
  }

  createLink(dashboard: SupersetDashboardWithMetadata): string {
    return this.menuService.createDashboardLink(dashboard);
  }

  navigateToDashboard(dash: SupersetDashboardWithMetadata) {
    // Track first
    this.store.dispatch(trackEvent({
      eventCategory: 'Dashboard',
      eventAction: `[Click] - ${dash.dashboardTitle} [${dash.contextName}]`
    }));

    // Navigate after a small delay to ensure Matomo sends the event
    setTimeout(() => {
      this.store.dispatch(navigate({url: this.createLink(dash)}));
    }, 50);
  }

  onPageChange($event: TablePageEvent) {
    const pageIndex = $event.first / $event.rows;   // 0-based
    const pageNumber = pageIndex + 1;

    this.store.dispatch(trackEvent({
      eventCategory: 'Dashboard (Home Page)',
      eventAction: '[Click Paging] - Moved to page ' + pageNumber
    }));
  }

  onFilter(event: Event, table: Table) {
    clearTimeout(this.filterTimer);
    const input = event.target as HTMLInputElement;
    const value = input.value;
    table.filterGlobal(value, 'contains');
    // debounce
    this.filterTimer = setTimeout(() => {
      table.filterGlobal(value, 'contains');
      const val = value || '(cleared)';
      this.store.dispatch(trackEvent({
        eventCategory: 'Dashboard (Home Page)',
        eventAction: '[Search] - Searched for ' + val
      }));
    }, 400);
  }
}
