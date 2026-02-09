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

import {AppState} from "../app/app.state";
import {createSelector} from "@ngrx/store";
import {MyDashboardsState} from "./my-dashboards.state";
import {ALL_DATA_DOMAINS} from "../app/app.constants";
import {selectQueryParam, selectRouteParam, selectUrl} from "../router/router.selectors";
import {selectCurrentUserCommentPermissions, selectProfile} from "../auth/auth.selector";
import {DashboardCommentEntry, DashboardCommentStatus, DashboardCommentVersion} from "./my-dashboards.model";
import {CommentPermissions} from "../users-management/users-management.model";

const myDashboardsState = (state: AppState) => state.myDashboards;
const metaInfoResourcesState = (state: AppState) => state.metaInfoResources;

export const selectDashboardId = selectRouteParam('id');
export const selectInstanceName = selectRouteParam('instanceName');
export const selectFilteredBy = selectQueryParam('filteredBy');
export const selectContextKey = selectRouteParam('contextKey');
export const selectContextName = selectQueryParam('contextName');

export const selectSelectedDataDomain = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.selectedDataDomain
);

export const selectAvailableDataDomains = createSelector(
  myDashboardsState,
  selectSelectedDataDomain,
  (state: MyDashboardsState, selectedDataDomain) => {
    if (selectedDataDomain) {
      return state.availableDataDomains.filter(dataDomain => dataDomain.id !== selectedDataDomain.id).sort((a, b) => a.name.localeCompare(b.name));
    }
    return state.availableDataDomains.sort((a, b) => a.name.localeCompare(b.name));

  }
);

export const selectAllAvailableDataDomains = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => {
    return [...state.availableDataDomains].sort((a, b) => a.name.localeCompare(b.name));

  }
);

export const selectMyDashboards = createSelector(
  myDashboardsState,
  selectSelectedDataDomain,
  (state: MyDashboardsState, selectedDataDomain) => {
    const allDashboards = [...state.myDashboards].sort((a, b) => a.dashboardTitle.localeCompare(b.dashboardTitle));
    if (selectedDataDomain === null || selectedDataDomain.id === '') {
      return allDashboards;
    }
    return allDashboards.filter(dashboard => dashboard.contextId === selectedDataDomain.id);
  }
);

export const selectMyDashboardsFiltered = createSelector(
  myDashboardsState,
  selectSelectedDataDomain,
  selectFilteredBy,
  (state: MyDashboardsState, selectedDataDomain, filteredByParam) => {
    let allDashboards = [...state.myDashboards].sort((a, b) => a.dashboardTitle.localeCompare(b.dashboardTitle));
    if (filteredByParam) {
      allDashboards = allDashboards.filter(dashboard => dashboard.contextId === filteredByParam);
    }
    if (selectedDataDomain === null || selectedDataDomain.id === '') {
      return allDashboards;
    }
    return allDashboards.filter(dashboard => dashboard.contextId === selectedDataDomain.id);
  }
);

export const selectAvailableDataDomainItems = createSelector(
  selectAvailableDataDomains,
  selectSelectedDataDomain,
  (availableDataDomains, selectedDataDomain) => {
    if (selectedDataDomain?.id === '') {
      return availableDataDomains
        .filter(availableDataDomain => availableDataDomain.id)
        .map(availableDataDomain => ({
          label: availableDataDomain.name,
          data: availableDataDomain
        }));
    }
    return [{
      label: selectedDataDomain?.name,
      data: selectedDataDomain
    }];
  }
);

export const selectAvailableDataDomainsWithAllEntry = createSelector(
  selectAvailableDataDomainItems,
  (availableDataDomains) => {
    return [
      {
        key: ALL_DATA_DOMAINS,
        label: ALL_DATA_DOMAINS
      },
      ...availableDataDomains.map(dataDomain => {
        return {
          key: dataDomain.data?.key,
          label: dataDomain.data?.name
        }
      })
    ]
  }
);

// Selector to get context name by context key
// Uses availableDataDomains first, falls back to contextName query param, then contextKey
export const selectContextNameByKey = createSelector(
  myDashboardsState,
  selectContextKey,
  selectContextName,
  (state: MyDashboardsState, contextKey, contextNameParam) => {
    if (!contextKey) {
      return undefined;
    }
    const domain = state.availableDataDomains.find(d => d.key === contextKey);
    const fallbackName = Array.isArray(contextNameParam) ? contextNameParam[0] : contextNameParam;
    return domain?.name || fallbackName || contextKey;
  }
);

