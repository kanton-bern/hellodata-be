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

import {AfterViewInit, Component, inject} from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectPublishedAndFilteredAnnouncements} from "../../../../store/announcement/announcement.selector";
import {combineLatest, debounceTime, Observable, tap} from "rxjs";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {
  loadPublishedAnnouncementsFiltered,
  markAnnouncementAsRead
} from "../../../../store/announcement/announcement.action";
import {DialogService, DynamicDialogRef} from "primeng/dynamicdialog";
import {
  PublishedAnnouncementsPopupComponent
} from "../published-announcements-popup/published-announcements-popup.component";
import {HideAllCurrentPublishedAnnouncementsService} from "../hide-all-current-published-announcements.service";
import {selectUrl} from "../../../../store/router/router.selectors";
import {naviElements} from "../../../../app-navi-elements";
import {take} from "rxjs/operators";
import {AsyncPipe} from '@angular/common';

@Component({
  providers: [DialogService],
  selector: 'app-published-announcements-wrapper',
  template: `
    @if (publishedAnnouncements$ | async) {
      <div>
      </div>
    }`,
  imports: [AsyncPipe]
})
export class PublishedAnnouncementsWrapperComponent implements AfterViewInit {
  private store = inject<Store<AppState>>(Store);
  dialogService = inject(DialogService);
  private hideAllCurrentAnnouncementsService = inject(HideAllCurrentPublishedAnnouncementsService);


  publishedAnnouncements$: Observable<any>;
  ref: DynamicDialogRef | null = null;

  constructor() {
    const store = this.store;

    this.publishedAnnouncements$ =
      combineLatest([
        this.store.select(selectPublishedAndFilteredAnnouncements),
        this.store.select(selectUrl)
      ]).pipe(
        debounceTime(700),
        tap(([announcements, currentUrl]) => {
          const skipAnnouncementsPopup =
            currentUrl.includes(naviElements.dataWarehouseViewer.path) || currentUrl.includes('advanced-analytics-viewer');
          if (skipAnnouncementsPopup || this.ref && announcements && announcements.length === 0) {
            this.ref?.close();
          } else if (!this.ref && announcements && announcements.length > 0) {
            this.openDialog(announcements);
          }
        }));
    store.dispatch(loadPublishedAnnouncementsFiltered());
  }

  ngAfterViewInit(): void {
    this.hide = this.hide.bind(this);
  }

  hide(announcement: Announcement): void {
    this.store.dispatch(markAnnouncementAsRead({announcement}));
  }

  private openDialog(announcements: Announcement[]) {
    this.ref = this.dialogService.open(PublishedAnnouncementsPopupComponent, {
      header: '',
      width: '90vw',
      contentStyle: {overflow: 'auto'},
      height: 'auto',
      closable: true,
      modal: true,
    });
    if (this.ref) {
      this.ref.onClose.subscribe(_ => {
        if (this.hideAllCurrentAnnouncementsService.hide) {
          for (const announcement of announcements) {
            this.hide(announcement);
          }
          this.hideAllCurrentAnnouncementsService.hide = false;
        }
      });
      this.ref.onClose.pipe(take(1)).subscribe(() => {
        if (this.hideAllCurrentAnnouncementsService.hide) {
          for (const announcement of announcements) {
            this.hide(announcement);
          }
          this.hideAllCurrentAnnouncementsService.hide = false;
        }
      });
    }
  }

}
