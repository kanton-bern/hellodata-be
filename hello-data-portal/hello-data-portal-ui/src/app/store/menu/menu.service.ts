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

import {Injectable} from "@angular/core";
import {combineLatest, map, Observable, switchMap} from "rxjs";
import {DataDomain, SupersetDashboard} from "../my-dashboards/my-dashboards.model";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectCurrentContextRoles, selectCurrentUserPermissions, selectCurrentUserPermissionsLoaded} from "../auth/auth.selector";
import {filter, take} from "rxjs/operators";
import {selectAvailableDataDomainItems, selectAvailableDataDomains, selectMyDashboards} from "../my-dashboards/my-dashboards.selector";
import {selectMyLineageDocs} from "../lineage-docs/lineage-docs.selector";
import {LineageDoc} from "../lineage-docs/lineage-docs.model";
import {TranslateService} from "../../shared/services/translate.service";
import {ALL_MENU_ITEMS} from "./menu.model";
import {LoadAppInfoResources} from "../metainfo-resource/metainfo-resource.action";
import {selectAppInfos} from "../metainfo-resource/metainfo-resource.selector";
import {MetaInfoResource} from "../metainfo-resource/metainfo-resource.model";
import {DATA_DOMAIN_ADMIN_ROLE, DATA_DOMAIN_EDITOR_ROLE} from "../users-management/users-management.model";

@Injectable({
  providedIn: 'root'
})
export class MenuService {
  private static readonly MY_DASHBOARDS_DETAIL = '/my-dashboards/detail/';
  private static readonly LINEAGE_DOCS_DETAIL = '/lineage-docs/detail/';
  dbMenuItemPrefix = ' > ';

  constructor(
    private _store: Store<AppState>,
    private _translateService: TranslateService
  ) {
  }

  public processNavigation(compactMode: boolean): Observable<any[]> {
    return this._store.select(selectCurrentUserPermissions).pipe(
      switchMap((currentUserPermissions) => {
        if (!currentUserPermissions || currentUserPermissions.length === 0) {
          // Permissions not yet loaded, return observable that will wait for them to be loaded
          return this._store.select(selectCurrentUserPermissionsLoaded).pipe(
            filter((loaded) => loaded),
            take(1),
            switchMap(() => {
              return this.permissionsLoadedProcessNavigation(compactMode);
            })
          );
        } else {
          // Permissions already loaded, check them immediately
          return this.internalProcessNavigation(compactMode, currentUserPermissions);
        }
      })
    );
  }

  public createDashboardLink(db: SupersetDashboard): string {
    const instanceName = db.instanceName;
    if (db.slug) {
      return MenuService.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.slug;
    } else {
      return MenuService.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.id;
    }
  }

