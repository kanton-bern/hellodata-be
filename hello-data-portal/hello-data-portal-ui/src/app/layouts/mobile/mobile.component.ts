import {Component, NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {RouterOutlet} from "@angular/router";
import {TranslocoModule} from "@ngneat/transloco";
import {ToastModule} from "primeng/toast";
import {ScrollTopModule} from "primeng/scrolltop";
import {UnsavedChangesModule} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {SidebarModule} from "primeng/sidebar";
import {MenuModule} from "primeng/menu";
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {TranslateService} from "../../shared/services/translate.service";
import {navigate} from "../../store/app/app.action";
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
  showDashboardMenu = false;
  showUserMenu = false;
  showDataDomainMenu = false;
  dataDomainSelectionItems: any[] = [];
  availableDataDomains$: Observable<DataDomain[]>;
  selectedDataDomain$: Observable<DataDomain | null>;

  constructor(private store: Store<AppState>, private translateService: TranslateService) {
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain);
    this.availableDataDomains$= this.store.select(selectAvailableDataDomains).pipe(tap(availableDataDomains => {
      this.dataDomainSelectionItems = [];
      for (const availableDataDomain of availableDataDomains) {
        this.dataDomainSelectionItems.push({
          label: this.translateService.translate(availableDataDomain.name),
          data: availableDataDomain
        })
      }
    }));
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

  navigateHome() {
    this.showDashboardMenu = false;
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
    TranslocoModule
  ],
  exports: [MobileComponent],
  declarations: [MobileComponent]
})
export class MobileModule {

}
