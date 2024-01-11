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

import {Action} from "@ngrx/store";
import {CreateUserForm, DashboardForUser, User, UserActionForPopup} from "./users-management.model";
import {ContextResponse, Role} from "./context-role.model";

export enum UsersManagementActionType {
  LOAD_USERS = '[USERS MANAGEMENT] Load Users',
  LOAD_USERS_SUCCESS = '[USERS MANAGEMENT] Load Users SUCCESS',
  SHOW_USER_ACTION_POP_UP = '[USERS MANAGEMENT] Show user action pop up',
  HIDE_USER_ACTION_POP_UP = '[USERS MANAGEMENT] Hide user action pop up',
  INVOKE_ACTION_FROM_USER_POPUP = '[USERS MANAGEMENT] Invoke User popup action',
  USER_POPUP_ACTION_SUCCESS = '[USERS MANAGEMENT] User popup action SUCCESS',
  CREATE_USER = '[USERS MANAGEMENT] Create user',
  CREATE_USER_SUCCESS = '[USERS MANAGEMENT] Create user SUCCESS',
  NAVIGATE_TO_USER_EDITION = '[USERS MANAGEMENT] Navigate to user edition',
  NAVIGATE_TO_USERS_MANAGEMENT = '[USERS MANAGEMENT] Navigate to users management',
  LOAD_USER_BY_ID = '[USERS MANAGEMENT] Load user by id',
  LOAD_USER_BY_ID_SUCCESS = '[USERS MANAGEMENT] Load user by id SUCCESS',
  LOAD_DASHBOARDS = '[USERS MANAGEMENT] Load dashboards with marked user',
  LOAD_DASHBOARDS_SUCCESS = '[USERS MANAGEMENT] Load dashboards with marked user SUCCESS',
  RESET_DASHBOARDS_FROM_BACKEND = '[USERS MANAGEMENT] Reset dashboards from backend',
  SYNC_ALL_USERS = '[USERS MANAGEMENT] Resync all users',
  SYNC_ALL_USERS_SUCCESS = '[USERS MANAGEMENT] Resync all users SUCCESS',
  LOAD_AVAILABLE_CONTEXT_ROLES = '[USERS MANAGEMENT] Load available context roles',
  LOAD_AVAILABLE_CONTEXT_ROLES_SUCCESS = '[USERS MANAGEMENT] Load available context roles SUCCESS',
  LOAD_AVAILABLE_CONTEXTS = '[USERS MANAGEMENT] Load available contexts',
  LOAD_AVAILABLE_CONTEXTS_SUCCESS = '[USERS MANAGEMENT] Load available contexts SUCCESS',
  SELECT_BUSINESS_DOMAIN_ROLE_FOR_EDITED_USER = '[USERS MANAGEMENT] Select Business Domain Role for edited user',
  SELECT_DATA_DOMAIN_ROLE_FOR_EDITED_USER = '[USERS MANAGEMENT] Select Data Domain Role for edited user',
  UPDATE_USER_ROLES = '[USERS MANAGEMENT] Update user context roles',
  UPDATE_USER_ROLES_SUCCESS = '[USERS MANAGEMENT] Update user context roles SUCCESS',
  LOAD_USER_CONTEXT_ROLES = '[USERS MANAGEMENT] Load user context roles',
  LOAD_USER_CONTEXT_ROLES_SUCCESS = '[USERS MANAGEMENT] Load user context roles SUCCESS',
  SET_SELECTED_DASHBOARD_FOR_USER = '[USERS MANAGEMENT] Set selected dashboard for user',
  LOAD_ADMIN_EMAILS = '[USERS MANAGEMENT] Load email addresses of all HD Administrators',
  LOAD_ADMIN_EMAILS_SUCCESS = '[USERS MANAGEMENT] Load email addresses of all HD Administrators SUCCESS'
}

export class LoadUsers implements Action {
  public readonly type = UsersManagementActionType.LOAD_USERS;
}

export class LoadUsersSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_USERS_SUCCESS;

  constructor(public payload: User[]) {
  }
}

export class ShowUserActionPopup implements Action {
  public readonly type = UsersManagementActionType.SHOW_USER_ACTION_POP_UP;

  constructor(public userActionForPopup: UserActionForPopup) {
  }
}

export class HideUserPopupAction implements Action {
  public readonly type = UsersManagementActionType.HIDE_USER_ACTION_POP_UP;
}

export class InvokeActionFromUserPopup implements Action {
  public readonly type = UsersManagementActionType.INVOKE_ACTION_FROM_USER_POPUP;
}

export class UserPopupActionSuccess implements Action {
  public readonly type = UsersManagementActionType.USER_POPUP_ACTION_SUCCESS;

  constructor(public email: string, public userActionForPopup: UserActionForPopup) {
  }
}

