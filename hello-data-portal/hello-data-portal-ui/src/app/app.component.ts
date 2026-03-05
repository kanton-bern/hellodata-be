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

import {Component, HostBinding, inject, OnDestroy, OnInit} from '@angular/core';
import {AppInfoService, ScreenService} from './shared/services';
import {Store} from "@ngrx/store";
import {AppState} from "./store/app/app.state";
import {selectCurrentBusinessDomain, selectFirstLogin} from "./store/auth/auth.selector";
import {filter, Observable, Subject, take, takeUntil, tap, timer} from "rxjs";
import {Title} from "@angular/platform-browser";
import {checkAuth, checkProfile} from "./store/auth/auth.action";
import {selectQueryParam} from "./store/router/router.selectors";
import {navigate} from "./store/app/app.action";
import {AsyncPipe} from '@angular/common';
import {RouterOutlet} from '@angular/router';
import {MobileComponent, SideNavOuterToolbarComponent} from "./layouts";
import {TranslocoModule} from "@jsverse/transloco";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  imports: [RouterOutlet, AsyncPipe, SideNavOuterToolbarComponent, MobileComponent, TranslocoModule]
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly screen = inject(ScreenService);
  appInfo = inject(AppInfoService);
  private readonly title = inject(Title);
  private readonly destroy$ = new Subject<void>();

  private static readonly REDIRECT_TO_PARAM = 'redirectTo';
  private static readonly FIRST_LOGIN_DELAY_MS = 5000;
  private static readonly FADE_OUT_DURATION_MS = 600;

  businessDomain$: Observable<string>;
  redirectTo$: Observable<any>;
  isMobile$: Observable<boolean>;
  showFirstLoginOverlay = false;
  overlayFadingOut = false;

  constructor() {
    const appInfo = this.appInfo;

    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain).pipe(tap(businessDomainName => {
      this.title.setTitle(`${appInfo.title} -- ${businessDomainName}`);
    }));

    this.redirectTo$ = this.store.select(selectQueryParam(AppComponent.REDIRECT_TO_PARAM)).pipe(tap(param => {
      console.debug('enabled redirect param?', param);
      if (param) {
        sessionStorage.setItem(AppComponent.REDIRECT_TO_PARAM, param as string);
        console.debug('saved redirect param to the session storage', param);
      }
    }));
    this.isMobile$ = this.screen.isMobile.pipe(tap(v => console.log('is mobile', v)));
  }

  @HostBinding('class') get getClass() {
    return Object.keys(this.screen.sizes).filter(cl => this.screen.sizes[cl]).join(' ');
  }

  ngOnInit(): void {
    // Restore auth session on page refresh. Skip on /callback to avoid race condition
    // with CallbackComponent which handles the OIDC code exchange itself.
    // Use window.location instead of router.url because Angular Router may not have
    // resolved the URL yet at bootstrap time.
    if (!window.location.pathname.includes('/callback')) {
      this.store.dispatch(checkAuth());
    }

    // Handle redirectTo param stored in sessionStorage (for opening new tab links)
    setTimeout(() => {
      const clearRedirectInterval = setInterval(() => {
        const redirectToParam = sessionStorage.getItem(AppComponent.REDIRECT_TO_PARAM);
        if (redirectToParam) {
          console.debug('found redirect param in the session storage, redirecting', redirectToParam);
          sessionStorage.removeItem(AppComponent.REDIRECT_TO_PARAM);
          this.store.dispatch(navigate({url: redirectToParam}));
        }
      }, 200);
      setTimeout(() => clearInterval(clearRedirectInterval), 5000);
    }, 500);
    this.checkProfile();
    this.watchFirstLogin();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private watchFirstLogin(): void {
    this.store.select(selectFirstLogin).pipe(
      filter(firstLogin => firstLogin),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      console.debug('[App] First login detected, showing overlay for', AppComponent.FIRST_LOGIN_DELAY_MS, 'ms');
      this.showFirstLoginOverlay = true;
      this.overlayFadingOut = false;
      // After display duration, trigger fade-out animation
      timer(AppComponent.FIRST_LOGIN_DELAY_MS).pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => {
        this.overlayFadingOut = true;
        // Remove from DOM after fade-out animation completes
        timer(AppComponent.FADE_OUT_DURATION_MS).pipe(
          takeUntil(this.destroy$)
        ).subscribe(() => {
          this.showFirstLoginOverlay = false;
          this.overlayFadingOut = false;
        });
      });
    });
  }

  private checkProfile() {
    const NORMAL_INTERVAL_MS = 30000;
    const FAST_INTERVAL_MS = 5000;
    const FAST_PHASE_MS = 120000;

    // Start the normal 30s polling immediately
    let profileTimer = setInterval(() => {
      console.debug("Check profile");
      this.store.dispatch(checkProfile());
    }, NORMAL_INTERVAL_MS);

    // If first login, temporarily switch to fast 5s polling, then revert to 30s
    this.store.select(selectFirstLogin).pipe(
      filter(firstLogin => firstLogin),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      console.debug('[App] First login detected, switching to fast profile polling');
      clearInterval(profileTimer);

      const fastTimer = setInterval(() => {
        console.debug("Check profile (fast)");
        this.store.dispatch(checkProfile());
      }, FAST_INTERVAL_MS);

      setTimeout(() => {
        clearInterval(fastTimer);
        profileTimer = setInterval(() => {
          console.debug("Check profile");
          this.store.dispatch(checkProfile());
        }, NORMAL_INTERVAL_MS);
      }, FAST_PHASE_MS);
    });
  }
}
