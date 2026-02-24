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

import {Component, inject, input, OnInit} from "@angular/core";
import {Context} from "../../../../../store/users-management/context-role.model";
import {DashboardGroupMembership} from "../../../../../store/users-management/users-management.model";
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../../store/app/app.state";
import {
  selectDashboardGroupMembershipsForUser,
  selectSelectedDashboardGroupIdsForUser
} from "../../../../../store/users-management/users-management.selector";
import {markUnsavedChanges} from "../../../../../store/unsaved-changes/unsaved-changes.actions";
import {
  loadDashboardGroupMemberships,
  setDashboardGroupMembershipForUser,
  updateUserRoles
} from "../../../../../store/users-management/users-management.action";
import {AsyncPipe} from "@angular/common";
import {Checkbox} from "primeng/checkbox";
import {Tag} from "primeng/tag";
import {FormsModule} from "@angular/forms";
import {TranslocoPipe} from "@jsverse/transloco";
import {map} from "rxjs/operators";


@Component({
  selector: 'app-dashboard-group-membership',
  templateUrl: './dashboard-group-membership.component.html',
  styleUrls: ['./dashboard-group-membership.component.scss'],
  imports: [Checkbox, Tag, FormsModule, AsyncPipe, TranslocoPipe]
})
export class DashboardGroupMembershipComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);

  readonly context = input.required<Context>();

  memberships$: Observable<DashboardGroupMembership[]>;
  selectedGroupIds$: Observable<string[]>;

  constructor() {
    this.memberships$ = this.store.select(selectDashboardGroupMembershipsForUser).pipe(
      map(allMemberships => allMemberships[this.context().contextKey] || [])
    );
    this.selectedGroupIds$ = this.store.select(selectSelectedDashboardGroupIdsForUser).pipe(
      map(allSelectedIds => allSelectedIds[this.context().contextKey] || [])
    );
  }

  ngOnInit() {
    this.store.dispatch(loadDashboardGroupMemberships({contextKey: this.context().contextKey}));
  }

  onMembershipChange(groupId: string, isMember: boolean) {
    this.store.dispatch(setDashboardGroupMembershipForUser({
      contextKey: this.context().contextKey,
      groupId,
      isMember
    }));
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  isGroupSelected(groupId: string, selectedIds: string[]): boolean {
    return selectedIds.includes(groupId);
  }
}
