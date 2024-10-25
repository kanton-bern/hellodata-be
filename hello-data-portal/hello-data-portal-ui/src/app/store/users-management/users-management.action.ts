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

import {createAction, props} from "@ngrx/store";
import {
  CreateUserForm,
  DashboardForUser,
  DashboardUsersResultDto,
  SubsystemUsersResultDto,
  User,
  UserActionForPopup
} from "./users-management.model";
import {ContextResponse, Role} from "./context-role.model";

export enum UsersManagementActionType {
  LOAD_USERS = '[USERS MANAGEMENT] Load Users',
  LOAD_USERS_SUCCESS = '[USERS MANAGEMENT] Load Users SUCCESS',
  LOAD_SUBSYSTEM_USERS = '[USERS MANAGEMENT] Load Subsystem Users',
  LOAD_SUBSYSTEM_USERS_SUCCESS = '[USERS MANAGEMENT] Load Subsystem Users SUCCESS',
  LOAD_SUBSYSTEM_USERS_FOR_DASHBOARDS = '[USERS MANAGEMENT] Load Subsystem Users for dashboards',
  LOAD_SUBSYSTEM_USERS_FOR_DASHBOARDS_SUCCESS = '[USERS MANAGEMENT] Load Subsystem Users for dashboards SUCCESS',
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
  LOAD_ADMIN_EMAILS_SUCCESS = '[USERS MANAGEMENT] Load email addresses of all HD Administrators SUCCESS',
  LOAD_SYNC_STATUS = '[USERS MANAGEMENT] Load sync status',
  LOAD_SYNC_STATUS_SUCCESS = '[USERS MANAGEMENT] Load sync status SUCCESS',
  UPDATE_USER_IN_STORE = '[USERS MANAGEMENT] Update user in store',
  DELETE_USER_IN_STORE = '[USERS MANAGEMENT] Delete user in store',
}

export const loadUsers = createAction(
  UsersManagementActionType.LOAD_USERS
);

export const loadUsersSuccess = createAction(
  UsersManagementActionType.LOAD_USERS_SUCCESS,
  props<{ payload: User[] }>()
);

export const updateUserInStore = createAction(
  UsersManagementActionType.UPDATE_USER_IN_STORE,
  props<{ userChanged: User }>()
);

export const deleteUserInStore = createAction(
  UsersManagementActionType.DELETE_USER_IN_STORE,
  props<{ userDeleted: User }>()
);

export const showUserActionPopup = createAction(
  UsersManagementActionType.SHOW_USER_ACTION_POP_UP,
  props<{ userActionForPopup: UserActionForPopup }>()
);

export const hideUserPopupAction = createAction(
  UsersManagementActionType.HIDE_USER_ACTION_POP_UP
);

export const invokeActionFromUserPopup = createAction(
  UsersManagementActionType.INVOKE_ACTION_FROM_USER_POPUP
);

export const userPopupActionSuccess = createAction(
  UsersManagementActionType.USER_POPUP_ACTION_SUCCESS,
  props<{ email: string, userActionForPopup: UserActionForPopup }>()
);

export const createUser = createAction(
  UsersManagementActionType.CREATE_USER,
  props<{ createUserForm: CreateUserForm }>()
);

export const createUserSuccess = createAction(
  UsersManagementActionType.CREATE_USER_SUCCESS,
  props<{ email: string, userId: string }>()
);

export const syncUsers = createAction(
  UsersManagementActionType.SYNC_ALL_USERS
);

export const syncUsersSuccess = createAction(
  UsersManagementActionType.SYNC_ALL_USERS_SUCCESS,
  props<{ status: string }>()
);

export const navigateToUserEdition = createAction(
  UsersManagementActionType.NAVIGATE_TO_USER_EDITION,
  props<{ userId: string }>()
);

export const loadUserById = createAction(
  UsersManagementActionType.LOAD_USER_BY_ID
);

export const loadUserByIdSuccess = createAction(
  UsersManagementActionType.LOAD_USER_BY_ID_SUCCESS,
  props<{ user: User }>()
);

export const navigateToUsersManagement = createAction(
  UsersManagementActionType.NAVIGATE_TO_USERS_MANAGEMENT
);

export const loadDashboards = createAction(
  UsersManagementActionType.LOAD_DASHBOARDS
);

export const loadDashboardsSuccess = createAction(
  UsersManagementActionType.LOAD_DASHBOARDS_SUCCESS,
  props<{ dashboards: DashboardForUser[] }>()
);

export const resetDashboardsFromBackend = createAction(
  UsersManagementActionType.RESET_DASHBOARDS_FROM_BACKEND,
  props<{ dashboards: DashboardForUser[] }>()
);

export const loadAvailableContextRoles = createAction(
  UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES
);

export const loadAvailableContextRolesSuccess = createAction(
  UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES_SUCCESS,
  props<{ payload: Role[] }>()
);

export const loadAvailableContexts = createAction(
  UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS
);

export const loadAvailableContextsSuccess = createAction(
  UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS_SUCCESS,
  props<{ payload: ContextResponse }>()
);

export const selectBusinessDomainRoleForEditedUser = createAction(
  UsersManagementActionType.SELECT_BUSINESS_DOMAIN_ROLE_FOR_EDITED_USER,
  props<{ selectedRole: Role }>()
);

export const selectDataDomainRoleForEditedUser = createAction(
  UsersManagementActionType.SELECT_DATA_DOMAIN_ROLE_FOR_EDITED_USER,
  props<{ selectedRoleForContext: any }>()
);

export const updateUserRoles = createAction(
  UsersManagementActionType.UPDATE_USER_ROLES
);

export const updateUserRolesSuccess = createAction(
  UsersManagementActionType.UPDATE_USER_ROLES_SUCCESS
);

export const loadUserContextRoles = createAction(
  UsersManagementActionType.LOAD_USER_CONTEXT_ROLES
);

export const loadUserContextRolesSuccess = createAction(
  UsersManagementActionType.LOAD_USER_CONTEXT_ROLES_SUCCESS,
  props<{ payload: any }>()
);

export const setSelectedDashboardForUser = createAction(
  UsersManagementActionType.SET_SELECTED_DASHBOARD_FOR_USER,
  props<{ dashboards: DashboardForUser[], contextKey: string }>()
);

export const loadAdminEmails = createAction(
  UsersManagementActionType.LOAD_ADMIN_EMAILS
);

export const loadAdminEmailsSuccess = createAction(
  UsersManagementActionType.LOAD_ADMIN_EMAILS_SUCCESS,
  props<{ payload: string[] }>()
);

export const loadSubsystemUsers = createAction(
  UsersManagementActionType.LOAD_SUBSYSTEM_USERS
);

export const loadSubsystemUsersSuccess = createAction(
  UsersManagementActionType.LOAD_SUBSYSTEM_USERS_SUCCESS,
  props<{ payload: SubsystemUsersResultDto[] }>()
);

export const loadSubsystemUsersForDashboards = createAction(
  UsersManagementActionType.LOAD_SUBSYSTEM_USERS_FOR_DASHBOARDS
);

export const loadSubsystemUsersForDashboardsSuccess = createAction(
  UsersManagementActionType.LOAD_SUBSYSTEM_USERS_FOR_DASHBOARDS_SUCCESS,
  props<{ payload: DashboardUsersResultDto[] }>()
);

export const loadSyncStatus = createAction(
  UsersManagementActionType.LOAD_SYNC_STATUS
);

export const loadSyncStatusSuccess = createAction(
  UsersManagementActionType.LOAD_SYNC_STATUS_SUCCESS,
  props<{ status: string }>()
);
