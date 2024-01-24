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

import {createAction, props} from "@ngrx/store";
import {Announcement} from "./announcement.model";

export enum AnnouncementActionType {
  //published announcements
  LOAD_PUBLISHED_ANNOUNCEMENTS = '[ANNOUNCEMENTS] Load ACTIVE ANNOUNCEMENTS',
  LOAD_PUBLISHED_ANNOUNCEMENTS_SUCCESS = '[ANNOUNCEMENTS] Load ACTIVE ANNOUNCEMENTS SUCCESS',
  MARK_ANNOUNCEMENT_AS_READ = '[ANNOUNCEMENTS] Mark the announcement as read',

  //management
  LOAD_ALL_ANNOUNCEMENTS = '[ANNOUNCEMENTS MANAGEMENT] Load all ANNOUNCEMENTS',
  LOAD_ALL_ANNOUNCEMENTS_SUCCESS = '[ANNOUNCEMENTS MANAGEMENT] Load ANNOUNCEMENTS SUCCESS',
  OPEN_ANNOUNCEMENT_EDITION = '[ANNOUNCEMENTS MANAGEMENT] Open ANNOUNCEMENT edition',
  LOAD_ANNOUNCEMENT_BY_ID = '[ANNOUNCEMENTS MANAGEMENT] Load ANNOUNCEMENT by id',
  LOAD_ANNOUNCEMENT_BY_ID_SUCCESS = '[ANNOUNCEMENTS MANAGEMENT] Load ANNOUNCEMENT by id success',
  SAVE_CHANGES_TO_ANNOUNCEMENT = '[ANNOUNCEMENTS MANAGEMENT] Save changes to ANNOUNCEMENT',
  SAVE_CHANGES_TO_ANNOUNCEMENT_SUCCESS = '[ANNOUNCEMENTS MANAGEMENT] Save changes to ANNOUNCEMENT success',
  SHOW_DELETE_ANNOUNCEMENT_POP_UP = '[ANNOUNCEMENTS MANAGEMENT] Show delete ANNOUNCEMENT popup',
  HIDE_DELETE_ANNOUNCEMENT_POP_UP = '[ANNOUNCEMENTS MANAGEMENT] Hide delete ANNOUNCEMENT popup',
  DELETE_ANNOUNCEMENT = '[ANNOUNCEMENTS MANAGEMENT] Delete ANNOUNCEMENT',
  DELETE_ANNOUNCEMENT_SUCCESS = '[ANNOUNCEMENTS MANAGEMENT] Delete ANNOUNCEMENT success',
  DELETE_EDITED_ANNOUNCEMENT = '[ANNOUNCEMENTS MANAGEMENT] Delete edited ANNOUNCEMENT',
  DELETE_EDITED_ANNOUNCEMENT_SUCCESS = '[ANNOUNCEMENTS MANAGEMENT] Delete edited ANNOUNCEMENT success',
}


export const loadAllAnnouncements = createAction(
  AnnouncementActionType.LOAD_ALL_ANNOUNCEMENTS
);

export const loadAllAnnouncementsSuccess = createAction(
  AnnouncementActionType.LOAD_ALL_ANNOUNCEMENTS_SUCCESS,
  props<{ payload: Announcement[] }>()
);

export const loadPublishedAnnouncements = createAction(
  AnnouncementActionType.LOAD_PUBLISHED_ANNOUNCEMENTS
);

export const loadPublishedAnnouncementsSuccess = createAction(
  AnnouncementActionType.LOAD_PUBLISHED_ANNOUNCEMENTS_SUCCESS,
  props<{ payload: Announcement[] }>()
);

export const openAnnouncementEdition = createAction(
  AnnouncementActionType.OPEN_ANNOUNCEMENT_EDITION,
  props<{ announcement: Announcement }>()
);

export const loadAnnouncementById = createAction(
  AnnouncementActionType.LOAD_ANNOUNCEMENT_BY_ID
);

export const loadAnnouncementByIdSuccess = createAction(
  AnnouncementActionType.LOAD_ANNOUNCEMENT_BY_ID_SUCCESS,
  props<{ announcement: Announcement }>()
);

export const saveChangesToAnnouncement = createAction(
  AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT,
  props<{ announcement: Announcement }>()
);

export const saveChangesToAnnouncementSuccess = createAction(
  AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT_SUCCESS,
  props<{ announcement: Announcement }>()
);

export const showDeleteAnnouncementPopup = createAction(
  AnnouncementActionType.SHOW_DELETE_ANNOUNCEMENT_POP_UP,
  props<{ announcement: Announcement }>()
);

export const hideDeleteAnnouncementPopup = createAction(
  AnnouncementActionType.HIDE_DELETE_ANNOUNCEMENT_POP_UP
);

export const deleteAnnouncement = createAction(
  AnnouncementActionType.DELETE_ANNOUNCEMENT
);

export const deleteAnnouncementSuccess = createAction(
  AnnouncementActionType.DELETE_ANNOUNCEMENT_SUCCESS,
  props<{ announcement: Announcement }>()
);

export const deleteEditedAnnouncement = createAction(
  AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT
);

export const deleteEditedAnnouncementSuccess = createAction(
  AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT_SUCCESS
);

export const markAnnouncementAsRead = createAction(
  AnnouncementActionType.MARK_ANNOUNCEMENT_AS_READ,
  props<{ announcement: Announcement }>()
);
