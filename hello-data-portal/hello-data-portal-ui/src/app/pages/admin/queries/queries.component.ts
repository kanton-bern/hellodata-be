import {BaseComponent} from "../../../shared/components/base/base.component";
import {Component, OnInit} from "@angular/core";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {loadQueries} from "../../../store/queries/queries.action";
import {selectParamContextKey} from "../../../store/queries/queries.selector";
import {Observable, tap} from "rxjs";

@Component({
  templateUrl: 'queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent extends BaseComponent implements OnInit {

  paramContextKey$: Observable<string | undefined>;

  constructor(private store: Store<AppState>) {
    super();
    this.paramContextKey$ = this.store.select(selectParamContextKey).pipe(tap(contextKey => {
      this.store.dispatch(loadQueries({contextKey: contextKey as string}));

    }));
  }

  override ngOnInit(): void {
    super.ngOnInit();
    console.log('on initialized queries');
  }

}
