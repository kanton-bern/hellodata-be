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

import {ActionReducerMap} from '@ngrx/store';
import {metaInfoResourceReducer} from "../metainfo-resource/metainfo-resource.reducer";
import {AppState} from "./app.state";
import {usersManagementReducer} from "../users-management/users-management.reducer";
import {authReducer} from "../auth/auth.reducer";
import {routerReducer} from "@ngrx/router-store";
import {portalRolesManagementReducer} from "../portal-roles-management/portal-roles-management.reducer";
import {announcementReducer} from "../announcement/announcement.reducer";
import {myDashboardsReducer} from "../my-dashboards/my-dashboards.reducer";
import {menuReducer} from "../menu/menu.reducer";
import {startPageReducer} from "../start-page/start-page.reducer";
import {faqReducer} from "../faq/faq.reducer";
import {summaryReducer} from "../summary/summary.reducer";
import {externalDashboardsReducer} from "../external-dashboards/external-dashboards.reducer";
import {myLineageDocsReducer} from "../lineage-docs/lineage-docs.reducer";
import {breadcrumbReducer} from "../breadcrumb/breadcrumb.reducer";
import {unsavedChangesReducer} from "../unsaved-changes/unsaved-changes.reducer";
import {queriesReducer} from "../queries/queries.reducer";
import {dashboardAccessReducer} from "../dashboard-access/dashboard-access.reducer";
import {dashboardGroupsReducer} from "../dashboard-groups/dashboard-groups.reducer";

export const appReducers: ActionReducerMap<AppState, any> = {
  auth: authReducer,
  router: routerReducer,
  metaInfoResources: metaInfoResourceReducer,
  usersManagement: usersManagementReducer,
  portalRolesManagement: portalRolesManagementReducer,
  announcements: announcementReducer,
  myDashboards: myDashboardsReducer,
  menu: menuReducer,
  startPage: startPageReducer,
  faq: faqReducer,
  summary: summaryReducer,
  externalDashboards: externalDashboardsReducer,
  myLineageDocs: myLineageDocsReducer,
  breadcrumbs: breadcrumbReducer,
  unsavedChanges: unsavedChangesReducer,
  queries: queriesReducer,
  dashboardAccess: dashboardAccessReducer,
  dashboardGroups: dashboardGroupsReducer
};
