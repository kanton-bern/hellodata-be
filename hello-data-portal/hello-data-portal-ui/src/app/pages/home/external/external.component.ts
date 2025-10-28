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
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {Table, TablePageEvent} from "primeng/table";
import {ExternalDashboard} from "../../../store/external-dashboards/external-dashboards.model";
import {selectExternalDashboards} from "../../../store/external-dashboards/external-dashboards.selector";
import {selectCurrentUserPermissions} from "../../../store/auth/auth.selector";
import {loadExternalDashboards} from "../../../store/external-dashboards/external-dasboards.action";
import {trackEvent} from "../../../store/app/app.action";

@Component({
  selector: 'app-external',
  templateUrl: './external.component.html',
  styleUrls: ['./external.component.scss'],
  standalone: false
})
export class ExternalComponent implements OnInit {
  @ViewChild('dt') dt!: Table | undefined;
  externalDashboards$: Observable<ExternalDashboard[]>;
  currentUserPermissions$: Observable<string[]>;
  filterValue = '';
  private filterTimer: any;

  constructor(private store: Store<AppState>) {
    this.externalDashboards$ = this.store.select(selectExternalDashboards);
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
  }

  ngOnInit(): void {
    this.store.dispatch(loadExternalDashboards());
    this.restoreExternalDashboardSearchFilter();
  }

  private restoreExternalDashboardSearchFilter() {
    const storageItem = sessionStorage.getItem("home-external-dashboards-table");
    if (storageItem) {
      const storageItemObject = JSON.parse(storageItem);
      const filterValue = storageItemObject.filters?.global?.value;
      if (filterValue) {
        this.filterValue = filterValue;
      }
    }
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

  onPageChange($event: TablePageEvent) {
    const pageIndex = $event.first / $event.rows;   // 0-based
    const pageNumber = pageIndex + 1;

    this.store.dispatch(trackEvent({
      eventCategory: 'External Dashboard (Home Page)',
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
      console.debug('Global filter:', value);
      this.store.dispatch(trackEvent({
        eventCategory: 'External Dashboard (Home Page)',
        eventAction: '[Search] - Searched for ' + val
      }));
    }, 400);
  }
}
