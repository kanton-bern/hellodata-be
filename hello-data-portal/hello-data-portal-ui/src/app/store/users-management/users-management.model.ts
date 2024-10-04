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

export const BUSINESS_DOMAIN_CONTEXT_TYPE = 'BUSINESS_DOMAIN';
export const DATA_DOMAIN_CONTEXT_TYPE = 'DATA_DOMAIN';
export const HELLODATA_ADMIN_ROLE = 'HELLODATA_ADMIN';
export const BUSINESS_DOMAIN_ADMIN_ROLE = 'BUSINESS_DOMAIN_ADMIN';
export const DATA_DOMAIN_ADMIN_ROLE = 'DATA_DOMAIN_ADMIN';
export const DATA_DOMAIN_EDITOR_ROLE = 'DATA_DOMAIN_EDITOR';
export const DATA_DOMAIN_VIEWER_ROLE = 'DATA_DOMAIN_VIEWER';
export const NONE_ROLE = 'NONE';

export interface User {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  enabled: boolean;
  emailVerified: boolean;
  createdTimestamp: number;
  requiredActions: string[];
  lastAccess: number | null;
  permissions: string[],
  invitationsCount: number,
  superuser: boolean,
  selectedLanguage: string | null
}

export interface CreateUserForm {
  user: AdUser,
  firstName: string,
  lastName: string
}

export interface CreateUserResponse {
  userId: string
}


export interface DashboardForUser {
  id: number,
  title: string,
  instanceName: string,
  instanceUserId: number,
  viewer: boolean,
  changedOnUtc: string,
  compositeId: string,
  contextKey: string
}

export interface DashboardResponse {
  dashboards: DashboardForUser[]
}

export interface UserActionForPopup {
  user: User,
  action: UserAction,
  actionFromUsersEdition: boolean
}

export enum UserAction {
  DELETE = 'Delete', DISABLE = 'Disable', ENABLE = 'Enable'
}

export interface ContextDashboardsForUser {
  contextKey: string,
  dashboards: DashboardForUser[]
}

export interface AdUser {
  email: string,
  firstName: string,
  lastName: string
  label: string
}

export interface SubsystemUserDto {
  name: string;
  surname: string;
  email: string;
  username: string;
  roles: string[];
  subsystemName: string;
}

export interface SubsystemUsersResultDto {
  instanceName: string;
  users: SubsystemUserDto[]
}
