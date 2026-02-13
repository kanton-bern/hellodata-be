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
import {Observable, Subscription, tap} from 'rxjs';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '../../../../store/app/app.state';
import {
  selectDashboardGroups,
  selectEditedDashboardGroup,
  selectEligibleUsers
} from '../../../../store/dashboard-groups/dashboard-groups.selector';
import {
  DashboardGroup,
  DashboardGroupCreateUpdate,
  DashboardGroupDomainUser,
  DashboardGroupEntry,
  DashboardGroupUserEntry
} from '../../../../store/dashboard-groups/dashboard-groups.model';
import {naviElements} from '../../../../app-navi-elements';
import {markUnsavedChanges} from '../../../../store/unsaved-changes/unsaved-changes.actions';
import {BaseComponent} from '../../../../shared/components/base/base.component';
import {
  deleteDashboardGroup,
  loadDashboardGroups,
  loadEligibleUsers,
  saveChangesToDashboardGroup,
  setActiveContextKey,
  showDeleteDashboardGroupPopup
} from '../../../../store/dashboard-groups/dashboard-groups.action';
import {navigate} from '../../../../store/app/app.action';
import {createBreadcrumbs} from '../../../../store/breadcrumb/breadcrumb.action';
import {AsyncPipe, DatePipe} from '@angular/common';
import {Button} from 'primeng/button';
import {Toolbar} from 'primeng/toolbar';
import {Tooltip} from 'primeng/tooltip';
import {InputText} from 'primeng/inputtext';
import {TranslocoPipe} from '@jsverse/transloco';
import {Ripple} from 'primeng/ripple';
import {
  DeleteDashboardGroupPopupComponent
} from '../delete-dashboard-group-popup/delete-dashboard-group-popup.component';
import {Divider} from 'primeng/divider';
import {loadMyDashboards} from '../../../../store/my-dashboards/my-dashboards.action';
import {
  selectAllAvailableDataDomains,
  selectMyDashboards
} from '../../../../store/my-dashboards/my-dashboards.selector';
import {SupersetDashboard} from '../../../../store/my-dashboards/my-dashboards.model';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Checkbox} from 'primeng/checkbox';
import {filter, take} from 'rxjs/operators';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';

