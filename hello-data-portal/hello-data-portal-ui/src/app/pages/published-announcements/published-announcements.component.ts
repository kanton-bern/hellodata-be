import {Component, OnInit} from "@angular/core";
import {DialogService} from "primeng/dynamicdialog";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {selectAllAnnouncementsByPublishedFlag} from "../../store/announcement/announcement.selector";
import {loadAllAnnouncements} from "../../store/announcement/announcement.action";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../app-navi-elements";
import {selectSelectedLanguage} from "../../store/auth/auth.selector";
import {Announcement} from "../../store/announcement/announcement.model";

@Component({
  providers: [DialogService],
  selector: 'app-published-announcements',
  templateUrl: './published-announcements.component.html',
  styleUrls: ['./published-announcements.component.scss']
})
export class PublishedAnnouncementsComponent implements OnInit {
  announcements$: Observable<any>;
  selectedLanguage$: Observable<any>;

  constructor(private store: Store<AppState>) {
    this.announcements$ = this.store.select(selectAllAnnouncementsByPublishedFlag(true));
    store.dispatch(loadAllAnnouncements());
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
  }

  getMessage(announcement: Announcement, selectedLanguage: any): string | undefined {
    return announcement?.messages?.[selectedLanguage.code];
  }

  ngOnInit(): void {
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.publishedAnnouncements.label,
          routerLink: naviElements.publishedAnnouncements.path
        }
      ]
    }));
  }
}
