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
import {DashboardCommentEntry, DataDomain} from "./my-dashboards.model";
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
  SEND_FOR_REVIEW = '[MYDASHBOARDS] Send for review',
  SEND_FOR_REVIEW_SUCCESS = '[MYDASHBOARDS] Send for review SUCCESS',
  SEND_FOR_REVIEW_ERROR = '[MYDASHBOARDS] Send for review ERROR',
  DECLINE_COMMENT = '[MYDASHBOARDS] Decline comment',
  DECLINE_COMMENT_SUCCESS = '[MYDASHBOARDS] Decline comment SUCCESS',
  DECLINE_COMMENT_ERROR = '[MYDASHBOARDS] Decline comment ERROR',
  CLONE_COMMENT_FOR_EDIT = '[MYDASHBOARDS] Clone comment for edit',
  CLONE_COMMENT_FOR_EDIT_SUCCESS = '[MYDASHBOARDS] Clone comment for edit SUCCESS',
  CLONE_COMMENT_FOR_EDIT_ERROR = '[MYDASHBOARDS] Clone comment for edit ERROR',
  RESTORE_COMMENT_VERSION = '[MYDASHBOARDS] Restore comment version',
  RESTORE_COMMENT_VERSION_SUCCESS = '[MYDASHBOARDS] Restore comment version SUCCESS',
  RESTORE_COMMENT_VERSION_ERROR = '[MYDASHBOARDS] Restore comment version ERROR',
  // Tags actions
  LOAD_AVAILABLE_TAGS = '[MYDASHBOARDS] Load available tags',
  LOAD_AVAILABLE_TAGS_SUCCESS = '[MYDASHBOARDS] Load available tags SUCCESS',
  LOAD_AVAILABLE_TAGS_ERROR = '[MYDASHBOARDS] Load available tags ERROR',
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
  props<{ dashboardId: number; contextKey: string; dashboardUrl: string }>()
);

export const loadDashboardComments = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS,
  props<{ dashboardId: number; contextKey: string; dashboardUrl: string; includeDeleted?: boolean }>()
);

export const loadDashboardCommentsSuccess = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS_SUCCESS,
  props<{ comments: DashboardCommentEntry[] }>()
);

export const loadDashboardCommentsError = createAction(
  MyDashboardsActionType.LOAD_DASHBOARD_COMMENTS_ERROR,
  props<{ error: any }>()
);

export const addComment = createAction(
  MyDashboardsActionType.ADD_COMMENT,
  props<{
    dashboardId: number;
    contextKey: string;
    dashboardUrl: string;
    text: string;
    pointerUrl?: string;
    tags?: string[]
  }>()
);

export const addCommentSuccess = createAction(
  MyDashboardsActionType.ADD_COMMENT_SUCCESS,
  props<{ comment: DashboardCommentEntry }>()
);

export const addCommentError = createAction(
  MyDashboardsActionType.ADD_COMMENT_ERROR,
  props<{ error: any }>()
);

export const updateComment = createAction(
  MyDashboardsActionType.UPDATE_COMMENT,
  props<{
    dashboardId: number;
    contextKey: string;
    commentId: string;
    text: string;
    pointerUrl?: string;
    entityVersion: number;
    tags?: string[];
  }>()
);

export const updateCommentSuccess = createAction(
  MyDashboardsActionType.UPDATE_COMMENT_SUCCESS,
  props<{ comment: DashboardCommentEntry }>()
);

export const updateCommentError = createAction(
  MyDashboardsActionType.UPDATE_COMMENT_ERROR,
  props<{ error: any }>()
);

export const deleteComment = createAction(
  MyDashboardsActionType.DELETE_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string; deleteEntire?: boolean; deletionReason?: string }>()
);

export const deleteCommentSuccess = createAction(
  MyDashboardsActionType.DELETE_COMMENT_SUCCESS,
  props<{
    commentId: string;
    restoredComment?: DashboardCommentEntry
  }>()
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
  props<{ comment: DashboardCommentEntry }>()
);

export const publishCommentError = createAction(
  MyDashboardsActionType.PUBLISH_COMMENT_ERROR,
  props<{ error: any }>()
);

export const sendForReview = createAction(
  MyDashboardsActionType.SEND_FOR_REVIEW,
  props<{ dashboardId: number; contextKey: string; commentId: string }>()
);

export const sendForReviewSuccess = createAction(
  MyDashboardsActionType.SEND_FOR_REVIEW_SUCCESS,
  props<{ comment: DashboardCommentEntry }>()
);

export const sendForReviewError = createAction(
  MyDashboardsActionType.SEND_FOR_REVIEW_ERROR,
  props<{ error: any }>()
);

export const declineComment = createAction(
  MyDashboardsActionType.DECLINE_COMMENT,
  props<{ dashboardId: number; contextKey: string; commentId: string; declineReason: string }>()
);

export const declineCommentSuccess = createAction(
  MyDashboardsActionType.DECLINE_COMMENT_SUCCESS,
  props<{ comment: DashboardCommentEntry }>()
);

export const declineCommentError = createAction(
  MyDashboardsActionType.DECLINE_COMMENT_ERROR,
  props<{ error: any }>()
);

// Clone published comment for editing (creates a draft copy with new text)
export const cloneCommentForEdit = createAction(
  MyDashboardsActionType.CLONE_COMMENT_FOR_EDIT,
  props<{
    dashboardId: number;
    contextKey: string;
    commentId: string;
    newText: string;
    newPointerUrl?: string;
    entityVersion: number;
    tags?: string[];
  }>()
);

export const cloneCommentForEditSuccess = createAction(
  MyDashboardsActionType.CLONE_COMMENT_FOR_EDIT_SUCCESS,
  props<{ clonedComment: DashboardCommentEntry; originalCommentId: string }>()
);

export const cloneCommentForEditError = createAction(
  MyDashboardsActionType.CLONE_COMMENT_FOR_EDIT_ERROR,
  props<{ error: any }>()
);

// Restore a specific version from history
export const restoreCommentVersion = createAction(
  MyDashboardsActionType.RESTORE_COMMENT_VERSION,
  props<{ dashboardId: number; contextKey: string; commentId: string; versionNumber: number }>()
);

export const restoreCommentVersionSuccess = createAction(
  MyDashboardsActionType.RESTORE_COMMENT_VERSION_SUCCESS,
  props<{ comment: DashboardCommentEntry }>()
);

export const restoreCommentVersionError = createAction(
  MyDashboardsActionType.RESTORE_COMMENT_VERSION_ERROR,
  props<{ error: any }>()
);

// Tags actions
export const loadAvailableTags = createAction(
  MyDashboardsActionType.LOAD_AVAILABLE_TAGS,
  props<{ dashboardId: number; contextKey: string }>()
);

export const loadAvailableTagsSuccess = createAction(
  MyDashboardsActionType.LOAD_AVAILABLE_TAGS_SUCCESS,
  props<{ tags: string[] }>()
);

export const loadAvailableTagsError = createAction(
  MyDashboardsActionType.LOAD_AVAILABLE_TAGS_ERROR,
  props<{ error: any }>()
);
