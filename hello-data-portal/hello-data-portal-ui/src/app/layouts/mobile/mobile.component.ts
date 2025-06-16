import {Component, NgModule} from "@angular/core";
import {HeaderModule, SummaryModule} from "../../shared/components";
import {CommonModule} from "@angular/common";
import {RouterLink, RouterOutlet} from "@angular/router";
import {ScrollPanelModule} from "primeng/scrollpanel";
import {TranslocoModule} from "@ngneat/transloco";
import {TooltipModule} from "primeng/tooltip";
import {DividerModule} from "primeng/divider";
import {ToastModule} from "primeng/toast";
import {ScrollTopModule} from "primeng/scrolltop";
import {UnsavedChangesModule} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {SideNavOuterToolbarComponent} from "../side-nav-outer-toolbar/side-nav-outer-toolbar.component";
import {SidebarModule} from "primeng/sidebar";
import {MenuModule} from "primeng/menu";
import {combineLatest, Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {TranslateService} from "../../shared/services/translate.service";
import {selectDisableLogout} from "../../store/auth/auth.selector";
import {navigate} from "../../store/app/app.action";
import {MenuItem} from "primeng/api";
import {selectAvailableDataDomains, selectSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.selector";
import {setSelectedDataDomain} from "../../store/my-dashboards/my-dashboards.action";
import {DataDomain} from "../../store/my-dashboards/my-dashboards.model";
import {AnimateModule} from "primeng/animate";
import {Ripple} from "primeng/ripple";

@Component({
  selector: 'mobile',
  templateUrl: './mobile.component.html',
  styleUrls: ['./mobile.component.scss']
})
export class MobileComponent {
  showSidebar = false;
  translationsLoaded$: Observable<any>;
  userMenuItems: MenuItem[] = [];
  dataDomainSelectionItems: any[] = [];
  availableDataDomains$: Observable<DataDomain[]>;
  selectedDataDomain$: Observable<DataDomain | null>;

  constructor(private store: Store<AppState>, private translateService: TranslateService) {
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain);
    this.translationsLoaded$ = combineLatest([
      this.translateService.selectTranslate('@Profile'),
      this.translateService.selectTranslate('@Logout'),
      this.store.select(selectDisableLogout)
    ]).pipe(tap(([profileTranslation, logoutTranslation, disableLogout]) => {
      this.userMenuItems = [
        {
          label: profileTranslation,
          icon: 'fas fa-light fa-user',
          command: () => {
            this.store.dispatch(navigate({url: '/profile'}));
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
    this.availableDataDomains$= this.store.select(selectAvailableDataDomains).pipe(tap(availableDataDomains => {
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

  onDataDomainChanged($event: any) {
    this.store.dispatch(setSelectedDataDomain({dataDomain: $event.item.data}));
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
    TranslocoModule
  ],
  exports: [MobileComponent],
  declarations: [MobileComponent]
})
export class MobileModule {

}
