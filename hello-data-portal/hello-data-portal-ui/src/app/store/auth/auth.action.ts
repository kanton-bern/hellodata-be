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
import {CurrentUserAuthData} from "./auth.state";

export enum AuthActionType {
  LOGIN = '[AUTH] Login',
  LOGIN_COMPLETE = '[AUTH] Login complete',
  FETCH_PERMISSIONS_SUCCESS = '[AUTH] Fetch permissions success',
  LOGOUT = '[AUTH] Logout',
  CHECK_AUTH = '[AUTH] Check auth',
  CHECK_AUTH_COMPLETE = '[AUTH] Check auth complete',
  AUTH_ERROR = '[AUTH] Auth error',
  FETCH_CONTEXT_ROLES = '[AUTH] Fetch context roles',
  FETCH_CONTEXT_ROLES_SUCCESS = '[AUTH] Fetch context roles success',
}

export class Login implements Action {
  public readonly type = AuthActionType.LOGIN;
}

export class LoginComplete implements Action {
  public readonly type = AuthActionType.LOGIN_COMPLETE;

  constructor(public profile: any, public isLoggedIn: boolean, public accessToken: any) {
  }
}

export class FetchPermissionSuccess implements Action {
  public readonly type = AuthActionType.FETCH_PERMISSIONS_SUCCESS;

  constructor(public currentUserAuthData: CurrentUserAuthData) {
  }
}

export class Logout implements Action {
  public readonly type = AuthActionType.LOGOUT;
}

export class CheckAuth implements Action {
  public readonly type = AuthActionType.CHECK_AUTH;
}

export class CheckAuthComplete implements Action {
  public readonly type = AuthActionType.CHECK_AUTH_COMPLETE;

  constructor(public isLoggedIn: boolean, public accessToken: any, public profile: any) {
  }
}

export class AuthError implements Action {
  public readonly type = AuthActionType.AUTH_ERROR;

  constructor(public error: Error) {
  }
}

export class FetchContextRoles implements Action {
  public readonly type = AuthActionType.FETCH_CONTEXT_ROLES;
}

export class FetchContextRolesSuccess implements Action {
  public readonly type = AuthActionType.FETCH_CONTEXT_ROLES_SUCCESS;

  constructor(public contextRoles: any[]) {
  }
}


export type AuthActions =
  Login | LoginComplete | Logout | CheckAuth | CheckAuthComplete | AuthError | FetchPermissionSuccess | FetchContextRoles | FetchContextRolesSuccess
