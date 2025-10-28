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

import {Component, OnInit, ViewChild} from "@angular/core";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {AppState} from "../../../store/app/app.state";
import {Store} from "@ngrx/store";
import {combineLatest, Observable, tap} from "rxjs";
import {DashboardAccess} from "../../../store/dashboard-access/dashboard-access.model";
import {
  selectAllDashboardAccess,
  selectDashboardAccessDataLoading,
  selectDashboardAccessTotalRecords
} from "../../../store/dashboard-access/dashboard-access.selector";
import {Table, TableLazyLoadEvent} from "primeng/table";
import {loadDashboardAccessPaginated} from "../../../store/dashboard-access/dashboard-access.action";
import {selectSelectedDataDomain} from "../../../store/my-dashboards/my-dashboards.selector";
import {scrollToTop} from "../../../shared/services/view-helpers";
import {naviElements} from "../../../app-navi-elements";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {DataDomain} from "../../../store/my-dashboards/my-dashboards.model";
import {map} from "rxjs/operators";

@Component({
  templateUrl: 'dashboard-access.component.html',
  styleUrls: ['./dashboard-access.component.scss'],
  standalone: false
})
export class DashboardAccessComponent extends BaseComponent implements OnInit {
  dashboardAccess$: Observable<DashboardAccess[]>;
  selectedDataDomain$: Observable<DataDomain | null>;
  filterValue = '';
  first = 0;
  @ViewChild('dt') table!: Table;
  totalRecords = 0;
  dataLoading$ = this.store.select(selectDashboardAccessDataLoading);
  usedContextKey: string | null = null;

  constructor(private store: Store<AppState>) {
    super();
    this.createBreadcrumbs();
    this.dashboardAccess$ = combineLatest([
      this.store.select(selectAllDashboardAccess),
      this.store.select(selectDashboardAccessTotalRecords)
    ]).pipe(
      tap(([_, totalRecords]) => {
        this.totalRecords = totalRecords;
      }),
      map(([dashboardAccessList, _]) => dashboardAccessList),
    );
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain).pipe(
      tap((dataDomain) => {
        if (this.table && this.usedContextKey !== dataDomain?.key) {
          const contextKey = dataDomain?.key ? dataDomain?.key : null;
          const sortField = this.table.sortField;
          const sortOrder = this.table.sortOrder > 0 ? 'asc' : 'desc';
          const pageSize = this.table.rows as number;
          this.store.dispatch(loadDashboardAccessPaginated({
            page: 0, size: pageSize, sort: `${sortField}, ${sortOrder}`, search: '', contextKey
          }));
          this.usedContextKey = contextKey;
        }
      })
    );
  }

  loadDashboardAccess(event: TableLazyLoadEvent, contextKey: string) {
    this.store.dispatch(loadDashboardAccessPaginated({
      page: event.first as number / (event.rows as number),
      size: event.rows as number,
      sort: event.sortField ? `${event.sortField}, ${event.sortOrder ? event.sortOrder > 0 ? 'asc' : 'desc' : ''}` : '',
      search: event.globalFilter ? event.globalFilter as string : '',
      contextKey
    }));
    this.usedContextKey = contextKey;
    scrollToTop();
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardAccess.label,
        }
      ]
    }));
  }
}
