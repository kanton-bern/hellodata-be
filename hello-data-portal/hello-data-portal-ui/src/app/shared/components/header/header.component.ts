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

import {Component, EventEmitter, Input, NgModule, Output} from '@angular/core';
import {CommonModule} from '@angular/common';

import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {combineLatest, Observable, tap} from "rxjs";
import {IUser} from "../../../store/auth/auth.model";
import {PublishedAnnouncementsModule} from "../published-announcement/published-announcements.module";
import {selectCurrentBusinessDomain, selectCurrentContextRolesFilterOffNone, selectDisableLogout, selectIsAuthenticated, selectProfile} from "../../../store/auth/auth.selector";
import {MenubarModule} from "primeng/menubar";
import {MegaMenuModule} from "primeng/megamenu";
import {MenuModule} from "primeng/menu";
import {ButtonModule} from "primeng/button";
import {SidebarModule} from "primeng/sidebar";
import {BreadcrumbComponent} from "../breadcrumb/breadcrumb.component";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {TranslocoModule} from "@ngneat/transloco";
import {DropdownModule} from "primeng/dropdown";
import {FormsModule} from "@angular/forms";
import {ToolbarModule} from "primeng/toolbar";
import {selectAvailableDataDomains, selectSelectedDataDomain} from "../../../store/my-dashboards/my-dashboards.selector";
import {DataDomain} from "../../../store/my-dashboards/my-dashboards.model";
import {RippleModule} from "primeng/ripple";
import {AnimateModule} from "primeng/animate";
import {environment} from "../../../../environments/environment";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {TranslateService} from "../../services/translate.service";
import {navigate} from "../../../store/app/app.action";
import {setSelectedDataDomain} from "../../../store/my-dashboards/my-dashboards.action";

@Component({
  selector: 'app-header',
  templateUrl: 'header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {

  @Output()
  menuToggle = new EventEmitter<boolean>();
  @Input()
  menuToggleEnabled = false;
  @Input()
  title!: string;
  userData$: Observable<IUser>;
  isAuthenticated$: Observable<boolean>;
  businessDomain$: Observable<string>;
  availableDataDomains$: Observable<DataDomain[]>;
  translationsLoaded$: Observable<any>;
  environment: Environment;

  userMenuItems: any[] = [];

  dataDomainSelectionItems: any = [];
  selectedDataDomain$: Observable<DataDomain | null>;
  currentUserContextRolesNotNone$: Observable<any>;

  constructor(private store: Store<AppState>, private translateService: TranslateService) {
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userData$ = this.store.select(selectProfile);
    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain);
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomains).pipe(tap(availableDataDomains => {
      this.dataDomainSelectionItems = [];
      for (const availableDataDomain of availableDataDomains) {
        this.dataDomainSelectionItems.push({
          label: translateService.translate(availableDataDomain.name),
          command: (event: any) => {
            this.onDataDomainChanged(event);
          },
          data: availableDataDomain
        })
      }
    }));
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

  onDataDomainChanged($event: any) {
    this.store.dispatch(setSelectedDataDomain({dataDomain: $event.item.data}));
  }

}

@NgModule({
  imports: [
    CommonModule,
    PublishedAnnouncementsModule,
    MenubarModule,
    MegaMenuModule,
    MenuModule,
    ButtonModule,
    SidebarModule,
    BreadcrumbModule,
    TranslocoModule,
    DropdownModule,
    FormsModule,
    ToolbarModule,
    RippleModule,
    AnimateModule,
    ConfirmDialogModule
  ],
  declarations: [HeaderComponent, BreadcrumbComponent],
  exports: [HeaderComponent]
})
export class HeaderModule {
}


export interface Environment {
  name: 'DEV' | 'TEST' | 'PROD';
  showEnvironment: boolean;
  color?: string;
}
