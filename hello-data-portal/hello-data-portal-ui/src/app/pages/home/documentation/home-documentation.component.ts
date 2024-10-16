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

import {Component, EventEmitter, Output} from '@angular/core';
import {Observable, tap} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectDocumentation} from "../../../store/summary/summary.selector";
import {selectCurrentUserPermissions, selectSelectedLanguage} from "../../../store/auth/auth.selector";
import {Documentation} from "../../../store/summary/summary.model";

@Component({
  selector: 'app-home-documentation',
  templateUrl: './home-documentation.component.html',
  styleUrls: ['./home-documentation.component.scss']
})
export class HomeDocumentationComponent {
  @Output() rightSidebarVisible = new EventEmitter<boolean>();
  currentUserPermissions$: Observable<string[]>;
  documentation$: Observable<any>;
  summarySidebarVisible = false;
  overlaySidebarVisible = false;
  selectedLanguage$: Observable<any>;

  documentation: Documentation = {
    texts: {}
  };

  constructor(private store: Store<AppState>) {
    this.documentation$ = this.store.select(selectDocumentation).pipe(tap(documentation => {
      this.documentation = documentation as Documentation;
    }));
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
  }

  toggleSummaryPanel() {
    this.summarySidebarVisible = !this.summarySidebarVisible;
    this.rightSidebarVisible.emit(this.summarySidebarVisible);
  }

  openOverlaySidebar() {
    this.overlaySidebarVisible = true;
  }
}
