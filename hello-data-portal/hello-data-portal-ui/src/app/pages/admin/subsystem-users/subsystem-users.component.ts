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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {
  clearSubsystemUsersCache,
  loadSubsystemUsersPaginated
} from "../../../store/users-management/users-management.action";
import {Observable, Subject, takeUntil} from "rxjs";
import {
  selectPaginatedSubsystemUsers,
  selectPaginatedSubsystemUsersLoading,
  selectPaginatedSubsystemUsersTotalRecords
} from "../../../store/users-management/users-management.selector";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {TableLazyLoadEvent, TableModule} from "primeng/table";
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
import {UserSubsystemRolesDto} from "../../../store/users-management/users-management.model";

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-subsystem-users',
  templateUrl: './subsystem-users.component.html',
  styleUrls: ['./subsystem-users.component.scss'],
  imports: [TableModule, PrimeTemplate, Button, InputText, Tag, AsyncPipe, TranslocoPipe, FormsModule, IconField,
    InputIcon, Ripple, Card, Tooltip]
})
export class SubsystemUsersComponent extends BaseComponent implements OnInit, OnDestroy {
  private static readonly FILTER_STORAGE_KEY = 'subsystem-users-filter-terms';

  users$: Observable<UserSubsystemRolesDto[]>;
  totalRecords$: Observable<number>;
  dataLoading$: Observable<boolean>;

  filterTerms: string[] = [];
  currentFilterInput = '';
  expandedRows: { [s: string]: boolean } = {};
  showInfoPanel = false;
  dynamicColumns: { field: string; header: string }[] = [];

  pageSize = 25;
  currentPage = 0;
  currentSort = 'email,asc';

  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly usersManagementService = inject(UsersManagementService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    this.filterTerms = this.loadFilterTerms();
    this.users$ = this.store.select(selectPaginatedSubsystemUsers);
    this.totalRecords$ = this.store.select(selectPaginatedSubsystemUsersTotalRecords);
    this.dataLoading$ = this.store.select(selectPaginatedSubsystemUsersLoading);
    this.createBreadcrumbs();
    this.loadPage();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize));
    this.pageSize = event.rows ?? this.pageSize;
    if (event.sortField) {
      const dir = event.sortOrder === 1 ? 'asc' : 'desc';
      this.currentSort = `${event.sortField},${dir}`;
    }
    this.loadPage();
  }

  addFilterTerm(event: Event): void {
    event.preventDefault();
    const term = this.currentFilterInput.trim();
    if (term && !this.filterTerms.includes(term)) {
      this.filterTerms = [...this.filterTerms, term];
      this.currentFilterInput = '';
      this.saveFilterTerms();
      this.currentPage = 0;
      this.loadPage();
    }
  }

  removeFilterTerm(index: number): void {
    this.filterTerms = this.filterTerms.filter((_, i) => i !== index);
    this.saveFilterTerms();
    this.currentPage = 0;
    this.loadPage();
  }

  clearAllFilters(): void {
    this.filterTerms = [];
    this.currentFilterInput = '';
    this.saveFilterTerms();
    this.currentPage = 0;
    this.loadPage();
  }

  getFilterPlaceholder(): string {
    return this.translateService.translate('@Search');
  }

  matchesFilter(value: string): boolean {
    if (!value || this.filterTerms.length === 0) return false;
    const normalizedValue = value.toLowerCase().replace(/_/g, ' ');
    return this.filterTerms.some(term => normalizedValue.includes(term.toLowerCase().replace(/_/g, ' ')));
  }

  shouldShowTag(roles: string[]): boolean {
    return roles && roles.length > 0;
  }

  translateValue(value: string): string {
    return value?.startsWith('@') ? this.translateService.translate(value) : value;
  }

  clearCache() {
    this.store.dispatch(clearSubsystemUsersCache());
    this.loadPage();
  }

  reload() {
    this.loadPage();
  }

  exportCsv(): void {
    const search = this.filterTerms.join(' ');
    this.usersManagementService.downloadSubsystemUsersCsvExport(search)
      .pipe(takeUntil(this.destroy$))
      .subscribe(blob => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'subsystem-users.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      });
  }

  exportBatchCsv(): void {
    this.usersManagementService.downloadBatchExportCsv()
      .pipe(takeUntil(this.destroy$))
      .subscribe(blob => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'batch-users-export.csv';
        link.click();
        URL.revokeObjectURL(link.href);
      });
  }

  getSubsystemKeys(user: UserSubsystemRolesDto): string[] {
    return Object.keys(user.subsystemRoles || {});
  }

  formatRoleName(role: string): string {
    if (!role) return '';
    return role.replace(/_/g, ' ');
  }

  /** Build dynamic columns from the first page of users */
  updateDynamicColumns(users: UserSubsystemRolesDto[]): void {
    const colSet = new Set<string>();
    for (const user of users) {
      for (const key of Object.keys(user.subsystemRoles || {})) {
        colSet.add(key);
      }
    }
    const newCols = Array.from(colSet).map(key => ({field: key, header: key}));
    if (JSON.stringify(newCols) !== JSON.stringify(this.dynamicColumns)) {
      this.dynamicColumns = newCols;
      this.cdr.markForCheck();
    }
  }

  private loadPage(): void {
    const search = this.filterTerms.join(' ');
    this.store.dispatch(loadSubsystemUsersPaginated({
      page: this.currentPage,
      size: this.pageSize,
      sort: this.currentSort,
      search
    }));
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
