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
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectUserForPopup} from "../../../../store/users-management/users-management.selector";
import {UserAction} from "../../../../store/users-management/users-management.model";
import {ConfirmationService, ConfirmEventType, PrimeTemplate} from "primeng/api";
import {TranslateService} from "../../../../shared/services/translate.service";
import {
  hideUserPopupAction,
  invokeActionFromUserPopup
} from "../../../../store/users-management/users-management.action";
import {AsyncPipe} from '@angular/common';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {Button} from 'primeng/button';
import {TranslocoPipe} from '@jsverse/transloco';

@Component({
  selector: 'app-actions-user-popup',
  templateUrl: './actions-user-popup.component.html',
  styleUrls: ['./actions-user-popup.component.scss'],
  imports: [ConfirmDialog, PrimeTemplate, Button, AsyncPipe, TranslocoPipe]
})
export class ActionsUserPopupComponent {
  private store = inject<Store<AppState>>(Store);
  private confirmationService = inject(ConfirmationService);
  private translateService = inject(TranslateService);

  selectUserForPopup$: Observable<any>;

  constructor() {
    this.selectUserForPopup$ = this.store.select(selectUserForPopup);
  }

  getActionTranslationKey(userForPopup: any) {
    return '@' + userForPopup.action;
  }

  getButtonTypeForAction(userForPopup: any) {
    switch (userForPopup.action) {
      case UserAction.ENABLE:
        return 'success';
      default:
        return 'danger'
    }
  }

  confirmAction(userForPopup: any) {
    if (userForPopup) {
      const msg = this.translateService.translate(this.getActionTranslationKey(userForPopup) + ' user question');
      this.confirmationService.confirm({
        message: msg,
        icon: 'fas fa-triangle-exclamation',
        accept: () => {
          this.store.dispatch(invokeActionFromUserPopup());
        },
        reject: (type: ConfirmEventType) => {
          this.store.dispatch(hideUserPopupAction());
        }
      });
    }
  }

}
