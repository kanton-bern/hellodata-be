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
import {HomeComponent} from './pages/home/home.component';
import {ProfileComponent} from './pages/profile/profile.component';
import {WorkspacesComponent} from "./pages/admin/workspaces/workspaces.component";
import {UserManagementComponent} from "./pages/admin/user-management/user-management.component";
import {MyDashboardsComponent} from "./pages/my-dashboards/my-dashboards.component";
import {SelectedWorkspaceComponent} from "./pages/admin/workspaces/selected-workspace/selected-workspace.component";
import {UserEditComponent} from "./pages/admin/user-management/user-edit/user-edit.component";
import {PortalRolesManagementComponent} from "./pages/admin/portal-roles-management/portal-roles-management.component";
import {PortalRoleEditComponent} from "./pages/admin/portal-roles-management/portal-role-edit/portal-role-edit.component";
import {AutoLoginPartialRoutesGuard} from "angular-auth-oidc-client";
import {AnnouncementsManagementComponent} from "./pages/admin/announcements-management/announcements-management.component";
import {AnnouncementEditComponent} from "./pages/admin/announcements-management/announcement-edit/announcement-edit.component";
import {CallbackComponent} from "./callback/callback.component";
import {PermissionsGuard} from "./auth/guards/permissions-guard.service";
import {ForbiddenComponent} from "./shared/components/not-allowed/forbidden.component";
import {EmbedMyDashboardComponent} from "./pages/my-dashboards/embed-my-dashboard.component";
import {FaqListComponent} from "./pages/admin/faq-management/faq-list.component";
import {FaqEditComponent} from "./pages/admin/faq-management/faq-edit/faq-edit.component";
import {LogoutComponent} from "./pages/logout/logout.component";
import {DocumentationManagementComponent} from "./pages/admin/documentation-management/documentation-management.component";
import {ExternalDashboardsComponent} from "./pages/my-dashboards/external-dashboards/external-dashboards.component";
import {ExternalDashboardEditComponent} from "./pages/my-dashboards/external-dashboards/external-dashboard-edit/external-dashboard-edit.component";
import {OrchestrationComponent} from "./pages/admin/orchestration/orchestration.component";
import {EmbeddedOrchestrationComponent} from "./pages/orchestration/embedded-orchestration.component";
import {EmbeddedDmViewerComponent} from "./pages/data-mart/embedded-dm-viewer.component";
import {EmbeddedLineageDocsComponent} from "./pages/lineage-docs/embedded/embedded-lineage-docs.component";
import {LineageDocsComponent} from "./pages/lineage-docs/lineage-docs.component";
import {naviElements} from "./app-navi-elements";
import {unsavedChangesGuard} from "./shared/guards/unsaved-changes.guard";
import {RedirectComponent} from "./shared/components/redirect/redirect.component";
import {DataWarehouseViewerComponent} from "./pages/data-warehouse/data-warehouse-viewer.component";
import {DashboardImportExportComponent} from "./pages/admin/dashboard-import-export/dashboard-import-export.component";

const routes: Routes = [

  {path: '', pathMatch: 'full', redirectTo: 'home'},
  {
    path: naviElements.home.path,
    component: HomeComponent,
    canActivate: [AutoLoginPartialRoutesGuard]
  },
  {
    path: naviElements.profile.path,
    component: ProfileComponent,
    canActivate: [AutoLoginPartialRoutesGuard],
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
        component: LineageDocsComponent,
        canActivate: [AutoLoginPartialRoutesGuard],
      },
      {
        path: naviElements.lineageDocsDetail.path,
        component: EmbeddedLineageDocsComponent,
        canActivate: [AutoLoginPartialRoutesGuard],
      },
    ]
  },
  {
    path: naviElements.orchestration.path,
    component: OrchestrationComponent,
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
        component: UserManagementComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['USER_MANAGEMENT'],
        }
      },
      {
        path: naviElements.userEdit.path,
        component: UserEditComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['USER_MANAGEMENT'],
        }
      },
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
        component: PortalRolesManagementComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['ROLE_MANAGEMENT'],
        }
      },
      {
        path: naviElements.roleCreate.path,
        component: PortalRoleEditComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ROLE_MANAGEMENT'],
        }
      },
      {
        path: naviElements.roleEdit.path,
        component: PortalRoleEditComponent,
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
        component: WorkspacesComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['WORKSPACES'],
        }
      },
      {
        path: naviElements.selectedWorkspace.path,
        component: SelectedWorkspaceComponent,
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
        component: EmbeddedDmViewerComponent,
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
        component: DataWarehouseViewerComponent,
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
        component: EmbeddedOrchestrationComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_ENG'],
        }
      },
      {
        path: naviElements.embeddedOrchestrationDetails.path,
        component: EmbeddedOrchestrationComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DATA_ENG'],
        }
      }
    ]
  },
  {path: naviElements.redirect.path, component: RedirectComponent},
  {
    path: naviElements.myDashboards.path,
    canActivate: [AutoLoginPartialRoutesGuard],
    children: [
      {
        path: '',
        component: MyDashboardsComponent,
        canActivate: [AutoLoginPartialRoutesGuard],
      },
      {
        path: naviElements.myDashboardDetail.path,
        component: EmbedMyDashboardComponent,
        canActivate: [AutoLoginPartialRoutesGuard],
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
        component: ExternalDashboardsComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: []
        }
      },
      {
        path: naviElements.externalDashboardEdit.path,
        component: ExternalDashboardEditComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['EXTERNAL_DASHBOARDS_MANAGEMENT'],
        }
      },
      {
        path: naviElements.externalDashboardCreate.path,
        component: ExternalDashboardEditComponent,
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
        component: AnnouncementsManagementComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
        }
      },
      {
        path: naviElements.announcementEdit.path,
        component: AnnouncementEditComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['ANNOUNCEMENT_MANAGEMENT'],
        }
      },
      {
        path: naviElements.announcementCreate.path,
        component: AnnouncementEditComponent,
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
        component: FaqListComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['FAQ_MANAGEMENT'],
        }
      },
      {
        path: naviElements.faqEdit.path,
        component: FaqEditComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        canDeactivate: [unsavedChangesGuard],
        data: {
          requiredPermissions: ['FAQ_MANAGEMENT'],
        }
      },
      {
        path: naviElements.faqCreate.path,
        component: FaqEditComponent,
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
        component: DocumentationManagementComponent,
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
        component: DashboardImportExportComponent,
        canActivate: [AutoLoginPartialRoutesGuard, PermissionsGuard],
        data: {
          requiredPermissions: ['DASHBOARD_IMPORT_EXPORT'],
        }
      },
    ]
  },
  {path: naviElements.callback.path, component: CallbackComponent},
  {
    path: naviElements.logout.path,
    component: LogoutComponent,
    canActivate: [AutoLoginPartialRoutesGuard],
  },
  {path: naviElements.forbidden.path, component: ForbiddenComponent, canActivate: [AutoLoginPartialRoutesGuard]},
  {path: naviElements.forbidden.path, component: ForbiddenComponent, canActivate: [AutoLoginPartialRoutesGuard]},
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
