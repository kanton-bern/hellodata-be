import {Injectable} from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {catchError, of, switchMap} from "rxjs";
import {showError} from "../app/app.action";
import {loadQueries, loadQueriesSuccess} from "./queries.action";
import {QueriesService} from "./queries.service";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";

@Injectable()
export class QueriesEffects {
  loadQueries$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadQueries),
      switchMap((action) => this._queriesService.getQueries(action.contextKey)),
      switchMap(result => of(loadQueriesSuccess({payload: result}))),
      catchError(e => of(showError({error: e})))
    )
  });

  constructor(
    private _actions$: Actions,
    private _queriesService: QueriesService,
    private _store: Store<AppState>
  ) {
  }
}
