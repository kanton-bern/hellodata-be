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
import {combineLatest, map, Observable, Subscription, tap} from "rxjs";
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
  DashboardForUser,
  DATA_DOMAIN_VIEWER_ROLE,
  NONE_ROLE,
  User,
  UserAction
} from "../../../../store/users-management/users-management.model";
import {selectIsSuperuser} from "../../../../store/auth/auth.selector";
import {FormBuilder, FormControl, FormGroup} from "@angular/forms";
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

@Component({
  selector: 'app-user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.scss'],
  standalone: false
})
export class UserEditComponent extends BaseComponent implements OnInit, OnDestroy {

  editedUser$: Observable<any>;
  businessDomains$: Observable<any>;
  dataDomains$: Observable<any>;
  availableBusinessDomainRoles$: Observable<any>;
  availableDataDomainRoles$: Observable<any>;
  /**
   * data domain context key as a key
   */
  dashboardTableVisibility = new Map<string, boolean>();
  userForm!: FormGroup;

  userSaveButtonDisabled$: Observable<boolean>;
  private userContextRoles$: Observable<any>;
  private userContextRolesSub!: Subscription;
  private editedUserSuperuser = false;

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
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
    this.userSaveButtonDisabled$ = this.store.select(selectUserSaveButtonDisabled).pipe(map(userSaveButtonDisabled => {
      return userSaveButtonDisabled;
    }));
    this.userContextRoles$ = combineLatest([
      this.store.select(selectUserContextRoles),
      this.store.select(selectIsSuperuser),
      this.store.select(selectEditedUser)
    ]).pipe(tap(([userContextRoles, isCurrentSuperuser, editedUser]) => {
      this.generateForm(userContextRoles, isCurrentSuperuser, editedUser ? editedUser.superuser : false);
    }));
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
    this.store.dispatch(selectBusinessDomainRoleForEditedUser({selectedRole: $event.value}));
    this.store.dispatch(markUnsavedChanges({action: updateUserRoles()}));
  }

  onDataDomainRoleSelected($event: any, dataDomain: Context) {
    if ($event.value.name === DATA_DOMAIN_VIEWER_ROLE) {
      this.dashboardTableVisibility.set(dataDomain.contextKey as string, true);
    } else if ($event.value.name !== DATA_DOMAIN_VIEWER_ROLE) {
      this.store.dispatch(setSelectedDashboardForUser({dashboards: [], contextKey: dataDomain.contextKey as string}));
      this.dashboardTableVisibility.set(dataDomain.contextKey as string, false);
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
      userContextRoles.forEach(userContextRole => {
        if (userContextRole.role.name === DATA_DOMAIN_VIEWER_ROLE) {
          this.dashboardTableVisibility.set(userContextRole.context.contextKey as string, true);
        }
        const disabled = editedUserSuperuser && !isCurrentSuperuser;
        const control = new FormControl({
          value: userContextRole.role,
          disabled
        });
        this.userForm.addControl(userContextRole.context.contextKey, control);
      });
    }
  }
}
