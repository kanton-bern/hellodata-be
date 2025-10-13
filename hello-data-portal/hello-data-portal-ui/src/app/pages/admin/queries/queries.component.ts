import {BaseComponent} from "../../../shared/components/base/base.component";
import {Component, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {loadQueriesPaginated, resetQueriesState} from "../../../store/queries/queries.action";
import {
  selectParamContextKey,
  selectQueries,
  selectQueriesLoading,
  selectQueriesTotalRecords
} from "../../../store/queries/queries.selector";
import {combineLatest, Observable, tap} from "rxjs";
import {naviElements} from "../../../app-navi-elements";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {
  selectAllAvailableDataDomains,
  selectSelectedDataDomain
} from "../../../store/my-dashboards/my-dashboards.selector";
import {Table, TableLazyLoadEvent} from "primeng/table";
import {map, take} from "rxjs/operators";
import {scrollToTop} from "../../../shared/services/view-helpers";
import {navigate} from "../../../store/app/app.action";

@Component({
  templateUrl: 'queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent extends BaseComponent implements OnInit, OnDestroy {

  paramContextKey$: Observable<any>;
  queries$: Observable<any>;
  queriesTotalRecords = 0;
  componentInitiated = false;
  queriesLoading$ = this.store.select(selectQueriesLoading);
  expandedRows = {};
  filterValue = '';
  first = 0;
  @ViewChild('dt') table!: Table;
  loadedQueriesForContextKey = '';
  selectedDataDomain$: Observable<any>;

  constructor(private store: Store<AppState>) {
    super();
    this.queries$ = combineLatest([
      this.store.select(selectQueries),
      this.store.select(selectQueriesTotalRecords)
    ]).pipe(
      tap(([_, queriesTotalRecords]) => {
        this.queriesTotalRecords = queriesTotalRecords;
      }),
      map(([queries, _]) => queries),
    );
    this.selectedDataDomain$ = this.store.select(selectSelectedDataDomain).pipe(tap((dataDomain) => {
      if (this.loadedQueriesForContextKey !== '' && dataDomain && (dataDomain.key !== '' && dataDomain.key !== this.loadedQueriesForContextKey)) {
        this.store.dispatch(navigate({url: 'home'}));
        this.store.dispatch(resetQueriesState());
      }
    }));
    this.paramContextKey$ =
      combineLatest([
        this.store.select(selectParamContextKey),
        this.store.select(selectAllAvailableDataDomains).pipe(take(1))
      ]).pipe(
        map(([contextKey, availableDataDomains]) => {
          this.first = 0;
          if (contextKey) {
            const dataDomain = availableDataDomains.filter(dataDomain => dataDomain.key === contextKey)[0];
            if (dataDomain) {
              this.createBreadcrumbs(dataDomain.name);
            }
            this.store.dispatch(loadQueriesPaginated({
              page: 0, size: 10, sort: 'dttm: desc', search: '', contextKey
            }));
          }
          return contextKey ? contextKey : '';
        }),
      );


  }

  ngOnDestroy(): void {
    this.store.dispatch(resetQueriesState());
  }

  loadQueries(event: TableLazyLoadEvent, contextKey: string) {
    this.store.dispatch(loadQueriesPaginated({
      page: event.first as number / (event.rows as number),
      size: event.rows as number,
      sort: event.sortField ? `${event.sortField}, ${event.sortOrder ? event.sortOrder > 0 ? 'asc' : 'desc' : ''}` : '',
      search: event.globalFilter ? event.globalFilter as string : '',
      contextKey
    }));
    this.componentInitiated = true;
    this.loadedQueriesForContextKey = contextKey;
    scrollToTop();
  }

  formatDuration(ms: number): string {
    const hours = Math.floor(ms / 3600000);
    const minutes = Math.floor((ms % 3600000) / 60000);
    const seconds = Math.floor((ms % 60000) / 1000);
    const millis = Math.floor(ms % 1000);

    const pad = (n: number, width = 2) => String(n).padStart(width, '0');

    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}.${pad(millis, 3)}`;
  }

  private createBreadcrumbs(dataDomainName: string): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.query.label,
        },
        {
          label: dataDomainName,
        }
      ]
    }));
  }

  protected readonly JSON = JSON;
}
