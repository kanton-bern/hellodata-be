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

import {Component, HostListener} from '@angular/core';
import {environment} from "../../../environments/environment";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {naviElements} from "../../app-navi-elements";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {Observable, tap} from "rxjs";
import {selectSelectedLanguage} from "../../store/auth/auth.selector";
import {CloudbeaverService} from "../../store/auth/cloudbeaver.service";

@Component({
  templateUrl: 'embedded-dm-viewer.component.html',
  styleUrls: ['./embedded-dm-viewer.component.scss']
})
export class EmbeddedDmViewerComponent {
  baseUrl = environment.subSystemsConfig.dmViewer.protocol + environment.subSystemsConfig.dmViewer.host
    + environment.subSystemsConfig.dmViewer.domain;
  iframeUrl = '';
  selectedLanguage$: Observable<any>;

  constructor(private store: Store<AppState>, private cloudbeaverService: CloudbeaverService) {
    const cookieName = 'cb-session-id';
    document.cookie = cookieName + "=; path=/cloudbeaver/; expires=Thu, 01 Jan 1970 00:00:00 UTC;";
    this.iframeUrl = this.baseUrl;
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage).pipe(tap(selectedLang => {
      if (selectedLang) {
        this.updateIframeUrl(selectedLang.code as string);
      }
    }));
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.embeddedDmViewer.label,
        }
      ]
    }));
    setInterval(() => {
      this.cloudbeaverService.renewSession();
    }, 60000 * 9);
  }

  updateIframeUrl(language: string) {
    this.iframeUrl = `${this.baseUrl}?lang=${language}`;
  }

  @HostListener("window:scroll", ["$event"])
  onWindowScroll() {
    setTimeout(function () {
      window.scrollBy(0, -60);
    }, 10);
    setTimeout(function () {
      document.querySelectorAll<HTMLElement>('.p-scrolltop-icon').forEach(element => {
        element.click();
      });
    }, 200);
  }

}
