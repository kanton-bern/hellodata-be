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

import { Injectable, inject } from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {LineageDocsService} from "./lineage-docs.service";
import {catchError, of, switchMap, withLatestFrom} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {selectAvailableDataDomains} from "../my-dashboards/my-dashboards.selector";
import {DataDomain} from "../my-dashboards/my-dashboards.model";
import {LineageDoc} from "./lineage-docs.model";
import {showError} from "../app/app.action";
import {loadMyLineageDocs, loadMyLineageDocsSuccess} from "./lineage-docs.action";
import {processNavigation} from "../menu/menu.action";

@Injectable()
export class LineageDocsEffects {
  private _actions$ = inject(Actions);
  private _docsService = inject(LineageDocsService);
  private _store = inject<Store<AppState>>(Store);


  loadMyLineageDocs$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyLineageDocs),
      switchMap(() => this._docsService.getProjectDocs()),
      withLatestFrom(this._store.select(selectAvailableDataDomains)),
      switchMap(([result, dataDomains]) => of(loadMyLineageDocsSuccess({payload: this._enhanceResult(result, dataDomains)}))),
      catchError(e => of(showError({error: e})))
    )
  });

  loadMyLineageDocsSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadMyLineageDocsSuccess),
      switchMap(() => of(processNavigation({compactMode: false}))),
    )
  });

  private _enhanceResult(result: LineageDoc[], dataDomains: DataDomain[]): LineageDoc[] {
    if (result) {
      return result.map(ld => {
          return {
            ...ld,
            contextName: this._getContextName(ld, dataDomains)
          }
        }
      )
    }
    return [];
  }

  private _getContextName(ld: LineageDoc, dataDomains: DataDomain[]) {
    const matchingDataDomain = dataDomains.find(dd => dd.key && dd.key === ld.contextKey);
    return matchingDataDomain ? matchingDataDomain.name : ld.contextKey;
  }
}
