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
import {selectDocumentation} from "../../../store/summary/summary.selector";
import {combineLatest, map, Observable, tap} from "rxjs";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {createOrUpdateDocumentation, loadDocumentation} from "../../../store/summary/summary.actions";
import {selectSelectedLanguage, selectSupportedLanguages} from "../../../store/auth/auth.selector";
import {Documentation} from "../../../store/summary/summary.model";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {take} from "rxjs/operators";

@Component({
  selector: 'app-documentation',
  templateUrl: './documentation-management.component.html',
  styleUrls: ['./documentation-management.component.scss']
})
export class DocumentationManagementComponent extends BaseComponent implements OnInit {
  documentationForm!: FormGroup;
  initForm$: Observable<any>;
  selectedLanguage$: Observable<any>;
  supportedLanguages$: Observable<string[]>;

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
    super();
    this.store.dispatch(loadDocumentation());
    this.initForm$ = combineLatest([
      this.store.select(selectDocumentation),
      this.store.select(selectSupportedLanguages).pipe(take(1))
    ]).pipe(
      tap(([doc, supportedLanguages]) => {
        const documentationCpy = {...doc};
        const languageAnnouncementFormGroups: { [key: string]: FormGroup } = {};
        supportedLanguages.forEach((language) => {
          if (!documentationCpy.texts) {
            documentationCpy.texts = {};
            documentationCpy.texts[language] = '';
          }
          languageAnnouncementFormGroups[language] = this.fb.group({
            text: [documentationCpy?.texts?.[language] || '', [Validators.required, Validators.minLength(3)]],
          });
        });

        this.documentationForm = this.fb.group({
          languages: this.fb.group(languageAnnouncementFormGroups),
        });
      }),
      map(([doc, supportedLanguages]) => {
        return  {doc: doc, langs: supportedLanguages};
      })
    );
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage);
    this.supportedLanguages$ = this.store.select(selectSupportedLanguages);
    this.createBreadcrumbs();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  createOrUpdateDocumentation(documentation: Documentation) {
    const documentationToBeSaved = {...documentation} as Documentation;
    const formDocumentation = this.documentationForm.getRawValue() as any;
    documentationToBeSaved.texts = Object.keys(formDocumentation.languages).reduce((acc, locale) => {
      acc[locale] = formDocumentation.languages[locale].text;
      return acc;
    }, {} as { [locale: string]: string });
    this.store.dispatch(createOrUpdateDocumentation({
      documentation: documentationToBeSaved
    }));
  }

  private createBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.documentationManagement.label,
          routerLink: naviElements.documentationManagement.path,
        }
      ]
    }));
  }

  getText(language: string): FormControl {
    const languagesGroup = this.documentationForm.get('languages') as FormGroup;
    const languageForm = languagesGroup.get(language) as FormGroup;
    return languageForm.get('text') as FormControl;
  }
}
