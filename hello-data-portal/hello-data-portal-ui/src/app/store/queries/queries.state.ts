export interface QueriesState {
  queries: any[],
  queriesLoading: boolean,
  queriesTotalRecords: number,
}

export interface CurrentQueryPagination {
  page: number,
  size: number,
  sort: string,
  search: string,
}

export const initialQueriesState: QueriesState = {
  queries: [],
  queriesLoading: false,
  queriesTotalRecords: 0,
}
