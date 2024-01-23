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

import {Component, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {CreateOrUpdateDocumentation, LoadDocumentation} from "../../../store/summary/summary.actions";
import {selectDocumentation} from "../../../store/summary/summary.selector";
import {Observable, tap} from "rxjs";
import {CreateBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../app-navi-elements";
import {MarkUnsavedChanges} from "../../../store/unsaved-changes/unsaved-changes.actions";
import {BaseComponent} from "../../../shared/components/base/base.component";

@Component({
  selector: 'app-documentation',
  templateUrl: './documentation-management.component.html',
  styleUrls: ['./documentation-management.component.scss']
})
export class DocumentationManagementComponent extends BaseComponent implements OnInit {
  documentation = '';
  documentation$: Observable<any>;

  constructor(private store: Store<AppState>) {
    super();
    this.store.dispatch(new LoadDocumentation());
    this.documentation$ = this.store.select(selectDocumentation).pipe(tap(doc => {
      this.documentation = doc;
    }));
    this.createBreadcrumbs();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  createOrUpdateDocumentation() {
    this.store.dispatch(new CreateOrUpdateDocumentation({text: this.documentation}))
  }

  onTextChange() {
    this.store.dispatch(new MarkUnsavedChanges(new CreateOrUpdateDocumentation({text: this.documentation})));
  }

  private createBreadcrumbs() {
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.documentationManagement.label,
        routerLink: naviElements.documentationManagement.path,
      }
    ]));
  }
}
