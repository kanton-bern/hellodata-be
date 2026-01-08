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

import {Component, inject, input} from '@angular/core';
import {Action, Store} from "@ngrx/store";
import {Observable, tap, withLatestFrom} from "rxjs";
import {AppState} from "../../../../store/app/app.state";
import {selectSelectedFaqForDeletion} from "../../../../store/faq/faq.selector";
import {ConfirmationService, MessageService, PrimeTemplate} from "primeng/api";
import {TranslateService} from "../../../../shared/services/translate.service";
import {hideDeleteFaqPopup} from "../../../../store/faq/faq.action";
import {AsyncPipe} from '@angular/common';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {Button} from 'primeng/button';
import {TranslocoPipe} from '@jsverse/transloco';
import {Ripple} from "primeng/ripple";

@Component({
  selector: 'app-delete-faq-popup',
  templateUrl: './delete-faq-popup.component.html',
  styleUrls: ['./delete-faq-popup.component.scss'],
  providers: [ConfirmationService, MessageService],
  imports: [ConfirmDialog, PrimeTemplate, Button, AsyncPipe, TranslocoPipe, Ripple]
})
export class DeleteFaqPopupComponent {
  readonly action = input.required<Action>();
  faqToBeDeleted$: Observable<any>;
  private store = inject<Store<AppState>>(Store);
  private confirmationService = inject(ConfirmationService);
  private translateService = inject(TranslateService);

  constructor() {
    this.faqToBeDeleted$ = this.store.select(selectSelectedFaqForDeletion)
      .pipe(
        withLatestFrom(this.translateService.selectTranslate('@Delete faq question'))
      )
      .pipe(tap(([faqForDeletion, msg]) => {
        if (faqForDeletion) {
          this.confirmationService.confirm({
            message: msg,
            icon: 'fas fa-triangle-exclamation',
            accept: () => {
              this.deleteFaq();
            },
            reject: () => {
              this.hideDeletionPopup();
            }
          });
        } else {
          this.hideDeletionPopup();
        }
      }));
  }

  deleteFaq() {
    this.store.dispatch(this.action());
  }

  hideDeletionPopup(): void {
    this.store.dispatch(hideDeleteFaqPopup());
  }

}
