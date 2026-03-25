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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, inject, OnDestroy, ViewChild} from '@angular/core';
import {NgTemplateOutlet} from '@angular/common';
import {Router} from '@angular/router';
import {Store} from '@ngrx/store';
import {AppState} from '../../../store/app/app.state';
import {BaseComponent} from '../../../shared/components/base/base.component';
import {createBreadcrumbs} from '../../../store/breadcrumb/breadcrumb.action';
import {naviElements} from '../../../app-navi-elements';
import {Subject, takeUntil} from 'rxjs';
import {selectAllAvailableDataDomains} from '../../../store/my-dashboards/my-dashboards.selector';
import {loadAvailableDataDomains, loadMyDashboards} from '../../../store/my-dashboards/my-dashboards.action';
import {DataDomain} from '../../../store/my-dashboards/my-dashboards.model';
import {
  BulkAssignmentRequest,
  BulkAssignmentResult,
  BulkDashboardInfo,
  BulkDomainAssignment,
  BUSINESS_DOMAIN_ADMIN_ROLE,
  BUSINESS_DOMAIN_CONTEXT_TYPE,
  DashboardGroupMembership,
  DATA_DOMAIN_CONTEXT_TYPE,
  HELLODATA_ADMIN_ROLE,
  NONE_ROLE,
  User,
  UserWithContextRolesDto
} from '../../../store/users-management/users-management.model';
import {UsersManagementService} from '../../../store/users-management/users-management.service';
import {DashboardGroupsService} from '../../../store/dashboard-groups/dashboard-groups.service';
import {ConfirmationService} from 'primeng/api';
import {TranslateService} from '../../../shared/services/translate.service';
import {FormsModule} from '@angular/forms';
import {TranslocoPipe} from '@jsverse/transloco';
import {Stepper, StepList, Step, StepPanels, StepPanel} from 'primeng/stepper';
import {Button} from 'primeng/button';
import {Checkbox} from 'primeng/checkbox';
import {Select} from 'primeng/select';
import {InputText} from 'primeng/inputtext';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {Tag} from 'primeng/tag';
import {Card} from 'primeng/card';
import {ProgressSpinner} from 'primeng/progressspinner';
import {Ripple} from 'primeng/ripple';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Tooltip} from 'primeng/tooltip';
import {SupersetDashboardWithMetadata} from '../../../store/start-page/start-page.model';

interface DomainAssignmentConfig {
  roleName: string;
  dashboards: Map<number, BulkDashboardInfo>;
  dashboardGroupIds: Set<string>;
}

interface RoleOption {
  label: string;
  value: string;
}

interface DomainSummaryItem {
  key: string;
  name: string;
  roleName: string;
  dashboardNames: string[];
  groupNames: string[];
}

interface UserContextRoleInfo {
  contextName: string;
  contextKey: string;
  contextType: string;
  roleName: string;
}

interface RoleFilterOption {
  label: string;
  value: string;
}

const EXCLUDED_BUSINESS_ROLES = new Set([HELLODATA_ADMIN_ROLE, BUSINESS_DOMAIN_ADMIN_ROLE]);

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-bulk-assignments-wizard',
  templateUrl: './bulk-assignments-wizard.component.html',
  styleUrls: ['./bulk-assignments-wizard.component.scss'],
  providers: [ConfirmationService],
  imports: [
    FormsModule,
    NgTemplateOutlet,
    TranslocoPipe,
    Stepper,
    StepList,
    Step,
    StepPanels,
    StepPanel,
    Button,
    Checkbox,
    Select,
    InputText,
    ConfirmDialog,
    Tag,
    Card,
    ProgressSpinner,
    Ripple,
    IconField,
    InputIcon,
    Tooltip,
  ]
})
export class BulkAssignmentsWizardComponent extends BaseComponent implements OnDestroy {
  @ViewChild('userGrid') userGridRef?: ElementRef;
  @ViewChild('carouselContent') carouselContentRef?: ElementRef;

  activeStep = 1;

  // Step 1 - Users
  allUsers: User[] = [];
  eligibleUsers: User[] = [];
  filteredUsers: User[] = [];
  selectedUserIds = new Set<string>();
  userSelectedMap: Record<string, boolean> = {};
  selectAllUsers = false;
  userSearchFilter = '';
  userPage = 0;
  readonly usersPerPage = 15;
  userContextRoles = new Map<string, UserContextRoleInfo[]>();
  userTooltipMap: Record<string, string> = {};
  rolesLoading = false;
  dataDomainRoleFilter = '';
  readonly dataDomainRoleFilterOptions: RoleFilterOption[] = [
    {label: 'All', value: ''},
    {label: 'DATA_DOMAIN_ADMIN', value: 'DATA_DOMAIN_ADMIN'},
    {label: 'DATA_DOMAIN_EDITOR', value: 'DATA_DOMAIN_EDITOR'},
    {label: 'DATA_DOMAIN_VIEWER', value: 'DATA_DOMAIN_VIEWER'},
    {label: 'DATA_DOMAIN_BUSINESS_SPECIALIST', value: 'DATA_DOMAIN_BUSINESS_SPECIALIST'},
    {label: 'NONE', value: 'NONE'},
  ];

