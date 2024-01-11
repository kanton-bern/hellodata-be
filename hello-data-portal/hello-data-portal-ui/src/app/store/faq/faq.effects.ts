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
import {Router} from "@angular/router";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {select, Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {FaqService} from "./faq.service";
import {catchError, map, of, switchMap, tap, withLatestFrom} from "rxjs";
import {Navigate, ShowError} from "../app/app.action";
import {
  DeleteEditedFaq,
  DeleteEditedFaqSuccess,
  DeleteFaq,
  DeleteFaqSuccess,
  FaqActionType,
  HideDeleteFaqPopup,
  LoadFaq,
  LoadFaqById,
  LoadFaqByIdSuccess,
  LoadFaqSuccess,
  OpenFaqEdition,
  SaveChangesToFaq,
  SaveChangesToFaqSuccess
} from "./faq.action";
import {selectParamFaqId, selectSelectedFaqForDeletion} from "../faq/faq.selector";
import {Faq} from "../faq/faq.model";
import {ClearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";

@Injectable()
export class FaqEffects {
  loadAllFaq$ = createEffect(() => this._actions$.pipe(
    ofType<LoadFaq>(FaqActionType.LOAD_FAQ),
    switchMap(() => this._faqService.getFaq()),
    switchMap(result => of(new LoadFaqSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  openFaqEdition$ = createEffect(() => this._actions$.pipe(
    ofType<OpenFaqEdition>(FaqActionType.OPEN_FAQ_EDITION),
    switchMap(action => {
      if (action.faq.id) {
        return of(new Navigate(`faq-management/edit/${action.faq.id}`));
      }
      return of(new Navigate('faq-management/create'));
    }),
    catchError(e => of(new ShowError(e)))
  ));

  loadFaqById$ = createEffect(() => this._actions$.pipe(
    ofType<LoadFaqById>(FaqActionType.LOAD_FAQ_BY_ID),
    withLatestFrom(this._store.pipe(select(selectParamFaqId))),
    switchMap(([action, faqId]) => this._faqService.getFaqById(faqId as string)),
    switchMap(result => of(new LoadFaqByIdSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  saveChangesToFaq$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToFaq>(FaqActionType.SAVE_CHANGES_TO_FAQ),
    switchMap((action: SaveChangesToFaq) => {
      return action.faq.id
        ? this._faqService.updateFaq({
          id: action.faq.id,
          title: action.faq.title as string,
          message: action.faq.message as string,
          contextKey: action.faq.contextKey as string
        }).pipe(
          tap(() => this._notificationService.success('@Faq updated successfully')),
          map(() => new SaveChangesToFaqSuccess(action.faq))
        )
        : this._faqService.createFaq({
          title: action.faq.title as string,
          message: action.faq.message as string,
          contextKey: action.faq.contextKey as string
        }).pipe(
          tap(() => this._notificationService.success('@Faq added successfully')),
          map(() => new SaveChangesToFaqSuccess(action.faq))
        )
    }),
  ));

  saveChangesToFaqSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToFaqSuccess>(FaqActionType.SAVE_CHANGES_TO_FAQ_SUCCESS),
    switchMap(action => of(new ClearUnsavedChanges(), new Navigate('faq-management'))),
    catchError(e => of(new ShowError(e)))
  ));

  deleteFaq$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteFaq>(FaqActionType.DELETE_FAQ),
    withLatestFrom(this._store.pipe(select(selectSelectedFaqForDeletion))),
    switchMap(([action, faq]) => this._faqService.deleteFaqById((faq as Faq).id as string).pipe(
      map(() => new DeleteFaqSuccess(faq as Faq)),
      catchError(e => of(new ShowError(e)))
    )),
  ));

  deleteFaqSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteFaqSuccess>(FaqActionType.DELETE_FAQ_SUCCESS),
    tap(action => this._notificationService.success('@Faq deleted successfully')),
    switchMap(() => of(new LoadFaq(), new HideDeleteFaqPopup()))
  ));

  deleteEditedFaq$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedFaq>(FaqActionType.DELETE_EDITED_FAQ),
    withLatestFrom(this._store.pipe(select(selectSelectedFaqForDeletion))),
    switchMap(([action, faqToBeDeleted]) => {
        return this._faqService.deleteFaqById((faqToBeDeleted as Faq).id as string).pipe(
          map(() => new DeleteEditedFaqSuccess()),
          catchError(e => of(new ShowError(e)))
        )
      }
    ),
  ));

  deleteEditedFaqSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedFaqSuccess>(FaqActionType.DELETE_EDITED_FAQ_SUCCESS),
    tap(action => this._notificationService.success('@Faq deleted successfully')),
    switchMap(() => of(new Navigate('faq-management'), new HideDeleteFaqPopup()))
  ));

  constructor(
    private _router: Router,
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _faqService: FaqService,
    private _notificationService: NotificationService
  ) {
  }
}
