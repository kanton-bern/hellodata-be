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

import { Component, inject, output } from '@angular/core';
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectDocumentationFilterEmpty} from "../../../store/summary/summary.selector";
import {
  selectCurrentUserPermissions,
  selectDefaultLanguage,
  selectSelectedLanguage
} from "../../../store/auth/auth.selector";
import {TranslateService} from "../../../shared/services/translate.service";
import { AsyncPipe } from '@angular/common';
import { Editor } from 'primeng/editor';
import { FormsModule } from '@angular/forms';
import { SharedModule } from 'primeng/api';

@Component({
    selector: 'app-home-documentation',
    templateUrl: './home-documentation.component.html',
    styleUrls: ['./home-documentation.component.scss'],
    imports: [Editor, FormsModule, SharedModule, AsyncPipe]
})
export class HomeDocumentationComponent {
  private store = inject<Store<AppState>>(Store);
  private translateService = inject(TranslateService);

  readonly rightSidebarVisible = output<boolean>();
  currentUserPermissions$: Observable<string[]>;
  documentation$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;

  constructor() {
    const store = this.store;

    this.documentation$ = this.store.select(selectDocumentationFilterEmpty);
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  getText(documentation: any, selectedLanguage: string, defaultLanguage: string) {
    const text = documentation.texts[selectedLanguage];
    if (!text || text.trim() === '') {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + documentation.texts[defaultLanguage];
    }
    return text;
  }
}
