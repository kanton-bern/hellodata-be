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

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {AutoLoginPartialRoutesGuard} from "angular-auth-oidc-client";
import {PermissionsGuard} from "./auth/guards/permissions-guard.service";
import {naviElements} from "./app-navi-elements";
import {unsavedChangesGuard} from "./shared/guards/unsaved-changes.guard";


const routes: Routes = [

  {path: '', pathMatch: 'full', redirectTo: 'home'},
  {
    path: naviElements.home.path,
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
    canActivate: [AutoLoginPartialRoutesGuard]
  },
  {
    path: naviElements.profile.path,
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AutoLoginPartialRoutesGuard],
  },
  {
    path: naviElements.publishedAnnouncements.path,
    loadComponent: () => import('./pages/published-announcements/published-announcements.component').then(m => m.PublishedAnnouncementsComponent),
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DASHBOARDS'], //minimal permission to see announcements
    },
  },
  {
    path: naviElements.lineageDocs.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DATA_LINEAGE'],
    },
    children: [
      {
        path: naviElements.lineageDocsList.path,
        loadComponent: () => import('./pages/lineage-docs/lineage-docs.component').then(m => m.LineageDocsComponent),
        canActivate: [AutoLoginPartialRoutesGuard],
      },
      {
        path: naviElements.lineageDocsDetail.path,
        loadComponent: () => import('./pages/lineage-docs/embedded/embedded-lineage-docs.component').then(m => m.EmbeddedLineageDocsComponent),
        canActivate: [AutoLoginPartialRoutesGuard],
      },
    ]
  },
  {
    path: naviElements.orchestration.path,
    loadComponent: () => import('./pages/admin/orchestration/orchestration.component').then(m => m.OrchestrationComponent),
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
  },
  {
    path: naviElements.userManagement.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['USER_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/user-management/user-management.component').then(m => m.UserManagementComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['USER_MANAGEMENT'],
        }
      },
      {
        path: naviElements.userEdit.path,
        loadComponent: () => import('./pages/admin/user-management/user-edit/user-edit.component').then(m => m.UserEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['USER_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.subsystemUsers.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['USER_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/subsystem-users/subsystem-users.component').then(m => m.SubsystemUsersComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['USER_MANAGEMENT'],
        }
      }
    ]
  },
  {
    path: naviElements.dashboardAccess.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DASHBOARD_ACCESS'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/dashboard-access/dashboard-access.component').then(m => m.DashboardAccessComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DASHBOARD_ACCESS'],
        }
      }
    ]
  },
  {
    path: naviElements.usersOverview.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['USERS_OVERVIEW'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/users-overview/users-overview.component').then(m => m.UsersOverviewComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['USERS_OVERVIEW'],
        }
      }
    ]
  },
  {
    path: naviElements.rolesManagement.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['ROLE_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/portal-roles-management/portal-roles-management.component').then(m => m.PortalRolesManagementComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['ROLE_MANAGEMENT'],
        }
      },
      {
        path: naviElements.roleCreate.path,
        loadComponent: () => import('./pages/admin/portal-roles-management/portal-role-edit/portal-role-edit.component').then(m => m.PortalRoleEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ROLE_MANAGEMENT'],
        }
      },
      {
        path: naviElements.roleEdit.path,
        loadComponent: () => import('./pages/admin/portal-roles-management/portal-role-edit/portal-role-edit.component').then(m => m.PortalRoleEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ROLE_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.workspaces.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['WORKSPACES'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/workspaces/workspaces.component').then(m => m.WorkspacesComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['WORKSPACES'],
        }
      },
      {
        path: naviElements.selectedWorkspace.path,
        loadComponent: () => import('./pages/admin/workspaces/selected-workspace/selected-workspace.component').then(m => m.SelectedWorkspaceComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['WORKSPACES'],
        }
      },
    ]
  },
  {
    path: naviElements.embeddedDmViewer.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DATA_MARTS'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/data-mart/embedded-dm-viewer.component').then(m => m.EmbeddedDmViewerComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_MARTS'],
        }
      }
    ]
  },
  {
    path: naviElements.dataWarehouseViewer.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DATA_DWH'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/data-warehouse/data-warehouse-viewer.component').then(m => m.DataWarehouseViewerComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_DWH'],
        }
      }
    ]
  },
  {
    path: naviElements.embeddedOrchestration.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DATA_ENG'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/orchestration/embedded-orchestration.component').then(m => m.EmbeddedOrchestrationComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_ENG'],
        }
      },
      {
        path: naviElements.embeddedOrchestrationDetails.path,
        loadComponent: () => import('./pages/orchestration/embedded-orchestration.component').then(m => m.EmbeddedOrchestrationComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_ENG'],
        }
      }
    ]
  },
  {
    path: naviElements.advancedAnalyticsViewer.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DATA_JUPYTER'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/advanced-analytics/advanced-analytics-viewer.component').then(m => m.AdvancedAnalyticsViewerComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_JUPYTER'],
        }
      }
    ]
  },
  {
    path: naviElements.redirect.path,
    loadComponent: () => import('./shared/components/redirect/redirect.component').then(m => m.RedirectComponent)
  },
  {
    path: naviElements.myDashboards.path,
    canActivate: [AutoLoginPartialRoutesGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/my-dashboards/my-dashboards.component').then(m => m.MyDashboardsComponent),
        canActivate: [AutoLoginPartialRoutesGuard],
      },
      {
        path: naviElements.myDashboardDetail.path,
        loadComponent: () => import('./pages/my-dashboards/embed-my-dashboard-wrapper/embed-my-dashboard-wrapper.component').then(m => m.EmbedMyDashboardWrapperComponent),
        canActivate: [AutoLoginPartialRoutesGuard],
      },
    ]
  },
  {
    path: naviElements.query.path,
    canActivate: [AutoLoginPartialRoutesGuard],
    data: {
      requiredPermissions: []
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/queries/queries.component').then(m => m.QueriesComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: []
        }
      },
    ]
  },
  {
    path: naviElements.externalDashboards.path,
    canActivate: [AutoLoginPartialRoutesGuard],
    data: {
      requiredPermissions: []
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/my-dashboards/external-dashboards/external-dashboards.component').then(m => m.ExternalDashboardsComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: []
        }
      },
      {
        path: naviElements.externalDashboardEdit.path,
        loadComponent: () => import('./pages/my-dashboards/external-dashboards/external-dashboard-edit/external-dashboard-edit.component').then(m => m.ExternalDashboardEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['EXTERNAL_DASHBOARDS_MANAGEMENT'],
        }
      },
      {
        path: naviElements.externalDashboardCreate.path,
        loadComponent: () => import('./pages/my-dashboards/external-dashboards/external-dashboard-edit/external-dashboard-edit.component').then(m => m.ExternalDashboardEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['EXTERNAL_DASHBOARDS_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.announcementsManagement.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/announcements-management/announcements-management.component').then(m => m.AnnouncementsManagementComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
        }
      },
      {
        path: naviElements.announcementEdit.path,
        loadComponent: () => import('./pages/admin/announcements-management/announcement-edit/announcement-edit.component').then(m => m.AnnouncementEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
        }
      },
      {
        path: naviElements.announcementCreate.path,
        loadComponent: () => import('./pages/admin/announcements-management/announcement-edit/announcement-edit.component').then(m => m.AnnouncementEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.faqManagement.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['FAQ_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/faq-management/faq-list.component').then(m => m.FaqListComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['FAQ_MANAGEMENT'],
        }
      },
      {
        path: naviElements.faqEdit.path,
        loadComponent: () => import('./pages/admin/faq-management/faq-edit/faq-edit.component').then(m => m.FaqEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['FAQ_MANAGEMENT'],
        }
      },
      {
        path: naviElements.faqCreate.path,
        loadComponent: () => import('./pages/admin/faq-management/faq-edit/faq-edit.component').then(m => m.FaqEditComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['FAQ_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.documentationManagement.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    canDeactivate: [unsavedChangesGuard],
    data: {
      requiredPermissions: ['DOCUMENTATION_MANAGEMENT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/documentation-management/documentation-management.component').then(m => m.DocumentationManagementComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DOCUMENTATION_MANAGEMENT'],
        }
      },
    ]
  },
  {
    path: naviElements.dashboardCopy.path,
    canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
    data: {
      requiredPermissions: ['DASHBOARD_IMPORT_EXPORT'],
    },
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/admin/dashboard-import-export/dashboard-import-export.component').then(m => m.DashboardImportExportComponent),
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DASHBOARD_IMPORT_EXPORT'],
        }
      },
    ]
  },
  {
    path: naviElements.callback.path,
    loadComponent: () => import('./callback/callback.component').then(m => m.CallbackComponent)
  },
  {
    path: naviElements.logout.path,
    loadComponent: () => import('./pages/logout/logout.component').then(m => m.LogoutComponent),
    canActivate: [AutoLoginPartialRoutesGuard],
  },
  {
    path: naviElements.forbidden.path,
    loadComponent: () => import('./shared/components/not-allowed/forbidden.component').then(m => m.ForbiddenComponent),
    canActivate: [AutoLoginPartialRoutesGuard]
  },
  {
    path: naviElements.forbidden.path,
    loadComponent: () => import('./shared/components/not-allowed/forbidden.component').then(m => m.ForbiddenComponent),
    canActivate: [AutoLoginPartialRoutesGuard]
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes),
  ],
  providers: [],
  exports: [RouterModule],
  declarations: []
})
export class AppRoutingModule {
}
