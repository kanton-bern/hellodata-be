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

import {Injectable, OnDestroy} from "@angular/core";
import {LangDefinition, TranslateParams, Translation, TranslocoScope, TranslocoService} from "@jsverse/transloco";
import {Observable, Subscription, switchMap, tap} from "rxjs";
import {filter} from "rxjs/operators";
import {HttpClient} from "@angular/common/http";
import {PrimeNGConfig} from "primeng/api";
import {HashMap} from "@jsverse/transloco/lib/utils/type.utils";

@Injectable({
  providedIn: 'root'
})
export class TranslateService implements OnDestroy {

  private readonly loadSub: Subscription;
  private readonly eventSub: Subscription;

  constructor(private translocoService: TranslocoService, private http: HttpClient, private primengConfig: PrimeNGConfig) {
    const activeLang = translocoService.getActiveLang();
    this.loadSub = translocoService.load(activeLang).subscribe(() => console.debug('Loaded translations for ' + activeLang));
    this.eventSub = translocoService.events$.pipe(
      filter(event => event.type === 'langChanged'),
      switchMap(event => {
        const timestamp = new Date().getTime();
        const url = `./assets/i18n/primeng/${event.payload.langName}.json?ts=${timestamp}`;
        return this.http.get<Translation>(url).pipe(
          tap(primengTranslations => {
            this.primengConfig.setTranslation(primengTranslations);
          }));
      })
    ).subscribe();
  }

  public translate(key: TranslateParams, params?: HashMap, lang?: string): string {
    return this.translocoService.translate(key, params, lang);
  }

  public selectTranslate(key: TranslateParams, params?: HashMap, lang?: string | TranslocoScope, _isObject?: boolean): Observable<string> {
    return this.translocoService.selectTranslate(key, params, lang, _isObject);
  }

  public setActiveLang(lang: string) {
    this.translocoService.setActiveLang(lang);
  }

  public getDefaultLanguage() {
    return this.translocoService.getDefaultLang();
  }

  public getAvailableLangs(): string[] {
    const availableLangs = [...this.translocoService.getAvailableLangs()];
    if (Array.isArray(availableLangs) && typeof availableLangs[0] === 'string') {
      return (availableLangs as string[]).sort((a, b) => a.localeCompare(b));
    }

    return (availableLangs as LangDefinition[]).map(langDef => langDef.label).sort((a, b) => a.localeCompare(b));
  }

  ngOnDestroy(): void {
    if (this.loadSub) {
      this.loadSub.unsubscribe();
    }
    if (this.eventSub) {
      this.eventSub.unsubscribe();
    }
  }
}
