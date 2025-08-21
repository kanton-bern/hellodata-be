export interface QueriesState {
  queries: any[],
  currentQueryPaginationByContextKey: any,
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
  currentQueryPaginationByContextKey: {},
  queriesLoading: false,
  queriesTotalRecords: 0,
}
