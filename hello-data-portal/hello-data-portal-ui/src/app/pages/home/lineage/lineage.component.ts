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

import { Component, OnInit, inject } from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {FormBuilder, FormControl, FormGroup} from "@angular/forms";
import {naviElements} from "../../../app-navi-elements";
import {LineageDoc} from "../../../store/lineage-docs/lineage-docs.model";
import {Observable} from "rxjs";
import {selectMyLineageDocs} from "../../../store/lineage-docs/lineage-docs.selector";
import { AsyncPipe } from '@angular/common';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
    selector: 'app-lineage',
    templateUrl: './lineage.component.html',
    styleUrls: ['./lineage.component.scss'],
    imports: [TableModule, PrimeTemplate, RouterLink, AsyncPipe, TranslocoPipe]
})
export class LineageComponent implements OnInit {
  private store = inject<Store<AppState>>(Store);
  private fb = inject(FormBuilder);

  projectDocsForm!: FormGroup;
  docs$: Observable<any>;

  constructor() {
    this.docs$ = this.store.select(selectMyLineageDocs);
  }

  ngOnInit(): void {
    this.projectDocsForm = this.fb.group({
      pd: new FormControl()
    });
  }

  openLineage(projectDoc: LineageDoc) {
    const id = projectDoc.name;
    const contextKey = projectDoc.contextKey;
    const urlEncodedProjectPath = encodeURIComponent(projectDoc.path);
    return `/${naviElements.lineageDocs.path}/detail/${contextKey}/${id}/${urlEncodedProjectPath}`;
  }
}
