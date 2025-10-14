import {createReducer, on} from "@ngrx/store";
import {initialQueriesState, QueriesState} from "./queries.state";
import {loadQueriesPaginated, loadQueriesSuccess, resetQueriesState} from "./queries.action";

export const queriesReducer = createReducer(
  initialQueriesState,
  on(loadQueriesPaginated, (state: QueriesState): QueriesState => {
    return {
      ...state,
      queries: [],
      queriesLoading: true,
    };
  }),
  on(loadQueriesSuccess, (state: QueriesState, {queries, totalElements, totalPages}): QueriesState => {
    return {
      ...state,
      queries,
      queriesLoading: false,
      queriesTotalRecords: totalElements,
      queriesTotalPages: totalPages,
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
