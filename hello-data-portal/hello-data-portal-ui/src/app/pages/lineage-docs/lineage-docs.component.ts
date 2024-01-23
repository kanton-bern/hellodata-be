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

import {Component, ElementRef, NgModule, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from "@angular/common";
import {Store} from "@ngrx/store";
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {EmbeddedLineageDocsComponent} from "./embedded/embedded-lineage-docs.component";
import {TranslocoModule} from "@ngneat/transloco";
import {DropdownModule} from "primeng/dropdown";
import {TooltipModule} from "primeng/tooltip";
import {LineageDoc} from "../../store/lineage-docs/lineage-docs.model";
import {AppState} from "../../store/app/app.state";
import {LineageDocsService} from "../../store/lineage-docs/lineage-docs.service";
import {SubsystemIframeModule} from "../../shared/components/subsystem-iframe/subsystem-iframe.component";
import {HdCommonModule} from "../../hd-common.module";
import {naviElements} from "../../app-navi-elements";
import {CreateBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {Observable} from "rxjs";
import {selectMyLineageDocs} from "../../store/lineage-docs/lineage-docs.selector";
import {RouterLink} from "@angular/router";
import {TableModule} from "primeng/table";
import {BaseComponent} from "../../shared/components/base/base.component";
import {navigate} from "../../store/app/app.action";

@Component({
  selector: 'app-docs',
  templateUrl: './lineage-docs.component.html',
  styleUrls: ['./lineage-docs.component.scss']
})
export class LineageDocsComponent extends BaseComponent implements OnInit {
  projectDocsForm!: FormGroup;
  docs$: Observable<any>;

  @ViewChild('availableProjectDocs') availableProjectDocs!: ElementRef;

  constructor(private store: Store<AppState>, private docsService: LineageDocsService, private fb: FormBuilder) {
    super();
    this.store.dispatch(new CreateBreadcrumbs([
      {
        label: naviElements.lineageDocsList.label,
        routerLink: naviElements.lineageDocsList.path
      }
    ]));
    this.docs$ = this.store.select(selectMyLineageDocs);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.projectDocsForm = this.fb.group({
      pd: new FormControl()
    });
  }

  openLineage(projectDoc: LineageDoc) {
    const id = projectDoc.name;
    const contextKey = projectDoc.contextKey;
    const urlEncodedProjectPath = encodeURIComponent(projectDoc.path);
    const docLink = `/${naviElements.lineageDocs.path}/detail/${contextKey}/${id}/${urlEncodedProjectPath}`;
    this.store.dispatch(navigate({url: docLink}));
  }
}

@NgModule({
  imports: [
    CommonModule,
    SubsystemIframeModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    HdCommonModule,
    TranslocoModule,
    DropdownModule,
    ReactiveFormsModule,
    TooltipModule,
    RouterLink,
    TableModule
  ],
  declarations: [LineageDocsComponent, EmbeddedLineageDocsComponent],
  exports: [LineageDocsComponent]
})
export class DocsModule {
}
