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

import {AfterViewInit, ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {clearSubsystemUsersCache, loadSubsystemUsers} from "../../../store/users-management/users-management.action";
import {first, Observable, Subject} from "rxjs";
import {
  selectSubsystemUsers,
  selectSubsystemUsersLoading
} from "../../../store/users-management/users-management.selector";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {map} from "rxjs/operators";
import {Table, TableModule} from "primeng/table";
import {TranslateService} from "../../../shared/services/translate.service";
import {AsyncPipe} from '@angular/common';
import {PrimeTemplate} from 'primeng/api';
import {Button} from 'primeng/button';
import {InputText} from 'primeng/inputtext';
import {Tag} from 'primeng/tag';
import {Card} from 'primeng/card';
import {TranslocoPipe} from '@jsverse/transloco';
import {FormsModule} from "@angular/forms";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {Ripple} from "primeng/ripple";
import {Tooltip} from "primeng/tooltip";
import {UsersManagementService} from "../../../store/users-management/users-management.service";

interface TableRow {
  email: string;

  [key: string]: any; // To allow dynamic columns for instanceNames
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-subsystem-users',
  templateUrl: './subsystem-users.component.html',
  styleUrls: ['./subsystem-users.component.scss'],
  imports: [TableModule, PrimeTemplate, Button, InputText, Tag, AsyncPipe, TranslocoPipe, FormsModule, IconField,
    InputIcon, Ripple, Card, Tooltip]
})
export class SubsystemUsersComponent extends BaseComponent implements OnInit, OnDestroy, AfterViewInit {
  private static readonly NOT_FOUND_IN_INSTANCE_TEXT = '@User not found in the instance';
  private static readonly NO_PERMISSIONS = '@User has no permissions in the instance';
  private static readonly FILTER_STORAGE_KEY = 'subsystem-users-filter-terms';
  @ViewChild('dt') table!: Table;
  tableData$: Observable<TableRow[]>;
  dynamicColumns$: Observable<any[]>;
  globalFilterFields$: Observable<string[]>;
  dataLoading$: Observable<boolean>;
  filterTerms: string[] = [];
  currentFilterInput = '';
  expandedRows: { [s: string]: boolean } = {};
  showInfoPanel = false;
  private allFilterFields: string[] = ['email'];
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly usersManagementService = inject(UsersManagementService);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    const store = this.store;

    this.filterTerms = this.loadFilterTerms();

