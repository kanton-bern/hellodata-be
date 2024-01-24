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

import {Injectable} from "@angular/core";
import {Actions, concatLatestFrom, createEffect, ofType} from "@ngrx/effects";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {FaqService} from "./faq.service";
import {catchError, map, of, switchMap, tap} from "rxjs";
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
import {Faq} from "../faq/faq.model";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError} from "../app/app.action";

@Injectable()
export class FaqEffects {
  loadAllFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadFaq),
      switchMap(() => this._faqService.getFaq()),
      switchMap(result => of(loadFaqSuccess({payload: result}))),
      catchError(e => of(showError(e)))
    )
  });

  openFaqEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(openFaqEdition),
      switchMap(action => {
        if (action.faq.id) {
          return of(navigate({url: `faq-management/edit/${action.faq.id}`}));
        }
        return of(navigate({url: 'faq-management/create'}));
      }),
      catchError(e => of(showError(e)))
    )
  });

  loadFaqById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadFaqById),
      concatLatestFrom(() => this._store.select(selectParamFaqId)),
      switchMap(([action, faqId]) => this._faqService.getFaqById(faqId as string)),
      switchMap(result => of(loadFaqByIdSuccess({faq: result}))),
      catchError(e => of(showError(e)))
    )
  });

  saveChangesToFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToFaq),
      switchMap((action) => {
        return action.faq.id
          ? this._faqService.updateFaq({
            id: action.faq.id,
            title: action.faq.title as string,
            message: action.faq.message as string,
            contextKey: action.faq.contextKey as string
          }).pipe(
            tap(() => this._notificationService.success('@Faq updated successfully')),
            map(() => saveChangesToFaqSuccess({faq: action.faq}))
          )
          : this._faqService.createFaq({
            title: action.faq.title as string,
            message: action.faq.message as string,
            contextKey: action.faq.contextKey as string
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
      switchMap(action => of(clearUnsavedChanges(), navigate({url: 'faq-management'}))),
      catchError(e => of(showError(e)))
    )
  });

  deleteFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteFaq),
      concatLatestFrom(() => this._store.select(selectSelectedFaqForDeletion)),
      switchMap(([action, faq]) => this._faqService.deleteFaqById((faq as Faq).id as string).pipe(
        map(() => deleteFaqSuccess({faq: faq as Faq})),
        catchError(e => of(showError(e)))
      )),
    )
  });

  deleteFaqSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteFaqSuccess),
      tap(action => this._notificationService.success('@Faq deleted successfully')),
      switchMap(() => of(loadFaq(), hideDeleteFaqPopup()))
    )
  });

  deleteEditedFaq$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedFaq),
      concatLatestFrom(() => this._store.select(selectSelectedFaqForDeletion)),
      switchMap(([action, faqToBeDeleted]) => {
          return this._faqService.deleteFaqById((faqToBeDeleted as Faq).id as string).pipe(
            map(() => deleteEditedFaqSuccess()),
            catchError(e => of(showError(e)))
          )
        }
      ),
    )
  });

  deleteEditedFaqSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedFaqSuccess),
      tap(action => this._notificationService.success('@Faq deleted successfully')),
      switchMap(() => of(navigate({url: 'faq-management'}), hideDeleteFaqPopup()))
    )
  });

  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _faqService: FaqService,
    private _notificationService: NotificationService
  ) {
  }
}
