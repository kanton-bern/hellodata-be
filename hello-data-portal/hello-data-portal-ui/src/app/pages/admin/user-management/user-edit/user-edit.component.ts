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

import {ChangeDetectorRef, Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {combineLatest, Observable, Subscription, tap} from "rxjs";
import {
  selectAllBusinessDomains,
  selectAllDataDomains,
  selectAvailableRolesForBusinessDomain,
  selectAvailableRolesForDataDomain,
  selectEditedUser,
  selectUserContextRoles,
  selectUserSaveButtonDisabled
} from "../../../../store/users-management/users-management.selector";
import {
  BUSINESS_DOMAIN_ADMIN_ROLE,
  CommentPermissions,
  DashboardForUser,
  DATA_DOMAIN_BUSINESS_SPECIALIST_ROLE,
  DATA_DOMAIN_VIEWER_ROLE,
  HELLODATA_ADMIN_ROLE,
  NONE_ROLE,
  User,
  UserAction
} from "../../../../store/users-management/users-management.model";
import {selectIsSuperuser} from "../../../../store/auth/auth.selector";
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Context} from "../../../../store/users-management/context-role.model";
import {naviElements} from "../../../../app-navi-elements";
import {markUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";
import {BaseComponent} from "../../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {
  loadAvailableContextRoles,
  loadAvailableContexts,
  loadDashboards,
  loadUserById,
  loadUserContextRoles,
  navigateToUsersManagement,
  selectBusinessDomainRoleForEditedUser,
  selectDataDomainRoleForEditedUser,
  setSelectedDashboardForUser,
  showUserActionPopup,
  updateUserRoles
} from "../../../../store/users-management/users-management.action";
import {AsyncPipe} from '@angular/common';
import {Toolbar} from 'primeng/toolbar';
import {Button} from 'primeng/button';
import {Tooltip} from 'primeng/tooltip';
import {Divider} from 'primeng/divider';
import {Select} from 'primeng/select';
import {Checkbox} from 'primeng/checkbox';
import {
  DashboardViewerPermissionsComponent
} from './dashboard-viewer-permissions/dashboard-viewer-permissions.component';
import {Ripple} from 'primeng/ripple';
import {ActionsUserPopupComponent} from '../actions-user-popup/actions-user-popup.component';
import {TranslocoPipe} from '@jsverse/transloco';

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, Toolbar, Button, Tooltip, Divider, Select, Checkbox, DashboardViewerPermissionsComponent, Ripple, ActionsUserPopupComponent, AsyncPipe, TranslocoPipe]
})
export class UserEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedUser$: Observable<any>;
  businessDomains$: Observable<any>;
  dataDomains$: Observable<any>;
  availableBusinessDomainRoles$: Observable<any>;
  availableDataDomainRoles$: Observable<any>;
  userSaveButtonDisabled$: Observable<boolean>;
  /**
   * data domain context key as a key
   */
  dashboardTableVisibility = new Map<string, boolean>();
  /**
   * Comment permissions per data domain context key
   */
  commentPermissions = new Map<string, CommentPermissions>();
  /**
   * Tracks if any business domain has admin role (HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN)
   */
  hasBusinessAdminRole = false;
  userForm!: FormGroup;
  saveDisabled = false;
  private readonly store = inject<Store<AppState>>(Store);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly userContextRoles$: Observable<any>;
  private userContextRolesSub!: Subscription;

  constructor() {
    super();
    this.store.dispatch(loadDashboards());
    this.store.dispatch(loadAvailableContextRoles());
    this.store.dispatch(loadAvailableContexts());
    this.store.dispatch(loadUserContextRoles());
    this.store.dispatch(loadUserById());
    this.editedUser$ = this.store.select(selectEditedUser).pipe(tap(editedUser => {
      this.createBreadcrumbs(editedUser);
    }));
    this.businessDomains$ = this.store.select(selectAllBusinessDomains);
    this.dataDomains$ = this.store.select(selectAllDataDomains);
    this.availableBusinessDomainRoles$ = this.store.select(selectAvailableRolesForBusinessDomain);
    this.availableDataDomainRoles$ = this.store.select(selectAvailableRolesForDataDomain);
    this.userContextRoles$ = combineLatest([
      this.store.select(selectUserContextRoles),
      this.store.select(selectIsSuperuser),
      this.store.select(selectEditedUser)
    ]).pipe(tap(([userContextRoles, isCurrentSuperuser, editedUser]) => {
      this.generateForm(userContextRoles, isCurrentSuperuser, editedUser ? editedUser.superuser : false);
    }));
    this.userSaveButtonDisabled$ = this.store.select(selectUserSaveButtonDisabled);
  }

  override ngOnInit() {
    super.ngOnInit();
    this.userContextRolesSub = this.userContextRoles$.subscribe();
  }

  ngOnDestroy() {
    if (this.userContextRolesSub) {
      this.userContextRolesSub.unsubscribe();
    }
  }

  cancel() {
    this.store.dispatch(navigateToUsersManagement());
  }


  showUserDisablePopup(data: User) {
    this.store.dispatch(showUserActionPopup({
      userActionForPopup: {
        user: data,
        action: UserAction.DISABLE,
        actionFromUsersEdition: true
      }
    }));
  }

  showUserEnablePopup(data: User) {
    this.store.dispatch(showUserActionPopup({
      userActionForPopup: {
        user: data,
        action: UserAction.ENABLE,
        actionFromUsersEdition: true
      }
    }));
  }

  onBusinessDomainRoleSelected($event: any, dataDomains: Context[], availableDataDomainRoles: any[]) {
    const isAdminRole = [HELLODATA_ADMIN_ROLE, BUSINESS_DOMAIN_ADMIN_ROLE].includes($event.value.name);
    this.hasBusinessAdminRole = isAdminRole;

    if ($event.value.name !== NONE_ROLE) {
      this.dashboardTableVisibility.forEach((value, key) => {
        this.dashboardTableVisibility.set(key, false);
      });
      const dataDomainAdmin = availableDataDomainRoles.find(dataDomainRole => dataDomainRole.name === 'DATA_DOMAIN_ADMIN');
      dataDomains.forEach(dataDomain => {
        if (this.userForm) {
          this.userForm.get(dataDomain?.contextKey as string)?.setValue(dataDomainAdmin);
        }
        this.store.dispatch(selectDataDomainRoleForEditedUser({
          selectedRoleForContext: {
            role: dataDomainAdmin,
            context: dataDomain
          }
        }));
      })
    }

    // Set all comment permissions to true when admin role is selected
    if (isAdminRole) {
      dataDomains.forEach(dataDomain => {
        this.commentPermissions.set(dataDomain.contextKey as string, {
          readComments: true,
          writeComments: true,
          reviewComments: true
        });
      });
    }

    this.store.dispatch(selectBusinessDomainRoleForEditedUser({selectedRole: $event.value}));
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  onDataDomainRoleSelected($event: any, dataDomain: Context) {
    const contextKey = dataDomain.contextKey as string;
    if ([DATA_DOMAIN_VIEWER_ROLE, DATA_DOMAIN_BUSINESS_SPECIALIST_ROLE].includes($event.value.name)) {
      this.dashboardTableVisibility.set(contextKey, true);
    } else if (![DATA_DOMAIN_VIEWER_ROLE, DATA_DOMAIN_BUSINESS_SPECIALIST_ROLE].includes($event.value.name)) {
      this.store.dispatch(setSelectedDashboardForUser({dashboards: [], contextKey}));
      this.dashboardTableVisibility.set(contextKey, false);
    }
    // Clear comment permissions when role is NONE
    if ($event.value.name === NONE_ROLE) {
      this.commentPermissions.set(contextKey, {
        readComments: false,
        writeComments: false,
        reviewComments: false
      });
    }
    this.store.dispatch(selectDataDomainRoleForEditedUser({
      selectedRoleForContext: {
        role: $event.value,
        context: dataDomain
      }
    }));
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  updateUser() {
    this.store.dispatch(updateUserRoles());
  }

  selectedDashboardsEvent(dashboards: DashboardForUser[], dataDomain: Context) {
    console.debug('selectedDashboardsEvent', dashboards);
    this.store.dispatch(setSelectedDashboardForUser({dashboards, contextKey: dataDomain.contextKey as string}));
  }

  getCommentPermissions(contextKey: string): CommentPermissions {
    // If business admin role, always return all permissions as true
    if (this.hasBusinessAdminRole) {
      return {
        readComments: true,
        writeComments: true,
        reviewComments: true
      };
    }
    if (!this.commentPermissions.has(contextKey)) {
      this.commentPermissions.set(contextKey, {
        readComments: false,
        writeComments: false,
        reviewComments: false
      });
    }
    return this.commentPermissions.get(contextKey)!;
  }

  onCommentPermissionChange(contextKey: string, permission: keyof CommentPermissions, value: boolean): void {
    const permissions = this.getCommentPermissions(contextKey);
    permissions[permission] = value;
    // Auto-enable readComments when writeComments or reviewComments is checked
    if (value && (permission === 'writeComments' || permission === 'reviewComments')) {
      permissions.readComments = true;
    }
    this.commentPermissions.set(contextKey, permissions);
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  isRoleNone(contextKey: string): boolean {
    const control = this.userForm?.get(contextKey);
    return control?.value?.name === NONE_ROLE;
  }

  isReadCommentDisabled(contextKey: string): boolean {
    if (this.isRoleNone(contextKey) || this.hasBusinessAdminRole) {
      return true;
    }
    const permissions = this.getCommentPermissions(contextKey);
    return permissions.writeComments || permissions.reviewComments;
  }

  isCommentPermissionChecked(contextKey: string, permission: keyof CommentPermissions): boolean {
    if (this.hasBusinessAdminRole) {
      return true;
    }
    return this.getCommentPermissions(contextKey)[permission];
  }

  private createBreadcrumbs(editedUser: User | null) {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.userManagement.label,
          routerLink: naviElements.userManagement.path
        },
        {
          label: editedUser?.email
        }
      ]
    }));
  }

  private generateForm(userContextRoles: any[], isCurrentSuperuser: boolean, editedUserSuperuser: boolean) {
    if (userContextRoles.length > 0) {
      this.userForm = this.fb.group({});

      // Check if any context has admin role (HELLODATA_ADMIN or BUSINESS_DOMAIN_ADMIN are only for business domains)
      this.hasBusinessAdminRole = userContextRoles.some(ucr =>
        [HELLODATA_ADMIN_ROLE, BUSINESS_DOMAIN_ADMIN_ROLE].includes(ucr.role.name)
      );

      userContextRoles.forEach(userContextRole => {
        const contextKey = userContextRole.context.contextKey as string;

        if ([DATA_DOMAIN_VIEWER_ROLE, DATA_DOMAIN_BUSINESS_SPECIALIST_ROLE].includes(userContextRole.role.name)) {
          this.dashboardTableVisibility.set(contextKey, true);
        }

        const disabled = editedUserSuperuser && !isCurrentSuperuser;
        this.saveDisabled = disabled;
        const control = new FormControl({
          value: userContextRole.role,
          disabled
        });
        this.userForm.addControl(contextKey, control);
      });

      // Force change detection to update checkbox states
      this.cdr.detectChanges();
    }
  }
}
