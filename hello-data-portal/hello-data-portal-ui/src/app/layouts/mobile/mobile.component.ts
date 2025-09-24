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

import {Component, NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {RouterOutlet} from "@angular/router";
import {TranslocoModule} from "@ngneat/transloco";
import {ToastModule} from "primeng/toast";
import {ScrollTopModule} from "primeng/scrolltop";
import {UnsavedChangesModule} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {SidebarModule} from "primeng/sidebar";
import {MenuModule} from "primeng/menu";
import {combineLatest, Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {TranslateService} from "../../shared/services/translate.service";
import {navigate, trackEvent} from "../../store/app/app.action";
import {
  selectAvailableDataDomains,
  selectMyDashboards,
  selectSelectedDataDomain
} from "../../store/my-dashboards/my-dashboards.selector";
import {setSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.action";
import {DataDomain, SupersetDashboard} from "../../store/my-dashboards/my-dashboards.model";
import {AnimateModule} from "primeng/animate";
import {Ripple} from "primeng/ripple";
import {map} from "rxjs/operators";
import {selectSelectedLanguage, selectSupportedLanguages} from "../../store/auth/auth.selector";
import {setSelectedLanguage} from "../../store/auth/auth.action";
import {FooterModule} from "../../shared/components";
import {AppInfoService} from "../../shared/services";
import {environment} from "../../../environments/environment";
import {MatomoTrackerDirective} from "ngx-matomo-client";

@Component({
  selector: 'app-mobile',
  templateUrl: './mobile.component.html',
  styleUrls: ['./mobile.component.scss']
})
export class MobileComponent {
  private static readonly MY_DASHBOARDS_DETAIL = '/my-dashboards/detail/';
  showDashboardMenu = false;
  showUserMenu = false;
  showDataDomainMenu = false;
  dataDomainSelectionItems: any[] = [];
  supportedLanguages: any[] = [];
  selectedLanguage: string | null = null;

  openSourceDataPlatformUrl = environment.footerConfig.openSourceDataPlatformUrl;
  licenseUrl = environment.footerConfig.licenseUrl;
  githubUrl = environment.footerConfig.githubUrl;
  versionLink = environment.footerConfig.versionLink;

  availableDataDomains$: Observable<DataDomain[]>;
  selectedDataDomain$: Observable<DataDomain | null>;
  groupedDashboards$: Observable<Map<string, any[]>>;
  languages$: Observable<any[]>;

  constructor(private store: Store<AppState>,
              public appInfo: AppInfoService,
              public translateService: TranslateService) {
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain);
    this.groupedDashboards$ = this.store.select(selectMyDashboards).pipe(
      map((dashboards: any[]) => {
        const grouped = new Map<string, any[]>();

        dashboards.forEach(dashboard => {
          const context = dashboard.contextName;
          if (!grouped.has(context)) {
            grouped.set(context, []);
          }
          grouped.get(context)!.push(dashboard);
        });

        return grouped;
      })
    );
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomains).pipe(tap(availableDataDomains => {
      this.dataDomainSelectionItems = [];
      for (const availableDataDomain of availableDataDomains) {
        this.dataDomainSelectionItems.push({
          label: availableDataDomain.name,
          data: availableDataDomain
        })
      }
    }));
    this.languages$ = this.getSupportedLanguages();
  }

  onDataDomainClicked(item: any) {
    this.store.dispatch(setSelectedDataDomain({dataDomain: item.data}));
    this.showDataDomainMenu = false;
  }

  navigateToProfile() {
    this.showUserMenu = false;
    this.store.dispatch(navigate({url: 'profile'}));
  }

  logout() {
    this.showUserMenu = false;
    this.store.dispatch(navigate({url: 'logout'}));
  }

  openDashboard(dash: any) {
    this.store.dispatch(trackEvent({
      eventCategory: 'Mobile Dashboard',
      eventAction: `[Click] - ${dash.dashboardTitle} [${dash.contextName}]`
    }));
    const link = this.createDashboardLink(dash);
    this.store.dispatch(navigate({url: link}));
    this.showDashboardMenu = false;
  }

  createDashboardLink(db: SupersetDashboard): string {
    const instanceName = db.instanceName;
    if (db.slug) {
      return MobileComponent.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.slug;
    } else {
      return MobileComponent.MY_DASHBOARDS_DETAIL + instanceName + '/' + db.id;
    }
  }

  getSupportedLanguages() {
    return combineLatest([
      this.store.select(selectSelectedLanguage),
      this.store.select(selectSupportedLanguages)
    ]).pipe(tap(([selectedLanguage, supportedLanguages]) => {
      this.selectedLanguage = selectedLanguage.code;
      const languagesLocal: any = [];
      for (const language of supportedLanguages) {
        languagesLocal.push({
          code: language,
          label: language.slice(0, 2)?.toUpperCase(),
          selected: this.selectedLanguage === language
        });
      }
      this.supportedLanguages = languagesLocal;
    }))
  }

  onLanguageChange(langCode: any) {
    this.store.dispatch(setSelectedLanguage({lang: langCode}));
    this.store.dispatch(trackEvent({
      eventCategory: 'Language Mobile',
      eventAction: '[Click] - Changed language to ' + langCode
    }));
  }

  navigateToHome() {
    this.showUserMenu = false;
    this.store.dispatch(navigate({url: 'home'}));
  }

}

@NgModule({
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarModule,
    ToastModule,
    UnsavedChangesModule,
    ScrollTopModule,
    MenuModule,
    AnimateModule,
    Ripple,
    TranslocoModule,
    FooterModule,
    MatomoTrackerDirective
  ],
  exports: [MobileComponent],
  declarations: [MobileComponent]
})
export class MobileModule {

}
