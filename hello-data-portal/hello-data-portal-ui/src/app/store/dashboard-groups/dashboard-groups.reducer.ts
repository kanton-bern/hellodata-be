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

import {createReducer, on} from "@ngrx/store";
import {DashboardGroupsState, initialDashboardGroupsState} from "./dashboard-groups.state";
import {
  hideDeleteDashboardGroupPopup,
  loadDashboardGroupByIdSuccess,
  loadDashboardGroups,
  loadDashboardGroupsSuccess,
  loadEligibleUsersSuccess,
  openDashboardGroupEdition,
  setActiveContextKey,
  showDeleteDashboardGroupPopup
} from "./dashboard-groups.action";

export const dashboardGroupsReducer = createReducer(
  initialDashboardGroupsState,
  on(loadDashboardGroups, (state: DashboardGroupsState): DashboardGroupsState => {
    return {
      ...state,
      loading: true
    };
  }),
  on(loadDashboardGroupsSuccess, (state: DashboardGroupsState, {payload, totalElements}): DashboardGroupsState => {
    return {
      ...state,
      dashboardGroups: payload,
      totalElements,
      loading: false
    };
  }),
  on(openDashboardGroupEdition, (state: DashboardGroupsState, {dashboardGroup}): DashboardGroupsState => {
    return {
      ...state,
      editedDashboardGroup: dashboardGroup || {
        name: '',
        contextKey: state.activeContextKey || '',
        entries: [],
        users: []
      }
    };
  }),
  on(loadDashboardGroupByIdSuccess, (state: DashboardGroupsState, {dashboardGroup}): DashboardGroupsState => {
    return {
      ...state,
      editedDashboardGroup: dashboardGroup
    };
  }),
  on(setActiveContextKey, (state: DashboardGroupsState, {contextKey}): DashboardGroupsState => {
    return {
      ...state,
      activeContextKey: contextKey
    };
  }),
  on(loadEligibleUsersSuccess, (state: DashboardGroupsState, {users}): DashboardGroupsState => {
    return {
      ...state,
      eligibleUsers: users
    };
  }),
  on(showDeleteDashboardGroupPopup, (state: DashboardGroupsState, {dashboardGroup}): DashboardGroupsState => {
    return {
      ...state,
      dashboardGroupForDeletion: dashboardGroup
    };
  }),
  on(hideDeleteDashboardGroupPopup, (state: DashboardGroupsState): DashboardGroupsState => {
    return {
      ...state,
      dashboardGroupForDeletion: null
    };
  }),
);
