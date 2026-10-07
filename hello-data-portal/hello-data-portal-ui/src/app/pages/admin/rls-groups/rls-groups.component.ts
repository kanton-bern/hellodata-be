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

import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {combineLatest, forkJoin} from 'rxjs';
import {map} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Store} from '@ngrx/store';
import {FormsModule} from '@angular/forms';
import {TranslocoPipe} from '@jsverse/transloco';
import {Card} from 'primeng/card';
import {TableModule} from 'primeng/table';
import {PrimeTemplate} from 'primeng/api';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {Toolbar} from 'primeng/toolbar';
import {AppState} from '../../../store/app/app.state';
import {BaseComponent} from '../../../shared/components/base/base.component';
import {createBreadcrumbs} from '../../../store/breadcrumb/breadcrumb.action';
import {showError, showSuccess} from '../../../store/app/app.action';
import {naviElements} from '../../../app-navi-elements';
import {RlsRolesService} from '../../../store/rls-roles/rls-roles.service';
import type {RlsDataDomain, RlsRole} from '../../../store/rls-roles/rls-roles.model';
import {ICON_REGISTRY} from '../../../shared/icons';
import {selectSelectedDataDomain} from '../../../store/my-dashboards/my-dashboards.selector';

interface RlsGroupRow extends RlsRole {
  contextKey: string;
  dataDomainName: string;
}

/**
 * Lets a data domain admin give the RLS roles (RLS_01 - RLS_15) of a data domain a meaningful name.
 * The data domain is taken from the global data domain selector in the header; if all data domains are selected,
 * the RLS roles of all data domains the user can manage are shown.
 */
@Component({
  selector: 'app-rls-groups',
  templateUrl: './rls-groups.component.html',
  imports: [FormsModule, PrimeTemplate, TranslocoPipe, Card, TableModule, InputText, Button, Ripple, Toolbar]
})
export class RlsGroupsComponent extends BaseComponent implements OnInit {
  protected readonly icons = ICON_REGISTRY;
  readonly maxNameLength = 150;
  readonly rows = signal<RlsGroupRow[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  // true if all data domains are selected in the header - the data domain column is shown
  readonly showAllDataDomains = signal(false);
  // true if a single data domain is selected in the header but the current user can't manage its RLS groups
  readonly notManageable = signal(false);
  private visibleDataDomains: RlsDataDomain[] = [];

  private readonly store = inject<Store<AppState>>(Store);
  private readonly rlsRolesService = inject(RlsRolesService);
  private readonly destroyRef = inject(DestroyRef);

  override ngOnInit(): void {
    super.ngOnInit();
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [{label: naviElements.rlsGroups.label, routerLink: naviElements.rlsGroups.path}]
    }));
    combineLatest([
      this.rlsRolesService.getManageableDataDomains(),
      this.store.select(selectSelectedDataDomain)
    ]).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([manageableDataDomains, selectedDataDomain]) => {
          const contextKey = selectedDataDomain?.key;
          this.showAllDataDomains.set(!contextKey);
          this.visibleDataDomains = contextKey
            ? manageableDataDomains.filter(dataDomain => dataDomain.key === contextKey)
            : manageableDataDomains;
          this.notManageable.set(!!contextKey && this.visibleDataDomains.length === 0);
          this.loadRows();
        },
        error: error => this.store.dispatch(showError({error}))
      });
  }

  hasDuplicateName(row: RlsGroupRow): boolean {
    const name = this.effectiveName(row);
    return this.rows().some(other => other.contextKey === row.contextKey && other.roleKey !== row.roleKey && this.effectiveName(other) === name);
  }

  isValid(): boolean {
    return this.rows().every(row => (row.name ?? '').length <= this.maxNameLength && !this.hasDuplicateName(row));
  }

  resetName(row: RlsGroupRow): void {
    row.name = row.roleKey;
  }

  save(): void {
    if (this.visibleDataDomains.length === 0 || !this.isValid()) {
      return;
    }
    this.saving.set(true);
    const updates = this.visibleDataDomains.map(dataDomain => {
      const payload = this.rows()
        .filter(row => row.contextKey === dataDomain.key)
        .map(row => ({roleKey: row.roleKey, name: (row.name ?? '').trim()}));
      return this.rlsRolesService.updateRlsRoleNames(dataDomain.key, payload).pipe(
        map(rlsRoles => this.toRows(dataDomain, rlsRoles))
      );
    });
    forkJoin(updates)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rowsPerDataDomain => {
          this.rows.set(rowsPerDataDomain.flat());
          this.saving.set(false);
          this.store.dispatch(showSuccess({message: '@RLS group names saved'}));
        },
        error: error => {
          this.saving.set(false);
          this.store.dispatch(showError({error}));
        }
      });
  }

  private loadRows(): void {
    if (this.visibleDataDomains.length === 0) {
      this.rows.set([]);
      return;
    }
    this.loading.set(true);
    const requests = this.visibleDataDomains.map(dataDomain =>
      this.rlsRolesService.getRlsRoles(dataDomain.key).pipe(map(rlsRoles => this.toRows(dataDomain, rlsRoles)))
    );
    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rowsPerDataDomain => {
          this.rows.set(rowsPerDataDomain.flat());
          this.loading.set(false);
        },
        error: error => {
          this.loading.set(false);
          this.store.dispatch(showError({error}));
        }
      });
  }

  private toRows(dataDomain: RlsDataDomain, rlsRoles: RlsRole[]): RlsGroupRow[] {
    return rlsRoles.map(rlsRole => ({...rlsRole, contextKey: dataDomain.key, dataDomainName: dataDomain.name}));
  }

  private effectiveName(row: RlsGroupRow): string {
    const name = (row.name ?? '').trim();
    return (name.length === 0 ? row.roleKey : name).toLowerCase();
  }
}
