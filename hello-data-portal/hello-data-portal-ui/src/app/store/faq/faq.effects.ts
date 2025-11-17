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
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {FaqService} from "./faq.service";
import {asyncScheduler, catchError, map, scheduled, switchMap, tap, withLatestFrom} from "rxjs";
import {
  deleteEditedFaq,
  deleteEditedFaqSuccess,
  deleteFaq,
  deleteFaqSuccess,
  hideDeleteFaqPopup,
  loadFaq,
  loadFaqById,
  loadFaqByIdSuccess,
  loadFaqSuccess,
  openFaqEdition,
  saveChangesToFaq,
  saveChangesToFaqSuccess
} from "./faq.action";
import {selectParamFaqId, selectSelectedFaqForDeletion} from "../faq/faq.selector";
import {Faq, FaqMessage} from "../faq/faq.model";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError} from "../app/app.action";

@Injectable()
export class FaqEffects {
  private _actions$ = inject(Actions);
  private _store = inject<Store<AppState>>(Store);
  private _faqService = inject(FaqService);
  private _notificationService = inject(NotificationService);

  loadAllFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadFaq),
      switchMap(() => this._faqService.getFaq()),
      switchMap(result => scheduled([loadFaqSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });

  openFaqEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(openFaqEdition),
      switchMap(action => {
        if (action.faq.id) {
          return scheduled([navigate({url: `faq-management/edit/${action.faq.id}`})], asyncScheduler);
        }
        return scheduled([navigate({url: 'faq-management/create'})], asyncScheduler);
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });

  loadFaqById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadFaqById),
      withLatestFrom(this._store.select(selectParamFaqId)),
      switchMap(([action, faqId]) => this._faqService.getFaqById(faqId as string)),
      switchMap(result => scheduled([loadFaqByIdSuccess({faq: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });

  saveChangesToFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToFaq),
      switchMap((action) => {
        return action.faq.id
          ? this._faqService.updateFaq({
            id: action.faq.id,
            contextKey: action.faq.contextKey as string,
            messages: action.faq.messages as { [locale: string]: FaqMessage }
          }).pipe(
            tap(() => this._notificationService.success('@Faq updated successfully')),
            map(() => saveChangesToFaqSuccess({faq: action.faq}))
          )
          : this._faqService.createFaq({
            contextKey: action.faq.contextKey as string,
            messages: action.faq.messages as { [locale: string]: FaqMessage }
          }).pipe(
            tap(() => this._notificationService.success('@Faq added successfully')),
            map(() => saveChangesToFaqSuccess({faq: action.faq}))
          )
      }),
    )
  });

  saveChangesToFaqSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToFaqSuccess),
      switchMap(action => scheduled([clearUnsavedChanges(), navigate({url: 'faq-management'})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });

  deleteFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteFaq),
      withLatestFrom(this._store.select(selectSelectedFaqForDeletion)),
      switchMap(([action, faq]) => this._faqService.deleteFaqById((faq as Faq).id as string).pipe(
        map(() => deleteFaqSuccess({faq: faq as Faq})),
        catchError(e => scheduled([showError({error: e})], asyncScheduler)),
      )),
    )
  });

  deleteFaqSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteFaqSuccess),
      tap(action => this._notificationService.success('@Faq deleted successfully')),
      switchMap(() => scheduled([loadFaq(), hideDeleteFaqPopup()], asyncScheduler)),
    )
  });

  deleteEditedFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedFaq),
      withLatestFrom(this._store.select(selectSelectedFaqForDeletion)),
      switchMap(([action, faqToBeDeleted]) => {
          return this._faqService.deleteFaqById((faqToBeDeleted as Faq).id as string).pipe(
            map(() => deleteEditedFaqSuccess()),
            catchError(e => scheduled([showError({error: e})], asyncScheduler)),
          )
        }
      ),
    )
  });

  deleteEditedFaqSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedFaqSuccess),
      tap(action => this._notificationService.success('@Faq deleted successfully')),
      switchMap(() => scheduled([navigate({url: 'faq-management'}), hideDeleteFaqPopup()], asyncScheduler)),
    )
  });
}