  // Step 2 - Data Domains
  allDataDomains: DataDomain[] = [];
  selectedDomainKeys = new Set<string>();
  selectedDomainKeysArray: string[] = [];
  selectAllDomains = false;

  // Step 3 - Roles & Resources
  domainAssignments = new Map<string, DomainAssignmentConfig>();
  dashboardsByDomain: Record<string, SupersetDashboardWithMetadata[]> = {};
  dashboardGroupsByDomain = new Map<string, DashboardGroupMembership[]>();
  readonly roleOptions: RoleOption[] = [
    {label: 'DATA_DOMAIN_ADMIN', value: 'DATA_DOMAIN_ADMIN'},
    {label: 'DATA_DOMAIN_EDITOR', value: 'DATA_DOMAIN_EDITOR'},
    {label: 'DATA_DOMAIN_VIEWER', value: 'DATA_DOMAIN_VIEWER'},
    {label: 'DATA_DOMAIN_BUSINESS_SPECIALIST', value: 'DATA_DOMAIN_BUSINESS_SPECIALIST'},
    {label: 'NONE', value: 'NONE'},
  ];

  // Step 4 - Summary
  isSubmitting = false;
  result: BulkAssignmentResult | null = null;
  summaryPageIndex = 0;
  maxVisitedPage = 0;
  cachedDomainSummary: DomainSummaryItem[] = [];
  cachedSelectedUsers: User[] = [];
  summaryReviewed = false;

