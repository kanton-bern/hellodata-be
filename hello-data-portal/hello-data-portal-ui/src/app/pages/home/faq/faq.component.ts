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
import {combineLatest, map, Observable} from "rxjs";
import {Faq} from "../../../store/faq/faq.model";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectFaq} from "../../../store/start-page/start-page.selector";
import {loadFaqStartPage} from "../../../store/start-page/start-page.action";
import {selectSelectedLanguage} from "../../../store/auth/auth.selector";

@Component({
  selector: 'app-faq',
  templateUrl: './faq.component.html',
  styleUrls: ['./faq.component.scss']
})
export class FaqComponent implements OnInit {
  faq$: Observable<GroupedFaq[]>;
  selectedLanguage$: Observable<any>;

  constructor(private store: Store<AppState>) {
    this.faq$ = this._getGroupedFaqs();
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
  }

  ngOnInit(): void {
    this.store.dispatch(loadFaqStartPage());
  }

  getTitle(faq: Faq, selectedLanguage: string): string | undefined {
    return faq?.messages?.[selectedLanguage]?.title;
  }

  getMessage(faq: Faq, selectedLanguage: string): string | undefined {
    return faq?.messages?.[selectedLanguage]?.message;
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
