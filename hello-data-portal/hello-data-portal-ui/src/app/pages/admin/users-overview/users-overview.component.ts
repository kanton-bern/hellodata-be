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

import {AfterViewInit, ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {AsyncPipe} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {TranslocoPipe} from "@jsverse/transloco";
import {Table, TableModule} from "primeng/table";
import {Tag} from "primeng/tag";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {combineLatest, first, map, Observable, Subject} from "rxjs";
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
import {Tooltip} from "primeng/tooltip";
import {DashboardUsersResultDto} from "../../../store/users-management/users-management.model";
import {Card} from 'primeng/card';

interface TableRow {
  email: string;

  [key: string]: any; // To allow dynamic columns for instanceNames
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-users-overview',
  templateUrl: './users-overview.component.html',
  styleUrls: ['./users-overview.component.scss'],
  imports: [TableModule, PrimeTemplate, Button, Tag, AsyncPipe, TranslocoPipe, Ripple, Card, FormsModule, InputText, IconField, InputIcon, Tooltip]
})
export class UsersOverviewComponent extends BaseComponent implements OnInit, OnDestroy, AfterViewInit {
  private static readonly NO_PERMISSIONS_TRANSLATION_KEY = '@No permissions';
  private static readonly FILTER_STORAGE_KEY = 'users-overview-filter-terms';
  readonly NO_TAG = '_no_tag';
  @ViewChild('dt') table!: Table;
  tableData$: Observable<TableRow[]>;
  dynamicColumns$: Observable<any[]>;
  globalFilterFields$: Observable<string[]>;
  dataLoading$: Observable<boolean>;
  filterTerms: string[] = [];
  currentFilterInput = '';
  expandedRows: { [s: string]: boolean } = {};
  private allFilterFields: string[] = ['email', 'businessDomainRole'];
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    const store = this.store;

    this.filterTerms = this.loadFilterTerms();

    store.dispatch(loadSubsystemUsersForDashboards());
    this.dynamicColumns$ = this.createDynamicColumns();
    this.tableData$ = this.createTableData();
    this.globalFilterFields$ = this.dynamicColumns$.pipe(
      map(columns => {
        const fields = ['email', 'businessDomainRole', ...columns.map(c => c.field)];
        this.allFilterFields = fields;
        return fields;
      })
    );
    this.createBreadcrumbs();
    this.dataLoading$ = this.store.select(selectSubsystemUsersForDashboardsLoading);
  }

  ngAfterViewInit(): void {
    // Re-apply restored filters once the table and data are ready
    if (this.filterTerms.length > 0) {
      this.tableData$.pipe(
        first(data => data.length > 0)
      ).subscribe(() => {
        // Small delay to ensure the table has rendered with the data
        setTimeout(() => this.applyCustomFilter(), 0);
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  addFilterTerm(event: Event): void {
    event.preventDefault();
    const term = this.currentFilterInput.trim();
    if (term && !this.filterTerms.includes(term)) {
      this.filterTerms = [...this.filterTerms, term];
      this.currentFilterInput = '';
      this.saveFilterTerms();
      this.applyCustomFilter();
    }
  }

  removeFilterTerm(index: number): void {
    this.filterTerms = this.filterTerms.filter((_, i) => i !== index);
    this.saveFilterTerms();
    this.applyCustomFilter();
  }

  clearAllFilters(): void {
    this.filterTerms = [];
    this.currentFilterInput = '';
    this.saveFilterTerms();
    this.applyCustomFilter();
  }

  getFilterPlaceholder(): string {
    return this.translateService.translate('@Search');
  }

  private applyCustomFilter(): void {
    if (!this.table) {
      return;
    }
    if (this.filterTerms.length === 0) {
      // Clear all filters
      this.table.filterGlobal('', 'contains');
      this.expandedRows = {};
      return;
    }
    // Use the first term for PrimeNG's built-in global filter (keeps paginator etc. working)
    // Then we refine with our custom callback
    this.table.filterGlobal(this.filterTerms[0], 'contains');

    // After PrimeNG applies the first filter, further narrow down with remaining terms (AND)
    if (this.filterTerms.length > 1 && this.table.filteredValue) {
      this.table.filteredValue = this.table.filteredValue.filter(row => this.rowMatchesAllTerms(row));
    }
    this.table.totalRecords = (this.table.filteredValue || this.table.value).length;

    // Auto-expand filtered rows
    const filteredData: TableRow[] = this.table.filteredValue || this.table.value || [];
    const expanded: { [s: string]: boolean } = {};
    filteredData.forEach(row => {
      expanded[row.email] = true;
    });
    this.expandedRows = expanded;
  }

  private rowMatchesAllTerms(row: TableRow): boolean {
    return this.filterTerms.every(term => {
      const lowerTerm = term.toLowerCase();
      return this.allFilterFields.some(field => {
        const val = row[field];
        return val && String(val).toLowerCase().includes(lowerTerm);
      });
    });
  }

  onGlobalFilterChange(table: Table): void {
    // Called by (onFilter) event — further narrow results for multi-term AND
    if (this.filterTerms.length > 1 && table.filteredValue) {
      table.filteredValue = table.filteredValue.filter(row => this.rowMatchesAllTerms(row));
      table.totalRecords = table.filteredValue.length;
    }

    if (this.filterTerms.length > 0) {
      const filteredData: TableRow[] = table.filteredValue || table.value || [];
      const expanded: { [s: string]: boolean } = {};
      filteredData.forEach(row => {
        expanded[row.email] = true;
      });
      this.expandedRows = expanded;
    } else {
      this.expandedRows = {};
    }
  }

  /** Returns true if ANY active filter term matches the given value (used for highlight glow) */
  matchesFilter(value: string): boolean {
    if (this.filterTerms.length === 0) {
      return false;
    }
    const lowerValue = value.toLowerCase();
    return this.filterTerms.some(term => lowerValue.includes(term.toLowerCase()));
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

  exportCsv(tableData: TableRow[], dynamicColumns: any[]) {
    const dataToExport: TableRow[] = this.table.filteredValue || tableData;
    const fixedHeaders = ['Email', 'Business Domain Role', 'Enabled'];
    const dynamicHeaders = dynamicColumns.map(c => this.translateValue(c.header));
    const headers = [...fixedHeaders, ...dynamicHeaders];

    const rows = dataToExport.map(row => {
      const fixedValues = [row['email'], row['businessDomainRole'] || '', row['enabled'] || ''];
      const dynamicValues = dynamicColumns.map(c => row[c.field] || '');
      return [...fixedValues, ...dynamicValues];
    });

    const csvContent = [
      headers.join(','),
      ...rows.map(r => r.map(v => `"${(v || '').replace(/"/g, '""')}"`).join(','))
    ].join('\n');

    const blob = new Blob(['\ufeff' + csvContent], {type: 'text/csv;charset=utf-8;'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'users-overview.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }

  private saveFilterTerms(): void {
    if (this.filterTerms.length > 0) {
      sessionStorage.setItem(UsersOverviewComponent.FILTER_STORAGE_KEY, JSON.stringify(this.filterTerms));
    } else {
      sessionStorage.removeItem(UsersOverviewComponent.FILTER_STORAGE_KEY);
    }
  }

  private loadFilterTerms(): string[] {
    try {
      const stored = sessionStorage.getItem(UsersOverviewComponent.FILTER_STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private createDynamicColumns(): Observable<any[]> {
    return this.store.select(selectSubsystemUsersForDashboards).pipe(
      map((subsystemUsers) =>
        subsystemUsers.map(subsystem => ({
          field: subsystem.instanceName,
          header: subsystem.contextName
        }))
      )
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


