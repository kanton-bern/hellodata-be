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
import {MyDashboardsState} from "./my-dashboards.state";
import {ALL_DATA_DOMAINS} from "../app/app.constants";
import {selectRouteParam, selectUrl} from "../router/router.selectors";
import {selectProfile} from "../auth/auth.selector";

const myDashboardsState = (state: AppState) => state.myDashboards;
const metaInfoResourcesState = (state: AppState) => state.metaInfoResources;

export const selectDashboardId = selectRouteParam('id');
export const selectInstanceName = selectRouteParam('instanceName');

export const selectSelectedDataDomain = createSelector(
  myDashboardsState,
  (state: MyDashboardsState) => state.selectedDataDomain
);

export const selectAvailableDataDomains = createSelector(
  myDashboardsState,
  selectSelectedDataDomain,
  (state: MyDashboardsState, selectedDataDomain) => {
    if (selectedDataDomain) {
      return state.availableDataDomains.filter(dataDomain => dataDomain.id !== selectedDataDomain.id);
    }
    return state.availableDataDomains;
  }
);

export const selectMyDashboards = createSelector(
  myDashboardsState,
  selectSelectedDataDomain,
  (state: MyDashboardsState, selectedDataDomain) => {
    if (selectedDataDomain === null || selectedDataDomain.id === '') {
      return state.myDashboards;
    }
    return state.myDashboards.filter(dashboard => dashboard.contextId === selectedDataDomain.id);
  }
);

export const selectAvailableDataDomainItems = createSelector(
  selectAvailableDataDomains,
  selectSelectedDataDomain,
  (availableDataDomains, selectedDataDomain) => {
    if (selectedDataDomain && selectedDataDomain.id === '') {
      return availableDataDomains
        .filter(availableDataDomain => availableDataDomain.id)
        .map(availableDataDomain => ({
          label: availableDataDomain.name,
          data: availableDataDomain
        }));
    }
    return [{
      label: selectedDataDomain?.name,
      data: selectedDataDomain
    }];
  }
);

export const selectDashboardByInstanceNameAndId = (instanceName: string, dashboardId: string) =>
  createSelector(
    myDashboardsState,
    (state) => state.myDashboards.find(entry => entry.instanceName === instanceName && (entry.slug === dashboardId || `${entry.id}` === dashboardId))
  );

export const selectAvailableDataDomainsWithAllEntry = createSelector(
  selectAvailableDataDomainItems,
  (availableDataDomains) => {
    return [
      {
        key: ALL_DATA_DOMAINS,
        label: ALL_DATA_DOMAINS
      },
      ...availableDataDomains.map(dataDomain => {
        return {
          key: dataDomain.data?.key,
          label: dataDomain.data?.name
        }
      })
    ]
  }
);

export const selectCurrentMyDashboardInfo = createSelector(
  myDashboardsState,
  metaInfoResourcesState,
  selectDashboardId,
  selectInstanceName,
  selectProfile,
  selectUrl,
  (myDashboardsState, metainfoResourcesState, dashboardId, instanceName, profile, currentUrl) => {
    return {
      appinfo: metainfoResourcesState.appInfos.find(entry => entry.instanceName === instanceName),
      dashboard: myDashboardsState.myDashboards.find(entry => entry.instanceName === instanceName && (entry.slug === dashboardId || `${entry.id}` === dashboardId)),
      profile,
      currentUrl
    }
  }
);
