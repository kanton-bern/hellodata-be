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

import {Component, Input, NgModule} from '@angular/core';
import {HeaderModule, SummaryModule} from '../../shared/components';
import {CommonModule} from '@angular/common';

import {RouterLink, RouterOutlet} from '@angular/router';
import {ScrollPanelModule} from "primeng/scrollpanel";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {selectNavItems} from "../../store/menu/menu.selector";
import {TranslocoModule} from "@jsverse/transloco";
import {TooltipModule} from "primeng/tooltip";
import {DividerModule} from "primeng/divider";
import {ToastModule} from "primeng/toast";
import {ScrollTopModule} from "primeng/scrolltop";
import {UnsavedChangesModule} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {selectCurrentUserPermissionsLoaded} from "../../store/auth/auth.selector";
import {MatomoTrackClickDirective, MatomoTrackerDirective} from "ngx-matomo-client";
import {navigate, openWindow, trackEvent} from "../../store/app/app.action";
import {TieredMenuModule} from "primeng/tieredmenu";
import {BadgeModule} from "primeng/badge";
import {MenuItem} from "primeng/api";
import {Ripple} from "primeng/ripple";

@Component({
  selector: 'app-side-nav-outer-toolbar',
  templateUrl: './side-nav-outer-toolbar.component.html',
  styleUrls: ['./side-nav-outer-toolbar.component.scss'],
  standalone: false
})
export class SideNavOuterToolbarComponent {

  @Input()
  title!: string;
  navItems$: Observable<any[]>;
  selectCurrentUserPermissionsLoaded$: Observable<boolean>;
  mouseEnterTimeoutId: number[] = [];

  constructor(private store: Store<AppState>) {
    this.navItems$ = this.store.select(selectNavItems);
    this.selectCurrentUserPermissionsLoaded$ = this.store.select(selectCurrentUserPermissionsLoaded);
  }

  navigateHome() {
    this.store.dispatch(navigate({url: 'home'}));
    this.store.dispatch(trackEvent({
      eventCategory: 'Menu Item',
      eventAction: '[Click] - Moved to Home'
    }));
  }

  openWindow(item: MenuItem) {
    let isRouterOrUrl = false;
    if (item.routerLink) {
      this.store.dispatch(navigate({url: item.routerLink}));
      isRouterOrUrl = true;
    }
    if (item.target || item.url) {
      this.store.dispatch(openWindow({url: item.url as string, target: item.target as string}));
      isRouterOrUrl = true;
    }
    if (isRouterOrUrl) {
      console.debug('openWindow', item);
      this.store.dispatch(trackEvent({
        eventCategory: 'Menu Item',
        eventAction: '[Click] - ' + item.label
      }));

    }
  }

  // hide menu on leave after timeout
  onMouseleave() {
    const timeoutId = setTimeout(() => {
      window.document.body.click();
    }, 1200);
    this.mouseEnterTimeoutId.push(timeoutId);
  }

  // reset menu hide timers back on menu
  onMouseEnter() {
    for (const timeout of this.mouseEnterTimeoutId) {
      clearTimeout(timeout);
    }
    this.mouseEnterTimeoutId = [];
  }
}

@NgModule({
  imports: [HeaderModule,
    CommonModule,
    RouterOutlet,
    ScrollPanelModule,
    RouterLink,
    TranslocoModule,
    TooltipModule,
    DividerModule,
    SummaryModule,
    ToastModule,
    ScrollTopModule,
    UnsavedChangesModule,
    MatomoTrackerDirective,
    MatomoTrackClickDirective, TieredMenuModule, BadgeModule, Ripple
  ],
  exports: [SideNavOuterToolbarComponent],
  declarations: [SideNavOuterToolbarComponent]
})
export class SideNavOuterToolbarModule {
}
