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

export enum AppActionType {
  APP_SHOW_INFO = '[APP] Show info',
  APP_SHOW_SUCCESS = '[APP] Show success',
  APP_SHOW_ERROR = '[APP] Show error',
  NAVIGATE = '[APP] Navigate',
}

export class ShowError implements Action {
  public readonly type = AppActionType.APP_SHOW_ERROR;

  constructor(public error: any) {
  }
}

export class ShowInfo implements Action {
  public readonly type = AppActionType.APP_SHOW_INFO;

  constructor(public message: string, public interpolateParams?: Record<string, unknown>) {
  }
}

export class ShowSuccess implements Action {
  public readonly type = AppActionType.APP_SHOW_SUCCESS;

  constructor(public message: string, public interpolateParams?: Record<string, unknown>) {
  }
}

export class Navigate implements Action {
  public readonly type = AppActionType.NAVIGATE;

  constructor(public url: string, public extras: any = {}) {
  }
}
