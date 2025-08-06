import {BaseComponent} from "../../../shared/components/base/base.component";
import {Component, OnInit} from "@angular/core";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {loadQueries, resetQueriesState} from "../../../store/queries/queries.action";
import {selectAllQueries, selectParamContextKey} from "../../../store/queries/queries.selector";
import {combineLatest, Observable, tap} from "rxjs";
import {naviElements} from "../../../app-navi-elements";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {selectAvailableDataDomains} from "../../../store/my-dashboards/my-dashboards.selector";

@Component({
  templateUrl: 'queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent extends BaseComponent implements OnInit {

  paramContextKey$: Observable<any>;
  queries$ = this.store.select(selectAllQueries);

  constructor(private store: Store<AppState>) {
    super();
    this.paramContextKey$ =
      combineLatest([
        this.store.select(selectParamContextKey),
        this.store.select(selectAvailableDataDomains)
      ]).pipe(
        tap(([contextKey, availableDataDomains]) => {
          if (contextKey) {
            this.store.dispatch(loadQueries({contextKey: contextKey}));
            const dataDomain = availableDataDomains.filter(dataDomain => dataDomain.key === contextKey)[0];
            this.createBreadcrumbs(dataDomain.name);
          } else {
            this.store.dispatch(resetQueriesState());
          }
        })
      );
  }

  formatChangedOn(changedOn: string[]) {
    const date = new Date(
      Number(changedOn[0]),                        // year
      Number(changedOn[1]) - 1,                    // month (0-based in JS)
      Number(changedOn[2]),                        // day
      Number(changedOn[3]),                        // hour
      Number(changedOn[4]),                        // minute
      Number(changedOn[5]),                        // second
      Math.floor(Number(changedOn[6]) / 1_000_000) // nanoseconds to milliseconds
    );
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

}
