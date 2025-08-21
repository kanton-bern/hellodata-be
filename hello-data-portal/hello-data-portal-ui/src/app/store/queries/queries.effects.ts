import {Injectable} from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {catchError, of, switchMap} from "rxjs";
import {showError} from "../app/app.action";
import {loadQueriesPaginated, loadQueriesSuccess} from "./queries.action";
import {QueriesService} from "./queries.service";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";

@Injectable()
export class QueriesEffects {

  loadQueriesPaginated$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadQueriesPaginated),
      switchMap(({
                   page,
                   size,
                   sort,
                   search,
                   contextKey
                 }) => this._queriesService.getQueriesPaginated(contextKey, page, size, sort, search)),
      switchMap(response => of(loadQueriesSuccess({
        queries: response.content,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      }))),
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
