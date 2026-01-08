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

import {initialMyDashboardsState, MyDashboardsState} from "./my-dashboards.state";
import {ALL_DATA_DOMAINS} from "../app/app.constants";
import {createReducer, on} from "@ngrx/store";
import {
  addCommentSuccess,
  cloneCommentForEditSuccess,
  deleteCommentSuccess,
  loadAvailableDataDomainsSuccess,
  loadDashboardCommentsSuccess,
  loadMyDashboardsSuccess,
  publishCommentSuccess,
  setCurrentDashboard,
  setSelectedDataDomain,
  unpublishCommentSuccess,
  updateCommentSuccess
} from "./my-dashboards.action";
import {DataDomain} from "./my-dashboards.model";

export const SELECTED_DATA_DOMAIN_KEY = 'Selected_Data_Domain';

export const myDashboardsReducer = createReducer(
  initialMyDashboardsState,
  on(loadMyDashboardsSuccess, (state: MyDashboardsState, {payload}): MyDashboardsState => {
    return {
      ...state,
      myDashboards: payload,
    };
  }),
  on(setSelectedDataDomain, (state: MyDashboardsState, {dataDomain}): MyDashboardsState => {
    localStorage.setItem(SELECTED_DATA_DOMAIN_KEY, JSON.stringify(dataDomain));
    return {
      ...state,
      selectedDataDomain: dataDomain
    }
  }),
  on(loadAvailableDataDomainsSuccess, (state: MyDashboardsState, {payload}): MyDashboardsState => {
    const uniqueDataDomains = [
      {
        id: '',
        name: ALL_DATA_DOMAINS,
        key: ''
      },
      ...payload
    ]
    const selectedDataDomain = uniqueDataDomains.find(dataDomain => dataDomain.id === state.selectedDataDomain?.id);
    let defaultDataDomain = uniqueDataDomains[0];
    const selectedDataDomainLocalStorage = localStorage.getItem(SELECTED_DATA_DOMAIN_KEY);
    if (selectedDataDomainLocalStorage) {
      defaultDataDomain = JSON.parse(selectedDataDomainLocalStorage) as DataDomain;
    }
    return {
      ...state,
      selectedDataDomain: selectedDataDomain ?? defaultDataDomain,
      availableDataDomains: uniqueDataDomains,
    }
  }),
  // Comments reducers
  on(setCurrentDashboard, (state: MyDashboardsState, {dashboardId, contextKey, dashboardUrl}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardId: dashboardId,
      currentDashboardContextKey: contextKey,
      currentDashboardUrl: dashboardUrl,
      currentDashboardComments: []
    }
  }),
  on(loadDashboardCommentsSuccess, (state: MyDashboardsState, {comments}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: comments
    }
  }),
  on(addCommentSuccess, (state: MyDashboardsState, {comment}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: [...state.currentDashboardComments, comment]
    }
  }),
  on(updateCommentSuccess, (state: MyDashboardsState, {comment}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: state.currentDashboardComments.map(c =>
        c.id === comment.id ? comment : c
      )
    }
  }),
  on(deleteCommentSuccess, (state: MyDashboardsState, {commentId, deletedDate, deletedBy}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: state.currentDashboardComments.map(c =>
        c.id === commentId ? {...c, deleted: true, deletedDate, deletedBy} : c
      )
    }
  }),
  on(publishCommentSuccess, (state: MyDashboardsState, {comment}): MyDashboardsState => {
    // If comment has previousVersionId, soft-delete the old version
    let comments = state.currentDashboardComments;
    if (comment.previousVersionId) {
      comments = comments.map(c =>
        c.id === comment.previousVersionId ? {
          ...c,
          deleted: true,
          deletedDate: Date.now(),
          deletedBy: 'System (replaced by new version)'
        } : c
      );
    }
    return {
      ...state,
      currentDashboardComments: comments.map(c =>
        c.id === comment.id ? comment : c
      )
    }
  }),
  on(unpublishCommentSuccess, (state: MyDashboardsState, {comment}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: state.currentDashboardComments.map(c =>
        c.id === comment.id ? comment : c
      )
    }
  }),
  on(cloneCommentForEditSuccess, (state: MyDashboardsState, {clonedComment}): MyDashboardsState => {
    return {
      ...state,
      currentDashboardComments: [...state.currentDashboardComments, clonedComment]
    }
  }),
);
