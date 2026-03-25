import {Component, inject, OnInit} from "@angular/core";
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
import {AsyncPipe, DatePipe} from "@angular/common";
import {Toolbar} from "primeng/toolbar";
import {Editor} from "primeng/editor";
import {FormsModule} from "@angular/forms";
import {SharedModule} from "primeng/api";
import {TranslocoPipe} from "@jsverse/transloco";

import {Card} from 'primeng/card';

@Component({
  providers: [DialogService],
  selector: 'app-published-announcements',
  templateUrl: './published-announcements.component.html',
  styleUrls: ['./published-announcements.component.scss'],
  imports: [Toolbar, Editor, FormsModule, SharedModule, AsyncPipe, DatePipe, TranslocoPipe, Card]
})
export class PublishedAnnouncementsComponent implements OnInit {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly translateService = inject(TranslateService);

  announcements$: Observable<any>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;

  constructor() {
    const store = this.store;

    this.announcements$ = this.store.select(selectAllAnnouncementsByPublishedFlag(true));
    store.dispatch(loadAllAnnouncements());
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  getMessage(announcement: Announcement, selectedLanguage: string, defaultLanguage: string): string | undefined {
    const message = this.findMessage(announcement, selectedLanguage);
    if (!message || message.trim() === '') {
      const fallback = this.findMessage(announcement, defaultLanguage);
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + (fallback ?? '');
    }
    return message;
  }

  private findMessage(announcement: Announcement, code: string | null | undefined): string | undefined {
    if (!code || !announcement?.messages) return undefined;
    const exact = announcement.messages[code];
    if (exact) return exact;
    const prefix = code.slice(0, 2).toLowerCase();
    const matchedKey = Object.keys(announcement.messages).find(k => k.slice(0, 2).toLowerCase() === prefix);
    return matchedKey ? announcement.messages[matchedKey] : undefined;
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
