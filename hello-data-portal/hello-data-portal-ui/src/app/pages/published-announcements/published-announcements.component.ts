import {Component} from "@angular/core";
import {DialogService} from "primeng/dynamicdialog";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {selectAllAnnouncementsByPublishedFlag} from "../../store/announcement/announcement.selector";
import {loadAllAnnouncements} from "../../store/announcement/announcement.action";

@Component({
  providers: [DialogService],
  selector: 'app-published-announcements',
  templateUrl: './published-announcements.component.html',
  styleUrls: ['./published-announcements.component.scss']
})
export class PublishedAnnouncementsComponent {
  announcements$: Observable<any>;

  constructor(private store: Store<AppState>) {
    this.announcements$ = this.store.select(selectAllAnnouncementsByPublishedFlag(true));
    store.dispatch(loadAllAnnouncements());
  }
}
