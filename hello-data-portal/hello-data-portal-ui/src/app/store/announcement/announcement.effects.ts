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

import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {catchError, map, of, switchMap, tap, withLatestFrom} from "rxjs";
import {Navigate, ShowError} from "../app/app.action";
import {
  AnnouncementActionType,
  DeleteAnnouncement,
  DeleteAnnouncementSuccess,
  DeleteEditedAnnouncement,
  DeleteEditedAnnouncementSuccess,
  HideDeleteAnnouncementPopup,
  LoadAllAnnouncements,
  LoadAllAnnouncementsSuccess,
  LoadAnnouncementById,
  LoadAnnouncementByIdSuccess,
  LoadPublishedAnnouncements,
  LoadPublishedAnnouncementsSuccess,
  MarkAnnouncementAsRead,
  OpenAnnouncementEdition,
  SaveChangesToAnnouncement,
  SaveChangesToAnnouncementSuccess
} from "./announcement.action";
import {AnnouncementService} from "./announcement.service";
import {selectParamAnnouncementId, selectSelectedAnnouncementForDeletion} from "./announcement.selector";
import {Announcement} from "./announcement.model";
import {ClearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";

@Injectable()
export class AnnouncementEffects {

  loadAllAnnouncements$ = createEffect(() => this._actions$.pipe(
    ofType<LoadAllAnnouncements>(AnnouncementActionType.LOAD_ALL_ANNOUNCEMENTS),
    switchMap(() => this._announcementService.getAllAnnouncements()),
    switchMap(result => of(new LoadAllAnnouncementsSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  loadPublishedAnnouncements$ = createEffect(() =>
    this._actions$.pipe(
      ofType<LoadPublishedAnnouncements>(AnnouncementActionType.LOAD_PUBLISHED_ANNOUNCEMENTS),
      switchMap(() => this._announcementService.getHiddenAnnouncements()),
      switchMap((hiddenAnnouncements) => this._announcementService.getPublishedAnnouncements().pipe(
        map(publishedAnnouncements => {
          return publishedAnnouncements.filter(publishedAnnouncement => {
            return !hiddenAnnouncements.some(hiddenAnnouncement => hiddenAnnouncement.id === publishedAnnouncement.id);
          });
        })
      )),
      switchMap((result) => of(new LoadPublishedAnnouncementsSuccess(result))),
    )
  );

  openAnnouncementEdition$ = createEffect(() => this._actions$.pipe(
    ofType<OpenAnnouncementEdition>(AnnouncementActionType.OPEN_ANNOUNCEMENT_EDITION),
    switchMap(action => {
      if (action.announcement.id) {
        return of(new Navigate(`announcements-management/edit/${action.announcement.id}`));
      }
      return of(new Navigate('announcements-management/create'));
    }),
    catchError(e => of(new ShowError(e)))
  ));

  loadAnnouncementById$ = createEffect(() => this._actions$.pipe(
    ofType<LoadAnnouncementById>(AnnouncementActionType.LOAD_ANNOUNCEMENT_BY_ID),
    withLatestFrom(this._store.select(selectParamAnnouncementId)),
    switchMap(([action, announcementId]) => this._announcementService.getAnnouncementById(announcementId as string)),
    switchMap(result => of(new LoadAnnouncementByIdSuccess(result))),
    catchError(e => of(new ShowError(e)))
  ));

  saveChangesToAnnouncement$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToAnnouncement>(AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT),
    switchMap((action: SaveChangesToAnnouncement) => {
      return action.announcement.id
        ? this._announcementService.updateAnnouncement({
          id: action.announcement.id,
          published: action.announcement.published as boolean,
          message: action.announcement.message as string,
        }).pipe(
          tap(() => this._notificationService.success('@Announcement updated successfully')),
          map(() => new SaveChangesToAnnouncementSuccess(action.announcement))
        )
        : this._announcementService.createAnnouncement({
          published: action.announcement.published as boolean,
          message: action.announcement.message as string,
        }).pipe(
          tap(() => this._notificationService.success('@Announcement added successfully')),
          map(() => new SaveChangesToAnnouncementSuccess(action.announcement))
        )
    }),
  ));

  saveChangesToAnnouncementSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<SaveChangesToAnnouncementSuccess>(AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT_SUCCESS),
    switchMap(action => of(new Navigate('announcements-management'), new ClearUnsavedChanges())),
    catchError(e => of(new ShowError(e)))
  ));

  deleteAnnouncement$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteAnnouncement>(AnnouncementActionType.DELETE_ANNOUNCEMENT),
    withLatestFrom(this._store.select(selectSelectedAnnouncementForDeletion)),
    switchMap(([action, announcement]) => this._announcementService.deleteAnnouncementById((announcement as Announcement).id as string).pipe(
      map(() => new DeleteAnnouncementSuccess(announcement as Announcement)),
      catchError(e => of(new ShowError(e)))
    )),
  ));

  deleteAnnouncementSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteAnnouncementSuccess>(AnnouncementActionType.DELETE_ANNOUNCEMENT_SUCCESS),
    tap(action => this._notificationService.success('@Announcement deleted successfully')),
    switchMap(() => of(new LoadAllAnnouncements(), new HideDeleteAnnouncementPopup()))
  ));

  deleteEditedAnnouncement$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedAnnouncement>(AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT),
    withLatestFrom(this._store.select(selectSelectedAnnouncementForDeletion)),
    switchMap(([action, announcementToBeDeleted]) => {
        return this._announcementService.deleteAnnouncementById((announcementToBeDeleted as Announcement).id as string).pipe(
          map(() => new DeleteEditedAnnouncementSuccess()),
          catchError(e => of(new ShowError(e)))
        )
      }
    ),
  ));

  deleteEditedAnnouncementSuccess$ = createEffect(() => this._actions$.pipe(
    ofType<DeleteEditedAnnouncementSuccess>(AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT_SUCCESS),
    tap(action => this._notificationService.success('@Announcement deleted successfully')),
    switchMap(() => of(new Navigate('announcements-management'), new HideDeleteAnnouncementPopup()))
  ));

  markAnnouncementAsRead$ = createEffect(() => this._actions$.pipe(
    ofType<MarkAnnouncementAsRead>(AnnouncementActionType.MARK_ANNOUNCEMENT_AS_READ),
    switchMap(action => {
      return this._announcementService.hideAnnouncement(action.announcement).pipe(
        map(() => new LoadPublishedAnnouncements()),
        catchError(e => of(new ShowError(e)))
      )
    })
  ));


  constructor(
    private _router: Router,
    private _actions$: Actions,
    private _store: Store<AppState>,
    private _announcementService: AnnouncementService,
    private _notificationService: NotificationService
  ) {
  }
}
