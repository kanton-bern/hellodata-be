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

import {Component, DestroyRef, HostListener, inject, input} from '@angular/core';

import {AsyncPipe, NgStyle, NgClass} from '@angular/common';
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {selectNavItems} from "../../store/menu/menu.selector";
import {NavigationEnd, Router} from "@angular/router";
import {TranslocoPipe} from "@jsverse/transloco";
import {Toast} from "primeng/toast";
import {ProgressBar} from "primeng/progressbar";
import {
  UnsavedChangesDialogComponent
} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {openWindow, trackEvent} from "../../store/app/app.action";
import {MenuItem} from "primeng/api";
import {HeaderComponent} from '../../shared/components';
import {environment} from "../../../environments/environment";
import {Environment} from "../../shared/components/header/header.component";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {filter} from "rxjs/operators";
import {ICON_REGISTRY} from '../../shared/icons';
import {LoadingService} from "../../shared/services/loading.service";

@Component({
  selector: 'app-side-nav-outer-toolbar',
  templateUrl: './side-nav-outer-toolbar.component.html',
  styleUrls: ['./side-nav-outer-toolbar.component.scss'],
  imports: [HeaderComponent,
    Toast, ProgressBar, UnsavedChangesDialogComponent, AsyncPipe, TranslocoPipe, NgStyle,
    NgClass,
]
})
export class SideNavOuterToolbarComponent {
  protected readonly icons = ICON_REGISTRY;
  private static readonly SIDEBAR_STATE_KEY = 'sidebar-minimized';
  private readonly store = inject<Store<AppState>>(Store);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly loadingService = inject(LoadingService);

  readonly title = input.required<string>();
  readonly isLoading$ = this.loadingService.isLoading$;
  navItems$: Observable<any[]>;
  sidebarMinimized = false;
  environment: Environment;

  // Navigation drawer state
  drawerOpen = false;
  activeDrawerItem: MenuItem | null = null;
  expandedSubItems = new Set<string>();

  constructor() {
    this.navItems$ = this.store.select(selectNavItems);
    const stored = sessionStorage.getItem(SideNavOuterToolbarComponent.SIDEBAR_STATE_KEY);
    this.sidebarMinimized = stored === null ? false : stored === 'true';
    this.environment = {
      name: environment.deploymentEnvironment.name,
      showEnvironment: environment.deploymentEnvironment.showEnvironment ?? true,
      color: environment.deploymentEnvironment.headerColor ? environment.deploymentEnvironment.headerColor : ''
    };
    if (this.environment.color) {
      document.documentElement.style.setProperty('--header-menu-bg', this.environment.color);
    }

    // Close drawer on route navigation
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.closeDrawer();
    });
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.closeDrawer();
  }

  @HostListener('document:mouseleave')
  onDocumentMouseLeave(): void {
    this.closeDrawer();
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

  /**
   * Handles click on a first-level navigation item.
   * Never closes an already open drawer: hovering the icon opens the drawer, so a
   * subsequent click on the same icon would otherwise close it right away.
   */
  onFirstLevelClick(item: MenuItem): void {
    if (item.items && item.items.length > 0) {
      this.openDrawerFor(item);
    } else {
      this.navigateToItem(item);
    }
  }

  /**
   * Opens the drawer when hovering over a first-level item with children.
   */
  onFirstLevelHover(item: MenuItem): void {
    if (item.items && item.items.length > 0) {
      this.openDrawerFor(item);
    } else if (this.drawerOpen) {
      this.closeDrawer();
    }
  }

  /**
   * Handles click on a second-level item in the drawer.
   */
  onSecondLevelClick(sub: MenuItem, index: number): void {
    if (sub.items?.length) {
      const key = this.getSubItemKey(sub, index);
      if (this.expandedSubItems.has(key)) {
        this.expandedSubItems.delete(key);
      } else {
        this.expandedSubItems.add(key);
      }
    } else {
      this.navigateToItem(sub);
    }
  }

  onThirdLevelClick(item: MenuItem): void {
    this.navigateToItem(item);
  }

  isSubItemExpanded(sub: MenuItem, index: number): boolean {
    return this.expandedSubItems.has(this.getSubItemKey(sub, index));
  }

  closeDrawer(): void {
    this.drawerOpen = false;
    this.activeDrawerItem = null;
    this.expandedSubItems.clear();
  }

  private openDrawerFor(item: MenuItem): void {
    if (this.activeDrawerItem !== item) {
      this.expandedSubItems.clear();
    }
    this.activeDrawerItem = item;
    this.drawerOpen = true;
  }

  private getSubItemKey(sub: MenuItem, index: number): string {
    return sub.id || `${sub.label}-${index}`;
  }

  private navigateToItem(item: MenuItem): void {
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
      this.store.dispatch(trackEvent({
        eventCategory: 'Menu Item',
        eventAction: '[Click] - ' + item.label
      }));
      this.closeDrawer();
    }
  }

}
