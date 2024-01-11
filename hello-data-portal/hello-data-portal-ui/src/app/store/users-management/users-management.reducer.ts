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
import {UsersManagementActions, UsersManagementActionType} from "./users-management.action";
import {BUSINESS_DOMAIN_CONTEXT_TYPE, DATA_DOMAIN_CONTEXT_TYPE} from "./users-management.model";
import {copy} from "../start-page/start-page.selector";

export const CURRENT_EDITED_USER_ID = 'UM_current_edited_user_id';
export const usersManagementReducer = (
  state = initialUsersManagementState,
  action: UsersManagementActions
): UsersManagementState => {
  switch (action.type) {
    case UsersManagementActionType.LOAD_USERS_SUCCESS: {
      for (const key in sessionStorage) {
        if (key.startsWith('UM_')) {
          sessionStorage.removeItem(key);
        }
      }
      return {
        ...state,
        users: action.payload,
        allDashboardsWithMarkedUser: [],
        allDashboardsWithMarkedUserFetched: false,
        userContextRoles: [],
        selectedDataDomainRolesForEditedUser: [],
        selectedBusinessContextRoleForEditedUser: null,
        editedUser: null,

      };
    }
    case UsersManagementActionType.SHOW_USER_ACTION_POP_UP: {
      return {
        ...state,
        userForPopup: action.userActionForPopup,
      };
    }
    case UsersManagementActionType.HIDE_USER_ACTION_POP_UP: {
      return {
        ...state,
        userForPopup: null
      };
    }
    case UsersManagementActionType.LOAD_USER_BY_ID_SUCCESS: {
      return {
        ...state,
        editedUser: action.user
      };
    }
    case UsersManagementActionType.LOAD_DASHBOARDS_SUCCESS: {
      return {
        ...state,
        allDashboardsWithMarkedUser: action.dashboards,
        allDashboardsWithMarkedUserFetched: true
      };
    }
    case UsersManagementActionType.LOAD_AVAILABLE_CONTEXT_ROLES_SUCCESS: {
      return {
        ...state,
        allAvailableContextRoles: action.payload
      };
    }
    case UsersManagementActionType.LOAD_AVAILABLE_CONTEXTS_SUCCESS: {
      return {
        ...state,
        allAvailableContexts: action.payload.contexts
      };
    }
    case UsersManagementActionType.SELECT_BUSINESS_DOMAIN_ROLE_FOR_EDITED_USER: {
      return {
        ...state,
        selectedBusinessContextRoleForEditedUser: action.selectedRole,
      };
    }
    case UsersManagementActionType.LOAD_USER_CONTEXT_ROLES_SUCCESS: {
      const selectedBusinessContextRole = action.payload.filter((userContextRole: any) => userContextRole.context.type === BUSINESS_DOMAIN_CONTEXT_TYPE)[0];
      const selectedDataDomainContextRoles = action.payload.filter((userContextRole: any) => userContextRole.context.type === DATA_DOMAIN_CONTEXT_TYPE);

      return {
        ...state,
        userContextRoles: action.payload,
        selectedBusinessContextRoleForEditedUser: selectedBusinessContextRole ? selectedBusinessContextRole.role : null,
        selectedDataDomainRolesForEditedUser: selectedDataDomainContextRoles ? selectedDataDomainContextRoles : []
      }
    }

    case UsersManagementActionType.SELECT_DATA_DOMAIN_ROLE_FOR_EDITED_USER: {
      const selectedContextId = action.selectedRoleForContext.context.id;
      let updatedDataDomainRoles;
      const isRoleForContextAlreadySelected = state.selectedDataDomainRolesForEditedUser.some(
        roleForContext => roleForContext.context.id === selectedContextId
      );
      if (state.selectedDataDomainRolesForEditedUser.length === 0) {
        // If the array is empty, simply add the selected element
        updatedDataDomainRoles = [action.selectedRoleForContext];
      } else {
        // Otherwise, update the existing element with the same context.id or add the selected element
        updatedDataDomainRoles = state.selectedDataDomainRolesForEditedUser.map(roleForContext => {
          if (roleForContext.context.id === selectedContextId) {
            // Replace the existing element with the same context.id
            return action.selectedRoleForContext;
          }
          return roleForContext;
        });

        // Add the selected element if it doesn't exist in the array
        if (!isRoleForContextAlreadySelected) {
          updatedDataDomainRoles.push(action.selectedRoleForContext);
        }
      }
      return {
        ...state,
        selectedDataDomainRolesForEditedUser: updatedDataDomainRoles
      };
    }
    case UsersManagementActionType.SET_SELECTED_DASHBOARD_FOR_USER: {
      const contextDashboardsForUser = copy(state.selectedDashboardsForUser);
      let found = false;
      contextDashboardsForUser.forEach(contextDashboardsForUser => {
        if (contextDashboardsForUser.contextKey === action.contextKey) {
          contextDashboardsForUser.dashboards = action.dashboards;
          found = true;
        }
      });
      if (!found) {
        contextDashboardsForUser.push({
          contextKey: action.contextKey,
          dashboards: action.dashboards
        })
      }
      return {
        ...state,
        selectedDashboardsForUser: contextDashboardsForUser
      };

    }
    case UsersManagementActionType.NAVIGATE_TO_USERS_MANAGEMENT: {
      return {
        ...state,
        selectedDashboardsForUser: [],
      };
    }
    case UsersManagementActionType.LOAD_ADMIN_EMAILS_SUCCESS: {
      return {
        ...state,
        adminEmails: action.payload
      };
    }
    default:
      return state;
  }
};
