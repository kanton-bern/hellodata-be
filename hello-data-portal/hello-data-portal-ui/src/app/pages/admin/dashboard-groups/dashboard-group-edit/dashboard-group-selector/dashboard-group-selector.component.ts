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

import {Component, inject, input, OnDestroy, OnInit, output} from '@angular/core';
import {Observable, Subscription} from 'rxjs';
import {Store} from '@ngrx/store';
import {AppState} from '../../../../../store/app/app.state';
import {SupersetDashboard} from '../../../../../store/my-dashboards/my-dashboards.model';
import {selectMyDashboards} from '../../../../../store/my-dashboards/my-dashboards.selector';
import {FormsModule} from '@angular/forms';
import {DashboardGroupEntry} from '../../../../../store/dashboard-groups/dashboard-groups.model';
import {MultiSelect} from 'primeng/multiselect';
import {TranslocoPipe} from '@jsverse/transloco';

interface DashboardSelectionItem {
  id: number;
  title: string;
}

@Component({
  selector: 'app-dashboard-group-selector',
  templateUrl: './dashboard-group-selector.component.html',
  styleUrls: ['./dashboard-group-selector.component.scss'],
  imports: [FormsModule, MultiSelect, TranslocoPipe]
})
export class DashboardGroupSelectorComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);

  readonly contextKey = input.required<string>();
  readonly preselectedEntries = input<DashboardGroupEntry[]>([]);

  allDashboardsForContext: DashboardSelectionItem[] = [];
  allDashboards$: Observable<SupersetDashboard[]>;
  selectedDashboards: DashboardSelectionItem[] = [];

  readonly selectedDashboardsEvent = output<DashboardGroupEntry[]>();

  private dashboardsSub?: Subscription;

  constructor() {
    this.allDashboards$ = this.store.select(selectMyDashboards);
  }

  ngOnInit() {
    this.dashboardsSub = this.allDashboards$.subscribe((allDashboards) => {
      if (allDashboards && allDashboards.length > 0) {
        this.extractDashboardsForSelectedContext(allDashboards);
        this.extractSelectedDashboards();
      }
    });
  }

  ngOnDestroy() {
    if (this.dashboardsSub) {
      this.dashboardsSub.unsubscribe();
    }
  }

  onSelectionChange($event: any) {
    this.selectedDashboards = $event.value;
    const entries: DashboardGroupEntry[] = this.selectedDashboards.map(dashboard => ({
      contextKey: this.contextKey(),
      dashboardId: dashboard.id,
      dashboardTitle: dashboard.title
    }));
    this.selectedDashboardsEvent.emit(entries);
  }

  private extractDashboardsForSelectedContext(allDashboards: SupersetDashboard[]) {
    const contextKey = this.contextKey();
    const dashboardsForContext = allDashboards.filter(dashboard => dashboard.contextKey === contextKey);
    this.allDashboardsForContext = dashboardsForContext.map((dashboard) => ({
      id: dashboard.id,
      title: dashboard.dashboardTitle
    }));
  }

  private extractSelectedDashboards() {
    const preselected = this.preselectedEntries();
    if (!preselected || preselected.length === 0) {
      return;
    }

    const contextKey = this.contextKey();
    const entriesForContext = preselected.filter(entry => entry.contextKey === contextKey);

    this.selectedDashboards = entriesForContext
      .map(entry => {
        const dashboard = this.allDashboardsForContext.find(d => d.id === entry.dashboardId);
        if (dashboard) {
          return dashboard;
        }
        // If dashboard not found in available list, still include it (might be deleted dashboard)
        return {
          id: entry.dashboardId,
          title: entry.dashboardTitle
        };
      })
      .filter(d => d !== null);
  }
}