export class CreateUser implements Action {
  public readonly type = UsersManagementActionType.CREATE_USER;

  constructor(public createUserForm: CreateUserForm) {
  }
}

export class CreateUserSuccess implements Action {
  public readonly type = UsersManagementActionType.CREATE_USER_SUCCESS;

  constructor(public email: string, public userId: string) {
  }

}

export class SyncUsers implements Action {
  public readonly type = UsersManagementActionType.SYNC_ALL_USERS;
}

export class SyncUsersSuccess implements Action {
  public readonly type = UsersManagementActionType.SYNC_ALL_USERS_SUCCESS;
}

export class NavigateToUserEdition implements Action {
  public readonly type = UsersManagementActionType.NAVIGATE_TO_USER_EDITION;

  constructor(public userId: string) {
  }
}

export class LoadUserById implements Action {
  public readonly type = UsersManagementActionType.LOAD_USER_BY_ID;
}

export class LoadUserByIdSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_USER_BY_ID_SUCCESS;

  constructor(public user: User) {
  }
}

export class NavigateToUsersManagement implements Action {
  public readonly type = UsersManagementActionType.NAVIGATE_TO_USERS_MANAGEMENT;
}

export class LoadDashboards implements Action {
  public readonly type = UsersManagementActionType.LOAD_DASHBOARDS;
}

export class LoadDashboardsSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_DASHBOARDS_SUCCESS;

  constructor(public dashboards: DashboardForUser[]) {
  }
}

export class ResetDashboardsFromBackend implements Action {
  public readonly type = UsersManagementActionType.RESET_DASHBOARDS_FROM_BACKEND;

  constructor(public dashboards: DashboardForUser[]) {
  }
}

export class LoadAvailableContextRoles implements Action {
  public readonly type = UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES;
}

export class LoadAvailableContextRolesSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES_SUCCESS;

  constructor(public payload: Role[]) {
  }
}

export class LoadAvailableContexts implements Action {
  public readonly type = UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS;
}

export class LoadAvailableContextsSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS_SUCCESS;

  constructor(public payload: ContextResponse) {
  }
}

export class SelectBusinessDomainRoleForEditedUser implements Action {
  public readonly type = UsersManagementActionType.SELECT_BUSINESS_DOMAIN_ROLE_FOR_EDITED_USER;

  constructor(public selectedRole: Role) {
  }
}

export class SelectDataDomainRoleForEditedUser implements Action {
  public readonly type = UsersManagementActionType.SELECT_DATA_DOMAIN_ROLE_FOR_EDITED_USER;

  constructor(public selectedRoleForContext: any) {
  }
}

export class UpdateUserRoles implements Action {
  public readonly type = UsersManagementActionType.UPDATE_USER_ROLES;
}

export class UpdateUserRolesSuccess implements Action {
  public readonly type = UsersManagementActionType.UPDATE_USER_ROLES_SUCCESS;
}

export class LoadUserContextRoles implements Action {
  public readonly type = UsersManagementActionType.LOAD_USER_CONTEXT_ROLES;
}

export class LoadUserContextRolesSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_USER_CONTEXT_ROLES_SUCCESS;

  constructor(public payload: any) {
  }
}

export class SetSelectedDashboardForUser implements Action {
  public readonly type = UsersManagementActionType.SET_SELECTED_DASHBOARD_FOR_USER;

  constructor(public dashboards: DashboardForUser[], public contextKey: string) {
  }
}

export class LoadAdminEmails implements Action {
  public readonly type = UsersManagementActionType.LOAD_ADMIN_EMAILS;
}

export class LoadAdminEmailsSuccess implements Action {
  public readonly type = UsersManagementActionType.LOAD_ADMIN_EMAILS_SUCCESS;

  constructor(public payload: string[]) {
  }
}

export type UsersManagementActions =
  LoadUsers
  | LoadUsersSuccess
  |
  InvokeActionFromUserPopup
  | UserPopupActionSuccess
  |
  ShowUserActionPopup
  | HideUserPopupAction
  |
  CreateUser
  | CreateUserSuccess
  |
  SyncUsers
  | SyncUsersSuccess
  |
  NavigateToUserEdition
  | LoadUserById
  | LoadUserByIdSuccess
  |
  NavigateToUsersManagement
  |
  LoadDashboards
  | LoadDashboardsSuccess
  |
  LoadAvailableContextRoles
  | LoadAvailableContextRolesSuccess
  | LoadAvailableContexts
  | LoadAvailableContextsSuccess
  | LoadUserContextRoles
  | LoadUserContextRolesSuccess
  |
  SelectBusinessDomainRoleForEditedUser
  | SelectDataDomainRoleForEditedUser
  | SetSelectedDashboardForUser
  |
  LoadAdminEmails
  | LoadAdminEmailsSuccess

