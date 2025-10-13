export interface QueriesState {
  queries: any[],
  queriesLoading: boolean,
  queriesTotalRecords: number,
  queriesTotalPages: number
}

export const initialQueriesState: QueriesState = {
  queries: [],
  queriesLoading: false,
  queriesTotalRecords: 0,
  queriesTotalPages: 0,
}
