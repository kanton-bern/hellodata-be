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
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  interval,
  map,
  Observable,
  Subject,
  Subscription,
  takeUntil,
  tap
} from "rxjs";
import {
  selectSyncStatus,
  selectUsersCopy,
  selectUsersLoading,
  selectUsersTotalRecords
} from "../../../store/users-management/users-management.selector";
import {AsyncPipe, DatePipe} from "@angular/common";
import {AdUser, CreateUserForm, User, UserAction} from "../../../store/users-management/users-management.model";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActionsUserPopupComponent} from "./actions-user-popup/actions-user-popup.component";
import {TranslocoPipe} from "@jsverse/transloco";
import {TableLazyLoadEvent, TableModule} from "primeng/table";
import {Tooltip} from "primeng/tooltip";
import {InputText} from "primeng/inputtext";
import {Button, ButtonDirective} from "primeng/button";
import {Toolbar} from "primeng/toolbar";
import {Ripple} from "primeng/ripple";
import {AutoComplete} from "primeng/autocomplete";
import {switchMap} from "rxjs/operators";
import {naviElements} from "../../../app-navi-elements";
import {UsersManagementService} from "../../../store/users-management/users-management.service";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {
  createUser,
  loadSyncStatus,
  loadUsers,
  navigateToUserEdition,
  showUserActionPopup,
  syncUsers
} from "../../../store/users-management/users-management.action";
import {selectProfile} from "../../../store/auth/auth.selector";
import {IUser} from "../../../store/auth/auth.model";
import {IconField} from "primeng/iconfield";
import {InputIcon} from "primeng/inputicon";
import {PrimeTemplate} from 'primeng/api';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, AutoComplete, PrimeTemplate, Tooltip, InputText, Toolbar, Button, TableModule, IconField, InputIcon, ButtonDirective, Ripple, ActionsUserPopupComponent, AsyncPipe, DatePipe, TranslocoPipe]
})
export class UserManagementComponent extends BaseComponent implements OnInit, OnDestroy {
  private store = inject<Store<AppState>>(Store);
  private fb = inject(FormBuilder);
  private userService = inject(UsersManagementService);


  users$: Observable<any>;
  syncStatus$: Observable<string>;
  usersLoading$: Observable<boolean>;
  usersTotalRecords = 0;
  loggedInUser$: Observable<IUser | undefined>;
  inviteForm!: FormGroup;
  suggestedAdUsers: AdUser[] = [];
  filterValue = '';
  syncStatusInterval$ = interval(30000);
  private searchSubscription?: Subscription;
  private readonly searchSubject = new Subject<string | undefined>();
  private destroy$ = new Subject<void>();

  constructor() {
    super();
    this.users$ = combineLatest([
      this.store.select(selectUsersCopy),
      this.store.select(selectUsersTotalRecords)
    ]).pipe(
      tap(([_, usersTotalRecords]) => {
        this.usersTotalRecords = usersTotalRecords;
      }),
      map(([users, _]) => users),
    );
    this.syncStatus$ = this.store.select(selectSyncStatus);
    this.loggedInUser$ = this.store.select(selectProfile);
    this.usersLoading$ = this.store.select(selectUsersLoading);
    this.store.dispatch(loadSyncStatus());
    this.createBreadcrumbs();
    this.createSearchSubscription();
    this.createInterval();
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.inviteForm = this.fb.group({
      user: [null, Validators.compose([Validators.required.bind(this),
        Validators.pattern("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$"),
        Validators.email.bind(this)])],
      firstName: [null, Validators.compose([Validators.required.bind(this), Validators.minLength(3), Validators.maxLength(255), Validators.pattern(/[\p{L}\p{N}].*/u)])],
      lastName: [null, Validators.compose([Validators.required.bind(this), Validators.minLength(3), Validators.maxLength(255), Validators.pattern(/[\p{L}\p{N}].*/u)])],
    });
    this.restoreUserTableSearchFilter();
  }

  public ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  createUser() {
    if (this.inviteForm.valid) {
      const inviteFormData = this.inviteForm.getRawValue() as CreateUserForm;
      this.store.dispatch(createUser({createUserForm: inviteFormData}));
      this.inviteForm.reset();
    }
  }

  editUser(data: User) {
    this.store.dispatch(navigateToUserEdition({userId: data.id}));
  }

  showUserDeletionPopup(data: User) {
    this.store.dispatch(showUserActionPopup({
      userActionForPopup: {
        user: data,
        action: UserAction.DELETE,
        actionFromUsersEdition: false
      }
    }));
  }

  enableUser(data: User) {
    this.store.dispatch(showUserActionPopup({
      userActionForPopup: {
        user: data,
        action: UserAction.ENABLE,
        actionFromUsersEdition: false
      }
    }));
  }

  disableUser(data: User) {
    this.store.dispatch(showUserActionPopup({
      userActionForPopup: {
        user: data,
        action: UserAction.DISABLE,
        actionFromUsersEdition: false
      }
    }));
  }

  syncUsers() {
    this.store.dispatch(syncUsers());
  }

  filterMails($event: any) {
    const searchQuery = $event.query;
    this.searchSubject.next(searchQuery?.trim());
    this.inviteForm.get('firstName')?.reset();
    this.inviteForm.get('lastName')?.reset();
  }

  onSelectEmail($event: any) {
    console.debug("onSelectEmail", $event);
    this.inviteForm.get('firstName')?.setValue($event.value.firstName);
    this.inviteForm.get('lastName')?.setValue($event.value.lastName);
    this.inviteForm.get('user')?.setErrors(null);
  }

  loadUsers(event: TableLazyLoadEvent) {
    this.store.dispatch(loadUsers({
      page: event.first as number / (event.rows as number),
      size: event.rows as number,
      sort: event.sortField ? `${event.sortField}, ${event.sortOrder ? event.sortOrder > 0 ? 'asc' : 'desc' : ''}` : '',
      search: event.globalFilter ? event.globalFilter as string : ''
    }));
  }

  private createSearchSubscription() {
    this.searchSubscription = this.searchSubject
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        switchMap((searchQuery: string | undefined) => {
          return this.userService.searchUserByEmail(searchQuery);
        })
      )
      .subscribe(
        (users: AdUser[]) => {
          return this.suggestedAdUsers = this.enhanceSuggestedAdUsers(users);
        }
      )
  }

  private createBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.userManagement.label,
          routerLink: naviElements.userManagement.path,
        }
      ]
    }));
  }

  private restoreUserTableSearchFilter() {
    const storageItem = sessionStorage.getItem("portal-users-table");
    if (storageItem) {
      const storageItemObject = JSON.parse(storageItem);
      const filterValue = storageItemObject.filters?.global?.value;
      if (filterValue) {
        this.filterValue = filterValue;
      }
    }
  }

  private enhanceSuggestedAdUsers(users: AdUser[]) {
    return users.map(user => {
      user.label = user.email + " (" + user.firstName + " " + user.lastName + ")";
      return user;
    });
  }

  private createInterval(): void {
    this.syncStatusInterval$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.store.dispatch(loadSyncStatus());
      });
  }
}


