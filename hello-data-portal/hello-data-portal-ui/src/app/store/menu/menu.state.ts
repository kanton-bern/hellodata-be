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
import {naviElements} from "../../app-navi-elements";
import {MenuItem} from "primeng/api";

export interface MenuState {
  navItems: MenuItem[]
}

export const initialMenuState: MenuState = {
  navItems: []
}


export const ALL_MENU_ITEMS: MenuItem[] = [
  {
    id: 'dashboardsMenu',
    label: '@Dashboards',
    icon: 'fas fa-light fa-chart-line',
    requiresAuthentication: true,
    requiredPermissions: ['DASHBOARDS'],
    items: [],
    isFirstLevel: true
  },
  {
    id: 'lineageMenu',
    label: '@Lineage',
    icon: 'fas fa-light fa-diagram-project',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_LINEAGE'],
    items: [],
    isFirstLevel: true
  },
  {
    id: 'dataMartsMenu',
    label: '@Data Marts',
    icon: 'fas fa-light fa-store',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_MARTS'],
    items: [],
    isFirstLevel: true
  },
  {
    id: 'dataEngMenu',
    label: '@Data Eng',
    icon: 'fas fa-light fa-dice-d6',
    requiresAuthentication: true,
    requiredPermissions: ['DATA_DWH', 'DATA_ENG', 'DATA_JUPYTER', 'DATA_FILEBROWSER'],
    items: [
      {
        id: 'dataEngViewerMenu',
        requiredPermissions: ['DATA_DWH'],
        label: '@DWH Viewer',
        url: environment.authConfig.redirectUrl + '?redirectTo=data-warehouse-viewer',
        target: '_blank'
      },
      {
        id: 'dataEngOrchestrationMenu',
        label: '@Orchestration',
        routerLink: naviElements.embeddedOrchestration.path,
        requiredPermissions: ['DATA_ENG']
      }
    ],
    isFirstLevel: true
  },
  {
    id: 'administrationMenu',
    label: '@Administration',
    icon: 'fas fa-light fa-gear',
    //requiredPermissions must have the same set of permissions as each of sub-items
    requiredPermissions: ['USER_MANAGEMENT', 'ROLE_MANAGEMENT', 'ANNOUNCEMENT_MANAGEMENT', 'FAQ_MANAGEMENT', 'DOCUMENTATION_MANAGEMENT', 'USERS_OVERVIEW', 'DASHBOARD_ACCESS', 'DASHBOARD_GROUPS_MANAGEMENT'],
    items: [
      {
        id: 'userManagementMenu',
        label: '@User management',
        routerLink: naviElements.userManagement.path,
        requiredPermissions: ['USER_MANAGEMENT']
      },
      {
        id: 'usersOverviewMenu',
        label: '@Users overview',
        routerLink: naviElements.usersOverview.path,
        requiredPermissions: ['USERS_OVERVIEW']
      },
      {
        id: 'dashboardAccessMenu',
        label: '@Dashboard access',
        routerLink: naviElements.dashboardAccess.path,
        requiredPermissions: ['DASHBOARD_ACCESS']
      },
      {
        id: 'portalRoleManagementMenu',
        label: '@Portal role management',
        routerLink: naviElements.rolesManagement.path,
        requiredPermissions: ['ROLE_MANAGEMENT']
      },
      {
        id: 'announcementsManagementMenu',
        label: '@Announcements',
        routerLink: naviElements.announcementsManagement.path,
        requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT']
      },
      {
        id: 'faqManagementMenu',
        label: '@Faq management',
        routerLink: naviElements.faqManagement.path,
        requiredPermissions: ['FAQ_MANAGEMENT']
      },
      {
        id: 'documentationManagementMenu',
        label: '@Documentation management',
        routerLink: naviElements.documentationManagement.path,
        requiredPermissions: ['DOCUMENTATION_MANAGEMENT']
      },
      {
        id: 'dashboardGroupsMenu',
        label: '@Dashboard groups',
        routerLink: naviElements.dashboardGroups.path,
        requiredPermissions: ['DASHBOARD_GROUPS_MANAGEMENT']
      },
      {
        id: 'dashboardImportExportMenu',
        label: '@Dashboard import-export',
        routerLink: naviElements.dashboardCopy.path,
        requiredPermissions: ['DASHBOARD_IMPORT_EXPORT']
      },
    ],
    isFirstLevel: true
  },
  {
    id: 'monitoringMenu',
    label: '@Monitoring',
    icon: 'fas fa-light fa-list-check',
    requiredPermissions: ['DEVTOOLS', 'WORKSPACES'],
    items: [
      {
        id: 'monitoringWorkspacesMenu',
        label: '@Workspaces',
        routerLink: naviElements.workspaces.path,
        requiredPermissions: ['WORKSPACES']
      },
      {
        id: 'subsystemUsersMenu',
        label: '@Subsystem users',
        routerLink: naviElements.subsystemUsers.path,
        requiredPermissions: ['USER_MANAGEMENT']
      },
    ],
    isFirstLevel: true
  },
  {
    id: 'devToolsMenu',
    label: '@DevTools',
    icon: 'fas fa-light fa-screwdriver-wrench',
    requiredPermissions: ['DEVTOOLS', 'QUERIES'],
    items: [
      {
        id: 'queriesMenu',
        label: '@Queries',
        requiredPermissions: ['QUERIES'],
        items: []
      },
      {
        id: 'devToolsMailboxMenu',
        label: '@Mailbox',
        url: environment.subSystemsConfig.devToolsMailbox.protocol + environment.subSystemsConfig.devToolsMailbox.host + environment.subSystemsConfig.devToolsMailbox.domain,
        target: '_blank'
      },
      {
        id: 'devToolsFileBrowserMenu',
        label: '@FileBrowser',
        url: environment.subSystemsConfig.devToolsFileBrowser.protocol + environment.subSystemsConfig.devToolsFileBrowser.host + environment.subSystemsConfig.devToolsFileBrowser.domain,
        target: '_blank'
      }
    ],
    isFirstLevel: true
  }
]


