import {inject, Injectable} from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {asyncScheduler, catchError, scheduled, switchMap} from "rxjs";
import {showError} from "../app/app.action";
import {loadQueriesPaginated, loadQueriesSuccess} from "./queries.action";
import {QueriesService} from "./queries.service";

@Injectable()
export class QueriesEffects {
  private _actions$ = inject(Actions);
  private _queriesService = inject(QueriesService);


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
      switchMap(response => scheduled([loadQueriesSuccess({
        queries: response.content,
        totalElements: response.totalElements,
        totalPages: response.totalPages
      })], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler)),
    )
  });
}
