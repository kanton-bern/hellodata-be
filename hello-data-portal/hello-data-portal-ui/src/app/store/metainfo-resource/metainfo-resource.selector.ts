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

import {createSelector} from "@ngrx/store";
import {MetaInfoResourceState} from "./metainfo-resource.state";
import {AppState} from "../app/app.state";
import {selectRouteParam} from "../router/router.selectors";
import {selectAllDataDomains} from "../users-management/users-management.selector";

const metaInfoResourcesState = (state: AppState) => state.metaInfoResources;

export const selectParamInstanceName = selectRouteParam('instanceName');
export const selectParamApiVersion = selectRouteParam('apiVersion');
export const selectParamModuleType = selectRouteParam('moduleType');

export const selectSelectedAppInfoResourcesParams = createSelector(
  selectParamInstanceName,
  selectParamApiVersion,
  selectParamModuleType,
  (instanceName, apiVersion, moduleType) => ({instanceName, apiVersion, moduleType})
);

export const selectAppInfos = createSelector(
  metaInfoResourcesState,
  (state: MetaInfoResourceState) => state.appInfos
);

export const selectAppInfoResources = createSelector(
  metaInfoResourcesState,
  (state: MetaInfoResourceState) => state.selectedAppInfoResources
);

export const selectSelectedAppInfoResource = createSelector(
  metaInfoResourcesState,
  (state: MetaInfoResourceState) => state.selectedAppInfoResource
);

export const selectPermissions = createSelector(
  metaInfoResourcesState,
  (state: MetaInfoResourceState) => {
    if (state.permissions.length > 0) {
      return state.permissions.flatMap(permissionResource => permissionResource.data);
    }
    return [];
  }
);

export const selectSelectedAppInfo = createSelector(
  metaInfoResourcesState,
  selectSelectedAppInfoResourcesParams,
  (state: MetaInfoResourceState, appInfoParams: any) => {
    return state.appInfos.filter(appInfo => appInfo.instanceName === appInfoParams.instanceName && appInfo.moduleType === appInfoParams.moduleType && appInfo.apiVersion === appInfoParams.apiVersion)[0]
  }
);

export const selectAppInfoByInstanceName = (instanceName: string) =>
  createSelector(
    metaInfoResourcesState,
    (state) => state.appInfos.find(entry => entry.instanceName === instanceName)
  );

export const selectAppInfoByModuleType = (moduleType: string) =>
  createSelector(
    metaInfoResourcesState,
    (state) => state.appInfos.filter(entry => entry.moduleType === moduleType)
  );

export const selectDataDomainByKey = (contextKey: string) =>
  createSelector(
    selectAllDataDomains,
    (dataDomains) => dataDomains.find(dataDomain => dataDomain.contextKey === contextKey)
  );
