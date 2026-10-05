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
import {combineLatest} from 'rxjs';
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
import {RlsDataDomain, RlsRole} from '../../../store/rls-roles/rls-roles.model';
import {ICON_REGISTRY} from '../../../shared/icons';
import {selectSelectedDataDomain} from '../../../store/my-dashboards/my-dashboards.selector';

/**
 * Lets a data domain admin give the RLS roles (RLS_01 - RLS_15) of a data domain a meaningful name.
 * The data domain is taken from the global data domain selector in the header.
 */
@Component({
  selector: 'app-rls-groups',
  templateUrl: './rls-groups.component.html',
  imports: [FormsModule, PrimeTemplate, TranslocoPipe, Card, TableModule, InputText, Button, Ripple, Toolbar]
})
export class RlsGroupsComponent extends BaseComponent implements OnInit {
  protected readonly icons = ICON_REGISTRY;
  readonly maxNameLength = 150;
  readonly rlsRoles = signal<RlsRole[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  // true if a single data domain is selected in the header but the current user can't manage its RLS groups
  readonly notManageable = signal(false);
  selectedDataDomain: RlsDataDomain | null = null;

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
          this.selectedDataDomain = contextKey ? manageableDataDomains.find(dataDomain => dataDomain.key === contextKey) ?? null : null;
          this.notManageable.set(!!contextKey && !this.selectedDataDomain);
          if (this.selectedDataDomain) {
            this.loadRlsRoles();
          } else {
            this.rlsRoles.set([]);
          }
        },
        error: error => this.store.dispatch(showError({error}))
      });
  }

  hasDuplicateName(rlsRole: RlsRole): boolean {
    const name = this.effectiveName(rlsRole);
    return this.rlsRoles().some(other => other.roleKey !== rlsRole.roleKey && this.effectiveName(other) === name);
  }

  isValid(): boolean {
    return this.rlsRoles().every(rlsRole => (rlsRole.name ?? '').length <= this.maxNameLength && !this.hasDuplicateName(rlsRole));
  }

  resetName(rlsRole: RlsRole): void {
    rlsRole.name = rlsRole.roleKey;
  }

  save(): void {
    if (!this.selectedDataDomain || !this.isValid()) {
      return;
    }
    this.saving.set(true);
    const payload = this.rlsRoles().map(rlsRole => ({roleKey: rlsRole.roleKey, name: (rlsRole.name ?? '').trim()}));
    this.rlsRolesService.updateRlsRoleNames(this.selectedDataDomain.key, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rlsRoles => {
          this.rlsRoles.set(rlsRoles);
          this.saving.set(false);
          this.store.dispatch(showSuccess({message: '@RLS group names saved'}));
        },
        error: error => {
          this.saving.set(false);
          this.store.dispatch(showError({error}));
        }
      });
  }

  private loadRlsRoles(): void {
    if (!this.selectedDataDomain) {
      return;
    }
    this.loading.set(true);
    this.rlsRolesService.getRlsRoles(this.selectedDataDomain.key)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rlsRoles => {
          this.rlsRoles.set(rlsRoles);
          this.loading.set(false);
        },
        error: error => {
          this.loading.set(false);
          this.store.dispatch(showError({error}));
        }
      });
  }

  private effectiveName(rlsRole: RlsRole): string {
    const name = (rlsRole.name ?? '').trim();
    return (name.length === 0 ? rlsRole.roleKey : name).toLowerCase();
  }
}
