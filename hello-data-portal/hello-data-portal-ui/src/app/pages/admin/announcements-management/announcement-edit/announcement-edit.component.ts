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

import {Component, OnDestroy, OnInit} from '@angular/core';
import {Observable, Subscription, tap} from "rxjs";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectEditedAnnouncement} from "../../../../store/announcement/announcement.selector";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {Navigate} from "../../../../store/app/app.action";
import {CreateBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../../app-navi-elements";
import {MarkUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";
import {BaseComponent} from "../../../../shared/components/base/base.component";
import {deleteEditedAnnouncement, saveChangesToAnnouncement, showDeleteAnnouncementPopup} from "../../../../store/announcement/announcement.action";

@Component({
  selector: 'app-announcement-edit',
  templateUrl: './announcement-edit.component.html',
  styleUrls: ['./announcement-edit.component.scss']
})
export class AnnouncementEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedAnnouncement$: Observable<any>;
  announcementForm!: FormGroup;
  formValueChangedSub!: Subscription;

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
    super();
    this.editedAnnouncement$ = this.store.select(selectEditedAnnouncement);
  }

  override ngOnInit(): void {
    super.ngOnInit();
    this.editedAnnouncement$ = this.store.select(selectEditedAnnouncement).pipe(
      tap(announcement => {
        this.announcementForm = this.fb.group({
          message: [announcement?.message, [Validators.required.bind(this), Validators.minLength(3)]],
          published: [announcement.published ? announcement.published : false]
        });
        if (announcement.id) {
          this.store.dispatch(new CreateBreadcrumbs([
            {
              label: naviElements.announcementsManagement.label,
              routerLink: naviElements.announcementsManagement.path,
            },
            {
              label: naviElements.announcementEdit.label,
            }
          ]));
        } else {
          this.store.dispatch(new CreateBreadcrumbs([
            {
              label: naviElements.announcementsManagement.label,
              routerLink: naviElements.announcementsManagement.path,
            },
            {
              label: naviElements.announcementCreate.label,
            }
          ]));
        }
        this.unsubFormValueChanges();
        this.formValueChangedSub = this.announcementForm.valueChanges.subscribe(newValues => {
          this.onChange(announcement);
        });
      })
    );
  }

  navigateToAnnouncementList() {
    this.store.dispatch(new Navigate('announcements-management'));
  }

  saveAnnouncement(editedAnnouncement: Announcement) {
    const announcementToBeSaved = {...editedAnnouncement} as Announcement;
    const formAnnouncement = this.announcementForm.getRawValue() as Announcement;
    announcementToBeSaved.message = formAnnouncement.message;
    announcementToBeSaved.published = formAnnouncement.published;
    this.store.dispatch(saveChangesToAnnouncement({announcement: announcementToBeSaved}));
  }

  openDeletePopup(editedAnnouncement: Announcement) {
    this.store.dispatch(showDeleteAnnouncementPopup({announcement: editedAnnouncement}));
  }

  getDeletionAction() {
    return deleteEditedAnnouncement();
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private onChange(editedAnnouncement: Announcement) {
    const announcementToBeSaved = {...editedAnnouncement} as Announcement;
    const formAnnouncement = this.announcementForm.getRawValue() as Announcement;
    announcementToBeSaved.message = formAnnouncement.message;
    announcementToBeSaved.published = formAnnouncement.published;
    this.store.dispatch(new MarkUnsavedChanges(saveChangesToAnnouncement({announcement: announcementToBeSaved}), announcementToBeSaved.id === undefined));
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }
}
