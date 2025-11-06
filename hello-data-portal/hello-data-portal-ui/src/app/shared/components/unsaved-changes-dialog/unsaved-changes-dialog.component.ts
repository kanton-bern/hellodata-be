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

import {Component, inject} from '@angular/core';
import {TranslocoPipe} from "@jsverse/transloco";
import {ConfirmDialog} from "primeng/confirmdialog";
import {AppState} from "../../../store/app/app.state";
import {Store} from "@ngrx/store";
import {selectStayOnPage} from "../../../store/unsaved-changes/unsaved-changes.selector";
import {AsyncPipe} from "@angular/common";
import {Button, ButtonDirective} from "primeng/button";
import {PrimeTemplate} from 'primeng/api';
import {Tooltip} from "primeng/tooltip";

@Component({
  selector: 'app-unsaved-changes-dialog',
  templateUrl: './unsaved-changes-dialog.component.html',
  styleUrls: ['./unsaved-changes-dialog.component.scss'],
  imports: [ConfirmDialog, PrimeTemplate, ButtonDirective, Button, TranslocoPipe, AsyncPipe, Tooltip]
})
export class UnsavedChangesDialogComponent {
  private store = inject<Store<AppState>>(Store);

  stayOnPage$ = this.store.select(selectStayOnPage);

}