    store.dispatch(loadSubsystemUsers());
    this.dynamicColumns$ = this.createDynamicColumns();
    this.tableData$ = this.createTableData();
    this.globalFilterFields$ = this.dynamicColumns$.pipe(
      map(columns => {
        const fields = ['email', ...columns.map(c => c.field)];
        this.allFilterFields = fields;
        return fields;
      })
    );
    this.createBreadcrumbs();
    this.dataLoading$ = this.store.select(selectSubsystemUsersLoading);
  }

  ngAfterViewInit(): void {
    if (this.filterTerms.length > 0) {
      this.tableData$.pipe(
        first(data => data.length > 0)
      ).subscribe(() => {
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
      this.table.filterGlobal('', 'contains');
      this.expandedRows = {};
      return;
    }
    // Use full dataset for custom matching (don't rely on PrimeNG pre-filter)
    this.table.filteredValue = null;
    const allData = this.table.value || [];
    this.table.filteredValue = allData.filter(row => this.rowMatchesAllTerms(row));
    this.table.totalRecords = this.table.filteredValue.length;

    const expanded: { [s: string]: boolean } = {};
    this.table.filteredValue.forEach(row => {
      expanded[row.email] = true;
    });
    this.expandedRows = expanded;
  }

  private normalizeForSearch(value: string): string {
    return value.toLowerCase().replace(/_/g, ' ');
  }

  private rowMatchesAllTerms(row: TableRow): boolean {
    return this.filterTerms.every(term => {
      const normalizedTerm = this.normalizeForSearch(term);

      const matchesField = this.allFilterFields.some(field => {
        const val = row[field];
        return val && this.normalizeForSearch(String(val)).includes(normalizedTerm);
      });
      if (matchesField) return true;

      if (row['_businessDomainRole'] &&
          this.normalizeForSearch(row['_businessDomainRole']).includes(normalizedTerm)) {
        return true;
      }

      if (row['_dataDomainRoles']?.length > 0) {
        return row['_dataDomainRoles'].some((ddr: any) =>
          this.normalizeForSearch(ddr.role || '').includes(normalizedTerm)
        );
      }

      return false;
    });
  }

  onGlobalFilterChange(table: Table): void {
    if (this.filterTerms.length > 0 && table.filteredValue) {
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

  matchesFilter(value: string): boolean {
    if (this.filterTerms.length === 0) {
      return false;
    }
    const normalizedValue = this.normalizeForSearch(value);
    return this.filterTerms.some(term => normalizedValue.includes(this.normalizeForSearch(term)));
  }

  shouldShowTag(value: any): boolean {
    if (value.includes(',') || !value.includes('@') && value !== 'false' && value !== 'true') {
      return true;
    }
    return false;
  }

  translateValue(value: string): string {
    if (value.startsWith('@')) {
      return this.translateService.translate(value);
    } else {
      return value;
    }
  }

  clearCache() {
    this.store.dispatch(clearSubsystemUsersCache());
  }

  reload() {
    this.store.dispatch(loadSubsystemUsers());
  }

  exportCsv(tableData: TableRow[], dynamicColumns: any[]) {
    const dataToExport: TableRow[] = this.table.filteredValue || tableData;
    const contextRolesHeader = this.translateService.translate('@Context roles');
    const fixedHeaders = ['Email', 'Enabled', contextRolesHeader];
    const dynamicHeaders = dynamicColumns.map(c => this.translateValue(c.header));
    const headers = [...fixedHeaders, ...dynamicHeaders];

    const rows = dataToExport.map(row => {
      const contextRoleParts: string[] = [];
      if (row['_businessDomainRole'] && row['_businessDomainRole'] !== 'NONE') {
        contextRoleParts.push(this.translateService.translate('@Business domain') + ': ' + this.formatRoleName(row['_businessDomainRole']));
      }
      if (row['_dataDomainRoles']?.length > 0) {
        for (const ddr of row['_dataDomainRoles']) {
          if (ddr.role && ddr.role !== 'NONE') {
            contextRoleParts.push(ddr.contextName + ': ' + this.formatRoleName(ddr.role));
          }
        }
      }
      const contextRolesValue = contextRoleParts.join('; ');
      const fixedValues = [row['email'], row['enabled'] || '', contextRolesValue];
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
    link.download = 'subsystem-users.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }

  exportBatchCsv() {
    this.usersManagementService.downloadBatchExportCsv().subscribe(blob => {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'batch-users-export.csv';
      link.click();
      URL.revokeObjectURL(link.href);
    });
  }

  private saveFilterTerms(): void {
    if (this.filterTerms.length > 0) {
      sessionStorage.setItem(SubsystemUsersComponent.FILTER_STORAGE_KEY, JSON.stringify(this.filterTerms));
    } else {
      sessionStorage.removeItem(SubsystemUsersComponent.FILTER_STORAGE_KEY);
    }
  }

  private loadFilterTerms(): string[] {
    try {
      const stored = sessionStorage.getItem(SubsystemUsersComponent.FILTER_STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private createDynamicColumns(): Observable<any[]> {
    return this.store.select(selectSubsystemUsers).pipe(
      map(subsystemUsers =>
        subsystemUsers.map(subsystem => ({
          field: subsystem.instanceName,
          header: subsystem.instanceName
        }))
      )
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

        tableRows.forEach(row => this.populateRowWithSubsystemData(row, subsystemUsers));
        return tableRows;
      }));
  }

  private populateRowWithSubsystemData(row: TableRow, subsystemUsers: any[]): void {
    subsystemUsers.forEach(subsystem => {
      const user = subsystem.users.find((u: any) => u.email === row.email);
      if (user) {
        row['enabled'] = '' + user.enabled;
        if (!row['_businessDomainRole'] && user.businessDomainRole) {
          row['_businessDomainRole'] = user.businessDomainRole;
        }
        if (!row['_dataDomainRoles'] && user.dataDomainRoles?.length > 0) {
          row['_dataDomainRoles'] = user.dataDomainRoles;
        }
      }
      row[subsystem.instanceName] = user
        ? user.roles.join(', ') || SubsystemUsersComponent.NO_PERMISSIONS
        : SubsystemUsersComponent.NOT_FOUND_IN_INSTANCE_TEXT;
    });
  }

  formatRoleName(role: string): string {
    if (!role) return '';
    return role.replace(/_/g, ' ');
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
}
