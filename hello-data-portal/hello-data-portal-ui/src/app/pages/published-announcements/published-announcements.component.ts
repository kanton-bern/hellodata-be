import {Component, OnInit} from "@angular/core";
import {DialogService} from "primeng/dynamicdialog";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable} from "rxjs";
import {loadAllAnnouncements} from "../../store/announcement/announcement.action";
import {createBreadcrumbs} from "../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../app-navi-elements";
import {selectDefaultLanguage, selectSelectedLanguage} from "../../store/auth/auth.selector";
import {Announcement} from "../../store/announcement/announcement.model";
import {selectAllAnnouncementsByPublishedFlag} from "../../store/announcement/announcement.selector";
import {TranslateService} from "../../shared/services/translate.service";

@Component({
  providers: [DialogService],
  selector: 'app-published-announcements',
  templateUrl: './published-announcements.component.html',
  styleUrls: ['./published-announcements.component.scss']
})
export class PublishedAnnouncementsComponent implements OnInit {
  announcements$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;

  constructor(private store: Store<AppState>, private translateService: TranslateService) {
    this.announcements$ = this.store.select(selectAllAnnouncementsByPublishedFlag(true));
    store.dispatch(loadAllAnnouncements());
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  getMessage(announcement: Announcement, selectedLanguage: string, defaultLanguage: any): string | undefined {
    const message = announcement?.messages?.[selectedLanguage];
    if (!message || message.trim() === '') {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + announcement?.messages?.[defaultLanguage];
    }
    return message;
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
