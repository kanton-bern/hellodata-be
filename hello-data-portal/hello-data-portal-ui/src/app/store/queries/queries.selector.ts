import {selectRouteParam} from "../router/router.selectors";
import {AppState} from "../app/app.state";
import {createSelector} from "@ngrx/store";
import {QueriesState} from "./queries.state";

export const selectParamContextKey = selectRouteParam('contextKey');

const queriesState = (state: AppState) => state.queries;


export const selectAllQueries = createSelector(
    queriesState,
    (state: QueriesState) => state.queries
);

export const selectQueriesTotalRecords = createSelector(
    queriesState,
    (state: QueriesState) => state.queriesTotalRecords
);