export const selectCurrentMyDashboardInfo = createSelector(
  myDashboardsState,
  metaInfoResourcesState,
  selectDashboardId,
  selectInstanceName,
  selectProfile,
  selectUrl,
  (myDashboardsState, metainfoResourcesState, dashboardId, instanceName, profile, currentUrl) => {
    return {
      appinfo: metainfoResourcesState.appInfos.find(entry => entry.instanceName === instanceName),
      dashboard: myDashboardsState.myDashboards.find(entry => entry.instanceName === instanceName && (entry.slug === dashboardId || `${entry.id}` === dashboardId)),
      profile,
      currentUrl
    }
  }
);

// Comments selectors
export const selectCurrentDashboardId = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.currentDashboardId
);

export const selectCurrentDashboardContextKey = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.currentDashboardContextKey
);

export const selectCurrentDashboardUrl = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.currentDashboardUrl
);

export const selectCurrentDashboardComments = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.currentDashboardComments
);

export const selectAvailableTags = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.availableTags
);

// Helper function to get active version from comment
const getActiveVersion = (comment: DashboardCommentEntry): DashboardCommentVersion | undefined =>
  comment.history.find(v => v.version === comment.activeVersion);

export const selectVisibleComments = createSelector(
  selectCurrentDashboardComments,
  (comments) => {
    // Backend already filters comments based on user permissions
    // Just ensure we're showing the correct active version for each comment
    return comments
      .map(comment => {
        const currentActive = getActiveVersion(comment);
        if (!currentActive) return null;

        return {
          ...comment,
          activeVersion: currentActive.version
        };
      })
      .filter((c): c is DashboardCommentEntry => c !== null)
      .sort((a, b) => a.createdDate - b.createdDate);
  }
);

export const selectPublishedComments = createSelector(
  selectCurrentDashboardComments,
  (comments) => comments.filter(c => {
    const activeVersion = getActiveVersion(c);
    return activeVersion?.status === DashboardCommentStatus.PUBLISHED;
  }).sort((a, b) => a.createdDate - b.createdDate)
);

export const selectDraftComments = createSelector(
  selectCurrentDashboardComments,
  (comments) => comments.filter(c => {
    const activeVersion = getActiveVersion(c);
    return activeVersion?.status === DashboardCommentStatus.DRAFT;
  }).sort((a, b) => a.createdDate - b.createdDate)
);

export const selectCommentsCount = createSelector(
  selectCurrentDashboardComments,
  (comments) => comments.length
);

export const selectPublishedCommentsCount = createSelector(
  selectPublishedComments,
  (comments) => comments.length
);

export const canEditComment = createSelector(
  selectProfile,
  selectCurrentDashboardContextKey,
  selectCurrentUserCommentPermissions,
  (profile, contextKey, commentPermissions) => (comment: DashboardCommentEntry) => {
    const currentUserEmail = profile?.email;
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || activeVersion.status === DashboardCommentStatus.DELETED || comment.deleted) return false;

    const perms: CommentPermissions | undefined = contextKey ? commentPermissions[contextKey] : undefined;

    // Review users can edit any comment
    if (perms?.reviewComments) return true;

    // Write users can only edit their own comments
    if (perms?.writeComments) {
      return !!(currentUserEmail && comment.authorEmail === currentUserEmail);
    }

    return false;
  }
);

export const canPublishComment = createSelector(
  selectCurrentDashboardContextKey,
  selectCurrentUserCommentPermissions,
  (contextKey, commentPermissions) => (comment: DashboardCommentEntry) => {
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || activeVersion.status === DashboardCommentStatus.DELETED || comment.deleted) return false;

    const perms: CommentPermissions | undefined = contextKey ? commentPermissions[contextKey] : undefined;
    if (!perms?.reviewComments) return false;

    return (activeVersion.status === DashboardCommentStatus.READY_FOR_REVIEW ||
        activeVersion.status === DashboardCommentStatus.DECLINED) &&
      activeVersion.text.length > 0;
  }
);

