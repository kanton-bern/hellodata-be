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
import {Checkbox} from 'primeng/checkbox';
import {TranslocoPipe} from '@jsverse/transloco';

interface DashboardSelectionItem {
  id: number;
  title: string;
}

@Component({
  selector: 'app-dashboard-group-selector',
  templateUrl: './dashboard-group-selector.component.html',
  styleUrls: ['./dashboard-group-selector.component.scss'],
  imports: [FormsModule, Checkbox, TranslocoPipe]
})
export class DashboardGroupSelectorComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);

  readonly contextKey = input.required<string>();
  readonly preselectedEntries = input<DashboardGroupEntry[]>([]);

  allDashboardsForContext: DashboardSelectionItem[] = [];
  filteredDashboards: DashboardSelectionItem[] = [];
  allDashboards$: Observable<SupersetDashboard[]>;
  selectedDashboards: DashboardSelectionItem[] = [];
  searchQuery = '';
  selectAll = false;

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
        this.applyFilter();
        this.updateSelectAllState();
      }
    });
  }

  ngOnDestroy() {
    if (this.dashboardsSub) {
      this.dashboardsSub.unsubscribe();
    }
  }

  isDashboardSelected(dashboard: DashboardSelectionItem): boolean {
    return this.selectedDashboards.some(d => d.id === dashboard.id);
  }

  onDashboardSelectionChange(dashboard: DashboardSelectionItem, selected: boolean) {
    if (selected) {
      if (!this.isDashboardSelected(dashboard)) {
        this.selectedDashboards = [...this.selectedDashboards, dashboard];
      }
    } else {
      this.selectedDashboards = this.selectedDashboards.filter(d => d.id !== dashboard.id);
    }
    this.updateSelectAllState();
    this.emitSelection();
  }

  onSelectAllChange(checked: boolean) {
    this.selectAll = checked;
    if (checked) {
      // Select all filtered dashboards
      this.filteredDashboards.forEach(dashboard => {
        if (!this.isDashboardSelected(dashboard)) {
          this.selectedDashboards = [...this.selectedDashboards, dashboard];
        }
      });
    } else {
      // Deselect all filtered dashboards
      const filteredIds = new Set(this.filteredDashboards.map(d => d.id));
      this.selectedDashboards = this.selectedDashboards.filter(d => !filteredIds.has(d.id));
    }
    this.emitSelection();
  }

  onSearchChange(query: string) {
    this.searchQuery = query;
    this.applyFilter();
    this.updateSelectAllState();
  }

  private applyFilter() {
    if (!this.searchQuery || this.searchQuery.trim() === '') {
      this.filteredDashboards = [...this.allDashboardsForContext];
    } else {
      const lowerQuery = this.searchQuery.toLowerCase();
      this.filteredDashboards = this.allDashboardsForContext.filter(
        d => d.title.toLowerCase().includes(lowerQuery)
      );
    }
  }

  private updateSelectAllState() {
    if (this.filteredDashboards.length === 0) {
      this.selectAll = false;
    } else {
      this.selectAll = this.filteredDashboards.every(d => this.isDashboardSelected(d));
    }
  }

  private emitSelection() {
    const entries: DashboardGroupEntry[] = this.selectedDashboards.map(dashboard => ({
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

    this.selectedDashboards = preselected
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
