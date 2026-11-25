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

import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {AsyncPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {Store} from '@ngrx/store';
import {Observable} from 'rxjs';
import {filter} from 'rxjs/operators';
import {Card} from 'primeng/card';
import {Button} from 'primeng/button';
import {Select} from 'primeng/select';
import {TranslocoPipe} from '@jsverse/transloco';
import {AppState} from '../../../../store/app/app.state';
import {selectAllDataDomains, selectEditedUser} from '../../../../store/users-management/users-management.selector';
import {loadAvailableContexts, loadUserById} from '../../../../store/users-management/users-management.action';
import {User} from '../../../../store/users-management/users-management.model';
import {createBreadcrumbs} from '../../../../store/breadcrumb/breadcrumb.action';
import {naviElements} from '../../../../app-navi-elements';
import {ICON_REGISTRY} from '../../../../shared/icons';
import {UserAuditHistoryComponent} from '../user-edit/user-audit-history/user-audit-history.component';

/**
 * The audit logs of one user on a page of their own (user management -> user -> Audit logs), filterable by data domain.
 */
@Component({
  selector: 'app-user-audit-logs',
  templateUrl: './user-audit-logs.component.html',
  imports: [AsyncPipe, FormsModule, Card, Button, Select, TranslocoPipe, UserAuditHistoryComponent]
})
export class UserAuditLogsComponent implements OnInit {
  protected readonly icons = ICON_REGISTRY;
  readonly editedUser$: Observable<User | null>;
  readonly dataDomains$: Observable<any[]>;
  /** The context key of the selected data domain, null for all of them. */
  selectedContextKey: string | null = null;

  private readonly store = inject<Store<AppState>>(Store);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.store.dispatch(loadAvailableContexts());
    this.store.dispatch(loadUserById());
    this.editedUser$ = this.store.select(selectEditedUser);
    this.dataDomains$ = this.store.select(selectAllDataDomains);
  }

  ngOnInit(): void {
    this.editedUser$.pipe(
      filter((user): user is User => !!user),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(user => this.createBreadcrumbs(user));
  }

  back(user: User) {
    this.router.navigate([naviElements.userManagement.path, 'edit', user.id]);
  }

  private createBreadcrumbs(user: User) {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.userManagement.label,
          routerLink: naviElements.userManagement.path
        },
        {
          label: user.email,
          routerLink: `${naviElements.userManagement.path}/edit/${user.id}`
        },
        {
          label: naviElements.userAuditLogs.label
        }
      ]
    }));
  }
}
