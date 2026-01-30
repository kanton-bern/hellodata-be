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

import {Component, inject, input} from '@angular/core';

import {AsyncPipe, NgClass} from '@angular/common';
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {distinctUntilChanged, Observable} from "rxjs";
import {selectNavItems} from "../../store/menu/menu.selector";
import {NavigationEnd, Router} from "@angular/router";
import {TranslocoPipe} from "@jsverse/transloco";
import {Tooltip} from "primeng/tooltip";
import {Toast} from "primeng/toast";
import {
  UnsavedChangesDialogComponent
} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {navigate, openWindow, trackEvent} from "../../store/app/app.action";
import {MenuItem} from "primeng/api";
import {HeaderComponent} from '../../shared/components';

@Component({
  selector: 'app-side-nav-outer-toolbar',
  templateUrl: './side-nav-outer-toolbar.component.html',
  styleUrls: ['./side-nav-outer-toolbar.component.scss'],
  imports: [Tooltip, HeaderComponent,
    Toast, UnsavedChangesDialogComponent, AsyncPipe, NgClass, TranslocoPipe]
})
export class SideNavOuterToolbarComponent {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly router = inject(Router);

  readonly title = input.required<string>();
  navItems$: Observable<any[]>;
  currentUrl = '';

  constructor() {
    this.navItems$ = this.store.select(selectNavItems).pipe(distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)));
    this.currentUrl = this.router.url;
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.currentUrl = event.urlAfterRedirects;
      }
    });
  }

  isActive(item: MenuItem): boolean {
    if (!item.routerLink) return false;
    const link = typeof item.routerLink === 'string' ? item.routerLink : item.routerLink.join('/');
    return this.currentUrl.startsWith('/' + link);
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

}


