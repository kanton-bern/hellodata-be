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

import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
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
import {DomainComment, DomainCommentsService} from './domain-comments.service';
import {DashboardCommentStatus} from '../../../store/my-dashboards/my-dashboards.model';
import {InputText} from 'primeng/inputtext';
import {FormsModule} from '@angular/forms';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';


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
export class DomainCommentsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject<Store<AppState>>(Store);
  private readonly domainCommentsService = inject(DomainCommentsService);

  contextKey: string = '';
  contextName: string = '';
  comments: DomainComment[] = [];
  loading = true;

  // For filtering
  globalFilterValue: string = '';


  ngOnInit(): void {
    this.contextKey = this.route.snapshot.paramMap.get('contextKey') || '';
    this.contextName = this.route.snapshot.queryParamMap.get('contextName') || this.contextKey;

    this.createBreadcrumbs();
    this.loadComments();
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
      next: (comments: DomainComment[]) => {
        this.comments = comments;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  getStatusSeverity(status: DashboardCommentStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (status) {
      case DashboardCommentStatus.PUBLISHED:
        return 'success';
      case DashboardCommentStatus.DRAFT:
        return 'warn';
      default:
        return 'info';
    }
  }

  getStatusLabel(status: DashboardCommentStatus): string {
    return status === DashboardCommentStatus.PUBLISHED ? '@Published' : '@Draft';
  }

  navigateToDashboard(comment: DomainComment): void {
    if (comment.dashboardId) {
      this.router.navigate([
        'my-dashboards',
        'detail',
        this.contextKey,
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