export const canDeleteComment = createSelector(
  selectProfile,
  selectCurrentDashboardContextKey,
  selectCurrentUserCommentPermissions,
  (profile, contextKey, commentPermissions) => (comment: DashboardCommentEntry) => {
    const currentUserEmail = profile?.email;
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || comment.deleted) return false;

    const perms: CommentPermissions | undefined = contextKey ? commentPermissions[contextKey] : undefined;

    // Review users can delete any comment
    if (perms?.reviewComments) return true;

    // Write users can delete their own comments
    if (perms?.writeComments) {
      return !!(currentUserEmail && comment.authorEmail === currentUserEmail);
    }

    return false;
  }
);

export const canViewCommentMetadata = createSelector(
  selectProfile,
  selectCurrentDashboardContextKey,
  selectCurrentUserCommentPermissions,
  (profile, contextKey, commentPermissions) => (comment: DashboardCommentEntry) => {
    const perms: CommentPermissions | undefined = contextKey ? commentPermissions[contextKey] : undefined;
    if (perms?.reviewComments) return true;
    return !!(profile?.email && comment.authorEmail === profile.email);
  }
);

export const canViewMetadataAndVersions = createSelector(
  selectCurrentDashboardContextKey,
  selectCurrentUserCommentPermissions,
  (contextKey, commentPermissions) => {
    const perms: CommentPermissions | undefined = contextKey ? commentPermissions[contextKey] : undefined;
    return !!perms?.reviewComments;
  }
);

// Selectors for domain-comments component (with explicit contextKey parameter)
export const canEditCommentForContext = (contextKey: string) => createSelector(
  selectProfile,
  selectCurrentUserCommentPermissions,
  (profile, commentPermissions) => (comment: DashboardCommentEntry) => {
    const currentUserEmail = profile?.email;
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || activeVersion.status === DashboardCommentStatus.DELETED || comment.deleted) return false;

    const perms: CommentPermissions | undefined = commentPermissions[contextKey];

    // Review users can edit any comment
    if (perms?.reviewComments) return true;

    // Write users can only edit their own comments
    if (perms?.writeComments) {
      return !!(currentUserEmail && comment.authorEmail === currentUserEmail);
    }

    return false;
  }
);

export const canPublishCommentForContext = (contextKey: string) => createSelector(
  selectCurrentUserCommentPermissions,
  (commentPermissions) => (comment: DashboardCommentEntry) => {
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || activeVersion.status === DashboardCommentStatus.DELETED || comment.deleted) return false;

    const perms: CommentPermissions | undefined = commentPermissions[contextKey];
    if (!perms?.reviewComments) return false;

    return (activeVersion.status === DashboardCommentStatus.READY_FOR_REVIEW ||
        activeVersion.status === DashboardCommentStatus.DECLINED) &&
      activeVersion.text.length > 0;
  }
);

export const canDeleteCommentForContext = (contextKey: string) => createSelector(
  selectProfile,
  selectCurrentUserCommentPermissions,
  (profile, commentPermissions) => (comment: DashboardCommentEntry) => {
    const currentUserEmail = profile?.email;
    const activeVersion = getActiveVersion(comment);
    if (!activeVersion || comment.deleted) return false;

    const perms: CommentPermissions | undefined = commentPermissions[contextKey];

    // Review users can delete any comment
    if (perms?.reviewComments) return true;

    // Write users can delete their own comments
    if (perms?.writeComments) {
      return !!(currentUserEmail && comment.authorEmail === currentUserEmail);
    }

    return false;
  }
);

export const canViewCommentMetadataForContext = (contextKey: string) => createSelector(
  selectProfile,
  selectCurrentUserCommentPermissions,
  (profile, commentPermissions) => (comment: DashboardCommentEntry) => {
    const perms: CommentPermissions | undefined = commentPermissions[contextKey];
    if (perms?.reviewComments) return true;
    return !!(profile?.email && comment.authorEmail === profile.email);
  }
);

export const canViewMetadataAndVersionsForContext = (contextKey: string) => createSelector(
  selectCurrentUserCommentPermissions,
  (commentPermissions) => {
    const perms: CommentPermissions | undefined = commentPermissions[contextKey];
    return !!perms?.reviewComments;
  }
);
