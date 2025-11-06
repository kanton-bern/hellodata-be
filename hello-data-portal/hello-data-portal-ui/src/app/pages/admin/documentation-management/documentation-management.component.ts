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
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectDocumentation} from "../../../store/summary/summary.selector";
import {combineLatest, map, Observable, tap} from "rxjs";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {createOrUpdateDocumentation, loadDocumentation} from "../../../store/summary/summary.actions";
import {
  selectDefaultLanguage,
  selectSelectedLanguage,
  selectSupportedLanguages
} from "../../../store/auth/auth.selector";
import {Documentation} from "../../../store/summary/summary.model";
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {take} from "rxjs/operators";
import {AsyncPipe} from '@angular/common';
import {Tab, TabList, TabPanel, TabPanels, Tabs} from 'primeng/tabs';
import {Editor} from 'primeng/editor';
import {Toolbar} from 'primeng/toolbar';
import {Button} from 'primeng/button';
import {TranslocoPipe} from '@jsverse/transloco';

@Component({
  selector: 'app-documentation',
  templateUrl: './documentation-management.component.html',
  styleUrls: ['./documentation-management.component.scss'],
  imports: [FormsModule, ReactiveFormsModule, Tabs, TabList, Tab, TabPanels, TabPanel, Editor, Toolbar, Button, AsyncPipe, TranslocoPipe]
})
export class DocumentationManagementComponent extends BaseComponent implements OnInit {
  private store = inject<Store<AppState>>(Store);
  private fb = inject(FormBuilder);

  documentationForm!: FormGroup;
  initForm$: Observable<any>;
  selectedLanguage$: Observable<any>;
  supportedLanguages$: Observable<string[]>;
  defaultLanguage$: Observable<string | null>;

  constructor() {
    super();
    this.defaultLanguage$ = this.store.select(selectDefaultLanguage);
    this.store.dispatch(loadDocumentation());
    this.initForm$ = combineLatest([
      this.store.select(selectDocumentation).pipe(take(2)),
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
        return {doc: doc, langs: supportedLanguages};
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

  getText(language: string): FormControl {
    const languagesGroup = this.documentationForm.get('languages') as FormGroup;
    const languageForm = languagesGroup.get(language) as FormGroup;
    return languageForm.get('text') as FormControl;
  }

  notFilled(language: string): boolean {
    const languagesGroup = this.documentationForm.get('languages') as FormGroup;
    const languageForm = languagesGroup?.get(language) as FormGroup;

    const messageControl = languageForm?.get('text') as FormControl;
    return !messageControl || messageControl.value === null || messageControl.value === undefined || messageControl.value.trim() === '';
  }

  isAtLeastOneLanguageFilled(): boolean {
    const languagesGroup = this.documentationForm.get('languages') as FormGroup;
    if (!languagesGroup) {
      return false;
    }

    const some = Object.values(languagesGroup.controls).some((languageControl) => {
      const group = languageControl as FormGroup;
      const valuePresent = group.get('text')?.value?.trim();
      return valuePresent && valuePresent !== '';
    });
    return some === true;
  }

  isAtLeastDefaultLanguageFilled(defaultLanguage: string): boolean {
    const languagesGroup = this.documentationForm.get('languages') as FormGroup;
    if (!languagesGroup) {
      return false;
    }

    const defaultLanguageControl = languagesGroup.get(defaultLanguage) as FormGroup;
    if (!defaultLanguageControl) {
      return false;
    }

    const filled = defaultLanguageControl.get('text')?.value?.trim()
    return filled;
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
}
