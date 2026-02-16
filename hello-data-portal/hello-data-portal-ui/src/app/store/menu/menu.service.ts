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

import {inject, Injectable} from "@angular/core";
import {combineLatest, map, Observable, switchMap} from "rxjs";
import {DataDomain, SupersetDashboard} from "../my-dashboards/my-dashboards.model";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {
  selectCurrentContextRoles,
  selectCurrentUserPermissions,
  selectCurrentUserPermissionsLoaded
} from "../auth/auth.selector";
import {
  selectAvailableDataDomainItems,
  selectMyDashboards,
  selectSelectedDataDomain
} from "../my-dashboards/my-dashboards.selector";
import {selectMyLineageDocs} from "../lineage-docs/lineage-docs.selector";
import {LineageDoc} from "../lineage-docs/lineage-docs.model";
import {TranslateService} from "../../shared/services/translate.service";
import {selectAppInfos} from "../metainfo-resource/metainfo-resource.selector";
import {MetaInfoResource} from "../metainfo-resource/metainfo-resource.model";
import {
  BUSINESS_DOMAIN_ADMIN_ROLE,
  DATA_DOMAIN_ADMIN_ROLE,
  DATA_DOMAIN_EDITOR_ROLE,
  HELLODATA_ADMIN_ROLE
} from "../users-management/users-management.model";
import {loadAppInfoResources} from "../metainfo-resource/metainfo-resource.action";
import {OpenedSubsystemsService} from "../../shared/services/opened-subsystems.service";
import {environment} from "../../../environments/environment";
import {ALL_MENU_ITEMS} from "./menu.state";
import {filter} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class MenuService {
  private static readonly MY_DASHBOARDS_DETAIL = '/my-dashboards/detail/';
  private static readonly QUERY_LIST = '/queries/list/';
  private static readonly LINEAGE_DOCS_DETAIL = '/lineage-docs/detail/';
  private readonly _store = inject<Store<AppState>>(Store);
  private readonly _translateService = inject(TranslateService);
  private readonly _openedSubsystemsService = inject(OpenedSubsystemsService);

  public processNavigation(): Observable<any[]> {
    return combineLatest([
      this._store.select(selectCurrentUserPermissionsLoaded),
      this._store.select(selectCurrentUserPermissions),
    ]).pipe(
      filter(([loaded]) => loaded), // wait until permissions loaded = true
      map(([, currentUserPermissions]) => currentUserPermissions),
      switchMap((currentUserPermissions) => {
        return this.internalProcessNavigation(currentUserPermissions);
      }),
    );
  }

  public createQueryLink(contextKey: string): string {
    return MenuService.QUERY_LIST + contextKey;
  }

  public createDashboardLink(db: SupersetDashboard): string {
    const instanceName = db.instanceName;
    if (db.slug) {
      return MenuService.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.slug;
    } else {
      return MenuService.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.id;
    }
  }

  private internalProcessNavigation(currentUserPermissions: string[]): Observable<any[]> {
    this._store.dispatch(loadAppInfoResources());
    return combineLatest([
      this._store.select(selectMyDashboards),
      this._store.select(selectMyLineageDocs),
      this._store.select(selectAppInfos),
      this._store.select(selectCurrentContextRoles),
      this._store.select(selectAvailableDataDomainItems),
      this._store.select(selectSelectedDataDomain)
    ]).pipe(
      map(([myDashboards, myDocs,
             appInfos, contextRoles, availableDomainItems, selectedDataDomain]) => {
        const filteredNavigationElements = this.filterNavigationByPermissions(ALL_MENU_ITEMS, currentUserPermissions);
        return filteredNavigationElements.map((item) => {
          if (item.routerLink && !(item.routerLink.startsWith("/"))) {
            item.routerLink = `/${item.routerLink}`;
          }
          const menuItem = {...item};

          // inject the users dashboards into the menu
          if (menuItem.label === '@Dashboards') {
            menuItem.items = this.createMyDashboardsSubNav(myDashboards, appInfos, contextRoles, selectedDataDomain);
          }
          // inject the users lineage docs into the menu
          if (menuItem.label === '@Lineage') {
            menuItem.items = this.createLineageDocsSubNav(myDocs, availableDomainItems);
          }
          if (menuItem.label === '@Data Marts') {
            menuItem.items = this.createDataMartsSubNav(availableDomainItems);
          }
          if (menuItem.id === 'dataEngMenu') {
            const sftpgo = appInfos.filter(appInfo => appInfo.moduleType === "SFTPGO");
            if (sftpgo && sftpgo.length > 0) {
              menuItem.items.push({
                id: 'filebrowserMenu',
                label: '@Filebrowser',
                url: environment.subSystemsConfig.filebrowser.protocol + environment.subSystemsConfig.filebrowser.host + environment.subSystemsConfig.filebrowser.domain,
                target: '_blank',
                requiredPermissions: ['DATA_FILEBROWSER']
              });
            }
            const jupyterhubSubNavs = this.createJupyterhubSubNav(appInfos, contextRoles, selectedDataDomain);
            for (const jupyterhubSubNav of jupyterhubSubNavs) {
              menuItem.items.push(jupyterhubSubNav);
            }
          }
          if (menuItem.id === 'devToolsMenu') {
            if (this.displayQueries(contextRoles)) {
              const queriesMenu = menuItem.items.find((item: {
                id: string;
              }) => item.id === 'queriesMenu');
              queriesMenu.items = this.createQueriesSubNav(menuItem, availableDomainItems);
            }
          }
          return menuItem;
        });
      })
    )
  }

  private createQueriesSubNav(menuItem: any[], availableDomainItems: any[]) {
    const result: any[] = [];
    const dataDomains = availableDomainItems.map(item => item.data).sort((a, b) => a!.key.toLowerCase().localeCompare(b!.key.toLowerCase()));
    for (const dataDomain of dataDomains) {
      if (dataDomain?.key) {
        result.push({
          id: 'queries_' + dataDomain!.key,
          label: dataDomain!.name,
          routerLink: this.createQueryLink(dataDomain!.key),
          requiredPermissions: ['QUERIES']
        });
      }
    }
    return result;
  }

  private filterNavigationByPermissions(navigationElements: any[], currentUserPermissions: string[]) {
    const filteredNavigationElements: any[] = [];
    navigationElements.forEach((item) => {
      //if the menu item has required permissions, check them
      const itemCopy = {...item};
      if (itemCopy.requiredPermissions) {
        const hasPermissionToView = itemCopy.requiredPermissions.some((requiredPermission: string) =>
          currentUserPermissions.includes(requiredPermission)
        );
        if (hasPermissionToView) {
          filteredNavigationElements.push(itemCopy);
        }
      } else {
        // the menu item doesn't have required permissions - it's public
        filteredNavigationElements.push(itemCopy);
      }
      if (itemCopy.items) {
        itemCopy.items = this.filterNavigationByPermissions(itemCopy.items, currentUserPermissions);
      }
    });
    return filteredNavigationElements;
  }

  private createMyDashboardsSubNav(dashboards: SupersetDashboard[], appInfos: MetaInfoResource[], contextRoles: any[], selectedDataDomain: any) {
    const myDashboards: any[] = [];
    myDashboards.push({id: 'dashboardList', label: '@Dashboard List', routerLink: 'my-dashboards'});

    // Filter dashboards by selectedDataDomain if one is selected
    let filteredDashboards = dashboards;
    if (selectedDataDomain?.key) {
      filteredDashboards = dashboards.filter(db => db.contextKey === selectedDataDomain.key);
    }

    this.groupAndInsertDashboardMenuItems(filteredDashboards, contextRoles, appInfos, myDashboards);
    this.insertSupersetInstanceLinkIfNoDashboards(myDashboards, appInfos, contextRoles, selectedDataDomain);
    myDashboards.push({
      id: 'externalDashboards',
      label: '@External dashboards',
      routerLink: 'external-dashboards',
      requiredPermissions: ['EXTERNAL_DASHBOARDS_MANAGEMENT']
    });
    return myDashboards;
  }

  private groupAndInsertDashboardMenuItems(dashboards: SupersetDashboard[], contextRoles: any[], appInfos: MetaInfoResource[], myDashboards: any[]) {
    const groupedByInstance: Map<string, SupersetDashboard[]> = new Map<string, SupersetDashboard[]>();
    dashboards.forEach(db => {
      const contextName = db.contextName;
      if (!groupedByInstance.has(contextName)) {
        groupedByInstance.set(contextName, []);
      }
      groupedByInstance.get(contextName)?.push(db)
    });
    const sortedByKey = new Map(
      [...groupedByInstance.entries()].sort((a, b) => a[0].localeCompare(b[0]))
    );
    let dashboardEntries: any[] = [];

    for (const [contextName, dashboards] of sortedByKey) {
      dashboardEntries = [];
      if (this.displaySupersetLink(contextName, contextRoles)) {
        this.addLinkToOpenSuperset(dashboardEntries, contextName, appInfos);
      }
      dashboards.forEach((db: SupersetDashboard) => {
        dashboardEntries.push({
          id: 'dashboardMenu' + db.id,
          label: db.dashboardTitle,
          routerLink: this.createDashboardLink(db)
        });
      });
      myDashboards.push({label: contextName, items: dashboardEntries});
    }
  }

  private addLinkToOpenSuperset(dashboardEntries: any[], contextName: string, appInfos: MetaInfoResource[]) {
    dashboardEntries.push({
      id: 'openSupersetInstance_' + contextName,
      label: "@Superset Instanz öffnen",
      url: this.getSupersetInstanceLink(contextName, appInfos),
      target: "_blank",
      requiredPermissions: ['DATA_ENG']
    });
  }

  private insertSupersetInstanceLinkIfNoDashboards(myDashboards: any[], appInfos: MetaInfoResource[], contextRoles: any[], selectedDataDomain: any) {
    const supersets = appInfos.filter(appInfo => appInfo.moduleType === 'SUPERSET');
    supersets.forEach((supersetInstance) => {
      const contextName = supersetInstance.businessContextInfo.subContext.name;
      const contextKey = supersetInstance.businessContextInfo.subContext.key;

      // Filter by selectedDataDomain if one is selected
      if (selectedDataDomain?.key && contextKey !== selectedDataDomain.key) {
        return;
      }

      if (this.displaySupersetLink(contextName, contextRoles)) {
        if (myDashboards.filter(item => item.label === contextName).length === 0) {
          const items: any[] = [];
          this.addLinkToOpenSuperset(items, contextName, appInfos);
          myDashboards.push({label: contextName, items});
        }
      }
    });
  }

  private displayQueries(contextRoles: any[]) {
    return contextRoles.find(contextRole => contextRole.role.name === HELLODATA_ADMIN_ROLE || contextRole.role.name === BUSINESS_DOMAIN_ADMIN_ROLE);
  }

  private displaySupersetLink(instanceName: string, contextRoles: any[]) {
    const contextRole = contextRoles.find(contextRole => contextRole.context.name === instanceName);
    if (contextRole) {
      return contextRole.role.name === DATA_DOMAIN_ADMIN_ROLE || contextRole.role.name === DATA_DOMAIN_EDITOR_ROLE;
    }
    return false;
  }

  private createLineageDocsSubNav(projectDocs: LineageDoc[], availableDataDomains: any[]) {
    const subMenuEntry: any[] = [];
    subMenuEntry.push({
      id: 'lineageDocsList',
      label: '@Lineage Docs List',
      routerLink: 'lineage-docs/list',
      requiredPermissions: ['DATA_LINEAGE']
    })
    const docsGroupedByContext = this.getLineageDocsGroupedByDataDomainContext(projectDocs, availableDataDomains);
    const sortedByKey = new Map(
      [...docsGroupedByContext.entries()].sort((a, b) => a[0].localeCompare(b[0]))
    );
    for (const [dataDomainContextName, lineageDocsInDomain] of sortedByKey) {
      const lineageDocsMenuEntries = this.getLineageDocsSubMenuItemsForDataDomain(lineageDocsInDomain);
      subMenuEntry.push({id: 'lineageDocsEntries', label: dataDomainContextName, items: lineageDocsMenuEntries});
    }
    return subMenuEntry;
  }

  private getLineageDocsGroupedByDataDomainContext(projectDocs: LineageDoc[], availableDataDomains: DataDomain[]) {
    const docsSortedByDomainKey = [...projectDocs]
    docsSortedByDomainKey.sort((a, b) => a.contextKey.toLowerCase().localeCompare(b.contextKey.toLowerCase()));
    const docsGroupedByContext: Map<string, LineageDoc[]> = new Map<string, LineageDoc[]>();
    docsSortedByDomainKey.forEach(pd => {
      const contextName = this.getContextName(pd, availableDataDomains);
      if (!docsGroupedByContext.has(contextName)) {
        docsGroupedByContext.set(contextName, []);
      }
      docsGroupedByContext.get(contextName)?.push(pd);
    });
    return docsGroupedByContext;
  }

  private getContextName(pd: LineageDoc, availableDataDomains: any[]) {
    const dataDomain = availableDataDomains.find(dataDomain => {
      if (dataDomain.data?.key) {
        return dataDomain.data.key === pd.contextKey;
      }
      return false;
    });
    return dataDomain ? dataDomain.data.name : pd.contextKey;
  }

  private getLineageDocsSubMenuItemsForDataDomain(lineageDoc: LineageDoc[]) {
    const sortedByNameLineageDocs = [...lineageDoc];
    sortedByNameLineageDocs.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
    return sortedByNameLineageDocs.map((lineageDoc: LineageDoc) => ({
      label: `${this._translateService.translate('@Doc')}`,
      routerLink: this.createLineageDocsLink(lineageDoc)
    }));
  }

  private createLineageDocsLink(lineageDoc: LineageDoc): string {
    const urlEncodedProjectPath = encodeURIComponent(lineageDoc.path);
    return `${MenuService.LINEAGE_DOCS_DETAIL}${lineageDoc.contextKey}/${lineageDoc.name}/${urlEncodedProjectPath}`;
  }

  private getSupersetInstanceLink(instanceName: string, appInfos: MetaInfoResource[]) {
    const metaInfoResource = appInfos.filter(appInfo => appInfo.moduleType === 'SUPERSET')
      .find(appInfo => appInfo.businessContextInfo.subContext?.name === instanceName);
    if (metaInfoResource) {
      this._openedSubsystemsService.rememberOpenedSubsystem(metaInfoResource.data.url + 'logout');
      const supersetUrl = metaInfoResource.data.url;
      const supersetLogoutUrl = supersetUrl + 'logout';
      const supersetLoginUrl = supersetUrl + `login/keycloak?next=${supersetUrl}`;
      return supersetLogoutUrl + `?redirect=${supersetLoginUrl}`;
    }
    return "#";
  }

  private createDataMartsSubNav(availableDataDomains: any[]) {
    const subMenuEntry: any[] = [];
    if (availableDataDomains.length > 0) {
      subMenuEntry.push({
        id: 'dataMartsDetails',
        label: '@DM Viewer Link',
        routerLink: '/embedded-dm-viewer',
        requiredPermissions: ['DATA_MARTS']
      })
    }
    return subMenuEntry;
  }

  private createJupyterhubSubNav(appInfos: MetaInfoResource[], contextRoles: any[], selectedDataDomain: any) {
    const jupyterhubs = appInfos.filter(appInfo => appInfo.moduleType === "JUPYTERHUB");
    const subMenuEntry: any[] = [];
    let filteredContexts = contextRoles.filter(contextRole => contextRole.context.type === 'DATA_DOMAIN' && contextRole.role.name === 'DATA_DOMAIN_ADMIN').map(contextRole => contextRole.context);
    if (selectedDataDomain?.id !== '') {
      filteredContexts = filteredContexts.filter(context => context.contextKey === selectedDataDomain?.key);
    }
    for (const filteredContext of filteredContexts) {
      if (jupyterhubs.some(jupyterhub => jupyterhub.businessContextInfo?.subContext?.key === filteredContext.contextKey)) {
        subMenuEntry.push({
          id: 'jupyterhub' + filteredContext.contextKey,
          label: 'Advanced Analytics ' + filteredContext.name,
          url: environment.authConfig.redirectUrl + '?redirectTo=advanced-analytics-viewer/' + filteredContext.contextKey,
          requiredPermissions: ['DATA_JUPYTER'],
          target: '_blank'
        });
      }
    }
    return subMenuEntry;

  }
}
