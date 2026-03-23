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

import {Component, inject, input, OnInit} from '@angular/core';
import {combineLatest, map, Observable} from "rxjs";
import {Faq} from "../../../store/faq/faq.model";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectFaq} from "../../../store/start-page/start-page.selector";
import {loadFaqStartPage} from "../../../store/start-page/start-page.action";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../../store/auth/auth.selector";
import {TranslateService} from "../../../shared/services/translate.service";
import {AsyncPipe} from '@angular/common';
import {Accordion, AccordionContent, AccordionHeader, AccordionPanel} from 'primeng/accordion';
import {MatomoTrackerDirective} from 'ngx-matomo-client';
import {TranslocoPipe} from '@jsverse/transloco';
import {Tooltip} from 'primeng/tooltip';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';

@Component({
  selector: 'app-faq',
  templateUrl: './faq.component.html',
  styleUrls: ['./faq.component.scss'],
  imports: [Accordion, AccordionPanel, AccordionHeader, MatomoTrackerDirective, AccordionContent, AsyncPipe, TranslocoPipe, Tooltip]
})
export class FaqComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);
  private readonly sanitizer = inject(DomSanitizer);

  title = input<string>('');
  faq$: Observable<GroupedFaq[]>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;

  constructor() {
    const store = this.store;

    this.faq$ = this._getGroupedFaqs();
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  ngOnInit(): void {
    this.store.dispatch(loadFaqStartPage());
  }

  getTitle(faq: Faq, selectedLanguage: string, defaultLanguage: any): string | undefined {
    const title = this.findMessage(faq, selectedLanguage)?.title;
    if (!title) {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + this.findMessage(faq, defaultLanguage)?.title
    }
    return title;
  }

  getMessage(faq: Faq, selectedLanguage: string, defaultLanguage: any): SafeHtml {
    const message = this.findMessage(faq, selectedLanguage)?.message;
    if (!message) {
      const fallback = '<p>' + this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '</p>' + (this.findMessage(faq, defaultLanguage)?.message || '');
      return this.sanitizer.bypassSecurityTrustHtml(fallback);
    }
    return this.sanitizer.bypassSecurityTrustHtml(message);
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

  private _getGroupedFaqs(): Observable<GroupedFaq[]> {
    return combineLatest([
      this.store.select(selectFaq),
    ]).pipe(
      map(([faqs]) => {
        const groupedFaqs: GroupedFaq[] = [];
        faqs.forEach(db => {
          const contextKey = db.contextKey ? db.contextKey : "ALL_DATA_DOMAINS";
          const contextName = db.contextName ? db.contextName : contextKey;
          if (groupedFaqs.filter(d => d.contextKey == contextKey).length == 0) {
            const items = {contextKey: contextKey, contextName: contextName, faqs: []} as GroupedFaq;
            groupedFaqs.push(items);
          }
          const faqGroup = groupedFaqs.find(element => {
            return element.contextKey == contextKey;
          });
          if (faqGroup) {
            faqGroup.faqs.push(db);
          }
        });
        return groupedFaqs;
      })
    )
  }

}

export interface GroupedFaq {
  contextKey: string;
  contextName: string;
  faqs: Faq[]
}
