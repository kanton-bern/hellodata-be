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

import {inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {asyncScheduler, Observable, scheduled} from "rxjs";
import {LineageDoc} from "./lineage-docs.model";
import {environment} from "../../../environments/environment";
import {selectCurrentUserPermissions} from "../auth/auth.selector";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {switchMap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})

export class LineageDocsService {
  protected httpClient = inject(HttpClient);
  private store = inject<Store<AppState>>(Store);

  dbtDocsCfg = environment.subSystemsConfig.dbtDocs;
  url = this.dbtDocsCfg.protocol + this.dbtDocsCfg.host + this.dbtDocsCfg.domain;
  baseDocsUrl = `${this.url}/api/projects-docs`;
  currentUserPermissions$: Observable<any>;

  constructor() {
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
  }

  public getProjectDocs(): Observable<LineageDoc[]> {
    return this.currentUserPermissions$.pipe(
      switchMap(permissions => {
        if (!permissions || permissions.length === 0) {
          return scheduled([[]], asyncScheduler);
        } else {
          return this.httpClient.get<LineageDoc[]>(`${this.baseDocsUrl}`);
        }
      })
    );
  }

  public getProjectPathUrl(uriComponentEncodedProjectPath: string): string {
    const decoded = decodeURIComponent(uriComponentEncodedProjectPath);
    return `${this.url}${decoded}`;
  }
}
