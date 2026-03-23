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
import {AppState} from "../../../store/app/app.state";
import {selectFilteredFaq} from "../../../store/faq/faq.selector";
import {Faq} from "../../../store/faq/faq.model";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {deleteFaq, loadFaq, openFaqEdition, showDeleteFaqPopup} from "../../../store/faq/faq.action";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../../store/auth/auth.selector";
import {AsyncPipe} from '@angular/common';
import {Toolbar} from 'primeng/toolbar';
import {PrimeTemplate, SharedModule} from 'primeng/api';
import {Button} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {Card} from 'primeng/card';
import {DeleteFaqPopupComponent} from './delete-faq-popup/delete-faq-popup.component';
import {TranslocoPipe} from '@jsverse/transloco';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';
import {TranslateService} from "../../../shared/services/translate.service";

@Component({
  selector: 'app-faq-list',
  templateUrl: './faq-list.component.html',
  styleUrls: ['./faq-list.component.scss'],
  imports: [Toolbar, PrimeTemplate, Ripple, TableModule, SharedModule, Button, Tooltip, DeleteFaqPopupComponent, AsyncPipe, TranslocoPipe, Card]
})
export class FaqListComponent extends BaseComponent implements OnInit {
  faq$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;
  expandedRows: { [key: string]: boolean } = {};
  private readonly store = inject<Store<AppState>>(Store);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly translateService = inject(TranslateService);

  constructor() {
    super();
    const store = this.store;

    this.faq$ = this.store.select(selectFilteredFaq);
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage);
    this.defaultLanguage$ = this.store.select(selectDefaultLanguage);
    store.dispatch(loadFaq());
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.faqManagement.label,
          routerLink: naviElements.faqManagement.path,
        }
      ]
    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }

  createFaq() {
    this.store.dispatch(openFaqEdition({faq: {}}));
  }

  editFaq(data: Faq) {
    this.store.dispatch(openFaqEdition({faq: data}));
  }

  showFaqDeletionPopup(data: Faq) {
    this.store.dispatch(showDeleteFaqPopup({faq: data}));
  }

  getDeletionAction() {
    return deleteFaq();
  }

  getTitle(faq: Faq, selectedLanguage: any, defaultLanguage: string): string | undefined {
    const code = selectedLanguage.code;
    const title = this.findMessage(faq, code)?.title;
    if (title) {
      return title;
    }
    const fallbackTitle = this.findMessage(faq, defaultLanguage)?.title;
    if (fallbackTitle) {
      return `${fallbackTitle} [${this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()})}]`;
    }
    return undefined;
  }

  /**
   * Finds a FAQ message by locale code, falling back to prefix matching
   * (e.g., 'de' matches 'de_CH') when exact match is not found.
   */
  private findMessage(faq: Faq, code: string | null | undefined) {
    if (!code || !faq?.messages) return undefined;
    const exact = faq.messages[code];
    if (exact) return exact;
    const prefix = code.slice(0, 2).toLowerCase();
    const matchedKey = Object.keys(faq.messages).find(k => k.slice(0, 2).toLowerCase() === prefix);
    return matchedKey ? faq.messages[matchedKey] : undefined;
  }


  getTranslations(faq: Faq): { locale: string; displayLocale: string; title: string; message: SafeHtml }[] {
    if (!faq?.messages) {
      return [];
    }
    return Object.entries(faq.messages).map(([locale, msg]) => ({
      locale,
      displayLocale: locale.split('_')[0].toUpperCase(),
      title: msg.title || '',
      message: this.sanitizer.bypassSecurityTrustHtml(msg.message || '')
    }));
  }
}
