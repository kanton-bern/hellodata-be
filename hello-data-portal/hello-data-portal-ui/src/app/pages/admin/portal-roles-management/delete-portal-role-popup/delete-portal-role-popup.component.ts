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

import { Component, Input, inject } from '@angular/core';
import {Observable, tap} from "rxjs";
import {Action, Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {
  selectSelectedPortalRoleForDeletion
} from "../../../../store/portal-roles-management/portal-roles-management.selector";
import { ConfirmationService, ConfirmEventType, PrimeTemplate } from "primeng/api";
import {TranslateService} from "../../../../shared/services/translate.service";
import {PortalRole} from "../../../../store/portal-roles-management/portal-roles-management.model";
import {hideDeletePortalRolePopup} from "../../../../store/portal-roles-management/portal-roles-management.action";
import { NgIf, AsyncPipe } from '@angular/common';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Button, ButtonDirective } from 'primeng/button';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
    selector: 'app-delete-role-popup[action]',
    templateUrl: './delete-portal-role-popup.component.html',
    styleUrls: ['./delete-portal-role-popup.component.scss'],
    imports: [NgIf, ConfirmDialog, PrimeTemplate, Button, ButtonDirective, AsyncPipe, TranslocoPipe]
})
export class DeletePortalRolePopupComponent {
  private store = inject<Store<AppState>>(Store);
  private confirmationService = inject(ConfirmationService);
  private translateService = inject(TranslateService);

  @Input()
  action!: Action;
  roleToBeDeleted$: Observable<any>;

  constructor() {
    this.roleToBeDeleted$ = this.store.select(selectSelectedPortalRoleForDeletion).pipe(tap(portalRoleForDeletion => {
      this.confirmDeleteRole(portalRoleForDeletion);
    }));
  }

  deleteRole() {
    this.store.dispatch(this.action);
  }

  hideDeletionPopup(): void {
    this.store.dispatch(hideDeletePortalRolePopup());
  }

  private confirmDeleteRole(portalRoleForDeletion: PortalRole | null) {
    if (portalRoleForDeletion) {
      this.confirmationService.confirm({
        message: this.translateService.translate('@Delete role question', {role: portalRoleForDeletion.name}),
        icon: 'fas fa-triangle-exclamation',
        accept: () => {
          this.deleteRole();
        },
        reject: (type: ConfirmEventType) => {
          this.hideDeletionPopup();
        }
      });
    } else {
      this.hideDeletionPopup();
    }
  }

}
