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

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Store} from '@ngrx/store';
import {AppState} from '../../../store/app/app.state';
import {TranslocoPipe} from '@jsverse/transloco';
import {DatePipe, SlicePipe} from '@angular/common';
import {Table, TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {createBreadcrumbs} from '../../../store/breadcrumb/breadcrumb.action';
import {naviElements} from '../../../app-navi-elements';
import {DomainDashboardComment, DomainDashboardCommentsService} from './domain-comments.service';
import {DashboardCommentStatus, DashboardCommentVersion} from '../../../store/my-dashboards/my-dashboards.model';
import {InputText} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {combineLatest, filter, Subscription} from 'rxjs';
import {selectContextKey, selectContextName} from '../../../store/my-dashboards/my-dashboards.selector';


@Component({
  selector: 'app-domain-comments',
  templateUrl: './domain-comments.component.html',
  styleUrls: ['./domain-comments.component.scss'],
  imports: [
    TranslocoPipe,
    DatePipe,
    SlicePipe,
    TableModule,
    Button,
    Tag,
    Tooltip,
    InputText,
    FormsModule,
    IconField,
    InputIcon
  ]
})
export class DomainDashboardCommentsComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly store = inject<Store<AppState>>(Store);
  private readonly domainCommentsService = inject(DomainDashboardCommentsService);

  private routeSubscription?: Subscription;

  contextKey: string = '';
  contextName: string | string[] = '';
  comments: DomainDashboardComment[] = [];
  loading = true;

  // For filtering
  globalFilterValue: string = '';


  ngOnInit(): void {
    // Subscribe to route params using ngrx selectors
    this.routeSubscription = combineLatest([
      this.store.select(selectContextKey),
      this.store.select(selectContextName)
    ]).pipe(
      filter(([contextKey]) => !!contextKey)
    ).subscribe(([contextKey, contextName]) => {
      // Only reload if contextKey changed
      if (contextKey !== this.contextKey) {
        this.contextKey = contextKey!;
        this.contextName = contextName || contextKey!;
        this.globalFilterValue = '';
        this.createBreadcrumbs();
        this.loadComments();
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.myDashboards.label,
          routerLink: naviElements.myDashboards.path
        },
        {
          label: this.contextName,
          routerLink: naviElements.myDashboards.path
        },
        {
          label: '@Domain Comments'
        }
      ]
    }));
  }

  private loadComments(): void {
    this.loading = true;
    this.domainCommentsService.getCommentsForDomain(this.contextKey).subscribe({
      next: (comments: DomainDashboardComment[]) => {
        this.comments = comments;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getStatusSeverity(status: DashboardCommentStatus | string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (status) {
      case DashboardCommentStatus.PUBLISHED:
      case 'PUBLISHED':
        return 'success';
      case DashboardCommentStatus.DRAFT:
      case 'DRAFT':
        return 'warn';
      default:
        return 'info';
    }
  }

  getStatusLabel(status: DashboardCommentStatus | string): string {
    return status === DashboardCommentStatus.PUBLISHED || status === 'PUBLISHED' ? '@Published' : '@DRAFT';
  }

  getActiveVersionData(comment: DomainDashboardComment): DashboardCommentVersion | undefined {
    if (!comment.history || comment.history.length === 0) {
      return undefined;
    }
    return comment.history.find(v => v.version === comment.activeVersion);
  }

  navigateToDashboard(comment: DomainDashboardComment): void {
    if (comment.dashboardId && comment.instanceName) {
      this.router.navigate([
        'my-dashboards',
        'detail',
        comment.instanceName,
        comment.dashboardId
      ]);
    }
  }

  onGlobalFilter(table: Table, event: Event): void {
    table.filterGlobal((event.target as HTMLInputElement).value, 'contains');
  }

  clearFilter(table: Table): void {
    this.globalFilterValue = '';
    table.clear();
  }
}

