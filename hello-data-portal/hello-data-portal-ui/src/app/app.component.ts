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

import {Component, HostBinding} from '@angular/core';
import {AppInfoService, ScreenService} from './shared/services';
import {Store} from "@ngrx/store";
import {AppState} from "./store/app/app.state";
import {selectCurrentBusinessDomain, selectIsAuthenticated} from "./store/auth/auth.selector";
import {Observable, tap} from "rxjs";
import {Title} from "@angular/platform-browser";
import {checkAuth} from "./store/auth/auth.action";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  isAuthenticated$: Observable<boolean>;
  businessDomain$: Observable<string>;
  checkAuth = false;

  constructor(private store: Store<AppState>, private screen: ScreenService, public appInfo: AppInfoService,
              private title: Title) {
    setTimeout(() => {
      this.checkAuth = true;
    }, 500);

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
  }

  @HostBinding('class') get getClass() {
    return Object.keys(this.screen.sizes).filter(cl => this.screen.sizes[cl]).join(' ');
  }

}
