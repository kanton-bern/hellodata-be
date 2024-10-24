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
import {CurrentUserAuthData} from "./auth.state";

export enum AuthActionType {
  LOGIN = '[AUTH] Login',
  LOGIN_COMPLETE = '[AUTH] Login complete',
  FETCH_PERMISSIONS_SUCCESS = '[AUTH] Fetch permissions success',
  LOGOUT = '[AUTH] Logout',
  CHECK_AUTH = '[AUTH] Check auth',
  CHECK_PROFILE = '[AUTH] Check profile',
  CHECK_AUTH_COMPLETE = '[AUTH] Check auth complete',
  AUTH_ERROR = '[AUTH] Auth error',
  FETCH_CONTEXT_ROLES = '[AUTH] Fetch context roles',
  FETCH_CONTEXT_ROLES_SUCCESS = '[AUTH] Fetch context roles success',
  SET_SELECTED_LANGUAGE = '[AUTH] Set selected language',
  SET_DEFAULT_LANGUAGE = '[AUTH] Set default language',
  SET_AVAILABLE_LANGUAGES = '[AUTH] Set available languages',
  SET_AVAILABLE_LANGUAGES_SUCCESS = '[AUTH] Set available languages success',
  SET_ACTIVE_TRANSLOCO_LANGUAGE = '[AUTH] Set active transloco language',
}

export const login = createAction(
  AuthActionType.LOGIN
);

export const loginComplete = createAction(
  AuthActionType.LOGIN_COMPLETE,
  props<{ profile: any, isLoggedIn: boolean, accessToken: any }>()
);

export const fetchPermissionSuccess = createAction(
  AuthActionType.FETCH_PERMISSIONS_SUCCESS,
  props<{ currentUserAuthData: CurrentUserAuthData }>()
);

export const logout = createAction(
  AuthActionType.LOGOUT
);

export const checkAuth = createAction(
  AuthActionType.CHECK_AUTH
);

export const checkAuthComplete = createAction(
  AuthActionType.CHECK_AUTH_COMPLETE,
  props<{ isLoggedIn: boolean, accessToken: any, profile: any }>()
);

export const checkProfile = createAction(
  AuthActionType.CHECK_PROFILE
);

export const authError = createAction(
  AuthActionType.AUTH_ERROR,
  props<{ error: Error }>()
);

export const fetchContextRoles = createAction(
  AuthActionType.FETCH_CONTEXT_ROLES
);

export const fetchContextRolesSuccess = createAction(
  AuthActionType.FETCH_CONTEXT_ROLES_SUCCESS,
  props<{ contextRoles: any[] }>()
);

export const setSelectedLanguage = createAction(
  AuthActionType.SET_SELECTED_LANGUAGE,
  props<{ lang: string }>()
);

export const setActiveTranslocoLanguage = createAction(
  AuthActionType.SET_ACTIVE_TRANSLOCO_LANGUAGE,
);

export const setDefaultLanguage = createAction(
  AuthActionType.SET_DEFAULT_LANGUAGE,
  props<{ lang: string }>()
);

export const setAvailableLanguages = createAction(
  AuthActionType.SET_AVAILABLE_LANGUAGES,
  props<{ langs: string[] }>()
);

export const setAvailableLanguagesSuccess = createAction(
  AuthActionType.SET_AVAILABLE_LANGUAGES_SUCCESS
);
