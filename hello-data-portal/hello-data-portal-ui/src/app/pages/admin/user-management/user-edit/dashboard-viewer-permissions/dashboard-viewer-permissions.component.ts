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

import {Component, EventEmitter, Input, Output} from "@angular/core";
import {Context} from "../../../../../store/users-management/context-role.model";
import {DashboardForUser} from "../../../../../store/users-management/users-management.model";
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../../store/app/app.state";
import {UpdateUserRoles} from "../../../../../store/users-management/users-management.action";
import {selectAllDashboardsWithMarkedUser, selectAllDashboardsWithMarkedUserFetched} from "../../../../../store/users-management/users-management.selector";
import {markUnsavedChanges} from "../../../../../store/unsaved-changes/unsaved-changes.actions";
import {take} from "rxjs/operators";

@Component({
  selector: 'app-dashboard-viewer-permissions',
  templateUrl: './dashboard-viewer-permissions.component.html',
  styleUrls: ['./dashboard-viewer-permissions.component.scss']
})
export class DashboardViewerPermissionsComponent {
  @Input()
  context!: Context;
  allDashboardsForContext: DashboardForUser[] = [];
  dashboards$: Observable<any>;
  dashboardsFetched$: Observable<boolean>;

  selectedDashboards: DashboardForUser[] = [];

  @Output()
  selectedDashboardsEvent = new EventEmitter<DashboardForUser[]>();

  constructor(private store: Store<AppState>) {
    this.dashboardsFetched$ = this.store.select(selectAllDashboardsWithMarkedUserFetched).pipe(tap(fetched => console.debug("dashboards fetched?", fetched)));
    this.dashboards$ =
      this.store.select(selectAllDashboardsWithMarkedUser).pipe(
        take(1),
        tap((allDashboards) => {
          this.extractDashboardsForSelectedContext(allDashboards);
          this.extractSelectedDashboards();
        }));
  }

  onSelectionChange($event: any) {
    this.selectedDashboards = $event.value;
    this.selectedDashboardsEvent.emit($event.value);
    this.store.dispatch(markUnsavedChanges({action: new UpdateUserRoles()}));
  }

  private extractDashboardsForSelectedContext(allDashboards: DashboardForUser[]) {
    const allDashboardsForContext = allDashboards.filter(dashboard => dashboard.contextKey === this.context.contextKey);
    this.allDashboardsForContext = allDashboardsForContext.map((item) => {
      return {...item}
    });
    console.debug(`${this.context.contextKey}` + " - all dashboards for context ", this.allDashboardsForContext);
  }

  private extractSelectedDashboards() {
    this.selectedDashboards = this.allDashboardsForContext.filter(dashboard => dashboard.viewer);
    console.debug(`${this.context.contextKey}` + " - selected dashboards " + this.context.contextKey, this.selectedDashboards);
  }

}
