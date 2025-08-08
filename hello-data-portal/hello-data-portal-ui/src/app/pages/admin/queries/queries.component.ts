import {BaseComponent} from "../../../shared/components/base/base.component";
import {Component, OnInit} from "@angular/core";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {loadQueriesPaginated, resetQueriesState} from "../../../store/queries/queries.action";
import {
  selectAllQueries,
  selectParamContextKey,
  selectQueriesLoading,
  selectQueriesTotalRecords
} from "../../../store/queries/queries.selector";
import {combineLatest, Observable, tap} from "rxjs";
import {naviElements} from "../../../app-navi-elements";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {selectAvailableDataDomains} from "../../../store/my-dashboards/my-dashboards.selector";
import {TableLazyLoadEvent} from "primeng/table";
import {map, take} from "rxjs/operators";
import {scrollToTop} from "../../../shared/services/view-helpers";

@Component({
  templateUrl: 'queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent extends BaseComponent implements OnInit {

  paramContextKey$: Observable<any>;
  queries$: Observable<any>;
  queriesTotalRecords = 0;
  componentInitiated = false;
  queriesLoading$ = this.store.select(selectQueriesLoading);
  expandedRows = {};
  filterValue = '';

  constructor(private store: Store<AppState>) {
    super();
    this.paramContextKey$ =
      combineLatest([
        this.store.select(selectParamContextKey),
        this.store.select(selectAvailableDataDomains).pipe(take(2))
      ]).pipe(
        map(([contextKey, availableDataDomains]) => {
          const dataDomain = availableDataDomains.filter(dataDomain => dataDomain.key === contextKey)[0];
          if (contextKey && dataDomain) {
            this.createBreadcrumbs(dataDomain.name);
            if (this.componentInitiated) {
              this.store.dispatch(loadQueriesPaginated({
                page: 0, size: 10, sort: '', search: '', contextKey
              }));
            }
          } else {
            this.store.dispatch(resetQueriesState());
          }
          return dataDomain?.key ? dataDomain.key : '';
        }),
      );

    this.queries$ = combineLatest([
      this.store.select(selectAllQueries),
      this.store.select(selectQueriesTotalRecords)
    ]).pipe(
      tap(([_, usersTotalRecords]) => {
        this.queriesTotalRecords = usersTotalRecords;
      }),
      map(([queries, _]) => queries),
    );
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
    scrollToTop();
  }

  formatChangedOn(changedOn: number) {
    const date = new Date(changedOn * 1000);
    const humanReadable = date.toLocaleString();
    return date.toLocaleString();
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