@Component({
  selector: 'app-dashboard-group-edit',
  templateUrl: './dashboard-group-edit.component.html',
  styleUrls: ['./dashboard-group-edit.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, Button, Toolbar, Tooltip, InputText, Divider,
    DeleteDashboardGroupPopupComponent, AsyncPipe, DatePipe, TranslocoPipe, Ripple,
    Tabs, TabList, Tab, TabPanels, TabPanel, Checkbox, IconField, InputIcon]
})
export class DashboardGroupEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedDashboardGroup$: Observable<DashboardGroup | null>;
  eligibleUsers$: Observable<DashboardGroupDomainUser[]>;
  allDashboards: SupersetDashboard[] = [];
  filteredDashboards: SupersetDashboard[] = [];
  filteredUsers: DashboardGroupDomainUser[] = [];
  allEligibleUsers: DashboardGroupDomainUser[] = [];

  dashboardGroupForm!: FormGroup;
  formValueChangedSub!: Subscription;

  // Selected items
  selectedDashboardIds: Set<number> = new Set();
  selectedUserIds: Set<string> = new Set();

  // Search filters
  dashboardSearchFilter = '';
  userSearchFilter = '';

  // Select all state
  selectAllDashboards = false;
  selectAllUsers = false;

  currentContextKey = '';
  currentDomainName = '';

  private readonly store = inject<Store<AppState>>(Store);
  private readonly fb = inject(FormBuilder);
  private allDashboardGroups: DashboardGroup[] = [];
  private currentGroupId?: string;

  constructor() {
    super();
    this.store.dispatch(loadMyDashboards());
    this.editedDashboardGroup$ = this.store.select(selectEditedDashboardGroup);
    this.eligibleUsers$ = this.store.select(selectEligibleUsers);

    // Subscribe to dashboards for the current domain
    this.store.select(selectMyDashboards).subscribe(dashboards => {
      this.allDashboards = dashboards.filter(d => d.contextKey === this.currentContextKey);
      this.applyDashboardFilter();
    });

    // Subscribe to eligible users
    this.eligibleUsers$.subscribe(users => {
      this.allEligibleUsers = users;
      this.applyUserFilter();
    });

    // Load all dashboard groups for validation
    this.store.select(selectDashboardGroups).subscribe(groups => {
      this.allDashboardGroups = groups;
    });
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.editedDashboardGroup$ = this.store.select(selectEditedDashboardGroup).pipe(
      tap((dashboardGroup) => {
        if (dashboardGroup) {
          this.currentContextKey = dashboardGroup.contextKey;
          this.store.dispatch(setActiveContextKey({contextKey: this.currentContextKey}));
          this.store.dispatch(loadDashboardGroups({
            contextKey: this.currentContextKey,
            page: 0,
            size: 1000,
            sort: 'name,asc'
          }));
          this.store.dispatch(loadEligibleUsers({contextKey: this.currentContextKey}));

          // Get domain name and create breadcrumbs - wait for domains to be loaded
          this.store.select(selectAllAvailableDataDomains).pipe(
            filter(domains => domains.length > 0),
            take(1)
          ).subscribe(domains => {
            const domain = domains.find(d => d.key === this.currentContextKey);
            if (domain) {
              this.currentDomainName = domain.name;
            } else {
              this.currentDomainName = this.currentContextKey;
            }
            // Create breadcrumbs after domain name is resolved
            if (dashboardGroup.id) {
              this.createEditBreadcrumbs(dashboardGroup.name);
            } else {
              this.createCreateBreadcrumbs();
            }
          });

          // Reload dashboards for this domain
          this.store.select(selectMyDashboards).pipe(take(1)).subscribe(dashboards => {
            this.allDashboards = dashboards.filter(d => d.contextKey === this.currentContextKey);
            this.applyDashboardFilter();
          });

          this.initForm(dashboardGroup);
          this.initSelectedItems(dashboardGroup);
        }
      })
    );
  }

  navigateToList() {
    this.store.dispatch(navigate({url: `${naviElements.dashboardGroups.path}/list/${this.currentContextKey}`}));
  }

  saveDashboardGroup(editedDashboardGroup: DashboardGroup) {
    const formValue = this.dashboardGroupForm.getRawValue();

    const entries: DashboardGroupEntry[] = this.allDashboards
      .filter(d => this.selectedDashboardIds.has(d.id))
      .map(d => ({dashboardId: d.id, dashboardTitle: d.dashboardTitle}));

    const users: DashboardGroupUserEntry[] = this.allEligibleUsers
      .filter(u => this.selectedUserIds.has(u.id))
      .map(u => ({id: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, roleName: u.roleName}));

    const dashboardGroup: DashboardGroupCreateUpdate = {
      id: editedDashboardGroup.id,
      name: formValue.name,
      contextKey: this.currentContextKey,
      entries,
      users
    };
    this.store.dispatch(saveChangesToDashboardGroup({dashboardGroup}));

    // Update breadcrumbs with new name after save
    if (editedDashboardGroup.id && formValue.name) {
      this.createEditBreadcrumbs(formValue.name);
    }
  }

  openDeletePopup(editedDashboardGroup: DashboardGroup) {
    this.store.dispatch(showDeleteDashboardGroupPopup({dashboardGroup: editedDashboardGroup}));
  }

  getDeletionAction() {
    return deleteDashboardGroup();
  }

  // Dashboard selection methods
  onDashboardSelectionChange(dashboardId: number, checked: boolean, editedDashboardGroup: DashboardGroup) {
    if (checked) {
      this.selectedDashboardIds.add(dashboardId);
    } else {
      this.selectedDashboardIds.delete(dashboardId);
    }
    this.updateSelectAllDashboardsState();
    this.onChange(editedDashboardGroup);
  }

  onSelectAllDashboardsChange(checked: boolean, editedDashboardGroup: DashboardGroup) {
    this.selectAllDashboards = checked;
    if (checked) {
      this.filteredDashboards.forEach(d => this.selectedDashboardIds.add(d.id));
    } else {
      this.filteredDashboards.forEach(d => this.selectedDashboardIds.delete(d.id));
    }
    this.onChange(editedDashboardGroup);
  }

  isDashboardSelected(dashboardId: number): boolean {
    return this.selectedDashboardIds.has(dashboardId);
  }

  onDashboardSearchChange() {
    this.applyDashboardFilter();
    this.updateSelectAllDashboardsState();
  }

  private applyDashboardFilter() {
    if (!this.dashboardSearchFilter) {
      this.filteredDashboards = [...this.allDashboards];
    } else {
      const filter = this.dashboardSearchFilter.toLowerCase();
      this.filteredDashboards = this.allDashboards.filter(d =>
        d.dashboardTitle.toLowerCase().includes(filter)
      );
    }
  }

  private updateSelectAllDashboardsState() {
    this.selectAllDashboards = this.filteredDashboards.length > 0 &&
      this.filteredDashboards.every(d => this.selectedDashboardIds.has(d.id));
  }

  // User selection methods
  onUserSelectionChange(userId: string, checked: boolean, editedDashboardGroup: DashboardGroup) {
    if (checked) {
      this.selectedUserIds.add(userId);
    } else {
      this.selectedUserIds.delete(userId);
    }
    this.updateSelectAllUsersState();
    this.onChange(editedDashboardGroup);
  }

  onSelectAllUsersChange(checked: boolean, editedDashboardGroup: DashboardGroup) {
    this.selectAllUsers = checked;
    if (checked) {
      this.filteredUsers.forEach(u => this.selectedUserIds.add(u.id));
    } else {
      this.filteredUsers.forEach(u => this.selectedUserIds.delete(u.id));
    }
    this.onChange(editedDashboardGroup);
  }

  isUserSelected(userId: string): boolean {
    return this.selectedUserIds.has(userId);
  }

  onUserSearchChange() {
    this.applyUserFilter();
    this.updateSelectAllUsersState();
  }

  private applyUserFilter() {
    if (!this.userSearchFilter) {
      this.filteredUsers = [...this.allEligibleUsers];
    } else {
      const filter = this.userSearchFilter.toLowerCase();
      this.filteredUsers = this.allEligibleUsers.filter(u =>
        u.firstName.toLowerCase().includes(filter) ||
        u.lastName.toLowerCase().includes(filter) ||
        u.email.toLowerCase().includes(filter)
      );
    }
  }

  private updateSelectAllUsersState() {
    this.selectAllUsers = this.filteredUsers.length > 0 &&
      this.filteredUsers.every(u => this.selectedUserIds.has(u.id));
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private initForm(dashboardGroup: DashboardGroup) {
    this.currentGroupId = dashboardGroup.id;
    this.dashboardGroupForm = this.fb.group({
      name: [
        dashboardGroup.name || '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(150),
          Validators.pattern(/^[\p{L}\p{N}].*/u),
          this.uniqueNameValidator.bind(this)
        ]
      ]
    });
    this.unsubFormValueChanges();
    this.formValueChangedSub = this.dashboardGroupForm.valueChanges.subscribe(() => {
      this.onChange(dashboardGroup);
    });
  }

  private uniqueNameValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }
    const nameExists = this.allDashboardGroups.some(group =>
      group.name.toLowerCase() === control.value.toLowerCase() &&
      group.id !== this.currentGroupId
    );
    return nameExists ? {duplicateName: true} : null;
  }

  private initSelectedItems(dashboardGroup: DashboardGroup) {
    this.selectedDashboardIds.clear();
    this.selectedUserIds.clear();

    if (dashboardGroup.entries) {
      dashboardGroup.entries.forEach(entry => {
        this.selectedDashboardIds.add(entry.dashboardId);
      });
    }

    if (dashboardGroup.users) {
      dashboardGroup.users.forEach(user => {
        this.selectedUserIds.add(user.id);
      });
    }

    this.updateSelectAllDashboardsState();
    this.updateSelectAllUsersState();
  }

  private onChange(editedDashboardGroup: DashboardGroup) {
    const formValue = this.dashboardGroupForm.getRawValue();

    const entries: DashboardGroupEntry[] = this.allDashboards
      .filter(d => this.selectedDashboardIds.has(d.id))
      .map(d => ({dashboardId: d.id, dashboardTitle: d.dashboardTitle}));

    const users: DashboardGroupUserEntry[] = this.allEligibleUsers
      .filter(u => this.selectedUserIds.has(u.id))
      .map(u => ({id: u.id, email: u.email, firstName: u.firstName, lastName: u.lastName, roleName: u.roleName}));

    const dashboardGroup: DashboardGroupCreateUpdate = {
      id: editedDashboardGroup.id,
      name: formValue.name,
      contextKey: this.currentContextKey,
      entries,
      users
    };
    this.store.dispatch(markUnsavedChanges({
      action: saveChangesToDashboardGroup({dashboardGroup}),
      stayOnPage: editedDashboardGroup.id === undefined
    }));
  }

  private createCreateBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardGroups.label,
        },
        {
          label: this.currentDomainName,
          routerLink: `${naviElements.dashboardGroups.path}/list/${this.currentContextKey}`,
        },
        {
          label: naviElements.dashboardGroupCreate.label,
        }
      ]
    }));
  }

  private createEditBreadcrumbs(groupName: string) {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.dashboardGroups.label,
        },
        {
          label: this.currentDomainName,
          routerLink: `${naviElements.dashboardGroups.path}/list/${this.currentContextKey}`,
        },
        {
          label: groupName,
        },
        {
          label: naviElements.dashboardGroupEdit.label,
        }
      ]
    }));
  }

  updateBreadcrumbsWithGroupName(groupName: string) {
    if (this.currentGroupId) {
      this.createEditBreadcrumbs(groupName);
    }
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
