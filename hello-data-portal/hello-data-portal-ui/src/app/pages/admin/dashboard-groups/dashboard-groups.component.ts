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

import {Component, inject, OnInit} from '@angular/core';
import {combineLatest, map, Observable, tap} from 'rxjs';
import {Store} from '@ngrx/store';
import {AppState} from '../../../store/app/app.state';
import {DashboardGroup, DashboardGroupEntry} from '../../../store/dashboard-groups/dashboard-groups.model';
import {
  selectDashboardGroups,
  selectDashboardGroupsLoading,
  selectDashboardGroupsTotalElements
} from '../../../store/dashboard-groups/dashboard-groups.selector';
import {selectAllAvailableDataDomains} from '../../../store/my-dashboards/my-dashboards.selector';
import {
  deleteDashboardGroup,
  loadDashboardGroups,
  openDashboardGroupEdition,
  showDeleteDashboardGroupPopup
} from '../../../store/dashboard-groups/dashboard-groups.action';
import {naviElements} from '../../../app-navi-elements';
import {BaseComponent} from '../../../shared/components/base/base.component';
import {createBreadcrumbs} from '../../../store/breadcrumb/breadcrumb.action';
import {AsyncPipe, DatePipe, KeyValuePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {PrimeTemplate} from 'primeng/api';
import {Button} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {TranslocoPipe} from '@jsverse/transloco';
import {Badge} from 'primeng/badge';
import {
  DeleteDashboardGroupPopupComponent
} from './delete-dashboard-group-popup/delete-dashboard-group-popup.component';

@Component({
  selector: 'app-dashboard-groups',
  templateUrl: './dashboard-groups.component.html',
  styleUrls: ['./dashboard-groups.component.scss'],
  imports: [FormsModule, PrimeTemplate, Button, Ripple, TableModule, Tooltip, InputText, IconField, InputIcon,
    Badge, DeleteDashboardGroupPopupComponent, AsyncPipe, DatePipe, KeyValuePipe, TranslocoPipe]
})
export class DashboardGroupsComponent extends BaseComponent implements OnInit {
  dashboardGroups$: Observable<DashboardGroup[]>;
  loading$: Observable<boolean>;
  totalRecords = 0;
  filterValue = '';
  domainNameMap: Map<string, string> = new Map();
  private readonly store = inject<Store<AppState>>(Store);

  constructor() {
    super();
    this.dashboardGroups$ = combineLatest([
      this.store.select(selectDashboardGroups),
      this.store.select(selectDashboardGroupsTotalElements)
    ]).pipe(
      tap(([, totalElements]) => {
        this.totalRecords = totalElements;
      }),
      map(([groups]) => groups),
    );
    this.loading$ = this.store.select(selectDashboardGroupsLoading);
    this.store.select(selectAllAvailableDataDomains).subscribe(domains => {
      this.domainNameMap = new Map(domains.map(d => [d.key, d.name]));
    });
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardGroups.label,
          routerLink: naviElements.dashboardGroups.path,
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.restoreTableSearchFilter();
  }

  createDashboardGroup() {
    this.store.dispatch(openDashboardGroupEdition({dashboardGroup: null}));
  }

  editDashboardGroup(group: DashboardGroup) {
    this.store.dispatch(openDashboardGroupEdition({dashboardGroup: group}));
  }

  showDeletionPopup(group: DashboardGroup) {
    this.store.dispatch(showDeleteDashboardGroupPopup({dashboardGroup: group}));
  }

  getDeletionAction() {
    return deleteDashboardGroup();
  }

  loadGroups(event: TableLazyLoadEvent) {
    let sortParam = '';
    if (event.sortField) {
      const direction = event.sortOrder && event.sortOrder > 0 ? 'asc' : 'desc';
      sortParam = `${event.sortField}, ${direction}`;
    }
    this.store.dispatch(loadDashboardGroups({
      page: (event.first ?? 0) / (event.rows ?? 10),
      size: event.rows ?? 10,
      sort: sortParam,
      search: event.globalFilter ? event.globalFilter as string : ''
    }));
  }

  getDomainName(contextKey: string): string {
    return this.domainNameMap.get(contextKey) ?? contextKey;
  }

  groupEntriesByDomain(entries: DashboardGroupEntry[]): Map<string, DashboardGroupEntry[]> {
    const grouped = new Map<string, DashboardGroupEntry[]>();
    if (!entries) {
      return grouped;
    }
    for (const entry of entries) {
      const list = grouped.get(entry.contextKey);
      if (list) {
        list.push(entry);
      } else {
        grouped.set(entry.contextKey, [entry]);
      }
    }
    return grouped;
  }

  private restoreTableSearchFilter() {
    const storageItem = sessionStorage.getItem('portal-dashboard-groups-table');
    if (storageItem) {
      try {
        const storageItemObject = JSON.parse(storageItem);
        const filterValue = storageItemObject.filters?.global?.value;
        if (filterValue) {
          this.filterValue = filterValue;
        }
      } catch {
        // ignore corrupted session storage data
      }
    }
  }
}
