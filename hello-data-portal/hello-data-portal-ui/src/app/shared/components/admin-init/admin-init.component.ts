import {Component, NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {SharedModule} from "primeng/api";
import {AppState} from "../../../store/app/app.state";
import {Store} from "@ngrx/store";
import {combineLatest, Observable, tap} from "rxjs";
import {selectIsBusinessDomainAdmin, selectIsSuperuser} from "../../../store/auth/auth.selector";
import {
  loadSubsystemUsers,
  loadSubsystemUsersForDashboards,
  loadUsers
} from "../../../store/users-management/users-management.action";

@Component({
  selector: 'app-admin-init',
  templateUrl: 'admin-init.component.html',
  styleUrls: ['./admin-init.component.scss']
})
export class AdminInitComponent {
  initStuffForAdmin$: Observable<any>;

  constructor(private store: Store<AppState>) {
    this.initStuffForAdmin$ = combineLatest([
      this.store.select(selectIsSuperuser),
      this.store.select(selectIsBusinessDomainAdmin)
    ]).pipe(
      tap(([isSuperuser, isBusinessDomainAdmin]) => {
        if (isSuperuser) {
          console.log('im a superuser and loading subsystem users!')
          store.dispatch(loadSubsystemUsers());
          this.store.dispatch(loadUsers());
          this.store.dispatch(loadSubsystemUsersForDashboards());
        } else if (isBusinessDomainAdmin) {
          console.log('im business domain admin and loading users!')
          this.store.dispatch(loadUsers());
          this.store.dispatch(loadSubsystemUsersForDashboards());
        }
      })
    );
  }
}

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
  ],
  declarations: [AdminInitComponent],
  exports: [AdminInitComponent]
})
export class AdminInitModule {
}
