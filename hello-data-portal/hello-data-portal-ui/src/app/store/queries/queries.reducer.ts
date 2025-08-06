import {createReducer, on} from "@ngrx/store";
import {initialQueriesState, QueriesState} from "./queries.state";
import {loadQueriesPaginated, loadQueriesSuccess, resetQueriesState} from "./queries.action";

export const queriesReducer = createReducer(
  initialQueriesState,
  on(loadQueriesPaginated, (state: QueriesState, {page, size, sort, search}): QueriesState => {
    return {
      ...state,
      queriesLoading: true,
      currentPagination: {
        page, size, sort, search
      }
    };
  }),
  on(loadQueriesSuccess, (state: QueriesState, {queries, totalElements, totalPages}): QueriesState => {
    return {
      ...state,
      queries,
      queriesLoading: false,
      queriesTotalRecords: totalElements,
    };
  }),
  on(resetQueriesState, (state: QueriesState): QueriesState => {
    return {
      ...state,
      queries: [],
    };
  })
);
