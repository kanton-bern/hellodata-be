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

import {Component, NgModule, OnInit} from '@angular/core';
import {CommonModule} from "@angular/common";
import {ReactiveFormsModule} from "@angular/forms";
import {PortalRoleEditComponent} from './portal-role-edit/portal-role-edit.component';
import {Action, Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {Observable} from "rxjs";
import {selectPortalRoles} from "../../../store/portal-roles-management/portal-roles-management.selector";
import {DeletePortalRolePopupComponent} from "./delete-portal-role-popup/delete-portal-role-popup.component";
import {PortalRole} from "../../../store/portal-roles-management/portal-roles-management.model";
import {TranslocoModule} from "@jsverse/transloco";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {SharedModule} from "primeng/api";
import {ToolbarModule} from "primeng/toolbar";
import {EditorModule} from "primeng/editor";
import {TableModule} from "primeng/table";
import {TagModule} from "primeng/tag";
import {TextareaModule} from 'primeng/textarea';
import {SelectModule} from 'primeng/select';
import {AutoCompleteModule} from "primeng/autocomplete";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {TooltipModule} from "primeng/tooltip";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {
  deletePortalRole,
  loadPortalRoles,
  openPortalRoleEdition,
  showDeletePortalRolePopup
} from "../../../store/portal-roles-management/portal-roles-management.action";

@Component({
  selector: 'app-roles-management',
  templateUrl: './portal-roles-management.component.html',
  styleUrls: ['./portal-roles-management.component.scss'],
  standalone: false
})
export class PortalRolesManagementComponent extends BaseComponent implements OnInit {

  roles$: Observable<any>;

  constructor(private store: Store<AppState>) {
    super();
    this.roles$ = this.store.select(selectPortalRoles);
    this.store.dispatch(loadPortalRoles());
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.rolesManagement.label,
          routerLink: naviElements.rolesManagement.path,
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  showRoleDeletionPopup(data: PortalRole) {
    this.store.dispatch(showDeletePortalRolePopup({role: data}));
  }

  getDeletionAction(): Action {
    return deletePortalRole();
  }

  editRole(data: PortalRole): void {
    this.store.dispatch(openPortalRoleEdition({role: data}));
  }

}


@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    ButtonModule,
    RippleModule,
    SharedModule,
    ToolbarModule,
    EditorModule,
    TableModule,
    TagModule,
    TextareaModule,
    SelectModule,
    AutoCompleteModule,
    ConfirmDialogModule,
    TooltipModule
  ],
  declarations: [
    PortalRolesManagementComponent,
    PortalRoleEditComponent,
    DeletePortalRolePopupComponent
  ],
  exports: []
})
export class RolesManagementModule {
}
