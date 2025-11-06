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

import {ChangeDetectionStrategy, Component, inject, input, output} from '@angular/core';
import {AsyncPipe, NgClass, NgStyle} from '@angular/common';

import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {combineLatest, Observable, tap} from "rxjs";
import {IUser} from "../../../store/auth/auth.model";

import {
  selectCurrentBusinessDomain,
  selectCurrentContextRolesFilterOffNone,
  selectDisableLogout,
  selectIsAuthenticated,
  selectProfile,
  selectSelectedLanguage,
  selectSupportedLanguages
} from "../../../store/auth/auth.selector";
import {Menu} from "primeng/menu";
import {BreadcrumbComponent} from "../breadcrumb/breadcrumb.component";
import {TranslocoPipe} from "@jsverse/transloco";
import {
  selectAvailableDataDomains,
  selectSelectedDataDomain
} from "../../../store/my-dashboards/my-dashboards.selector";
import {DataDomain} from "../../../store/my-dashboards/my-dashboards.model";
import {Ripple} from "primeng/ripple";
import {environment} from "../../../../environments/environment";
import {TranslateService} from "../../services/translate.service";
import {navigate, trackEvent} from "../../../store/app/app.action";
import {setSelectedDataDomain} from "../../../store/my-dashboards/my-dashboards.action";
import {MenuItem} from "primeng/api";
import {setSelectedLanguage} from "../../../store/auth/auth.action";
import {Tooltip} from "primeng/tooltip";
import {
  PublishedAnnouncementsWrapperComponent
} from '../published-announcement/published-announcements-wrapper/published-announcements-wrapper.component';

@Component({
  selector: 'app-header',
  templateUrl: 'header.component.html',
  styleUrls: ['./header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgStyle, Tooltip, Ripple, NgClass, PublishedAnnouncementsWrapperComponent, BreadcrumbComponent, Menu, AsyncPipe, TranslocoPipe]
})
export class HeaderComponent {
  private store = inject<Store<AppState>>(Store);
  private translateService = inject(TranslateService);


  readonly menuToggle = output<boolean>();
  readonly menuToggleEnabled = input(false);
  readonly title = input.required<string>();

  userData$: Observable<IUser>;
  languages$: Observable<any[]>;
  isAuthenticated$: Observable<boolean>;
  businessDomain$: Observable<string>;
  availableDataDomains$: Observable<DataDomain[]>;
  selectedDataDomain$: Observable<DataDomain | null>;
  currentUserContextRolesNotNone$: Observable<any>;

  translationsLoaded$: Observable<any>;

  environment: Environment;
  userMenuItems: MenuItem[] = [];
  dataDomainSelectionItems: any[] = [];
  supportedLanguages: any[] = [];

  selectedLanguage: string | null = null;

  constructor() {
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userData$ = this.store.select(selectProfile);
    this.languages$ = this.getSupportedLanguages();
    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain);
    this.availableDataDomains$ = this.getAvailableDataDomains();
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain);
    this.environment = {
      name: environment.deploymentEnvironment.name,
      showEnvironment: environment.deploymentEnvironment.showEnvironment != undefined ? environment.deploymentEnvironment.showEnvironment : true,
      color: environment.deploymentEnvironment.headerColor ? environment.deploymentEnvironment.headerColor : ''
    };
    this.currentUserContextRolesNotNone$ = this.store.select(selectCurrentContextRolesFilterOffNone)
    this.translationsLoaded$ = combineLatest([
      this.translateService.selectTranslate('@Profile'),
      this.translateService.selectTranslate('@Logout'),
      this.translateService.selectTranslate('@View announcements'),
      this.store.select(selectDisableLogout)
    ]).pipe(tap(([profileTranslation, logoutTranslation, announcementsTranslation, disableLogout]) => {
      this.userMenuItems = [
        {
          label: profileTranslation,
          icon: 'fas fa-light fa-user',
          command: () => {
            this.store.dispatch(navigate({url: '/profile'}));
          }
        },
        {
          label: announcementsTranslation,
          icon: 'fas fa-light fa-bell',
          command: () => {
            this.store.dispatch(navigate({url: '/published-announcements'}));
          }
        },
      ];
      if (!disableLogout) {
        this.userMenuItems.push({
          label: logoutTranslation,
          icon: 'fas fa-light fa-power-off',
          command: () => {
            this.store.dispatch(navigate({url: '/logout'}));
          }
        })
      }
    }));

  }

  private getAvailableDataDomains() {
    return this.store.select(selectAvailableDataDomains).pipe(tap(availableDataDomains => {
      this.dataDomainSelectionItems = [];
      for (const availableDataDomain of availableDataDomains) {
        this.dataDomainSelectionItems.push({
          label: this.translateService.translate(availableDataDomain.name),
          command: (event: any) => {
            this.onDataDomainChanged(event);
          },
          data: availableDataDomain
        })
      }
    }));
  }

  private getSupportedLanguages() {
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
          selected: language.startsWith(this.selectedLanguage as string)
        });
      }
      this.supportedLanguages = languagesLocal;
    }))
  }

  onDataDomainChanged($event: any) {
    this.store.dispatch(setSelectedDataDomain({dataDomain: $event.item.data}));
  }

  onLanguageChange(langCode: any) {
    this.store.dispatch(trackEvent({
      eventCategory: 'Language',
      eventAction: '[Click] - Changed language to ' + langCode
    }));
    this.store.dispatch(setSelectedLanguage({lang: langCode}))
  }
}


export interface Environment {
  name: 'DEV' | 'TEST' | 'PROD';
  showEnvironment: boolean;
  color?: string;
}
