import {createAction, props} from "@ngrx/store";

export enum QueriesActionType {
    LOAD_QUERIES_PAGINATED = '[QUERIES] Load QUERIES Paginated',
    LOAD_QUERIES_SUCCESS = '[QUERIES] Load QUERIES success',
    RESET_QUERY_STATE = '[QUERIES] Reset QUERIES state',
}

export const loadQueriesSuccess = createAction(
    QueriesActionType.LOAD_QUERIES_SUCCESS,
    props<{ queries: any[], totalElements: number, totalPages: number }>()
);

export const loadQueriesPaginated = createAction(
    QueriesActionType.LOAD_QUERIES_PAGINATED,
    props<{ page: number, size: number, sort: string, search: string, contextKey: string }>()
);

export const resetQueriesState = createAction(
    QueriesActionType.RESET_QUERY_STATE,
);
