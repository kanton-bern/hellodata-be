import {createAction, props} from "@ngrx/store";

export enum QueriesActionType {
  LOAD_QUERIES = '[QUERIES] Load QUERIES',
  LOAD_QUERIES_SUCCESS = '[QUERIES] Load QUERIES success',
  RESET_QUERY_STATE = '[QUERIES] Reset QUERIES state',
}

export const loadQueries = createAction(
  QueriesActionType.LOAD_QUERIES,
  props<{ contextKey: string }>()
);

export const loadQueriesSuccess = createAction(
  QueriesActionType.LOAD_QUERIES_SUCCESS,
  props<{ payload: any[] }>()
);

export const resetQueriesState = createAction(
  QueriesActionType.RESET_QUERY_STATE,
);
