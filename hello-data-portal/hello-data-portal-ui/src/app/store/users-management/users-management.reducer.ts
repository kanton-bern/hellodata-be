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

import {initialUsersManagementState, UsersManagementState} from "./users-management.state";
import {
  deleteUserInStore,
  hideUserPopupAction,
  loadAdminEmailsSuccess,
  loadAvailableContextRolesSuccess,
  loadAvailableContextsSuccess,
  loadDashboardsSuccess,
  loadSubsystemUsersForDashboardsSuccess,
  loadSubsystemUsersSuccess,
  loadSyncStatusSuccess,
  loadUserByIdSuccess,
  loadUserContextRolesSuccess,
  loadUsers,
  loadUsersSuccess,
  navigateToUsersManagement,
  selectBusinessDomainRoleForEditedUser,
  selectDataDomainRoleForEditedUser,
  setSelectedDashboardForUser,
  showUserActionPopup,
  syncUsersSuccess,
  updateUserInStore,
  updateUserRoles,
  updateUserRolesSuccess
} from "./users-management.action";
import {BUSINESS_DOMAIN_CONTEXT_TYPE, DATA_DOMAIN_CONTEXT_TYPE} from "./users-management.model";
import {copy} from "../start-page/start-page.selector";
import {createReducer, on} from "@ngrx/store";


