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

import {
  ContextDashboardsForUser,
  DashboardForUser,
  DashboardUsersResultDto,
  SubsystemUsersResultDto,
  User,
  UserActionForPopup
} from "./users-management.model";
import {Context, Role} from "./context-role.model";

export interface UsersManagementState {
  permissionsForCurrentUser: string[],
  users: User[],
  usersLoading: boolean,
  usersTotalRecords: number,
  userForPopup: UserActionForPopup | null
  editedUser: User | null,
  allDashboardsWithMarkedUser: DashboardForUser[],
  allDashboardsWithMarkedUserFetched: boolean,
  allAvailableContextRoles: Role[],
  allAvailableContexts: Context[],
  userContextRoles: any[],
  selectedBusinessContextRoleForEditedUser: Role | null,
  selectedDataDomainRolesForEditedUser: any[],
  selectedDashboardsForUser: ContextDashboardsForUser[],
  adminEmails: string[],
  userSaveButtonDisabled: boolean,
  subsystemUsers: SubsystemUsersResultDto[],
  subsystemUsersLoading: boolean,
  subsystemUsersForDashboards: DashboardUsersResultDto[],
  syncStatus: string
}

export const initialUsersManagementState: UsersManagementState = {
  permissionsForCurrentUser: [],
  users: [],
  usersLoading: false,
  usersTotalRecords: 0,
  userForPopup: null,
  editedUser: null,
  allDashboardsWithMarkedUser: [],
  allDashboardsWithMarkedUserFetched: false,
  allAvailableContextRoles: [],
  allAvailableContexts: [],
  userContextRoles: [],
  selectedBusinessContextRoleForEditedUser: null,
  selectedDataDomainRolesForEditedUser: [],
  selectedDashboardsForUser: [],
  adminEmails: [],
  userSaveButtonDisabled: false,
  subsystemUsers: [],
  subsystemUsersLoading: false,
  subsystemUsersForDashboards: [],
  syncStatus: 'COMPLETED'
}