  private readonly store = inject<Store<AppState>>(Store);
  private readonly usersManagementService = inject(UsersManagementService);
  private readonly dashboardGroupsService = inject(DashboardGroupsService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    this.createBreadcrumbs();
    this.loadUsers();
    this.store.dispatch(loadAvailableDataDomains());
    this.store.dispatch(loadMyDashboards());

    this.store.select(selectAllAvailableDataDomains).pipe(
      takeUntil(this.destroy$)
    ).subscribe(domains => {
      this.allDataDomains = domains.filter(d => d.key !== '');
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Step 1 - Users
  onUserSearchChange(): void {
    this.userPage = 0;
    this.applyUserFilter();
  }

  onRoleFilterChange(): void {
    this.userPage = 0;
    this.applyUserFilter();
  }

  getUserTooltip(userId: string): string {
    return this.userTooltipMap[userId] || '';
  }

  private buildTooltipCache(): void {
    const map: Record<string, string> = {};
    for (const [userId, roles] of this.userContextRoles) {
      const dataDomainRoles = roles.filter(r => r.contextType === DATA_DOMAIN_CONTEXT_TYPE);
      if (dataDomainRoles.length === 0) {
        map[userId] = 'No data domain roles';
      } else {
        map[userId] = dataDomainRoles
          .map(r => `${r.contextName}: ${r.roleName.replace(/_/g, ' ')}`)
          .join('\n');
      }
    }
    this.userTooltipMap = map;
  }

  isUserExcluded(userId: string): boolean {
    const roles = this.userContextRoles.get(userId);
    if (!roles) return false;
    return roles.some(r =>
      r.contextType === BUSINESS_DOMAIN_CONTEXT_TYPE && EXCLUDED_BUSINESS_ROLES.has(r.roleName)
    );
  }

  paginatedUsers: User[] = [];
  totalUserPages = 0;

  private updatePagination(): void {
    this.totalUserPages = Math.ceil(this.filteredUsers.length / this.usersPerPage);
    const start = this.userPage * this.usersPerPage;
    this.paginatedUsers = this.filteredUsers.slice(start, start + this.usersPerPage);
  }

  nextUserPage(): void {
    if (this.userPage < this.totalUserPages - 1) {
      this.userPage++;
      this.updatePagination();
      this.animatePageTransition('20px');
    }
  }

  prevUserPage(): void {
    if (this.userPage > 0) {
      this.userPage--;
      this.updatePagination();
      this.animatePageTransition('-20px');
    }
  }

  onUserSelectionChange(userId: string, checked: boolean): void {
    if (checked) {
      this.selectedUserIds.add(userId);
    } else {
      this.selectedUserIds.delete(userId);
    }
    this.rebuildUserSelectedMap();
    this.updateSelectAllUsersState();
  }

  isUserSelected(userId: string): boolean {
    return this.selectedUserIds.has(userId);
  }

  onSelectAllUsersChange(checked: boolean): void {
    if (checked) {
      this.filteredUsers.forEach(u => this.selectedUserIds.add(u.id));
    } else {
      this.filteredUsers.forEach(u => this.selectedUserIds.delete(u.id));
    }
    this.selectAllUsers = checked;
    this.rebuildUserSelectedMap();
  }

  private rebuildUserSelectedMap(): void {
    const map: Record<string, boolean> = {};
    for (const id of this.selectedUserIds) {
      map[id] = true;
    }
    this.userSelectedMap = map;
  }

  // Step 2 - Data Domains
  onDomainSelectionChange(domainKey: string, checked: boolean): void {
    if (checked) {
      this.selectedDomainKeys.add(domainKey);
      if (!this.domainAssignments.has(domainKey)) {
        this.domainAssignments.set(domainKey, {
          roleName: NONE_ROLE,
          dashboards: new Map(),
          dashboardGroupIds: new Set(),
        });
      }
    } else {
      this.selectedDomainKeys.delete(domainKey);
      this.domainAssignments.delete(domainKey);
    }
    this.selectedDomainKeysArray = [...this.selectedDomainKeys];
    this.updateSelectAllDomainsState();
  }

  isDomainSelected(domainKey: string): boolean {
    return this.selectedDomainKeys.has(domainKey);
  }

  onSelectAllDomainsChange(checked: boolean): void {
    if (checked) {
      this.allDataDomains.forEach(d => {
        this.selectedDomainKeys.add(d.key);
        if (!this.domainAssignments.has(d.key)) {
          this.domainAssignments.set(d.key, {
            roleName: NONE_ROLE,
            dashboards: new Map(),
            dashboardGroupIds: new Set(),
          });
        }
      });
    } else {
      this.allDataDomains.forEach(d => {
        this.selectedDomainKeys.delete(d.key);
        this.domainAssignments.delete(d.key);
      });
    }
    this.selectAllDomains = checked;
    this.selectedDomainKeysArray = [...this.selectedDomainKeys];
  }

  // Step 3 - Roles
  onRoleChange(domainKey: string, roleName: string): void {
    const assignment = this.domainAssignments.get(domainKey);
    if (assignment) {
      assignment.roleName = roleName;
      if (!this.showDashboardSelection(roleName)) {
        assignment.dashboards.clear();
        assignment.dashboardGroupIds.clear();
      }
    }
  }

  getRoleForDomain(domainKey: string): string {
    return this.domainAssignments.get(domainKey)?.roleName || NONE_ROLE;
  }

  showDashboardSelection(roleName: string): boolean {
    return roleName === 'DATA_DOMAIN_VIEWER' || roleName === 'DATA_DOMAIN_BUSINESS_SPECIALIST';
  }

  getDashboardsForDomain(contextKey: string): SupersetDashboardWithMetadata[] {
    return this.dashboardsByDomain[contextKey] || [];
  }

  getDashboardGroupsForDomain(contextKey: string): DashboardGroupMembership[] {
    return this.dashboardGroupsByDomain.get(contextKey) || [];
  }

  isDashboardSelected(domainKey: string, dashboardId: number): boolean {
    return this.domainAssignments.get(domainKey)?.dashboards.has(dashboardId) || false;
  }

  onDashboardSelectionChange(domainKey: string, dashboard: SupersetDashboardWithMetadata, checked: boolean): void {
    const assignment = this.domainAssignments.get(domainKey);
    if (assignment) {
      if (checked) {
        assignment.dashboards.set(dashboard.id, {
          id: dashboard.id,
          title: dashboard.dashboardTitle,
          instanceName: dashboard.instanceName,
        });
      } else {
        assignment.dashboards.delete(dashboard.id);
      }
    }
  }

  isDashboardGroupSelected(domainKey: string, groupId: string): boolean {
    return this.domainAssignments.get(domainKey)?.dashboardGroupIds.has(groupId) || false;
  }

  onDashboardGroupSelectionChange(domainKey: string, groupId: string, checked: boolean): void {
    const assignment = this.domainAssignments.get(domainKey);
    if (assignment) {
      if (checked) {
        assignment.dashboardGroupIds.add(groupId);
      } else {
        assignment.dashboardGroupIds.delete(groupId);
      }
    }
  }

  loadDashboardGroupsForDomains(): void {
    for (const domainKey of this.selectedDomainKeys) {
      if (!this.dashboardGroupsByDomain.has(domainKey)) {
        this.dashboardGroupsService.getDashboardGroups(domainKey, 0, 1000).pipe(
          takeUntil(this.destroy$)
        ).subscribe(response => {
          const memberships: DashboardGroupMembership[] = response.content.map(g => ({
            groupId: g.id!,
            groupName: g.name,
            isMember: false,
            dashboardTitles: g.entries.map(e => e.dashboardTitle),
          }));
          this.dashboardGroupsByDomain.set(domainKey, memberships);
          this.cdr.markForCheck();
        });
      }
    }
  }

  // Step 4 - Summary
  getSelectedDomainNames(): DomainSummaryItem[] {
    const items: DomainSummaryItem[] = [];
    for (const key of this.selectedDomainKeys) {
      const domain = this.allDataDomains.find(d => d.key === key);
      const assignment = this.domainAssignments.get(key);
      const dashboardNames = assignment
        ? Array.from(assignment.dashboards.values()).map(d => d.title)
        : [];
      const groups = this.dashboardGroupsByDomain.get(key) || [];
      const groupNames = assignment
        ? groups.filter(g => assignment.dashboardGroupIds.has(g.groupId)).map(g => g.groupName)
        : [];
      items.push({
        key,
        name: domain?.name || key,
        roleName: assignment?.roleName || NONE_ROLE,
        dashboardNames,
        groupNames,
      });
    }
    return items;
  }

  getSelectedUsers(): User[] {
    return this.allUsers.filter(u => this.selectedUserIds.has(u.id));
  }

  formatRoleName(role: string): string {
    if (!role) return '';
    return role.replace(/_/g, ' ');
  }

  nextSummaryPage(): void {
    const total = this.cachedDomainSummary.length;
    if (this.summaryPageIndex < total - 1) {
      this.animateCarousel('30px');
      this.summaryPageIndex++;
      if (this.summaryPageIndex > this.maxVisitedPage) {
        this.maxVisitedPage = this.summaryPageIndex;
      }
      if (this.maxVisitedPage >= total - 1) {
        this.summaryReviewed = true;
      }
    }
  }

  prevSummaryPage(): void {
    if (this.summaryPageIndex > 0) {
      this.animateCarousel('-30px');
      this.summaryPageIndex--;
    }
  }

  private animateCarousel(direction: string): void {
    const el = this.carouselContentRef?.nativeElement;
    if (el) {
      el.animate([
        {opacity: 0, transform: `translateX(${direction})`},
        {opacity: 1, transform: 'translateX(0)'}
      ], {duration: 250, easing: 'ease-out'});
    }
  }

  allSummaryPagesReviewed(): boolean {
    return this.summaryReviewed;
  }

  goToSummary(activateCallback: (val: number) => void): void {
    this.summaryPageIndex = 0;
    this.maxVisitedPage = 0;
    this.cachedDomainSummary = this.getSelectedDomainNames();
    this.cachedSelectedUsers = this.getSelectedUsers();
    this.summaryReviewed = this.cachedDomainSummary.length <= 1;
    activateCallback(4);
  }

  confirmApply(): void {
    const msg = this.translateService.translate('@Confirm bulk assignment', {count: this.selectedUserIds.size});
    this.confirmationService.confirm({
      message: msg,
      icon: 'fas fa-triangle-exclamation',
      acceptButtonStyleClass: 'p-button-success',
      accept: () => {
        this.applyBulkAssignment();
      },
    });
  }

  getDomainName(key: string): string {
    return this.allDataDomains.find(d => d.key === key)?.name || key;
  }

  onStepActivate(nextStep: number, activateCallback: (val: number) => void): void {
    if (nextStep === 3) {
      this.loadDashboardsFromStore();
      this.loadDashboardGroupsForDomains();
    }
    activateCallback(nextStep);
  }

  private applyBulkAssignment(): void {
    this.isSubmitting = true;
    this.result = null;
    this.cdr.markForCheck();

    const domainAssignments: BulkDomainAssignment[] = [];
    for (const key of this.selectedDomainKeys) {
      const config = this.domainAssignments.get(key);
      if (config) {
        domainAssignments.push({
          contextKey: key,
          roleName: config.roleName,
          dashboards: Array.from(config.dashboards.values()),
          dashboardGroupIds: Array.from(config.dashboardGroupIds),
        });
      }
    }

    const request: BulkAssignmentRequest = {
      userIds: Array.from(this.selectedUserIds),
      domainAssignments,
    };

    this.usersManagementService.executeBulkAssignment(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (res) => {
        this.result = res;
        this.isSubmitting = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.result = {
          updatedCount: 0,
          skippedCount: 0,
          failedCount: this.selectedUserIds.size,
          errors: [err.message || 'Unknown error occurred'],
        };
        this.isSubmitting = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadUsers(): void {
    this.rolesLoading = true;
    this.usersManagementService.getAllUsersWithContextRoles().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (usersWithRoles: UserWithContextRolesDto[]) => {
        // Build allUsers from the bulk response
        this.allUsers = usersWithRoles.map(u => ({
          id: u.id,
          firstName: u.firstName || '',
          lastName: u.lastName || '',
          email: u.email || '',
          enabled: true,
          superuser: false,
        } as User));

        // Build context roles map from the bulk response
        this.userContextRoles.clear();
        for (const u of usersWithRoles) {
          const roles: UserContextRoleInfo[] = [];
          if (u.businessDomainRole) {
            roles.push({
              contextName: 'Business Domain',
              contextKey: '',
              contextType: BUSINESS_DOMAIN_CONTEXT_TYPE,
              roleName: u.businessDomainRole,
            });
          }
          if (u.dataDomainRoles) {
            for (const ddr of u.dataDomainRoles) {
              roles.push({
                contextName: ddr.contextName,
                contextKey: ddr.contextKey,
                contextType: DATA_DOMAIN_CONTEXT_TYPE,
                roleName: ddr.role,
              });
            }
          }
          this.userContextRoles.set(u.id, roles);
        }

        this.buildTooltipCache();
        this.eligibleUsers = this.allUsers.filter(u => !this.isUserExcluded(u.id));
        this.rolesLoading = false;
        this.applyUserFilter();
        this.cdr.markForCheck();
      },
      error: () => {
        this.rolesLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadDashboardsFromStore(): void {
    this.store.select((state: AppState) => state.myDashboards.myDashboards).pipe(
      takeUntil(this.destroy$)
    ).subscribe(dashboards => {
      // Pre-group dashboards by context key
      const grouped: Record<string, SupersetDashboardWithMetadata[]> = {};
      for (const d of dashboards) {
        if (!grouped[d.contextKey]) {
          grouped[d.contextKey] = [];
        }
        grouped[d.contextKey].push(d);
      }
      this.dashboardsByDomain = grouped;
      this.cdr.markForCheck();
    });
  }

  private applyUserFilter(): void {
    const searchTerm = this.userSearchFilter.toLowerCase();
    let result = [...this.eligibleUsers];

    if (searchTerm) {
      result = result.filter(u =>
        u.firstName.toLowerCase().includes(searchTerm) ||
        u.lastName.toLowerCase().includes(searchTerm) ||
        u.email.toLowerCase().includes(searchTerm)
      );
    }

    if (this.dataDomainRoleFilter) {
      result = result.filter(u => {
        const roles = this.userContextRoles.get(u.id) || [];
        const dataDomainRoles = roles.filter(r => r.contextType === DATA_DOMAIN_CONTEXT_TYPE);
        if (this.dataDomainRoleFilter === NONE_ROLE) {
          return dataDomainRoles.length === 0 || dataDomainRoles.every(r => r.roleName === NONE_ROLE);
        }
        return dataDomainRoles.some(r => r.roleName === this.dataDomainRoleFilter);
      });
    }

    this.filteredUsers = result;
    this.updatePagination();
  }

  private updateSelectAllUsersState(): void {
    this.selectAllUsers = this.filteredUsers.length > 0 &&
      this.filteredUsers.every(u => this.selectedUserIds.has(u.id));
  }

  private updateSelectAllDomainsState(): void {
    this.selectAllDomains = this.allDataDomains.length > 0 &&
      this.allDataDomains.every(d => this.selectedDomainKeys.has(d.key));
  }

  private animatePageTransition(direction: string): void {
    const el = this.userGridRef?.nativeElement;
    if (el) {
      el.animate([
        {opacity: 0, transform: `translateX(${direction})`},
        {opacity: 1, transform: 'translateX(0)'}
      ], {duration: 200, easing: 'ease-out'});
    }
  }

  private createBreadcrumbs(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.userManagement.label,
          routerLink: naviElements.userManagement.path,
        },
        {
          label: naviElements.bulkAssignments.label,
          routerLink: naviElements.bulkAssignments.path,
        }
      ]
    }));
  }

  startOver(): void {
    this.router.navigateByUrl('/', {skipLocationChange: true}).then(() => {
      this.router.navigate([naviElements.bulkAssignments.path]);
    });
  }

  goToUserManagement(): void {
    this.router.navigate([naviElements.userManagement.path]);
  }
}
