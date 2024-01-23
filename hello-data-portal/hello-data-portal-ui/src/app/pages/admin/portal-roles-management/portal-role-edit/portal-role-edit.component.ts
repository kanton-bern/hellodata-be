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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Observable, Subscription, tap} from "rxjs";
import {selectAvailablePermissions, selectEditedPortalRole} from "../../../../store/portal-roles-management/portal-roles-management.selector";
import {Navigate} from "../../../../store/app/app.action";
import {DeleteEditedPortalRole, SaveChangesToPortalRole, ShowDeletePortalRolePopup} from "../../../../store/portal-roles-management/portal-roles-management.action";
import {PortalRole} from "../../../../store/portal-roles-management/portal-roles-management.model";
import {LoadAppInfoResources, LoadPermissionResources} from "../../../../store/metainfo-resource/metainfo-resource.action";
import {selectAppInfos} from "../../../../store/metainfo-resource/metainfo-resource.selector";
import {selectAvailableDataDomainItems} from "../../../../store/my-dashboards/my-dashboards.selector";
import {CreateBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../../app-navi-elements";
import {MarkUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";

@Component({
  selector: 'app-role-edit',
  templateUrl: './portal-role-edit.component.html',
  styleUrls: ['./portal-role-edit.component.scss']
})
export class PortalRoleEditComponent implements OnInit, OnDestroy {

  editedRole$: Observable<any>;
  workspaces$: Observable<any>;
  availableDataDomains$: Observable<any>;
  availableDataPermissions$: Observable<any>;
  roleForm!: FormGroup;

  allPermissions: any[] = [];
  filteredPermissions: any[] = [];
  formValueChangedSub!: Subscription;

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomainItems);
    this.editedRole$ = this.store.select(selectEditedPortalRole);
    this.availableDataPermissions$ = this.store.select(selectAvailablePermissions).pipe(
      tap(availablePermissions => {
        this.allPermissions = availablePermissions;
        this.filteredPermissions = availablePermissions;
      }));
    this.workspaces$ = this.store.select(selectAppInfos);
    this.store.dispatch(new LoadPermissionResources());
    this.store.dispatch(new LoadAppInfoResources());
  }

  ngOnInit(): void {
    this.editedRole$ = this.store.select(selectEditedPortalRole).pipe(
      tap(role => {
        this.roleForm = this.fb.group({
          name: [role?.name, Validators.compose([Validators.required.bind(this), Validators.minLength(3), Validators.maxLength(255), Validators.pattern('[A-Za-z0-9_ ]*')])],
          description: [role?.description, Validators.maxLength(2048)],
          permissions: [role?.permissions ? role.permissions : []]
        });
        this.createBreadcrumbs(role);
        this.unsubFormValueChanges();
        this.formValueChangedSub = this.roleForm.valueChanges.subscribe(newValues => {
          this.onChange(role);
        });
      })
    );
  }

  saveRole(editedRole: any): void {
    const role = this.roleForm.getRawValue() as any;
    role.id = editedRole.id;
    role.contextKey = role.dataDomain;
    this.store.dispatch(new SaveChangesToPortalRole(role));
  }

  openDeletePopup(editedRole: PortalRole): void {
    this.store.dispatch(new ShowDeletePortalRolePopup(editedRole));
  }

  navigateToRoleList(): void {
    this.store.dispatch(new Navigate('roles-management'));
  }

  getDeletionAction() {
    return new DeleteEditedPortalRole();
  }

  filterPermission($event: any) {
    const filtered = [];
    const query = $event.query;
    for (let i = 0; i < this.allPermissions.length; i++) {
      const permission = this.allPermissions[i];
      if (permission.toLowerCase().indexOf(query.toLowerCase()) == 0) {
        filtered.push(permission);
      }
    }
    this.filteredPermissions = filtered.filter(perm => !this.roleForm.get('permissions')!.getRawValue().includes(perm));
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private createBreadcrumbs(role: PortalRole) {
    if (role.id) {
      this.createBreadcrumbsEditedRole(role);
    } else {
      this.createBreadcrumbsNewRole();
    }
  }

  private createBreadcrumbsNewRole() {
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.rolesManagement.label,
        routerLink: naviElements.rolesManagement.path,
      },
      {
        label: naviElements.roleCreate.label,
      }
    ]));
  }

  private createBreadcrumbsEditedRole(role: PortalRole) {
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.rolesManagement.label,
        routerLink: naviElements.rolesManagement.path,
      },
      {
        label: role.name,
      }
    ]));
  }

  private onChange(editedRole: PortalRole) {
    const role = this.roleForm.getRawValue() as any;
    role.id = editedRole.id;
    role.contextKey = role.dataDomain;
    this.store.dispatch(new MarkUnsavedChanges(new SaveChangesToPortalRole(role), role.id === undefined));
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
