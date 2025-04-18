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

import {AppState} from "../app/app.state";
import {selectRouteParam} from "../router/router.selectors";
import {createSelector} from "@ngrx/store";
import {AnnouncementState} from "./announcement.state";
import {AuthState} from "../auth/auth.state";

const announcementsState = (state: AppState) => state.announcements;
const authState = (state: AppState) => state.auth;

export const selectParamAnnouncementId = selectRouteParam('announcementId');

export const selectAllAnnouncements = createSelector(
  announcementsState,
  (state: AnnouncementState) => state.allAnnouncements
);

export const selectAllAnnouncementsByPublishedFlag = (published: boolean) => createSelector(
  announcementsState,
  authState,
  (state: AnnouncementState, authState: AuthState) => [...state.allAnnouncements].filter(announcement => announcement.published === published).sort((a1, a2) => {
    const firstPublishedDate = a1.publishedDate ? a1.publishedDate : 0;
    const secondPublishedDate = a2.publishedDate ? a2.publishedDate : 0;
    return secondPublishedDate - firstPublishedDate;
  }).filter(a => a.messages && authState.defaultLanguage && a.messages[authState.defaultLanguage])
);

export const selectPublishedAndFilteredAnnouncements = createSelector(
  announcementsState,
  authState,
  (state: AnnouncementState, authState: AuthState) => {
    return [...state.publishedAnnouncementsFiltered].sort((a1, a2) => {
      const firstPublishedDate = a1.publishedDate ? a1.publishedDate : 0;
      const secondPublishedDate = a2.publishedDate ? a2.publishedDate : 0;
      return secondPublishedDate - firstPublishedDate;
    }).filter(a => a.messages && authState.defaultLanguage && a.messages[authState.defaultLanguage])
  }
);

export const selectEditedAnnouncement = createSelector(
  announcementsState,
  (state: AnnouncementState) => state.editedAnnouncement
);

export const selectSelectedAnnouncementForDeletion = createSelector(
  announcementsState,
  (state: AnnouncementState) => state.announcementForDeletion
);

