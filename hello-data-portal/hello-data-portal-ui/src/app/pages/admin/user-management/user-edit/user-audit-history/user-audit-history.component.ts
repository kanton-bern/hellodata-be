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

import {ChangeDetectorRef, Component, DestroyRef, inject, Input, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Actions, ofType} from '@ngrx/effects';
import {DatePipe} from '@angular/common';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {PrimeTemplate} from 'primeng/api';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {Ripple} from 'primeng/ripple';
import {ICON_REGISTRY} from '../../../../../shared/icons';
import {FormsModule} from '@angular/forms';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {InputText} from 'primeng/inputtext';
import {debounceTime, distinctUntilChanged, Subject} from 'rxjs';
import {TranslocoPipe, TranslocoService} from '@jsverse/transloco';
import {UsersManagementService} from '../../../../../store/users-management/users-management.service';
import {UserAuditChange, UserAuditLog} from '../../../../../store/users-management/users-management.model';
import {updateUserRolesSuccess, userPopupActionSuccess} from '../../../../../store/users-management/users-management.action';

/** The roles of a business domain; a role change from or to one of them is a change in the business domain. */
const BUSINESS_DOMAIN_ROLES = new Set(['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']);
/** Remembers in this browser whether the History shows the domain changes of all entries. */
const EXPANDED_STORAGE_KEY = 'user-audit-history-all-expanded';
/** The translation keys of the codes the search also matches by their translated label. */
const CODE_PREFIXES = ['@History action ', '@History source ', '@History field '];

/** The changes of an entry in one domain (domain null: the changes without a domain). */
export interface ChangeGroup {
  key: string;
  domain: string | null;
  businessDomain: boolean;
  changes: UserAuditChange[];
}

@Component({
  selector: 'app-user-audit-history',
  templateUrl: './user-audit-history.component.html',
  styleUrl: './user-audit-history.component.scss',
  imports: [TableModule, PrimeTemplate, DatePipe, TranslocoPipe, Button, Tooltip, Ripple, FormsModule, IconField, InputIcon, InputText]
})
export class UserAuditHistoryComponent implements OnInit {
  @Input({required: true}) userId!: string;

  entries: UserAuditLog[] = [];
  totalRecords = 0;
  /** The entries (by id) whose domain changes are shown, like the rows of the user overview. */
  expandedRows: Record<string, boolean> = {};
  allExpanded = this.loadAllExpanded();
  protected readonly icons = ICON_REGISTRY;
  /** The text the History is searched for (in the actor, action, source, changes and domain names). */
  search = '';
  private readonly search$ = new Subject<string>();
  /** The groups of each loaded entry, computed once (the template asks for them on every check). */
  private readonly groups = new WeakMap<UserAuditLog, ChangeGroup[]>();
  loading = false;
  first = 0;
  readonly pageSize = 20;

