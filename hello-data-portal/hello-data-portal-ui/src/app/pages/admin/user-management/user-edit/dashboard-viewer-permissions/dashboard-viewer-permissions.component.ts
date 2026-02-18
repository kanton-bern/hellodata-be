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

import {Component, inject, input, output} from "@angular/core";
import {Context} from "../../../../../store/users-management/context-role.model";
import {DashboardForUser} from "../../../../../store/users-management/users-management.model";
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../../store/app/app.state";
import {
  selectAllDashboardsWithMarkedUser,
  selectAllDashboardsWithMarkedUserFetched
} from "../../../../../store/users-management/users-management.selector";
import {markUnsavedChanges} from "../../../../../store/unsaved-changes/unsaved-changes.actions";
import {updateUserRoles} from "../../../../../store/users-management/users-management.action";
import {AsyncPipe} from "@angular/common";
import {Checkbox} from "primeng/checkbox";
import {FormsModule} from "@angular/forms";
import {TranslocoPipe} from "@jsverse/transloco";


@Component({
  selector: 'app-dashboard-viewer-permissions',
  templateUrl: './dashboard-viewer-permissions.component.html',
  styleUrls: ['./dashboard-viewer-permissions.component.scss'],
  imports: [Checkbox, FormsModule, AsyncPipe, TranslocoPipe]
})
export class DashboardViewerPermissionsComponent {
  private readonly store = inject<Store<AppState>>(Store);

  readonly context = input.required<Context>();
  allDashboardsForContext: DashboardForUser[] = [];
  filteredDashboards: DashboardForUser[] = [];
  dashboards$: Observable<any>;
  dashboardsFetched$: Observable<boolean>;

  selectAll = false;
  searchQuery = '';

  readonly selectedDashboardsEvent = output<DashboardForUser[]>();

  constructor() {
    this.dashboardsFetched$ = this.store.select(selectAllDashboardsWithMarkedUserFetched).pipe(tap(fetched => console.debug("dashboards fetched?", fetched)));
    this.dashboards$ =
      this.store.select(selectAllDashboardsWithMarkedUser).pipe(
        tap((allDashboards) => {
          this.extractDashboardsForSelectedContext(allDashboards);
          this.filteredDashboards = [...this.allDashboardsForContext];
          this.updateSelectAllState();
        }));
  }

  onDashboardSelectionChange(dashboard: DashboardForUser, selected: boolean) {
    dashboard.viewer = selected;
    this.updateSelectAllState();
    this.emitSelectedDashboards();
  }

  onSelectAllChange(selected: boolean) {
    // Update all dashboards - create new objects to trigger change detection
    this.allDashboardsForContext = this.allDashboardsForContext.map(dashboard => ({
      ...dashboard,
      viewer: selected
    }));
    // Rebuild filtered dashboards based on current search
    if (this.searchQuery.trim() === '') {
      this.filteredDashboards = [...this.allDashboardsForContext];
    } else {
      const lowerQuery = this.searchQuery.toLowerCase();
      this.filteredDashboards = this.allDashboardsForContext.filter(
        dashboard => dashboard.title.toLowerCase().includes(lowerQuery)
      );
    }
    this.updateSelectAllState();
    this.emitSelectedDashboards();
  }

  onSearchChange(query: string) {
    this.searchQuery = query;
    if (query.trim() === '') {
      this.filteredDashboards = [...this.allDashboardsForContext];
    } else {
      const lowerQuery = query.toLowerCase();
      this.filteredDashboards = this.allDashboardsForContext.filter(
        dashboard => dashboard.title.toLowerCase().includes(lowerQuery)
      );
    }
    this.updateSelectAllState();
  }


  private updateSelectAllState() {
    this.selectAll = this.filteredDashboards.length > 0 &&
      this.filteredDashboards.every(dashboard => dashboard.viewer);
  }

  private emitSelectedDashboards() {
    const selectedDashboards = this.allDashboardsForContext.filter(dashboard => dashboard.viewer);
    this.selectedDashboardsEvent.emit(selectedDashboards);
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  private extractDashboardsForSelectedContext(allDashboards: DashboardForUser[]) {
    const allDashboardsForContext = allDashboards.filter(dashboard => dashboard.contextKey === this.context().contextKey);
    this.allDashboardsForContext = allDashboardsForContext
      .map((item) => ({...item}))
      .sort((a, b) => a.title.localeCompare(b.title));
    console.debug(`${this.context().contextKey}` + " - all dashboards for context ", this.allDashboardsForContext);
  }
}
