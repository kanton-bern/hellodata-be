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

import {createSelector} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {UsersManagementState} from "./users-management.state";
import {selectRouteParam} from "../router/router.selectors";
import {
  BUSINESS_DOMAIN_ADMIN_ROLE,
  BUSINESS_DOMAIN_CONTEXT_TYPE,
  DATA_DOMAIN_ADMIN_ROLE,
  DATA_DOMAIN_CONTEXT_TYPE,
  HELLODATA_ADMIN_ROLE,
  NONE_ROLE
} from "./users-management.model";
import {selectCurrentUserPermissions, selectIsSuperuser} from "../auth/auth.selector";

const usersManagementState = (state: AppState) => state.usersManagement;
export const selectParamUserId = selectRouteParam('userId');

export const selectUsers = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.users
);

export const selectUsersTotalRecords = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.usersTotalRecords
);

export const selectUsersLoading = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.usersLoading
);

export const selectSubsystemUsers = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.subsystemUsers
);

export const selectSubsystemUsersForDashboards = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.subsystemUsersForDashboards
);

export const selectUsersCopy = createSelector(
  usersManagementState,
  (state: UsersManagementState) => [...state.users]
);
export const selectUserForPopup = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.userForPopup
);

export const selectEditedUser = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.editedUser
);

export const selectAllDashboardsWithMarkedUser = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.allDashboardsWithMarkedUser
);

export const selectAllDashboardsWithMarkedUserFetched = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.allDashboardsWithMarkedUserFetched
);

export const selectAllBusinessDomains = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.allAvailableContexts.filter(context => context.type === BUSINESS_DOMAIN_CONTEXT_TYPE)
);

export const selectAllDataDomains = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.allAvailableContexts.filter(context => context.type === DATA_DOMAIN_CONTEXT_TYPE).sort((a, b) => {
    if (a.name && b.name) {
      return a.name?.localeCompare(b.name)
    }
    return 0;
  })
);

export const selectUserContextRoles = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.userContextRoles
);

export const selectEditedUserIsBusinessContextAdmin = createSelector(
  usersManagementState,
  (state: UsersManagementState) => {
    if (state.userContextRoles.length > 0) {
      return state.userContextRoles.some(userContextRole => userContextRole.context.type === BUSINESS_DOMAIN_CONTEXT_TYPE && (userContextRole.role.name === HELLODATA_ADMIN_ROLE || userContextRole.role.name === BUSINESS_DOMAIN_ADMIN_ROLE));
    }
    return false;
  }
);

export const selectEditedUserIsAdminAndCurrentIsSuperuser = createSelector(
  usersManagementState,
  selectIsSuperuser,
  (state: UsersManagementState, isCurrentUserSuperuser) => {
    if (state.userContextRoles.length > 0) {
      return {
        isBusinessDomainAdmin: state.userContextRoles.some(userContextRole => userContextRole.context.type === BUSINESS_DOMAIN_CONTEXT_TYPE && (userContextRole.role.name === HELLODATA_ADMIN_ROLE || userContextRole.role.name === BUSINESS_DOMAIN_ADMIN_ROLE)),
        isCurrentUserSuperuser
      }
    }
    return {
      isBusinessDomainAdmin: false,
      isCurrentUserSuperuser
    };
  }
);

export const selectAvailableRolesForBusinessDomain = createSelector(
  usersManagementState,
  selectCurrentUserPermissions,
  selectEditedUserIsAdminAndCurrentIsSuperuser,
  (state: UsersManagementState, currentUserPermissions, result) => {
    if (!result.isCurrentUserSuperuser && currentUserPermissions.some(permission => permission === 'USER_MANAGEMENT')) {
      return state.allAvailableContextRoles.filter(role => role.name === NONE_ROLE || role.name === BUSINESS_DOMAIN_ADMIN_ROLE || role.contextType === BUSINESS_DOMAIN_CONTEXT_TYPE);
    }
    if (!result.isBusinessDomainAdmin && !result.isCurrentUserSuperuser) {
      return state.allAvailableContextRoles.filter(role => role.name === NONE_ROLE);
    }
    return state.allAvailableContextRoles.filter(role => role.contextType === BUSINESS_DOMAIN_CONTEXT_TYPE || role.contextType === null);
  }
);

export const selectAvailableRolesForDataDomain = createSelector(
  usersManagementState,
  (state: UsersManagementState) => {
    if (state.selectedBusinessContextRoleForEditedUser && (state.selectedBusinessContextRoleForEditedUser.name === HELLODATA_ADMIN_ROLE || state.selectedBusinessContextRoleForEditedUser.name === BUSINESS_DOMAIN_ADMIN_ROLE)) {
      return state.allAvailableContextRoles.filter(role => role.name === DATA_DOMAIN_ADMIN_ROLE);
    }
    return state.allAvailableContextRoles.filter(role => role.contextType === DATA_DOMAIN_CONTEXT_TYPE || role.contextType === null).sort((roleA, roleB) => {
      if (roleA.name === NONE_ROLE && roleB.name !== NONE_ROLE) {
        return -1; // roleA with name 'NONE' comes first
      } else if (roleA.name !== NONE_ROLE && roleB.name === NONE_ROLE) {
        return 1; // roleB with name 'NONE' comes first
      } else {
        return 0; // Both have the same name (either 'NONE' or not 'NONE')
      }
    });
  });

export const selectSelectedRolesForUser = createSelector(
  usersManagementState,
  selectParamUserId,
  (state: UsersManagementState, userId: string | undefined) => {
    return {
      userId,
      businessDomainRole: state.selectedBusinessContextRoleForEditedUser,
      dataDomainRoles: state.selectedDataDomainRolesForEditedUser
    }
  }
);

export const selectSyncStatus = createSelector(
  usersManagementState,
  (state: UsersManagementState) => {
    return state.syncStatus
  }
);


export const selectDashboardsForUser = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.selectedDashboardsForUser
);

export const selectAdminEmails = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.adminEmails
);

export const selectUserSaveButtonDisabled = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.userSaveButtonDisabled
);

export const selectSubsystemUsersLoading = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.subsystemUsersLoading
);

export const selectSubsystemUsersForDashboardsLoading = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.subsystemUsersForDashboardsLoading
);

export const selectCurrentPagination = createSelector(
  usersManagementState,
  (state: UsersManagementState) => state.currentPagination
);


