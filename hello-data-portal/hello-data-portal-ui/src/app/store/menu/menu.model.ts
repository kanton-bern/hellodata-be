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

import {environment} from "../../../environments/environment";

export const ALL_MENU_ITEMS: any[] = [
  {
    id: 'dashboardsMenu',
    text: '@Dashboards',
    icon: 'fas fa-light fa-chart-line',
    requiresAuthentication: true,
    requiredPermissions: ['DASHBOARDS'],
    items: []
  },
  {
    id: 'lineageMenu',
    text: '@Lineage',
    icon: 'fas fa-light fa-diagram-project',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_LINEAGE'],
    items: []
  },
  {
    id: 'dataMartsMenu',
    text: '@Data Marts',
    icon: 'fas fa-light fa-store',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_MARTS'],
    items: []
  },
  {
    id: 'dataEngMenu',
    text: '@Data Eng',
    icon: 'fas fa-light fa-dice-d6',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_DWH', 'DATA_ENG', 'DATA_JUPYTER'],
    items: [
      {
        id: 'dataEngViewerMenu',
        requiredPermissions: ['DATA_DWH'],
        text: '@DWH Viewer',
        url: environment.authConfig.redirectUrl + '?redirectTo=data-warehouse-viewer',
        target: '_blank'
      },
      {
        id: 'dataEngOrchestrationMenu',
        text: '@Orchestration',
        routerLink: '/embedded-orchestration',
        requiredPermissions: ['DATA_ENG']
      },
    ]
  },
  {
    id: 'administrationMenu',
    text: '@Administration',
    icon: 'fas fa-light fa-gear',
    //requiredPermissions must have the same set of permissions as each of sub-items
    requiredPermissions: ['USER_MANAGEMENT', 'ROLE_MANAGEMENT', 'ANNOUNCEMENT_MANAGEMENT', 'FAQ_MANAGEMENT', 'DOCUMENTATION_MANAGEMENT', 'USERS_OVERVIEW'],
    items: [
      {
        id: 'userManagementMenu',
        text: '@User management',
        routerLink: '/user-management',
        requiredPermissions: ['USER_MANAGEMENT']
      },
      {
        id: 'usersOverviewMenu',
        text: '@Users overview',
        routerLink: '/users-overview',
        requiredPermissions: ['USERS_OVERVIEW']
      },
      {
        id: 'portalRoleManagementMenu',
        text: '@Portal role management',
        routerLink: '/roles-management',
        requiredPermissions: ['ROLE_MANAGEMENT']
      },
      {
        id: 'announcementsManagementMenu',
        text: '@Announcements',
        routerLink: '/announcements-management',
        requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT']
      },
      {
        id: 'faqManagementMenu',
        text: '@Faq management',
        routerLink: '/faq-management',
        requiredPermissions: ['FAQ_MANAGEMENT']
      },
      {
        id: 'documentationManagementMenu',
        text: '@Documentation management',
        routerLink: '/documentation-management',
        requiredPermissions: ['DOCUMENTATION_MANAGEMENT']
      },
      {
        id: 'dashboardImportExportMenu',
        text: '@Dashboard import-export',
        routerLink: '/dashboard-import-export',
        requiredPermissions: ['DASHBOARD_IMPORT_EXPORT']
      }
    ]
  },
  {
    id: 'monitoringMenu',
    text: '@Monitoring',
    icon: 'fas fa-light fa-list-check',
    requiredPermissions: ['DEVTOOLS', 'WORKSPACES'],
    items: [
      {
        id: 'monitoringStatusMenu',
        text: '@Status',
        url: environment.subSystemsConfig.monitoringStatus.protocol + environment.subSystemsConfig.monitoringStatus.host + environment.subSystemsConfig.monitoringStatus.domain,
        target: '_blank'
      },
      {
        id: 'monitoringWorkspacesMenu',
        text: '@Workspaces',
        routerLink: '/workspaces',
        requiredPermissions: ['WORKSPACES']
      },
      {
        id: 'subsystemUsersMenu',
        text: '@Subsystem users',
        routerLink: '/subsystem-users',
        requiredPermissions: ['USER_MANAGEMENT']
      },
    ]
  },
  {
    id: 'devToolsMenu',
    text: '@DevTools',
    icon: 'fas fa-light fa-screwdriver-wrench',
    requiredPermissions: ['DEVTOOLS'],
    items: [
      {
        id: 'devToolsMailboxMenu',
        text: '@Mailbox',
        url: environment.subSystemsConfig.devToolsMailbox.protocol + environment.subSystemsConfig.devToolsMailbox.host + environment.subSystemsConfig.devToolsMailbox.domain,
        target: '_blank'
      },
      {
        id: 'devToolsFileBrowserMenu',
        text: '@FileBrowser',
        url: environment.subSystemsConfig.devToolsFileBrowser.protocol + environment.subSystemsConfig.devToolsFileBrowser.host + environment.subSystemsConfig.devToolsFileBrowser.domain,
        target: '_blank'
      }
    ]
  }
]
