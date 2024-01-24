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

import {
  hideDeleteAnnouncementPopup,
  loadAllAnnouncementsSuccess,
  loadAnnouncementByIdSuccess,
  loadPublishedAnnouncementsSuccess,
  showDeleteAnnouncementPopup
} from "./announcement.action";
import {AnnouncementState, initialAnnouncementState} from "./announcement.state";
import {createReducer, on} from "@ngrx/store";


export const announcementReducer = createReducer(
  initialAnnouncementState,
  on(loadPublishedAnnouncementsSuccess, (state: AnnouncementState, {payload}): AnnouncementState => {
    return {
      ...state,
      publishedAnnouncements: payload
    };
  }),
  on(loadAllAnnouncementsSuccess, (state: AnnouncementState, {payload}): AnnouncementState => {
    return {
      ...state,
      allAnnouncements: payload,
      editedAnnouncement: {}
    };
  }),
  on(loadAnnouncementByIdSuccess, (state: AnnouncementState, {announcement}): AnnouncementState => {
    return {
      ...state,
      editedAnnouncement: announcement
    };
  }),
  on(showDeleteAnnouncementPopup, (state: AnnouncementState, {announcement}): AnnouncementState => {
    return {
      ...state,
      announcementForDeletion: announcement
    };
  }),
  on(hideDeleteAnnouncementPopup, (state: AnnouncementState): AnnouncementState => {
    return {
      ...state,
      announcementForDeletion: null
    };
  }),
);