  private internalProcessNavigation(compactMode: boolean, currentUserPermissions: string[]): Observable<any[]> {
    this._store.dispatch(new LoadAppInfoResources());
    return combineLatest([
      this._store.select(selectMyDashboards),
      this._store.select(selectMyLineageDocs),
      this._store.select(selectAvailableDataDomains),
      this._store.select(selectAppInfos),
      this._store.select(selectCurrentContextRoles),
      this._store.select(selectAvailableDataDomainItems)
    ]).pipe(
      map(([myDashboards, myDocs, availableDataDomains,
             appInfos, contextRoles, availableDomainItems]) => {
        const filteredNavigationElements = this.filterNavigationByPermissions(ALL_MENU_ITEMS, currentUserPermissions);
        return filteredNavigationElements.map((item) => {
          if (item.routerLink && !(/^\//.test(item.routerLink))) {
            item.routerLink = `/${item.routerLink}`;
          }
          const menuItem = {...item, expanded: !compactMode};

          // inject the users dashboards into the menu
          if (menuItem.text === '@Dashboards') {
            menuItem.items = this.createMyDashboardsSubNav(myDashboards, appInfos, contextRoles);
          }
          // inject the users lineage docs into the menu
          if (menuItem.text === '@Lineage') {
            menuItem.items = this.createLineageDocsSubNav(myDocs, availableDataDomains);
          }
          if (menuItem.text === '@Data Marts') {
            menuItem.items = this.createDataMartsSubNav(availableDomainItems);
          }
          return menuItem;
        });
      })
    )
  }

  private filterNavigationByPermissions(navigationElements: any[], currentUserPermissions: string[]) {
    const filteredNavigationElements: any[] = [];
    navigationElements.forEach((item) => {
      //if the menu item has required permissions, check them
      if (item.requiredPermissions) {
        const hasPermissionToView = item.requiredPermissions.some((requiredPermission: string) =>
          currentUserPermissions.includes(requiredPermission)
        );
        if (hasPermissionToView) {
          filteredNavigationElements.push(item);
        }
      } else {
        // the menu item doesn't have required permissions - it's public
        filteredNavigationElements.push(item);
      }
      if (item.items) {
        item.items = this.filterNavigationByPermissions(item.items, currentUserPermissions);
      }
    });
    return filteredNavigationElements;
  }

  private permissionsLoadedProcessNavigation(compactMode: boolean): Observable<any[]> {
    return this._store.select(selectCurrentUserPermissions).pipe(
      switchMap((permissions) => {
        return this.internalProcessNavigation(compactMode, permissions);
      })
    )
  }

  private createMyDashboardsSubNav(dashboards: SupersetDashboard[], appInfos: MetaInfoResource[], contextRoles: any[]) {
    const myDashboards: any[] = [];
    myDashboards.push({id: 'dashboarList', text: '@List', routerLink: 'my-dashboards'})

    const groupedByInstance: Map<string, SupersetDashboard[]> = new Map<string, SupersetDashboard[]>();
    dashboards.forEach(db => {
      const contextName = db.contextName;
      if (!groupedByInstance.has(contextName)) {
        groupedByInstance.set(contextName, []);
      }
      groupedByInstance.get(contextName)?.push(db)
    });
    let dashboardEntries: any[] = [];

    for (const [instanceName, dashboards] of groupedByInstance) {
      dashboardEntries = [];
      if (this.displaySupersetLink(instanceName, contextRoles)) {
        dashboardEntries.push({
          id: 'openSupersetInstance_' + instanceName,
          text: "@Superset Instanz öffnen",
          url: this.getSupersetInstanceLink(instanceName, appInfos),
          target: "_blank",
          requiredPermissions: ['DATA_ENG']
        });
      }
      dashboards.forEach((db: SupersetDashboard) => {
        dashboardEntries.push({
          id: 'dashboardMenu' + db.id,
          text: this.dbMenuItemPrefix + db.dashboardTitle,
          routerLink: this.createDashboardLink(db)
        });
      });
      myDashboards.push({text: instanceName, items: dashboardEntries});
    }
    myDashboards.push({
      id: 'externalDashboards',
      text: '@External dashboards',
      routerLink: 'external-dashboards',
      requiredPermissions: ['EXTERNAL_DASHBOARDS_MANAGEMENT']
    })
    return myDashboards;
  }

  private displaySupersetLink(instanceName: string, contextRoles: any[]) {
    const contextRole = contextRoles.find(contextRole => contextRole.context.name === instanceName);
    if (contextRole) {
      return contextRole.role.name === DATA_DOMAIN_ADMIN_ROLE || contextRole.role.name === DATA_DOMAIN_EDITOR_ROLE;
    }
    return false;
  }

  private createLineageDocsSubNav(projectDocs: LineageDoc[], availableDataDomains: DataDomain[]) {
    const subMenuEntry: any[] = [];
    subMenuEntry.push({id: 'lineageDocsList', text: '@List', routerLink: 'lineage-docs/list', requiredPermissions: ['DATA_LINEAGE']})
    const docsGroupedByContext = this.getLineageDocsGroupedByDataDomainContext(projectDocs, availableDataDomains);
    for (const [dataDomainContextKey, lineageDocsInDomain] of docsGroupedByContext) {
      const lineageDocsMenuEntries = this.getLineageDocsSubMenuItemsForDataDomain(lineageDocsInDomain);
      subMenuEntry.push({id: 'lineageDocsEntries', text: dataDomainContextKey, items: lineageDocsMenuEntries});
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

  private getContextName(pd: LineageDoc, availableDataDomains: DataDomain[]) {
    const dataDomain = availableDataDomains.find(dataDomain => dataDomain.key === pd.contextKey);
    return dataDomain ? dataDomain.name : pd.contextKey;
  }

  private getLineageDocsSubMenuItemsForDataDomain(lineageDoc: LineageDoc[]) {
    const sortedByNameLineageDocs = [...lineageDoc];
    sortedByNameLineageDocs.sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
    return sortedByNameLineageDocs.map((lineageDoc: LineageDoc) => ({
      text: `${this.dbMenuItemPrefix}${lineageDoc.name} ${this._translateService.translate('@Data Lineage')}`,
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
    return metaInfoResource ? metaInfoResource.data.url : "#";
  }

  private createDataMartsSubNav(availableDataDomains: any[]) {
    const subMenuEntry: any[] = [];
    //subMenuEntry.push({text: '@List', routerLink: '/datamart-list', requiredPermissions: ['DATA_MARTS']})
    const sortedAvailableDataDomains = [...availableDataDomains].sort((a, b) => a.label.localeCompare(b.label));
    sortedAvailableDataDomains.forEach((item, index) => {
      const myDashboards: any[] = [];
      myDashboards.push({
        id: 'dataMartsDetails' + index,
        text: '@DM Viewer Link',
        routerLink: '/embedded-dm-viewer',
        requiredPermissions: ['DATA_MARTS']
      })
      subMenuEntry.push({id: 'dm' + index, text: item.label, items: myDashboards, requiredPermissions: ['DATA_MARTS']});
    });
    return subMenuEntry;
  }
}
