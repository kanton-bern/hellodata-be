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

import {Component, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {loadSubsystemUsers} from "../../../store/users-management/users-management.action";
import {Observable, tap} from "rxjs";
import {selectSubsystemUsers} from "../../../store/users-management/users-management.selector";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {map} from "rxjs/operators";

interface TableRow {
  email: string;

  [key: string]: any; // To allow dynamic columns for instanceNames
}

@Component({
  selector: 'app-subsystem-users',
  templateUrl: './subsystem-users.component.html',
  styleUrls: ['./subsystem-users.component.scss']
})
export class SubsystemUsersComponent extends BaseComponent implements OnInit {

  tableData$: Observable<TableRow[]>;
  columns$: Observable<any[]>; // Observable for dynamic columns

  constructor(private store: Store<AppState>) {
    super();
    store.dispatch(loadSubsystemUsers());
    // Observable for dynamic columns (instanceName)
    this.columns$ = this.store.select(selectSubsystemUsers).pipe(
      map(subsystemUsers =>
        subsystemUsers.map(subsystem => ({
          field: subsystem.instanceName,
          header: subsystem.instanceName
        }))
      ),
      tap(col => console.log("col", col))
    );

    // Observable for table data (rows based on email)
    this.tableData$ = this.store.select(selectSubsystemUsers).pipe(
      map(subsystemUsers => {
        const uniqueEmails = Array.from(
          new Set(subsystemUsers.flatMap(su => su.users.map(user => user.email)))
        );

        // Create table rows by email
        const tableRows: TableRow[] = uniqueEmails.map(email => ({
          email,
        }));

        // Populate rows with roles for each instanceName
        tableRows.forEach(row => {
          subsystemUsers.forEach(subsystem => {
            const user = subsystem.users.find(user => user.email === row.email);
            row[subsystem.instanceName] = user ? user.roles.join(', ') || '-' : '-';
          });
        });

        return tableRows;
      }));

    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.subsystemUsers.label,
          routerLink: naviElements.subsystemUsers.path,
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

}
