import {createReducer, on} from "@ngrx/store";
import {initialQueriesState, QueriesState} from "./queries.state";
import {loadQueriesPaginated, loadQueriesSuccess, resetQueriesState} from "./queries.action";

export const queriesReducer = createReducer(
  initialQueriesState,
  on(loadQueriesPaginated, (state: QueriesState, {page, size, sort, search, contextKey}): QueriesState => {
    const newCurrentPagination = {...state.currentQueryPaginationByContextKey}
    newCurrentPagination[contextKey] = {page, size, sort, search};
    return {
      ...state,
      queriesLoading: true,
      currentQueryPaginationByContextKey: newCurrentPagination
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
      queriesLoading: false,
      queriesTotalRecords: 0
    };
  })
);
