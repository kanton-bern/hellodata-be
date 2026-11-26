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
import {TranslocoPipe, TranslocoService} from '@jsverse/transloco';
import {UsersManagementService} from '../../../../../store/users-management/users-management.service';
import {UserAuditChange, UserAuditLog} from '../../../../../store/users-management/users-management.model';
import {updateUserRolesSuccess, userPopupActionSuccess} from '../../../../../store/users-management/users-management.action';

@Component({
  selector: 'app-user-audit-history',
  templateUrl: './user-audit-history.component.html',
  imports: [TableModule, PrimeTemplate, DatePipe, TranslocoPipe]
})
export class UserAuditHistoryComponent implements OnInit {
  @Input({required: true}) userId!: string;

  entries: UserAuditLog[] = [];
  totalRecords = 0;
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
    this.service.getUserAuditLog(this.userId, page, rows).subscribe({
      next: result => {
        this.entries = result.content;
        this.totalRecords = result.totalElements;
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

  context(entry: UserAuditLog): string {
    return entry.contextName ?? entry.contextKey ?? '';
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
