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

@Component({
  selector: 'mobile',
  templateUrl: './mobile.component.html',
  styleUrls: ['./mobile.component.scss']
})
export class MobileComponent {
  showSidebar = false;
}

@NgModule({
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarModule,
    ToastModule,
    UnsavedChangesModule,
    ScrollTopModule
  ],
  exports: [MobileComponent],
  declarations: [MobileComponent]
})
export class MobileModule {

  constructor() {
    const intervalId = setInterval(() => {
      console.log('MobileModule Runs every 3 seconds');
    }, 3000);
  }
}
