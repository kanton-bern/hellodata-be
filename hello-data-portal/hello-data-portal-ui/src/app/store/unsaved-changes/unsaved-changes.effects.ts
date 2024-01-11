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

import {Actions, createEffect, ofType} from "@ngrx/effects";
import {catchError, of, switchMap, withLatestFrom} from "rxjs";
import {ShowError} from "../app/app.action";
import {select, Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {Injectable} from "@angular/core";
import {ClearUnsavedChanges, RunSaveAction, UnsavedChangesActionType} from "./unsaved-changes.actions";
import {selectActionToRun} from "./unsaved-changes.selector";

@Injectable()
export class UnsavedChangesEffects {
  runSaveAction = createEffect(() => this._actions$.pipe(
    ofType<RunSaveAction>(UnsavedChangesActionType.RUN_SAVE_ACTION),
    withLatestFrom(this._store.pipe(select(selectActionToRun))),
    switchMap(([result, actionToRun]) => {
      if (actionToRun) {
        return of(actionToRun, new ClearUnsavedChanges())
      }
      return of(new ClearUnsavedChanges());
    }),
    catchError(e => of(new ShowError(e), new ClearUnsavedChanges()))
  ));


  constructor(
    private _actions$: Actions,
    private _store: Store<AppState>,
  ) {
  }
}
