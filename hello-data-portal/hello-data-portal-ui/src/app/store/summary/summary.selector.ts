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
import {createSelector} from "@ngrx/store";
import {SummaryState} from "./summary.state";
import {selectSelectedDataDomain} from "../my-dashboards/my-dashboards.selector";
import {AuthState} from "../auth/auth.state";

const summaryState = (state: AppState) => state.summary;
const authState = (state: AppState) => state.auth;

export const selectDocumentationFilterEmpty = createSelector(
  summaryState,
  authState,
  (state: SummaryState, authState: AuthState) => {
    if (state.documentation && state.documentation.texts && Object.keys(state.documentation.texts).length > 0
      && authState.defaultLanguage && state.documentation.texts[authState.defaultLanguage]) {
      return state.documentation;
    }
    return null;
  }
);


export const selectDocumentation = createSelector(
  summaryState,
  authState,
  (state: SummaryState) => state.documentation
);

export const selectPipelines = createSelector(
  summaryState,
  selectSelectedDataDomain,
  (state: SummaryState, selectedDataDomain) => {
    if (selectedDataDomain === null || selectedDataDomain.id === '') {
      return state.pipelines
    }
    return state.pipelines.filter(pipeline => pipeline.contextKey === selectedDataDomain.key);
  }
);

export const selectStorageSize = createSelector(
  summaryState,
  (state: SummaryState) => state.storageMonitoringResult
);


