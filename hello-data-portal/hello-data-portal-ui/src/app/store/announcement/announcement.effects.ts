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

import {inject, Injectable} from "@angular/core";
import {Actions, createEffect, ofType} from "@ngrx/effects";
import {asyncScheduler, catchError, map, scheduled, switchMap, tap, withLatestFrom} from 'rxjs';
import {Store} from "@ngrx/store";
import {AppState} from "../app/app.state";
import {NotificationService} from "../../shared/services/notification.service";
import {
  deleteAnnouncement,
  deleteAnnouncementSuccess,
  deleteEditedAnnouncement,
  deleteEditedAnnouncementSuccess,
  hideDeleteAnnouncementPopup,
  loadAllAnnouncements,
  loadAllAnnouncementsSuccess,
  loadAnnouncementById,
  loadAnnouncementByIdSuccess,
  loadPublishedAnnouncementsFiltered,
  loadPublishedAnnouncementsFilteredSuccess,
  markAnnouncementAsRead,
  openAnnouncementEdition,
  saveChangesToAnnouncement,
  saveChangesToAnnouncementSuccess,
} from "./announcement.action";
import {AnnouncementService} from "./announcement.service";
import {selectParamAnnouncementId, selectSelectedAnnouncementForDeletion} from "./announcement.selector";
import {Announcement} from "./announcement.model";
import {clearUnsavedChanges} from "../unsaved-changes/unsaved-changes.actions";
import {navigate, showError} from "../app/app.action";

@Injectable()
export class AnnouncementEffects {
  private _actions$ = inject(Actions);
  private _store = inject<Store<AppState>>(Store);
  private _announcementService = inject(AnnouncementService);
  private _notificationService = inject(NotificationService);


  loadAllAnnouncements$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAllAnnouncements),
      switchMap(() => this._announcementService.getAllAnnouncements()),
      switchMap(result => scheduled([loadAllAnnouncementsSuccess({payload: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadPublishedAnnouncements$ = createEffect(() => {
      return this._actions$.pipe(
        ofType(loadPublishedAnnouncementsFiltered),
        switchMap(() => this._announcementService.getHiddenAnnouncements()),
        switchMap((hiddenAnnouncements) => this._announcementService.getPublishedAnnouncements().pipe(
          tap(publishedAnnouncements => {
            console.debug("published announcements", publishedAnnouncements)
          }),
          map(publishedAnnouncements => {
            return publishedAnnouncements.filter(publishedAnnouncement => {
              return !hiddenAnnouncements.some(hiddenAnnouncement => hiddenAnnouncement.id === publishedAnnouncement.id);
            });
          })
        )),
        switchMap((result) => scheduled([loadPublishedAnnouncementsFilteredSuccess({payload: result})], asyncScheduler)),
      )
    }
  );

  openAnnouncementEdition$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(openAnnouncementEdition),
      switchMap(action => {
        if (action.announcement.id) {
          return scheduled([navigate({url: `announcements-management/edit/${action.announcement.id}`})], asyncScheduler);
        }
        return scheduled([navigate({url: 'announcements-management/create'})], asyncScheduler);
      }),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  loadAnnouncementById$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(loadAnnouncementById),
      withLatestFrom(this._store.select(selectParamAnnouncementId)),
      switchMap(([action, announcementId]) => this._announcementService.getAnnouncementById(announcementId as string)),
      switchMap(result => scheduled([loadAnnouncementByIdSuccess({announcement: result})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  saveChangesToAnnouncement$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToAnnouncement),
      switchMap((action) => {
        return action.announcement.id
          ? this._announcementService.updateAnnouncement({
            id: action.announcement.id,
            published: action.announcement.published as boolean,
            messages: action.announcement.messages as any,
          }).pipe(
            tap(() => this._notificationService.success('@Announcement updated successfully')),
            map(() => saveChangesToAnnouncementSuccess({announcement: action.announcement}))
          )
          : this._announcementService.createAnnouncement({
            published: action.announcement.published as boolean,
            messages: action.announcement.messages as any,
          }).pipe(
            tap(() => this._notificationService.success('@Announcement added successfully')),
            map(() => saveChangesToAnnouncementSuccess({announcement: action.announcement}))
          )
      }),
    )
  });

  saveChangesToAnnouncementSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(saveChangesToAnnouncementSuccess),
      switchMap(action => scheduled([clearUnsavedChanges(), navigate({url: 'announcements-management'})], asyncScheduler)),
      catchError(e => scheduled([showError({error: e})], asyncScheduler))
    )
  });

  deleteAnnouncement$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteAnnouncement),
      withLatestFrom(this._store.select(selectSelectedAnnouncementForDeletion)),
      switchMap(([action, announcement]) => this._announcementService.deleteAnnouncementById((announcement as Announcement).id as string).pipe(
        map(() => deleteAnnouncementSuccess({announcement: announcement as Announcement})),
        catchError(e => scheduled([showError({error: e})], asyncScheduler))
      )),
    )
  });

  deleteAnnouncementSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteAnnouncementSuccess),
      tap(action => this._notificationService.success('@Announcement deleted successfully')),
      switchMap(() => scheduled([loadAllAnnouncements(), hideDeleteAnnouncementPopup()], asyncScheduler))
    )
  });

  deleteEditedAnnouncement$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedAnnouncement),
      withLatestFrom(this._store.select(selectSelectedAnnouncementForDeletion)),
      switchMap(([action, announcementToBeDeleted]) => {
          return this._announcementService.deleteAnnouncementById((announcementToBeDeleted as Announcement).id as string).pipe(
            map(() => deleteEditedAnnouncementSuccess()),
            catchError(e => scheduled([showError({error: e})], asyncScheduler))
          )
        }
      ),
    )
  });

  deleteEditedAnnouncementSuccess$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(deleteEditedAnnouncementSuccess),
      tap(action => this._notificationService.success('@Announcement deleted successfully')),
      switchMap(() => scheduled([navigate({url: 'announcements-management'}), hideDeleteAnnouncementPopup()], asyncScheduler))
    )
  });

  markAnnouncementAsRead$ = createEffect(() => {
    return this._actions$.pipe(
      ofType(markAnnouncementAsRead),
      switchMap(action => {
        return this._announcementService.hideAnnouncement(action.announcement).pipe(
          map(() => loadPublishedAnnouncementsFiltered()),
          catchError(e => scheduled([showError({error: e})], asyncScheduler))
        )
      })
    )
  });
}
