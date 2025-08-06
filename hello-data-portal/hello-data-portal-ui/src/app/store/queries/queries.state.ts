export interface QueriesState {
  queries: any[],
  currentPagination: any,
  queriesLoading: boolean,
  queriesTotalRecords: number,
}

export const initialQueriesState: QueriesState = {
  queries: [],
  currentPagination: null,
  queriesLoading: false,
  queriesTotalRecords: 0,
}
