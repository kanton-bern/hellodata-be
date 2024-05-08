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

export const naviElements: any = {
  home: {
    path: 'home',
    label: ''
  },
  profile: {
    path: 'profile',
    label: '@Profile'
  },
  publishedAnnouncements: {
    path: 'published-announcements',
    label: '@Announcements'
  },
  lineageDocs: {
    path: 'lineage-docs',
    label: '@Docs'
  },
  lineageDocsList: {
    path: 'list',
    label: '@Docs'
  },
  lineageDocsDetail: {
    path: 'detail/:context/:id/:path',
    label: ''
  },
  orchestration: {
    path: 'orchestration',
    label: '@Orchestration'
  },
  userManagement: {
    path: 'user-management',
    label: '@User management'
  },
  userEdit: {
    path: 'edit/:userId',
  },
  rolesManagement: {
    path: 'roles-management',
    label: '@Portal roles management'
  },
  roleCreate: {
    path: 'create',
    label: '@Create'
  },
  roleEdit: {
    path: 'edit/:roleId',
    label: '@Edit'
  },
  workspaces: {
    path: 'workspaces',
    label: '@Workspaces'
  },
  selectedWorkspace: {
    path: 'selected-workspace/:instanceName/:moduleType/:apiVersion',
    label: '@Detail'
  },
  embeddedDmViewer: {
    path: 'embedded-dm-viewer',
    label: '@Data Marts'
  },
  dataWarehouseViewer: {
    path: 'data-warehouse-viewer',
    label: '@DWH Viewer'
  },
  embeddedOrchestration: {
    path: 'embedded-orchestration',
    label: '@Orchestration'
  },
  embeddedOrchestrationDetails: {
    path: 'details/:id',
    label: '@Detail'
  },
  myDashboards: {
    path: 'my-dashboards',
    label: '@Dashboards'
  },
  myDashboardDetail: {
    path: 'detail/:instanceName/:id',
    label: '@Detail'
  },
  externalDashboards: {
    path: 'external-dashboards',
    label: '@External dashboards'
  },
  externalDashboardEdit: {
    path: 'edit/:externalDashboardId',
    label: '@Edit'
  },
  externalDashboardCreate: {
    path: 'create',
    label: '@Create'
  },
  announcementsManagement: {
    path: 'announcements-management',
    label: '@Announcements'
  },
  announcementEdit: {
    path: 'edit/:announcementId',
    label: '@Edit'
  },
  announcementCreate: {
    path: 'create',
    label: '@Create'
  },
  faqManagement: {
    path: 'faq-management',
    label: '@Faq management'
  },
  faqEdit: {
    path: 'edit/:faqId',
    label: '@Edit'
  },
  faqCreate: {
    path: 'create',
    label: '@Create'
  },
  documentationManagement: {
    path: 'documentation-management',
    label: '@Documentation management'
  },
  dashboardCopy: {
    path: 'dashboard-import-export',
    label: '@Dashboard import-export'
  },
  logout: {
    path: 'logout'
  },
  forbidden: {
    path: 'forbidden'
  },
  callback: {
    path: 'callback'
  },
  redirect: {
    path: 'redirect/:location',
    label: '@Orchestration'
  },
}
