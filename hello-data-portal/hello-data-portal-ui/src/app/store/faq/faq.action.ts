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
import {Faq} from "./faq.model";

export enum FaqActionType {
  //published announcements
  LOAD_FAQ = '[FAQ MANAGEMENT] Load FAQ',
  LOAD_FAQ_SUCCESS = '[FAQ MANAGEMENT] Load FAQ SUCCESS',

  OPEN_FAQ_EDITION = '[FAQ MANAGEMENT] Open FAQ edition',
  LOAD_FAQ_BY_ID = '[FAQ MANAGEMENT] Load FAQ by id',
  LOAD_FAQ_BY_ID_SUCCESS = '[FAQ MANAGEMENT] Load FAQ by id success',
  SAVE_CHANGES_TO_FAQ = '[FAQ MANAGEMENT] Save changes to FAQ',
  SAVE_CHANGES_TO_FAQ_SUCCESS = '[FAQ MANAGEMENT] Save changes to FAQ success',
  SHOW_DELETE_FAQ_POP_UP = '[FAQ MANAGEMENT] Show delete FAQ popup',
  HIDE_DELETE_FAQ_POP_UP = '[FAQ MANAGEMENT] Hide delete FAQ popup',
  DELETE_FAQ = '[FAQ MANAGEMENT] Delete FAQ',
  DELETE_FAQ_SUCCESS = '[FAQ MANAGEMENT] Delete FAQ success',
  DELETE_EDITED_FAQ = '[FAQ MANAGEMENT] Delete edited FAQ',
  DELETE_EDITED_FAQ_SUCCESS = '[FAQ MANAGEMENT] Delete edited FAQ success',
}

export class LoadFaq implements Action {
  public readonly type = FaqActionType.LOAD_FAQ;
}

export class LoadFaqSuccess implements Action {
  public readonly type = FaqActionType.LOAD_FAQ_SUCCESS;

  constructor(public payload: Faq[]) {
  }
}


export class OpenFaqEdition implements Action {
  public readonly type = FaqActionType.OPEN_FAQ_EDITION;

  constructor(public faq: Faq = {}) {
  }
}

export class LoadFaqById implements Action {
  public readonly type = FaqActionType.LOAD_FAQ_BY_ID;
}

export class LoadFaqByIdSuccess implements Action {
  public readonly type = FaqActionType.LOAD_FAQ_BY_ID_SUCCESS;

  constructor(public faq: Faq) {
  }
}

export class SaveChangesToFaq implements Action {
  public readonly type = FaqActionType.SAVE_CHANGES_TO_FAQ;

  constructor(public faq: Faq) {
  }
}

export class SaveChangesToFaqSuccess implements Action {
  public readonly type = FaqActionType.SAVE_CHANGES_TO_FAQ_SUCCESS;

  constructor(public faq: Faq) {
  }
}

export class ShowDeleteFaqPopup implements Action {
  public readonly type = FaqActionType.SHOW_DELETE_FAQ_POP_UP;

  constructor(public faq: Faq) {
  }
}

export class HideDeleteFaqPopup implements Action {
  public readonly type = FaqActionType.HIDE_DELETE_FAQ_POP_UP;
}

export class DeleteFaq implements Action {
  public readonly type = FaqActionType.DELETE_FAQ;

}

export class DeleteFaqSuccess implements Action {
  public readonly type = FaqActionType.DELETE_FAQ_SUCCESS;

  constructor(public faq: Faq) {
  }
}

export class DeleteEditedFaq implements Action {
  public readonly type = FaqActionType.DELETE_EDITED_FAQ;
}

export class DeleteEditedFaqSuccess implements Action {
  public readonly type = FaqActionType.DELETE_EDITED_FAQ_SUCCESS;
}


export type FaqActions =
  LoadFaq | LoadFaqSuccess | OpenFaqEdition | LoadFaqById | LoadFaqByIdSuccess |
  SaveChangesToFaq | SaveChangesToFaqSuccess | ShowDeleteFaqPopup | HideDeleteFaqPopup |
  DeleteFaq | DeleteFaqSuccess | DeleteEditedFaq | DeleteEditedFaqSuccess

