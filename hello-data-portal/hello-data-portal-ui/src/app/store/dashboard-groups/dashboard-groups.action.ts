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
import {DashboardGroup, DashboardGroupCreateUpdate, DashboardGroupDomainUser} from "./dashboard-groups.model";

export enum DashboardGroupsActionType {
  LOAD_DASHBOARD_GROUPS = '[DASHBOARD GROUPS] Load dashboard groups',
  LOAD_DASHBOARD_GROUPS_SUCCESS = '[DASHBOARD GROUPS] Load dashboard groups success',
  OPEN_DASHBOARD_GROUP_EDITION = '[DASHBOARD GROUPS] Open dashboard group edition',
  LOAD_DASHBOARD_GROUP_BY_ID = '[DASHBOARD GROUPS] Load dashboard group by id',
  LOAD_DASHBOARD_GROUP_BY_ID_SUCCESS = '[DASHBOARD GROUPS] Load dashboard group by id success',
  SAVE_CHANGES_TO_DASHBOARD_GROUP = '[DASHBOARD GROUPS] Save changes to dashboard group',
  SAVE_CHANGES_TO_DASHBOARD_GROUP_SUCCESS = '[DASHBOARD GROUPS] Save changes to dashboard group success',
  SHOW_DELETE_DASHBOARD_GROUP_POPUP = '[DASHBOARD GROUPS] Show delete dashboard group popup',
  HIDE_DELETE_DASHBOARD_GROUP_POPUP = '[DASHBOARD GROUPS] Hide delete dashboard group popup',
  DELETE_DASHBOARD_GROUP = '[DASHBOARD GROUPS] Delete dashboard group',
  DELETE_DASHBOARD_GROUP_SUCCESS = '[DASHBOARD GROUPS] Delete dashboard group success',
  SET_ACTIVE_CONTEXT_KEY = '[DASHBOARD GROUPS] Set active context key',
  LOAD_ELIGIBLE_USERS = '[DASHBOARD GROUPS] Load eligible users',
  LOAD_ELIGIBLE_USERS_SUCCESS = '[DASHBOARD GROUPS] Load eligible users success',
}

export const loadDashboardGroups = createAction(
  DashboardGroupsActionType.LOAD_DASHBOARD_GROUPS,
  props<{ contextKey: string; page: number; size: number; sort?: string; search?: string }>()
);

export const loadDashboardGroupsSuccess = createAction(
  DashboardGroupsActionType.LOAD_DASHBOARD_GROUPS_SUCCESS,
  props<{ payload: DashboardGroup[]; totalElements: number }>()
);

export const openDashboardGroupEdition = createAction(
  DashboardGroupsActionType.OPEN_DASHBOARD_GROUP_EDITION,
  props<{ dashboardGroup: DashboardGroup | null }>()
);

export const loadDashboardGroupById = createAction(
  DashboardGroupsActionType.LOAD_DASHBOARD_GROUP_BY_ID
);

export const loadDashboardGroupByIdSuccess = createAction(
  DashboardGroupsActionType.LOAD_DASHBOARD_GROUP_BY_ID_SUCCESS,
  props<{ dashboardGroup: DashboardGroup }>()
);

export const saveChangesToDashboardGroup = createAction(
  DashboardGroupsActionType.SAVE_CHANGES_TO_DASHBOARD_GROUP,
  props<{ dashboardGroup: DashboardGroupCreateUpdate }>()
);

export const saveChangesToDashboardGroupSuccess = createAction(
  DashboardGroupsActionType.SAVE_CHANGES_TO_DASHBOARD_GROUP_SUCCESS
);

export const showDeleteDashboardGroupPopup = createAction(
  DashboardGroupsActionType.SHOW_DELETE_DASHBOARD_GROUP_POPUP,
  props<{ dashboardGroup: DashboardGroup }>()
);

export const hideDeleteDashboardGroupPopup = createAction(
  DashboardGroupsActionType.HIDE_DELETE_DASHBOARD_GROUP_POPUP
);

export const deleteDashboardGroup = createAction(
  DashboardGroupsActionType.DELETE_DASHBOARD_GROUP
);

export const deleteDashboardGroupSuccess = createAction(
  DashboardGroupsActionType.DELETE_DASHBOARD_GROUP_SUCCESS
);

export const setActiveContextKey = createAction(
  DashboardGroupsActionType.SET_ACTIVE_CONTEXT_KEY,
  props<{ contextKey: string }>()
);

export const loadEligibleUsers = createAction(
  DashboardGroupsActionType.LOAD_ELIGIBLE_USERS,
  props<{ contextKey: string }>()
);

export const loadEligibleUsersSuccess = createAction(
  DashboardGroupsActionType.LOAD_ELIGIBLE_USERS_SUCCESS,
  props<{ users: DashboardGroupDomainUser[] }>()
);

