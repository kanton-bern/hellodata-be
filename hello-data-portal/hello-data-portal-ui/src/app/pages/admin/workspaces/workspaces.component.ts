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

import {Component, inject, OnInit} from '@angular/core';
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {selectAppInfos} from "../../../store/metainfo-resource/metainfo-resource.selector";
import {AppState} from "../../../store/app/app.state";
import {AsyncPipe} from "@angular/common";
import {Ripple} from "primeng/ripple";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {NgArrayPipesModule} from "ngx-pipes";
import {navigate} from "../../../store/app/app.action";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {loadAppInfoResources} from "../../../store/metainfo-resource/metainfo-resource.action";

import {Card} from 'primeng/card';

@Component({
  selector: 'app-workspaces',
  templateUrl: './workspaces.component.html',
  styleUrls: ['./workspaces.component.scss'],
  imports: [Ripple, AsyncPipe, NgArrayPipesModule, Card]
})
export class WorkspacesComponent extends BaseComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);


  appInfos$: Observable<any>;

  constructor() {
    super();
    this.store.dispatch(loadAppInfoResources());
    this.appInfos$ = this.store.select(selectAppInfos);
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.workspaces.label,
          routerLink: naviElements.workspaces.path,
        },
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  navigateToSelectedWorkspace(selectedAppInfo: any) {
    this.store.dispatch(navigate({url: `workspaces/selected-workspace/${selectedAppInfo.instanceName}/${selectedAppInfo.moduleType}/${selectedAppInfo.apiVersion}`}))
  }

}


