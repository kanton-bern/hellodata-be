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

import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {TranslocoPipe} from "@jsverse/transloco";
import {TableModule} from "primeng/table";
import {Tag} from "primeng/tag";
import {Button} from "primeng/button";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {combineLatest, map, Observable, Subject} from "rxjs";
import {
  clearSubsystemUsersForDashboardsCache,
  loadSubsystemUsersForDashboards
} from "../../../store/users-management/users-management.action";
import {
  selectSubsystemUsersForDashboards,
  selectSubsystemUsersForDashboardsLoading
} from "../../../store/users-management/users-management.selector";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {TranslateService} from "../../../shared/services/translate.service";
import {PrimeTemplate} from "primeng/api";
import {Ripple} from "primeng/ripple";
import {DashboardUsersResultDto} from "../../../store/users-management/users-management.model";

interface TableRow {
  email: string;

  [key: string]: any; // To allow dynamic columns for instanceNames
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-users-overview',
  templateUrl: './users-overview.component.html',
  styleUrls: ['./users-overview.component.scss'],
  imports: [TableModule, PrimeTemplate, Button, Tag, AsyncPipe, TranslocoPipe, Ripple]
})
export class UsersOverviewComponent extends BaseComponent implements OnInit, OnDestroy {
  private static readonly NO_PERMISSIONS_TRANSLATION_KEY = '@No permissions';
  readonly NO_TAG = '_no_tag';
  tableData$: Observable<TableRow[]>;
  columns$: Observable<any[]>;
  dataLoading$: Observable<boolean>;
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    const store = this.store;

    store.dispatch(loadSubsystemUsersForDashboards());
    this.columns$ = this.createDynamicColumns();
    this.tableData$ = this.createTableData();
    this.createBreadcrumbs();
    this.dataLoading$ = this.store.select(selectSubsystemUsersForDashboardsLoading);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  shouldShowTag(value: string, noTag: any): boolean {
    if (noTag) {
      return false;
    }
    if (value.includes(',') || !value.includes('@') && !value.includes('true') && !value.includes('false')) {
      return true;
    }
    return false;
  }

  getTagSeverity(value: string): "success" | "secondary" | "info" | "warn" | "danger" | "contrast" | undefined {
    const valTrimmed = value.trim();
    if (valTrimmed.includes('Admin') || valTrimmed.includes('HELLODATA_ADMIN')) {
      return 'danger';
    }
    if (valTrimmed.includes('BUSINESS_DOMAIN_ADMIN')) {
      return 'warn';
    }
    if (valTrimmed.includes('NONE')) {
      return 'secondary';
    }
    return value.trim().startsWith('BI_') ? undefined : 'success';
  }

  translateValue(value: string): string {
    if (value.startsWith('@')) {
      return this.translateService.translate(value);
    } else {
      return value; // No translation needed
    }
  }

  clearCache() {
    this.store.dispatch(clearSubsystemUsersForDashboardsCache());
  }

  reload() {
    this.store.dispatch(loadSubsystemUsersForDashboards());
  }

  private createDynamicColumns(): Observable<any[]> {
    return this.store.select(selectSubsystemUsersForDashboards).pipe(
      map((subsystemUsers) => [
        {field: 'email', header: '@Users'},
        // {field: 'businessDomainRole', header: '@Business Domain Role'},
        {field: 'enabled', header: '@Enabled'},
        ...subsystemUsers.map(subsystem => ({
          field: subsystem.instanceName,
          header: subsystem.contextName
        }))
      ])
    );
  }

  private createTableData(): Observable<TableRow[]> {
    return combineLatest([
      this.store.select(selectSubsystemUsersForDashboards),
      this.translateService.selectTranslate(UsersOverviewComponent.NO_PERMISSIONS_TRANSLATION_KEY)
    ]).pipe(
      map(([subsystemUsers, noPermissionsTranslation]) => {
        const uniqueEmails = Array.from(
          new Set(subsystemUsers.flatMap(su => su.users.map(user => user.email)))
        );

        const tableRows: TableRow[] = uniqueEmails.map(email => ({
          email,
        })).sort((a, b) => a.email.localeCompare(b.email));

        tableRows.forEach(row => {
          subsystemUsers.forEach(subsystem => {
            this.createTableRow(subsystem, row, noPermissionsTranslation);
          });
        });

        return tableRows;
      }));
  }

  private createTableRow(subsystem: DashboardUsersResultDto, row: TableRow, noPermissionsTranslation: string) {
    const user = subsystem.users.find(user => user.email === row.email);
    if (user) {
      row['enabled'] = '' + user.enabled;
      row['businessDomainRole'] = user.businessDomainRole || '';
    }
    const value = user ? user.roles.join(', ') || noPermissionsTranslation : noPermissionsTranslation;
    row[subsystem.instanceName] = value;
    if (value === noPermissionsTranslation) {
      row[subsystem.instanceName + this.NO_TAG] = true
    }
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.usersOverview.label,
          routerLink: naviElements.usersOverview.path,
        }
      ]
    }));
  }
}


