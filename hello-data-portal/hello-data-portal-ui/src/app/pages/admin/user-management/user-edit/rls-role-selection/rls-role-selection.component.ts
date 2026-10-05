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
import {combineLatest, Observable} from "rxjs";
import {map} from "rxjs/operators";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../../store/app/app.state";
import {
  selectRlsRolesForUser,
  selectSelectedRlsRolesForUser
} from "../../../../../store/users-management/users-management.selector";
import {
  loadRlsRolesForUser,
  setRlsRolesForUser,
  updateUserRoles
} from "../../../../../store/users-management/users-management.action";
import {markUnsavedChanges} from "../../../../../store/unsaved-changes/unsaved-changes.actions";
import {RlsRole} from "../../../../../store/rls-roles/rls-roles.model";
import {AsyncPipe} from "@angular/common";
import {Checkbox} from "primeng/checkbox";
import {FormsModule} from "@angular/forms";
import {TranslocoPipe} from "@jsverse/transloco";

interface RlsRoleSelection {
  rlsRoles: RlsRole[];
  selectedRoleKeys: string[];
}

/**
 * Optional selection of the row level security groups (RLS_01 - RLS_15) of a BI viewer in a data domain
 */
@Component({
  selector: 'app-rls-role-selection',
  templateUrl: './rls-role-selection.component.html',
  imports: [Checkbox, FormsModule, AsyncPipe, TranslocoPipe]
})
export class RlsRoleSelectionComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);

  readonly context = input.required<Context>();

  selection$: Observable<RlsRoleSelection>;

  constructor() {
    this.selection$ = combineLatest([
      this.store.select(selectRlsRolesForUser),
      this.store.select(selectSelectedRlsRolesForUser)
    ]).pipe(
      map(([allRlsRoles, allSelected]) => ({
        rlsRoles: allRlsRoles[this.context().contextKey] || [],
        selectedRoleKeys: allSelected[this.context().contextKey] || []
      }))
    );
  }

  ngOnInit() {
    this.store.dispatch(loadRlsRolesForUser({contextKey: this.context().contextKey}));
  }

  onRlsRoleChange(roleKey: string, checked: boolean, selectedRoleKeys: string[]) {
    const roleKeys = checked
      ? [...selectedRoleKeys.filter(key => key !== roleKey), roleKey]
      : selectedRoleKeys.filter(key => key !== roleKey);
    this.store.dispatch(setRlsRolesForUser({
      contextKey: this.context().contextKey,
      roleKeys: roleKeys.sort((a, b) => a.localeCompare(b))
    }));
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }
}