export const usersManagementReducer = createReducer(
  initialUsersManagementState,
  on(loadUsers, (state: UsersManagementState): UsersManagementState => {
    return {
      ...state,
      usersLoading: true,
    };
  }),
  on(loadUsersSuccess, (state: UsersManagementState, {users, totalElements}): UsersManagementState => {
    for (const key in sessionStorage) {
      if (key.startsWith('UM_')) {
        sessionStorage.removeItem(key);
      }
    }
    return {
      ...state,
      users: users,
      usersLoading: false,
      usersTotalRecords: totalElements,
      allDashboardsWithMarkedUser: [],
      allDashboardsWithMarkedUserFetched: false,
      userContextRoles: [],
      selectedDataDomainRolesForEditedUser: [],
      selectedBusinessContextRoleForEditedUser: null,
      editedUser: null,
    };
  }),
  on(loadSubsystemUsersSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    return {
      ...state,
      subsystemUsers: payload
    };
  }),
  on(loadSubsystemUsersForDashboardsSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    return {
      ...state,
      subsystemUsersForDashboards: payload
    };
  }),
  on(showUserActionPopup, (state: UsersManagementState, {userActionForPopup}): UsersManagementState => {
    return {
      ...state,
      userForPopup: userActionForPopup,
    };
  }),
  on(hideUserPopupAction, (state: UsersManagementState): UsersManagementState => {
    return {
      ...state,
      userForPopup: null,
    };
  }),
  on(loadUserByIdSuccess, (state: UsersManagementState, {user}): UsersManagementState => {
    return {
      ...state,
      editedUser: user
    };
  }),
  on(loadDashboardsSuccess, (state: UsersManagementState, {dashboards}): UsersManagementState => {
    return {
      ...state,
      allDashboardsWithMarkedUser: dashboards,
      allDashboardsWithMarkedUserFetched: true
    };
  }),
  on(loadAvailableContextRolesSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    return {
      ...state,
      allAvailableContextRoles: payload
    };
  }),
  on(loadAvailableContextsSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    return {
      ...state,
      allAvailableContexts: payload.contexts
    };
  }),
  on(selectBusinessDomainRoleForEditedUser, (state: UsersManagementState, {selectedRole}): UsersManagementState => {
    return {
      ...state,
      selectedBusinessContextRoleForEditedUser: selectedRole,
    };
  }),
  on(loadUserContextRolesSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    const selectedBusinessContextRole = payload.filter((userContextRole: any) => userContextRole.context.type === BUSINESS_DOMAIN_CONTEXT_TYPE)[0];
    const selectedDataDomainContextRoles = payload.filter((userContextRole: any) => userContextRole.context.type === DATA_DOMAIN_CONTEXT_TYPE);

    return {
      ...state,
      userContextRoles: payload,
      selectedBusinessContextRoleForEditedUser: selectedBusinessContextRole ? selectedBusinessContextRole.role : null,
      selectedDataDomainRolesForEditedUser: selectedDataDomainContextRoles ? selectedDataDomainContextRoles : []
    }
  }),
  on(selectDataDomainRoleForEditedUser, (state: UsersManagementState, {selectedRoleForContext}): UsersManagementState => {
    const selectedContextId = selectedRoleForContext.context.id;
    let updatedDataDomainRoles;
    const isRoleForContextAlreadySelected = state.selectedDataDomainRolesForEditedUser.some(
      roleForContext => roleForContext.context.id === selectedContextId
    );
    if (state.selectedDataDomainRolesForEditedUser.length === 0) {
      // If the array is empty, simply add the selected element
      updatedDataDomainRoles = [selectedRoleForContext];
    } else {
      // Otherwise, update the existing element with the same context.id or add the selected element
      updatedDataDomainRoles = state.selectedDataDomainRolesForEditedUser.map(roleForContext => {
        if (roleForContext.context.id === selectedContextId) {
          // Replace the existing element with the same context.id
          return selectedRoleForContext;
        }
        return roleForContext;
      });

      // Add the selected element if it doesn't exist in the array
      if (!isRoleForContextAlreadySelected) {
        updatedDataDomainRoles.push(selectedRoleForContext);
      }
    }
    return {
      ...state,
      selectedDataDomainRolesForEditedUser: updatedDataDomainRoles
    };
  }),
  on(setSelectedDashboardForUser, (state: UsersManagementState, {contextKey, dashboards}): UsersManagementState => {
    const contextDashboardsForUser = copy(state.selectedDashboardsForUser);
    let found = false;
    contextDashboardsForUser.forEach(contextDashboardsForUser => {
      if (contextDashboardsForUser.contextKey === contextKey) {
        contextDashboardsForUser.dashboards = dashboards;
        found = true;
      }
    });
    if (!found) {
      contextDashboardsForUser.push({
        contextKey: contextKey,
        dashboards: dashboards
      })
    }
    return {
      ...state,
      selectedDashboardsForUser: contextDashboardsForUser
    };
  }),
  on(navigateToUsersManagement, (state: UsersManagementState): UsersManagementState => {
    return {
      ...state,
      selectedDashboardsForUser: [],
    };
  }),
  on(loadAdminEmailsSuccess, (state: UsersManagementState, {payload}): UsersManagementState => {
    return {
      ...state,
      adminEmails: payload
    };
  }),
  on(updateUserRoles, (state: UsersManagementState): UsersManagementState => {
    return {
      ...state,
      userSaveButtonDisabled: true
    };
  }),
  on(updateUserRolesSuccess, (state: UsersManagementState): UsersManagementState => {
    return {
      ...state,
      userSaveButtonDisabled: false
    };
  }),
  on(syncUsersSuccess, (state: UsersManagementState, {status}): UsersManagementState => {
    return {
      ...state,
      syncStatus: status
    };
  }),
  on(loadSyncStatusSuccess, (state: UsersManagementState, {status}): UsersManagementState => {
    return {
      ...state,
      syncStatus: status
    };
  }),
  on(updateUserInStore, (state: UsersManagementState, {userChanged}): UsersManagementState => {
    return {
      ...state,
      users: state.users.map(user => user.email === userChanged.email ? {...userChanged} : user),
      editedUser: state.editedUser ? userChanged : state.editedUser
    };
  }),
  on(deleteUserInStore, (state: UsersManagementState, {userDeleted}): UsersManagementState => {
    return {
      ...state,
      users: state.users.filter(user => user.email !== userDeleted.email),
    };
  }),
);

export const CURRENT_EDITED_USER_ID = 'UM_current_edited_user_id';
