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
import {CommentEntry, DataDomain} from "./my-dashboards.model";
import {SupersetDashboardWithMetadata} from "../start-page/start-page.model";

export enum MyDashboardsActionType {
  LOAD_MY_DASHBOARDS = '[MYDASHBOARDS] Load MYDASHBOARDS',
  LOAD_MY_DASHBOARDS_SUCCESS = '[MYDASHBOARDS] Load MYDASHBOARDS SUCCESS',
  SET_SELECTED_DATA_DOMAIN = '[MYDASHBOARDS] Set selected Data Domain',

  LOAD_AVAILABLE_DATA_DOMAINS = '[MYDASHBOARDS] Load available Data Domains',
  LOAD_AVAILABLE_DATA_DOMAINS_SUCCESS = '[MYDASHBOARDS] Load available Data Domains SUCCESS',

  UPLOAD_DASHBOARDS_FILE_SUCCESS = '[MYDASHBOARDS] Upload dashboards file SUCCESS',
  UPLOAD_DASHBOARDS_FILE_ERROR = '[MYDASHBOARDS] Upload dashboards file ERROR',

  // Comments actions
  SET_CURRENT_DASHBOARD = '[MYDASHBOARDS] Set current dashboard',
  LOAD_DASHBOARD_COMMENTS = '[MYDASHBOARDS] Load dashboard comments',
  LOAD_DASHBOARD_COMMENTS_SUCCESS = '[MYDASHBOARDS] Load dashboard comments SUCCESS',
  LOAD_DASHBOARD_COMMENTS_ERROR = '[MYDASHBOARDS] Load dashboard comments ERROR',
  ADD_COMMENT = '[MYDASHBOARDS] Add comment',
  ADD_COMMENT_SUCCESS = '[MYDASHBOARDS] Add comment SUCCESS',
  ADD_COMMENT_ERROR = '[MYDASHBOARDS] Add comment ERROR',
  UPDATE_COMMENT = '[MYDASHBOARDS] Update comment',
  UPDATE_COMMENT_SUCCESS = '[MYDASHBOARDS] Update comment SUCCESS',
  UPDATE_COMMENT_ERROR = '[MYDASHBOARDS] Update comment ERROR',
  DELETE_COMMENT = '[MYDASHBOARDS] Delete comment',
  DELETE_COMMENT_SUCCESS = '[MYDASHBOARDS] Delete comment SUCCESS',
  DELETE_COMMENT_ERROR = '[MYDASHBOARDS] Delete comment ERROR',
  PUBLISH_COMMENT = '[MYDASHBOARDS] Publish comment',
  PUBLISH_COMMENT_SUCCESS = '[MYDASHBOARDS] Publish comment SUCCESS',
  PUBLISH_COMMENT_ERROR = '[MYDASHBOARDS] Publish comment ERROR',
  UNPUBLISH_COMMENT = '[MYDASHBOARDS] Unpublish comment',
  UNPUBLISH_COMMENT_SUCCESS = '[MYDASHBOARDS] Unpublish comment SUCCESS',
  UNPUBLISH_COMMENT_ERROR = '[MYDASHBOARDS] Unpublish comment ERROR',
}

export const loadMyDashboards = createAction(
  MyDashboardsActionType.LOAD_MY_DASHBOARDS
);

export const loadMyDashboardsSuccess = createAction(
  MyDashboardsActionType.LOAD_MY_DASHBOARDS_SUCCESS,
  props<{ payload: SupersetDashboardWithMetadata[] }>()
);

export const setSelectedDataDomain = createAction(
  MyDashboardsActionType.SET_SELECTED_DATA_DOMAIN,
  props<{ dataDomain: DataDomain }>()
);

export const loadAvailableDataDomains = createAction(
  MyDashboardsActionType.LOAD_AVAILABLE_DATA_DOMAINS
);

export const loadAvailableDataDomainsSuccess = createAction(
  MyDashboardsActionType.LOAD_AVAILABLE_DATA_DOMAINS_SUCCESS,
  props<{ payload: DataDomain[] }>()
);

export const uploadDashboardsSuccess = createAction(
  MyDashboardsActionType.UPLOAD_DASHBOARDS_FILE_SUCCESS,
);

export const uploadDashboardsError = createAction(
  MyDashboardsActionType.UPLOAD_DASHBOARDS_FILE_ERROR,
  props<{ error: any }>()
);

// Comments actions
export const setCurrentDashboard = createAction(
  MyDashboardsActionType.SET_CURRENT_DASHBOARD,
  props<{ dashboardId: number; contextKey: string }>()
);

export const loadDashboardComments = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS,
  props<{ dashboardId: number; contextKey: string }>()
);

export const loadDashboardCommentsSuccess = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS_SUCCESS,
  props<{ comments: CommentEntry[] }>()
);

export const loadDashboardCommentsError = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS_ERROR,
  props<{ error: any }>()
);

export const addComment = createAction(
  MyDashboardsActionType.ADD_COMMENT,
  props<{ dashboardId: number; contextKey: string; text: string }>()
);

export const addCommentSuccess = createAction(
  MyDashboardsActionType.ADD_COMMENT_SUCCESS,
  props<{ comment: CommentEntry }>()
);

export const addCommentError = createAction(
  MyDashboardsActionType.ADD_COMMENT_ERROR,
  props<{ error: any }>()
);

export const updateComment = createAction(
  MyDashboardsActionType.UPDATE_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string; text: string }>()
);

export const updateCommentSuccess = createAction(
  MyDashboardsActionType.UPDATE_COMMENT_SUCCESS,
  props<{ comment: CommentEntry }>()
);

export const updateCommentError = createAction(
  MyDashboardsActionType.UPDATE_COMMENT_ERROR,
  props<{ error: any }>()
);

export const deleteComment = createAction(
  MyDashboardsActionType.DELETE_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string }>()
);

export const deleteCommentSuccess = createAction(
  MyDashboardsActionType.DELETE_COMMENT_SUCCESS,
  props<{ commentId: string }>()
);

export const deleteCommentError = createAction(
  MyDashboardsActionType.DELETE_COMMENT_ERROR,
  props<{ error: any }>()
);

export const publishComment = createAction(
  MyDashboardsActionType.PUBLISH_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string }>()
);

export const publishCommentSuccess = createAction(
  MyDashboardsActionType.PUBLISH_COMMENT_SUCCESS,
  props<{ comment: CommentEntry }>()
);

export const publishCommentError = createAction(
  MyDashboardsActionType.PUBLISH_COMMENT_ERROR,
  props<{ error: any }>()
);

export const unpublishComment = createAction(
  MyDashboardsActionType.UNPUBLISH_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string }>()
);

export const unpublishCommentSuccess = createAction(
  MyDashboardsActionType.UNPUBLISH_COMMENT_SUCCESS,
  props<{ comment: CommentEntry }>()
);

export const unpublishCommentError = createAction(
  MyDashboardsActionType.UNPUBLISH_COMMENT_ERROR,
  props<{ error: any }>()
);

