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

import {Component, ElementRef, HostListener, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {LineageDocsService} from "../../../store/lineage-docs/lineage-docs.service";
import {Navigate} from "../../../store/app/app.action";
import {CreateBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {LoadAvailableContexts} from "../../../store/users-management/users-management.action";
import {Context} from "../../../store/users-management/context-role.model";
import {selectLineageInfo} from "../../../store/lineage-docs/lineage-docs.selector";

@Component({
  templateUrl: 'embedded-lineage-docs.component.html',
  styleUrls: ['./embedded-lineage-docs.component.scss']
})
export class EmbeddedLineageDocsComponent {
  url!: string;
  projectId!: string;

  lineageInfo$: Observable<any>;

  @ViewChild('container') container!: ElementRef;
  @ViewChild('doc') docIframe!: ElementRef;

  constructor(private store: Store<AppState>, private docsService: LineageDocsService, private route: ActivatedRoute) {
    this.store.dispatch(new LoadAvailableContexts());
    this.lineageInfo$ = this.store.select(selectLineageInfo).pipe(tap((lineageInfo) => {
      if (lineageInfo) {
        this.url = docsService.getProjectPathUrl(lineageInfo.path as string);
        this.createBreadCrumbs(lineageInfo.dataDomain);
        this.projectId = lineageInfo.projectId as string;
        console.debug("Embed ProjectDocs. Open url", this.url);
      }
    }))
  }

  cancel() {
    this.store.dispatch(new Navigate('/lineage-docs/list'));
  }

  @HostListener("window:scroll", ["$event"])
  onWindowScroll() {
    setTimeout(function () {
      window.scrollBy(0, -60);
    }, 10);
    setTimeout(function () {
      document.querySelectorAll<HTMLElement>('.layout-topbar-button').forEach(element => {
        element.style.visibility = "visible";
      });
    }, 1000);

  }

  private createBreadCrumbs(dataDomain: Context | undefined) {
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.lineageDocsList.label,
        routerLink: naviElements.lineageDocs.path + '/' + naviElements.lineageDocsList.path
      },
      {
        label: dataDomain?.name,
      },
      {
        label: this.projectId + ' Data Lineage',
      }
    ]));
  }
}
