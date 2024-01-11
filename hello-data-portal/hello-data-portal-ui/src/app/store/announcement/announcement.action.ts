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

import {Action} from "@ngrx/store";
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


export class LoadAllAnnouncements implements Action {
  public readonly type = AnnouncementActionType.LOAD_ALL_ANNOUNCEMENTS;
}

export class LoadAllAnnouncementsSuccess implements Action {
  public readonly type = AnnouncementActionType.LOAD_ALL_ANNOUNCEMENTS_SUCCESS;

  constructor(public payload: Announcement[]) {
  }
}

export class LoadPublishedAnnouncements implements Action {
  public readonly type = AnnouncementActionType.LOAD_PUBLISHED_ANNOUNCEMENTS;
}

export class LoadPublishedAnnouncementsSuccess implements Action {
  public readonly type = AnnouncementActionType.LOAD_PUBLISHED_ANNOUNCEMENTS_SUCCESS;

  constructor(public payload: Announcement[]) {
  }
}

export class OpenAnnouncementEdition implements Action {
  public readonly type = AnnouncementActionType.OPEN_ANNOUNCEMENT_EDITION;

  constructor(public announcement: Announcement = {}) {
  }
}

export class LoadAnnouncementById implements Action {
  public readonly type = AnnouncementActionType.LOAD_ANNOUNCEMENT_BY_ID;
}

export class LoadAnnouncementByIdSuccess implements Action {
  public readonly type = AnnouncementActionType.LOAD_ANNOUNCEMENT_BY_ID_SUCCESS;

  constructor(public announcement: Announcement) {
  }
}

export class SaveChangesToAnnouncement implements Action {
  public readonly type = AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT;

  constructor(public announcement: Announcement) {
  }
}

export class SaveChangesToAnnouncementSuccess implements Action {
  public readonly type = AnnouncementActionType.SAVE_CHANGES_TO_ANNOUNCEMENT_SUCCESS;

  constructor(public announcement: Announcement) {
  }
}

export class ShowDeleteAnnouncementPopup implements Action {
  public readonly type = AnnouncementActionType.SHOW_DELETE_ANNOUNCEMENT_POP_UP;

  constructor(public announcement: Announcement) {
  }
}

export class HideDeleteAnnouncementPopup implements Action {
  public readonly type = AnnouncementActionType.HIDE_DELETE_ANNOUNCEMENT_POP_UP;
}

export class DeleteAnnouncement implements Action {
  public readonly type = AnnouncementActionType.DELETE_ANNOUNCEMENT;

}

export class DeleteAnnouncementSuccess implements Action {
  public readonly type = AnnouncementActionType.DELETE_ANNOUNCEMENT_SUCCESS;

  constructor(public announcement: Announcement) {
  }
}

export class DeleteEditedAnnouncement implements Action {
  public readonly type = AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT;
}

export class DeleteEditedAnnouncementSuccess implements Action {
  public readonly type = AnnouncementActionType.DELETE_EDITED_ANNOUNCEMENT_SUCCESS;
}

export class MarkAnnouncementAsRead implements Action {
  public readonly type = AnnouncementActionType.MARK_ANNOUNCEMENT_AS_READ;

  constructor(public announcement: Announcement) {
  }
}


export type AnnouncementsActions =
  LoadAllAnnouncements | LoadAllAnnouncementsSuccess | LoadPublishedAnnouncements | LoadPublishedAnnouncementsSuccess |
  OpenAnnouncementEdition | LoadAnnouncementById | LoadAnnouncementByIdSuccess |
  SaveChangesToAnnouncement | SaveChangesToAnnouncementSuccess | ShowDeleteAnnouncementPopup | HideDeleteAnnouncementPopup |
  DeleteAnnouncement | DeleteAnnouncementSuccess | DeleteEditedAnnouncement | DeleteEditedAnnouncementSuccess |
  MarkAnnouncementAsRead
