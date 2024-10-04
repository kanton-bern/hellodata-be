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

import {AuthState, initialAuthState} from "./auth.state";
import {
  fetchContextRolesSuccess,
  fetchPermissionSuccess,
  loginComplete,
  logout,
  setAvailableLanguages,
  setDefaultLanguage,
  setSelectedLanguage
} from "./auth.action";
import {createReducer, on} from "@ngrx/store";

export const authReducer = createReducer(
  initialAuthState,
  on(loginComplete, (state: AuthState, {profile, isLoggedIn}): AuthState => {
    return {
      ...state,
      profile: profile,
      isLoggedIn: isLoggedIn,
    };
  }),
  on(setDefaultLanguage, (state: AuthState, {lang}): AuthState => {
    return {
      ...state,
      defaultLanguage: lang,
    };
  }),
  on(setAvailableLanguages, (state: AuthState, {langs}): AuthState => {
    return {
      ...state,
      supportedLanguages: langs,
    };
  }),
  on(fetchPermissionSuccess, (state: AuthState, {currentUserAuthData}): AuthState => {
    return {
      ...state,
      permissions: currentUserAuthData.permissions,
      isSuperuser: currentUserAuthData.isSuperuser,
      businessDomain: currentUserAuthData.businessDomain,
      permissionsLoaded: true,
      disableLogout: currentUserAuthData.disableLogout,
      userDisabled: currentUserAuthData.userDisabled,
      selectedLanguage: currentUserAuthData.selectedLanguage
    };
  }),
  on(logout, (state: AuthState): AuthState => {
    return {
      ...state,
      profile: null,
      isLoggedIn: false,
      permissionsLoaded: false,
      contextRoles: [],
      permissions: [],
      isSuperuser: false,
      businessDomain: '',
      userDisabled: false
    };
  }),
  on(setSelectedLanguage, (state: AuthState, {lang}): AuthState => {
    return {
      ...state,
      selectedLanguage: lang
    };
  }),
  on(fetchContextRolesSuccess, (state: AuthState, {contextRoles}): AuthState => {
    return {
      ...state,
      contextRoles: contextRoles
    };
  }),
);
