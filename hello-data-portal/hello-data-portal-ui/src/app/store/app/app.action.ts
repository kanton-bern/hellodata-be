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

export enum AppActionType {
  APP_SHOW_INFO = '[APP] Show info',
  APP_SHOW_SUCCESS = '[APP] Show success',
  APP_SHOW_ERROR = '[APP] Show error',
  NAVIGATE = '[APP] Navigate',
  NAVIGATE_TO_LIST = '[APP] Navigate to list',
  TRACK_EVENT = '[APP] Track event',
  OPEN_WINDOW = '[APP] Open window',
}

export const showError = createAction(
  AppActionType.APP_SHOW_ERROR,
  props<{ error: any }>()
);

export const showInfo = createAction(
  AppActionType.APP_SHOW_INFO,
  props<{ message: string, interpolateParams?: Record<string, unknown> }>()
);

export const showSuccess = createAction(
  AppActionType.APP_SHOW_SUCCESS,
  props<{ message: string, interpolateParams?: Record<string, unknown> }>()
);

export const navigate = createAction(
  AppActionType.NAVIGATE,
  props<{ url: string, extras?: any }>()
);

export const navigateToList = createAction(
  AppActionType.NAVIGATE_TO_LIST,
);

export const trackEvent = createAction(
  AppActionType.TRACK_EVENT,
  props<{ eventCategory: string, eventAction: string, eventName?: string, eventValue?: number }>()
);

export const openWindow = createAction(
  AppActionType.OPEN_WINDOW,
  props<{ url: string, target: string }>()
);
