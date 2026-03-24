///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {Component, inject} from '@angular/core';
import {Observable} from "rxjs";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {selectAllAnnouncements} from "../../../store/announcement/announcement.selector";
import {Announcement} from "../../../store/announcement/announcement.model";
import {naviElements} from "../../../app-navi-elements";
import {BaseComponent} from "../../../shared/components/base/base.component";
import {
  deleteAnnouncement,
  loadAllAnnouncements,
  openAnnouncementEdition,
  showDeleteAnnouncementPopup
} from "../../../store/announcement/announcement.action";
import {createBreadcrumbs} from "../../../store/breadcrumb/breadcrumb.action";
import {selectSelectedLanguage} from "../../../store/auth/auth.selector";
import {AsyncPipe, DatePipe} from '@angular/common';
import {Toolbar} from 'primeng/toolbar';
import {PrimeTemplate, SharedModule} from 'primeng/api';
import {Button} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {TableModule} from 'primeng/table';
import {Tooltip} from 'primeng/tooltip';
import {Card} from 'primeng/card';
import {DeleteAnnouncementPopupComponent} from './delete-announcement-popup/delete-announcement-popup.component';
import {TranslocoPipe} from '@jsverse/transloco';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';

@Component({
  selector: 'app-announcements-management',
  templateUrl: './announcements-management.component.html',
  styleUrls: ['./announcements-management.component.scss'],
  imports: [Toolbar, PrimeTemplate, Ripple, TableModule, Tooltip, SharedModule, Button, DeleteAnnouncementPopupComponent, AsyncPipe, DatePipe, TranslocoPipe, Card]
})
export class AnnouncementsManagementComponent extends BaseComponent {
  allAnnouncements$: Observable<Announcement[]>;
  selectedLanguage$: Observable<{ code: string; typeTranslationKey: string }>;
  expandedRows: { [key: string]: boolean } = {};

  private readonly store = inject<Store<AppState>>(Store);
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    super();
    this.allAnnouncements$ = this.store.select(selectAllAnnouncements);
    this.selectedLanguage$ = this.store.select(selectSelectedLanguage);
    this.store.dispatch(loadAllAnnouncements());
    this.store.dispatch(createBreadcrumbs({
      breadcrumbs: [
        {
          label: naviElements.announcementsManagement.label,
          routerLink: naviElements.announcementsManagement.path,
        }
      ]
    }));
  }

  createAnnouncement() {
    this.store.dispatch(openAnnouncementEdition({announcement: {}}));
  }

  editAnnouncement(data: Announcement) {
    this.store.dispatch(openAnnouncementEdition({announcement: data}));
  }

  showAnnouncementDeletionPopup(data: Announcement) {
    this.store.dispatch(showDeleteAnnouncementPopup({announcement: data}));
  }

  getDeletionAction() {
    return deleteAnnouncement();
  }

  getMessage(announcement: Announcement, selectedLanguage: any): SafeHtml {
    const message = this.findAnnouncementMessage(announcement, selectedLanguage.code) || '';
    return this.sanitizer.bypassSecurityTrustHtml(message);
  }

  private findAnnouncementMessage(announcement: Announcement, code: string | null | undefined): string | undefined {
    if (!code || !announcement?.messages) return undefined;
    const exact = announcement.messages[code];
    if (exact) return exact;
    const prefix = code.slice(0, 2).toLowerCase();
    const matchedKey = Object.keys(announcement.messages).find(k => k.slice(0, 2).toLowerCase() === prefix);
    return matchedKey ? announcement.messages[matchedKey] : undefined;
  }

  getTranslations(announcement: Announcement): { locale: string; displayLocale: string; message: SafeHtml }[] {
    if (!announcement?.messages) {
      return [];
    }
    return Object.entries(announcement.messages).map(([locale, message]) => ({
      locale,
      displayLocale: locale.split('_')[0].toUpperCase(),
      message: this.sanitizer.bypassSecurityTrustHtml(message || '')
    }));
  }
}
