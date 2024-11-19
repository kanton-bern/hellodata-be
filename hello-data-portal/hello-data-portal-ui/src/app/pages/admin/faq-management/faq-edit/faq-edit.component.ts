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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {combineLatest, map, Observable, Subscription, tap} from "rxjs";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectEditedFaq} from "../../../../store/faq/faq.selector";
import {Faq} from "../../../../store/faq/faq.model";
import {naviElements} from "../../../../app-navi-elements";
import {selectAvailableDataDomainsWithAllEntry} from "../../../../store/my-dashboards/my-dashboards.selector";
import {ALL_DATA_DOMAINS} from "../../../../store/app/app.constants";
import {markUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";
import {BaseComponent} from "../../../../shared/components/base/base.component";
import {navigate} from "../../../../store/app/app.action";
import {createBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {deleteEditedFaq, saveChangesToFaq, showDeleteFaqPopup} from "../../../../store/faq/faq.action";
import {TranslateService} from "../../../../shared/services/translate.service";
import {selectSupportedLanguages} from "../../../../store/auth/auth.selector";
import {take} from "rxjs/operators";

@Component({
  selector: 'app-faq-edit',
  templateUrl: './faq-edit.component.html',
  styleUrls: ['./faq-edit.component.scss']
})
export class FaqEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedFaq$: Observable<Faq>;
  faqForm!: FormGroup;
  availableDataDomains$: Observable<any>;
  supportedLanguages$: Observable<string[]>;
  formValueChangedSub!: Subscription;

  constructor(private store: Store<AppState>, private fb: FormBuilder, private translateService: TranslateService) {
    super();

    this.supportedLanguages$ = this.store.select(selectSupportedLanguages);

    this.availableDataDomains$ = combineLatest([
      this.store.select(selectAvailableDataDomainsWithAllEntry),
      this.translateService.selectTranslate(ALL_DATA_DOMAINS)
    ]).pipe(map(([dataDomains, msg]) => {
      const dataDomainsCopy = [...dataDomains];
      dataDomainsCopy.forEach(dataDomain => {
        if (dataDomain.key === ALL_DATA_DOMAINS) {
          dataDomain.label = msg;
        }
      })
      return dataDomainsCopy;
    }));
    this.editedFaq$ = this.getEditedFaq();
  }

  navigateToFaqList() {
    this.store.dispatch(navigate({url: 'faq-management'}));
  }

  saveFaq(editedFaq: Faq) {
    const faqToBeSaved = {id: editedFaq.id} as any;
    const formFaq = this.faqForm.getRawValue() as any;
    faqToBeSaved.title = formFaq.title;
    faqToBeSaved.messages = formFaq.languages;
    if (formFaq.dataDomain !== ALL_DATA_DOMAINS) {
      faqToBeSaved.contextKey = formFaq.dataDomain;
    }
    this.store.dispatch(saveChangesToFaq({faq: faqToBeSaved}));
  }

  openDeletePopup(editedFaq: Faq) {
    this.store.dispatch(showDeleteFaqPopup({faq: editedFaq}));
  }

  getDeletionAction() {
    return deleteEditedFaq();
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  getMessage(language: string): FormControl {
    const languagesGroup = this.faqForm.get('languages') as FormGroup;
    const languageForm = languagesGroup.get(language) as FormGroup;
    return languageForm.get('message') as FormControl;
  }

  getTitle(language: string): FormControl {
    const languagesGroup = this.faqForm.get('languages') as FormGroup;
    const languageForm = languagesGroup.get(language) as FormGroup;
    return languageForm.get('title') as FormControl;
  }

  getErrorMessageForTitle(language: string): string {
    const titleControl = this.getTitle(language);
    if (titleControl?.errors?.['required']) {
      return 'First name is required';
    }
    if (titleControl?.errors?.['minlength']) {
      return `First name must be at least ${titleControl.errors['minlength'].requiredLength} characters long`;
    }
    if (titleControl?.errors?.['maxlength']) {
      return `First name cannot be more than ${titleControl.errors['maxlength'].requiredLength} characters long`;
    }

    // Add other error checks as needed
    return '';
  }

  getHeaderNameWithStatus(language: string) {
    this.translateService.selectTranslate(ALL_DATA_DOMAINS);
    return language.slice(0, 2).toUpperCase()
  }

  notFilled(language: string): boolean {
    const languagesGroup = this.faqForm.get('languages') as FormGroup;
    const languageForm = languagesGroup?.get(language) as FormGroup;

    const messageControl = languageForm?.get('message') as FormControl;
    return !messageControl || messageControl.value === null || messageControl.value === undefined || messageControl.value.trim() === '';
  }

  private getEditedFaq() {
    return combineLatest([
      this.store.select(selectEditedFaq),
      this.store.select(selectSupportedLanguages).pipe(take(1))
    ]).pipe(
      tap(([faq, supportedLanguages]) => {
        const faqCpy = JSON.parse(JSON.stringify(faq));

        const languageFaqFormGroups: { [key: string]: FormGroup } = {};

        supportedLanguages.forEach((language) => {
          if (!faqCpy.messages) {
            faqCpy.messages = {};
            faqCpy.messages[language] = {title: '', message: ''}
          }
          languageFaqFormGroups[language] = this.fb.group({
            title: [faqCpy?.messages?.[language]?.title || '', [Validators.required, Validators.minLength(3)]],
            message: [faqCpy?.messages?.[language]?.message || '', [Validators.required, Validators.minLength(3)]],
          });
        });

        this.faqForm = this.fb.group({
          languages: this.fb.group(languageFaqFormGroups),
          dataDomain: [faqCpy && faqCpy.contextKey ? faqCpy.contextKey : ALL_DATA_DOMAINS],
        });

        if (faqCpy.id) {
          this.createEditFaqBreadcrumbs();
        } else {
          this.createCreateFaqBreadcrumbs();
        }

        this.unsubFormValueChanges();
        this.formValueChangedSub = this.faqForm.valueChanges.subscribe(newValues => {
          this.onChange(faqCpy);
        });
      }),
      map(([faqCpy]) => faqCpy)
    );
  }

  private createCreateFaqBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.faqManagement.label,
          routerLink: naviElements.faqManagement.path,
        },
        {
          label: naviElements.faqCreate.label,
        }
      ]
    }));
  }

  private createEditFaqBreadcrumbs() {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.faqManagement.label,
          routerLink: naviElements.faqManagement.path,
        },
        {
          label: naviElements.faqEdit.label,
        }
      ]
    }));
  }

  private onChange(editedFaq: Faq) {
    const faqToBeSaved = {id: editedFaq.id} as any;
    const formFaq = this.faqForm.getRawValue() as any;
    faqToBeSaved.title = formFaq.title;
    faqToBeSaved.message = formFaq.message;
    if (formFaq.dataDomain !== ALL_DATA_DOMAINS) {
      faqToBeSaved.contextKey = formFaq.dataDomain;
    }
    this.store.dispatch(markUnsavedChanges({
      action: saveChangesToFaq(faqToBeSaved),
      stayOnPage: faqToBeSaved.id === undefined
    }));
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