  private readonly service = inject(UsersManagementService);
  private readonly transloco = inject(TranslocoService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly actions$ = inject(Actions);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    // a save or enable/disable on this page writes a new audit entry - show it right away on the first page
    this.actions$.pipe(
      ofType(updateUserRolesSuccess, userPopupActionSuccess),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.refresh());
    this.search$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.refresh());
  }

  onSearchInput(value: string) {
    this.search$.next(value.trim());
  }

  clearSearch() {
    this.search = '';
    this.search$.next('');
  }

  /** The actions, sources and fields whose label in the current language contains the search, e.g. "Rolle" -> role. */
  matchingCodes(search: string): string[] {
    const term = search.trim().toLowerCase();
    if (!term) {
      return [];
    }
    const translation = this.transloco.getTranslation(this.transloco.getActiveLang()) ?? {};
    return Object.entries(translation)
      .filter(([, label]) => typeof label === 'string' && label.toLowerCase().includes(term))
      .flatMap(([key]) => CODE_PREFIXES.filter(prefix => key.startsWith(prefix)).map(prefix => key.slice(prefix.length)));
  }

  refresh() {
    this.first = 0;
    this.fetch(0, this.pageSize);
  }

  load(event: TableLazyLoadEvent) {
    const rows = event.rows ?? this.pageSize;
    this.first = event.first ?? 0;
    const page = Math.floor(this.first / rows);
    // deferred: the table emits the first lazy load while it is being rendered
    queueMicrotask(() => this.fetch(page, rows));
  }

  private fetch(page: number, rows: number) {
    this.loading = true;
    this.cdr.markForCheck();
    const search = this.search.trim();
    this.service.getUserAuditLog(this.userId, page, rows, search, this.matchingCodes(search)).subscribe({
      next: result => {
        this.entries = result.content;
        this.totalRecords = result.totalElements;
        if (this.allExpanded) {
          this.expandAllRows();
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  who(entry: UserAuditLog): string {
    return entry.actorEmail ?? this.transloco.translate('@History source ' + entry.source);
  }

  /** The change lines of an entry; a new user's initial values (e-mail, enabled, roles NONE ...) are obvious, so none. */
  visibleChanges(entry: UserAuditLog): UserAuditChange[] {
    return entry.action === 'USER_CREATED' ? [] : entry.changes;
  }

  /**
   * The change lines of an entry in groups: the changes without a domain (e-mail, enabled ...), then the business domain
   * role, then one group per data domain (by name) with its role first, so the changes of two data domains do not mix.
   * Each domain group has the domain as its heading, so its lines do not repeat it.
   */
  groupedChanges(entry: UserAuditLog): ChangeGroup[] {
    let groups = this.groups.get(entry);
    if (!groups) {
      groups = this.group(entry);
      this.groups.set(entry, groups);
    }
    return groups;
  }

  private group(entry: UserAuditLog): ChangeGroup[] {
    const changes = this.visibleChanges(entry);
    const businessDomains = new Set(changes
      .filter(c => c.field === 'role' && (BUSINESS_DOMAIN_ROLES.has(String(c.before)) || BUSINESS_DOMAIN_ROLES.has(String(c.after))))
      .map(c => c.contextKey));
    const byDomain = new Map<string, ChangeGroup>();
    for (const change of changes.filter(c => c.contextKey)) {
      const group = byDomain.get(change.contextKey!)
        ?? {key: change.contextKey!, domain: change.contextName ?? change.contextKey, businessDomain: false, changes: [] as UserAuditChange[]};
      group.changes.push(change);
      byDomain.set(change.contextKey!, group);
    }
    const domains = [...byDomain.values()];
    domains.forEach(group => group.businessDomain = businessDomains.has(group.key));
    domains.forEach(group => group.changes.sort((a, b) => Number(b.field === 'role') - Number(a.field === 'role')));
    const dataDomains = domains.filter(group => !businessDomains.has(group.key))
      .sort((a, b) => (a.domain ?? '').localeCompare(b.domain ?? ''));
    return [
      {key: '', domain: null, businessDomain: false, changes: changes.filter(c => !c.contextKey)},
      ...domains.filter(group => businessDomains.has(group.key)),
      ...dataDomains,
    ].filter(group => group.changes.length > 0);
  }

  /** The changes without a domain (e-mail, enabled ...), shown in the row itself. */
  plainChanges(entry: UserAuditLog): UserAuditChange[] {
    return this.groupedChanges(entry).find(group => group.domain === null)?.changes ?? [];
  }

  /** The changes in the business domain and the data domains, one card each in the expanded row. */
  domainGroups(entry: UserAuditLog): ChangeGroup[] {
    return this.groupedChanges(entry).filter(group => group.domain !== null);
  }

  /** The domains an entry changed, e.g. "Demo, Showcase", shown in the row while it is collapsed. */
  domainSummary(entry: UserAuditLog): string {
    return this.domainGroups(entry).map(group => group.domain).join(', ');
  }

  toggleExpandAll(): void {
    this.allExpanded = !this.allExpanded;
    this.saveAllExpanded();
    if (this.allExpanded) {
      this.expandAllRows();
    } else {
      this.expandedRows = {};
    }
    this.cdr.markForCheck();
  }

  private expandAllRows(): void {
    this.expandedRows = Object.fromEntries(this.entries
      .filter(entry => this.domainGroups(entry).length > 0)
      .map(entry => [entry.id, true]));
  }

  private saveAllExpanded(): void {
    try {
      localStorage.setItem(EXPANDED_STORAGE_KEY, String(this.allExpanded));
    } catch {
      // storage not available, the state is only kept until the page is left
    }
  }

  private loadAllExpanded(): boolean {
    try {
      return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  }

  /** The translated field of a change, e.g. "Role". */
  fieldLabel(change: UserAuditChange): string {
    return this.transloco.translate('@History field ' + change.field);
  }

  formatChange(change: UserAuditChange): string {
    return `${this.changeLabel(change)}: ${this.changeValues(change)}`;
  }

  /** What changed, e.g. "Role [Data domain]" (shown in bold). */
  changeLabel(change: UserAuditChange): string {
    const field = this.transloco.translate('@History field ' + change.field);
    const ctx = change.contextName ?? change.contextKey;
    return ctx ? `${field} [${ctx}]` : field;
  }

  /** How it changed, e.g. "DATA_DOMAIN_ADMIN -> DATA_DOMAIN_VIEWER". */
  changeValues(change: UserAuditChange): string {
    return `${this.formatValue(change.before)} -> ${this.formatValue(change.after)}`;
  }

  private formatValue(value: string | boolean | string[] | null): string {
    if (value == null) {
      return '-';
    }
    if (Array.isArray(value)) {
      return value.length > 0 ? value.join(', ') : '-';
    }
    return String(value);
  }
}
