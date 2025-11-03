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

import { Component, HostBinding, OnInit, inject } from '@angular/core';
import {AppInfoService, ScreenService} from './shared/services';
import {Store} from "@ngrx/store";
import {AppState} from "./store/app/app.state";
import {selectCurrentBusinessDomain, selectIsAuthenticated} from "./store/auth/auth.selector";
import {Observable, tap} from "rxjs";
import {Title} from "@angular/platform-browser";
import {checkAuth, checkProfile} from "./store/auth/auth.action";
import {selectQueryParam} from "./store/router/router.selectors";
import {navigate} from "./store/app/app.action";
import {AsyncPipe, NgIf} from '@angular/common';
import {RouterOutlet} from '@angular/router';
import {MobileComponent, SideNavOuterToolbarComponent} from "./layouts";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  imports: [NgIf, RouterOutlet, AsyncPipe, SideNavOuterToolbarComponent, MobileComponent]
})
export class AppComponent implements OnInit {
  private store = inject<Store<AppState>>(Store);
  private screen = inject(ScreenService);
  appInfo = inject(AppInfoService);
  private title = inject(Title);


  private static readonly REDIRECT_TO_PARAM = 'redirectTo';

  isAuthenticated$: Observable<boolean>;
  businessDomain$: Observable<string>;
  checkAuth = false;
  redirectTo$: Observable<any>;
  isMobile$: Observable<boolean>;

  constructor() {
    const appInfo = this.appInfo;

    setTimeout(() => {
      this.checkAuth = true;
    }, 1500);

    this.isAuthenticated$ = this.store.select(selectIsAuthenticated).pipe(
      tap(isAuthenticated => {
        console.debug('is authenticated', isAuthenticated)
        if (!isAuthenticated) {
          this.store.dispatch(checkAuth());
        }
      }))
    this.businessDomain$ = this.store.select(selectCurrentBusinessDomain).pipe(tap(businessDomainName => {
      this.title.setTitle(`${appInfo.title} -- ${businessDomainName}`);
    }));

    this.redirectTo$ = this.store.select(selectQueryParam(AppComponent.REDIRECT_TO_PARAM)).pipe(tap(param => {
      // temporary omit the '/auth request done twice' random problem by the auth lib, which blocks the new tab fullscreen DWH viewer opening
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
    setTimeout(() => {
      // temporary omit the '/auth request done twice' random problem by the auth lib, which blocks the new tab fullscreen DWH viewer opening
      const clearRedirectInterval = setInterval(() => {
        const redirectToParam = sessionStorage.getItem(AppComponent.REDIRECT_TO_PARAM);
        if (redirectToParam) {
          console.debug('found redirect param in the session storage, redirecting', redirectToParam);
          sessionStorage.removeItem(AppComponent.REDIRECT_TO_PARAM);
          this.store.dispatch(navigate({url: redirectToParam as string}));
        }
      }, 200);
      setTimeout(() => clearInterval(clearRedirectInterval), 5000);
    }, 500);

    this.checkProfile();
  }

  private checkProfile() {
    setInterval(() => {
      console.debug("Check profile")
      this.store.dispatch(checkProfile());
    }, 30000);
  }
}
