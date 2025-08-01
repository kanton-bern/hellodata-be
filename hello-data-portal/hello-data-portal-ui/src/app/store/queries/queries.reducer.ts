import {createReducer, on} from "@ngrx/store";
import {initialQueriesState, QueriesState} from "./queries.state";
import {loadQueriesSuccess} from "./queries.action";

export const queriesReducer = createReducer(
  initialQueriesState,
  on(loadQueriesSuccess, (state: QueriesState, {payload}): QueriesState => {
    return {
      ...state,
      queries: payload,
    };
  })
);
