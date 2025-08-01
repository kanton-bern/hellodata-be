import {createAction, props} from "@ngrx/store";

export enum QueriesActionType {
  LOAD_QUERIES = '[QUERIES MANAGEMENT] Load QUERIES',
  LOAD_QUERIES_SUCCESS = '[QUERIES MANAGEMENT] Load QUERIES success',
}

export const loadQueries = createAction(
  QueriesActionType.LOAD_QUERIES,
  props<{ contextKey: string }>()
);

export const loadQueriesSuccess = createAction(
  QueriesActionType.LOAD_QUERIES_SUCCESS,
  props<{ payload: any[] }>()
);
