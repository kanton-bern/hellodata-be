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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {loadSubsystemUsers} from "../../../store/users-management/users-management.action";
import {interval, Observable, Subject, takeUntil} from "rxjs";
import {selectSubsystemUsers} from "../../../store/users-management/users-management.selector";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {map} from "rxjs/operators";
import {Table} from "primeng/table";
import {loadRoleResources} from "../../../store/metainfo-resource/metainfo-resource.action";
import {TranslateService} from "../../../shared/services/translate.service";

interface TableRow {
  email: string;

  [key: string]: any; // To allow dynamic columns for instanceNames
}

@Component({
  selector: 'app-subsystem-users',
  templateUrl: './subsystem-users.component.html',
  styleUrls: ['./subsystem-users.component.scss']
})
export class SubsystemUsersComponent extends BaseComponent implements OnInit, OnDestroy {
  private static readonly NOT_FOUND_IN_INSTANCE_TEXT = '@User not found in the instance';
  private static readonly NO_PERMISSIONS = '@User has no permissions in the instance';
  tableData$: Observable<TableRow[]>;
  columns$: Observable<any[]>;
  interval$ = interval(30000);
  private destroy$ = new Subject<void>();

  constructor(private store: Store<AppState>, private translateService: TranslateService) {
    super();
    store.dispatch(loadSubsystemUsers());
    this.columns$ = this.createDynamicColumns();
    this.tableData$ = this.createTableData();
    this.createBreadcrumbs();
    this.createInterval();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  applyFilter(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  clear(table: Table, filterInput: HTMLInputElement): void {
    table.clear();
    filterInput.value = '';
  }

  shouldShowTag(value: any): boolean {
    if (value.includes(',') || !value.startsWith('@') && value !== 'false' && value !== 'true') {
      return true;
    }
    return false;
  }

  translateValue(value: string): string {
    if (value.startsWith('@')) {
      return this.translateService.translate(value);
    } else {
      return value; // No translation needed
    }
  }

  private createDynamicColumns(): Observable<any[]> {
    return this.store.select(selectSubsystemUsers).pipe(
      map(subsystemUsers => [
        {field: 'email', header: '@Users'},
        {field: 'enabled', header: '@Enabled'},
        ...subsystemUsers.map(subsystem => ({
          field: subsystem.instanceName,
          header: subsystem.instanceName
        }))
      ])
    );
  }

  private createTableData(): Observable<TableRow[]> {
    return this.store.select(selectSubsystemUsers).pipe(
      map(subsystemUsers => {
        const uniqueEmails = Array.from(
          new Set(subsystemUsers.flatMap(su => su.users.map(user => user.email)))
        );

        const tableRows: TableRow[] = uniqueEmails.map(email => ({
          email,
        })).sort((a, b) => a.email.localeCompare(b.email));

        tableRows.forEach(row => {
          subsystemUsers.forEach(subsystem => {
            const user = subsystem.users.find(user => user.email === row.email);
            if (user) {
              row['enabled'] = '' + user?.enabled;
            }
            row[subsystem.instanceName] = user ? user.roles.join(', ') || SubsystemUsersComponent.NO_PERMISSIONS : SubsystemUsersComponent.NOT_FOUND_IN_INSTANCE_TEXT;
          });
        });
        console.log('table rows?', tableRows)
        return tableRows;
      }));
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.subsystemUsers.label,
          routerLink: naviElements.subsystemUsers.path,
        }
      ]
    }));
  }

  private createInterval(): void {
    this.interval$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.store.dispatch(loadRoleResources());
        this.store.dispatch(loadSubsystemUsers());
      });
  }

}
