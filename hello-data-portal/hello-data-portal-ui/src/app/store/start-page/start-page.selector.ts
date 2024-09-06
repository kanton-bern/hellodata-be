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
import {StartPageState} from "./start-page.state";
import {selectSelectedDataDomain} from "../my-dashboards/my-dashboards.selector";
import {selectRouteParam} from "../router/router.selectors";

const startPageState = (state: AppState) => state.startPage;
const metaInfoResourcesState = (state: AppState) => state.metaInfoResources;

export const selectDataDomainKeyParam = selectRouteParam('dataDomainKey');
export const selectFaq = createSelector(
  startPageState,
  selectSelectedDataDomain,
  (state: StartPageState, selectedDataDomain) => {
    if (selectedDataDomain === null || selectedDataDomain.id === '') {
      return state.faq;
    }
    return state.faq.filter(faq => faq.contextKey === null || faq.contextKey === selectedDataDomain.key);
  }
);

export function copy(array: any[]): any[] {
  const result: any = [];

  for (const element of array) {
    const assign = Object.assign({}, element);
    result.push(assign);
    if (assign.items) {
      assign.items = copy(assign.items);
    }
  }
  return result;
}

export const selectCurrentJupyterhubLink = createSelector(
  startPageState,
  metaInfoResourcesState,
  selectDataDomainKeyParam,
  (state: StartPageState, metainfoResourcesState, dataDomainKeyParam) => {
    console.log('inside selector', metainfoResourcesState, dataDomainKeyParam)
    const currentDataDomainJupyterhub = metainfoResourcesState.appInfos.filter(appinfo => appinfo.moduleType === 'JUPYTERHUB' && appinfo.businessContextInfo?.subContext.key === dataDomainKeyParam)[0];
    if (currentDataDomainJupyterhub) {
      return currentDataDomainJupyterhub.data.url;
    }
    console.error('Error: Cannot find jupyterhub for selected data domain: dataDomainKeyParam ', dataDomainKeyParam);
  }
);
