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

import {MetaInfoResourceState} from "../metainfo-resource/metainfo-resource.state";
import {UsersManagementState} from "../users-management/users-management.state";
import {AuthState} from "../auth/auth.state";
import {RouterReducerState} from "@ngrx/router-store";
import {PortalRolesManagementState} from "../portal-roles-management/portal-roles-management.state";
import {AnnouncementState} from "../announcement/announcement.state";
import {MyDashboardsState} from "../my-dashboards/my-dashboards.state";
import {MenuState} from "../menu/menu.state";
import {StartPageState} from "../start-page/start-page.state";
import {FaqState} from "../faq/faq.state";
import {SummaryState} from "../summary/summary.state";
import {ExternalDashboardsState} from "../external-dashboards/external-dashboards.state";
import {LineageDocsState} from "../lineage-docs/lineageDocsState";
import {BreadcrumbState} from "../breadcrumb/breadcrumb.state";
import {UnsavedChangesState} from "../unsaved-changes/unsaved-changes.state";
import {QueriesState} from "../queries/queries.state";
import {DashboardAccessState} from "../dashboard-access/dashboard-access.state";

export interface AppState {
  readonly auth: AuthState;
  readonly router: RouterReducerState<any>;
  readonly metaInfoResources: MetaInfoResourceState;
  readonly usersManagement: UsersManagementState;
  readonly portalRolesManagement: PortalRolesManagementState;
  readonly announcements: AnnouncementState;
  readonly myDashboards: MyDashboardsState;
  readonly menu: MenuState;
  readonly startPage: StartPageState,
  readonly faq: FaqState,
  readonly summary: SummaryState,
  readonly externalDashboards: ExternalDashboardsState,
  readonly myLineageDocs: LineageDocsState,
  readonly breadcrumbs: BreadcrumbState,
  readonly unsavedChanges: UnsavedChangesState,
  readonly queries: QueriesState,
  readonly dashboardAccess: DashboardAccessState
}
