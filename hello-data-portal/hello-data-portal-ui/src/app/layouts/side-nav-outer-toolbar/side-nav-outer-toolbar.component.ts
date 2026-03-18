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

import {AsyncPipe, NgStyle} from '@angular/common';
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {selectNavItems} from "../../store/menu/menu.selector";
import {Router} from "@angular/router";
import {TranslocoPipe} from "@jsverse/transloco";
import {Toast} from "primeng/toast";
import {
  UnsavedChangesDialogComponent
} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {openWindow, trackEvent} from "../../store/app/app.action";
import {MenuItem} from "primeng/api";
import {HeaderComponent} from '../../shared/components';
import {environment} from "../../../environments/environment";
import {Environment} from "../../shared/components/header/header.component";

@Component({
  selector: 'app-side-nav-outer-toolbar',
  templateUrl: './side-nav-outer-toolbar.component.html',
  styleUrls: ['./side-nav-outer-toolbar.component.scss'],
  imports: [HeaderComponent,
    Toast, UnsavedChangesDialogComponent, AsyncPipe, TranslocoPipe, NgStyle]
})
export class SideNavOuterToolbarComponent {
  private static readonly SIDEBAR_STATE_KEY = 'sidebar-minimized';
  private readonly store = inject<Store<AppState>>(Store);
  private readonly router = inject(Router);

  readonly title = input.required<string>();
  navItems$: Observable<any[]>;
  sidebarMinimized = false;
  environment: Environment;

  constructor() {
    this.navItems$ = this.store.select(selectNavItems);
    const stored = sessionStorage.getItem(SideNavOuterToolbarComponent.SIDEBAR_STATE_KEY);
    this.sidebarMinimized = stored === null ? false : stored === 'true';
    this.environment = {
      name: environment.deploymentEnvironment.name,
      showEnvironment: environment.deploymentEnvironment.showEnvironment ?? true,
      color: environment.deploymentEnvironment.headerColor ? environment.deploymentEnvironment.headerColor : ''
    };
    // Expose headerColor as a CSS custom property so global p-menu popups can use it
    if (this.environment.color) {
      document.documentElement.style.setProperty('--header-menu-bg', this.environment.color);
    }
  }

  toggleSidebar(): void {
    this.sidebarMinimized = !this.sidebarMinimized;
    sessionStorage.setItem(SideNavOuterToolbarComponent.SIDEBAR_STATE_KEY, String(this.sidebarMinimized));
  }

  navigateHome() {
    this.router.navigate(['home']);
    this.store.dispatch(trackEvent({
      eventCategory: 'Menu Item',
      eventAction: '[Click] - Moved to Home'
    }));
  }

  openWindow(item: MenuItem) {
    let isRouterOrUrl = false;
    if (item.routerLink) {
      this.router.navigate([item.routerLink]);
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

  /**
   * Repositions the second-level flyout menu (position:absolute) so it
   * stays within the viewport with PADDING px at the top and bottom.
   * Does NOT set overflow or maxHeight — the second-level must keep
   * overflow visible so the third-level can fly out.
   */
  repositionSecondLevelMenu(event: MouseEvent): void {
    const li = event.currentTarget as HTMLElement;
    const submenu = li.querySelector('.second-level-menu') as HTMLElement;
    if (!submenu) {
      return;
    }
    // Reset so measurements are accurate
    submenu.style.top = '0';

    requestAnimationFrame(() => {
      const PADDING = 8;
      const rect = submenu.getBoundingClientRect();
      const viewportHeight = window.innerHeight;

      if (rect.bottom > viewportHeight - PADDING) {
        const overflow = rect.bottom - (viewportHeight - PADDING);
        const newTop = -overflow;
        const minTop = -(rect.top - PADDING);
        submenu.style.top = `${Math.max(newTop, minTop)}px`;
      }
    });
  }

  /**
   * Repositions the third-level flyout menu (position:absolute) so it
   * stays within the viewport with PADDING px at the top and bottom.
   * Also initialises the scroll-indicator arrows.
   */
  repositionThirdLevelMenu(event: MouseEvent): void {
    const li = event.currentTarget as HTMLElement;
    const submenu = li.querySelector('.third-level-menu') as HTMLElement;
    if (!submenu) {
      return;
    }
    // Reset so measurements are accurate
    submenu.style.top = '0';

    requestAnimationFrame(() => {
      const PADDING = 8;
      const rect = submenu.getBoundingClientRect();
      const viewportHeight = window.innerHeight;

      if (rect.bottom > viewportHeight - PADDING) {
        const overflow = rect.bottom - (viewportHeight - PADDING);
        const newTop = -overflow;
        const minTop = -(rect.top - PADDING);
        submenu.style.top = `${Math.max(newTop, minTop)}px`;
      }
      submenu.style.maxHeight = `${viewportHeight - 2 * PADDING}px`;

      // Update scroll indicators
      const list = submenu.querySelector('.third-level-menu__list') as HTMLElement;
      if (list) {
        this.updateScrollIndicators(submenu, list);
      }
    });
  }

  /**
   * Handle scroll events on the third-level list to update arrow indicators.
   */
  onThirdLevelScroll(event: Event): void {
    const list = event.target as HTMLElement;
    const submenu = list.closest('.third-level-menu') as HTMLElement;
    if (submenu && list) {
      this.updateScrollIndicators(submenu, list);
    }
  }

  /**
   * Scroll the third-level list by `delta` pixels when an arrow is clicked.
   * Uses a custom eased animation so larger distances are clearly visible.
   */
  scrollThirdLevel(event: MouseEvent, delta: number): void {
    event.stopPropagation();
    const indicator = event.currentTarget as HTMLElement;
    const wrapper = indicator.closest('.third-level-menu') as HTMLElement;
    const list = wrapper?.querySelector('.third-level-menu__list') as HTMLElement;
    if (!list) {
      return;
    }

    const duration = 400; // ms
    const start = list.scrollTop;
    const startTime = performance.now();

    const easeInOutCubic = (t: number): number =>
      t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;

    const step = (now: number) => {
      const elapsed = Math.min((now - startTime) / duration, 1);
      list.scrollTop = start + delta * easeInOutCubic(elapsed);
      if (elapsed < 1) {
        requestAnimationFrame(step);
      }
    };
    requestAnimationFrame(step);
  }

  /**
   * Show/hide scroll-indicator arrows based on current scroll position.
   * Uses CSS class toggling for smooth transitions.
   */
  private updateScrollIndicators(wrapper: HTMLElement, list: HTMLElement): void {
    const upArrow = wrapper.querySelector('.scroll-indicator--up') as HTMLElement;
    const downArrow = wrapper.querySelector('.scroll-indicator--down') as HTMLElement;
    if (!upArrow || !downArrow) {
      return;
    }

    const isScrollable = list.scrollHeight > list.clientHeight;
    const atTop = list.scrollTop <= 1;
    const atBottom = list.scrollTop + list.clientHeight >= list.scrollHeight - 1;

    upArrow.classList.toggle('visible', isScrollable && !atTop);
    downArrow.classList.toggle('visible', isScrollable && !atBottom);
  }

}
