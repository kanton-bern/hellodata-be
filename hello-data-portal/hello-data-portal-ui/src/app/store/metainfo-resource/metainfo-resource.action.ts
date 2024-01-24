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

import {createAction, props} from '@ngrx/store';
import {MetaInfoResource} from "./metainfo-resource.model";

export enum MetaInfoResourceActionType {
  LOAD_APP_INFOS = '[METAINFO RESOURCES] Load AppInfo Resources',
  LOAD_APP_INFOS_SUCCESS = '[METAINFO RESOURCES] Load AppInfo Resources success',
  LOAD_ROLES = '[METAINFO RESOURCES] Load Role Resources',
  LOAD_ROLES_SUCCESS = '[METAINFO RESOURCES] Load Role Resources success',
  LOAD_PERMISSIONS = '[METAINFO RESOURCES] Load Permission Resources',
  LOAD_PERMISSIONS_SUCCESS = '[METAINFO RESOURCES] Load Permission Resources success',
  LOAD_SELECTED_APP_INFO_RESOURCES = '[METAINFO RESOURCES] Load Selected AppInfo Resources',
  LOAD_SELECTED_APP_INFOS_RESOURCES_SUCCESS = '[METAINFO RESOURCES] Load Selected AppInfo Resources success',
  LOAD_SELECTED_APP_INFO_RESOURCE = '[METAINFO RESOURCES] Load Selected AppInfo Resource',
}

export const loadAppInfoResources = createAction(
  MetaInfoResourceActionType.LOAD_APP_INFOS
);

export const loadAppInfoResourcesSuccess = createAction(
  MetaInfoResourceActionType.LOAD_APP_INFOS_SUCCESS,
  props<{ result: MetaInfoResource[] }>()
);

export const loadRoleResources = createAction(
  MetaInfoResourceActionType.LOAD_ROLES
);

export const loadRoleResourcesSuccess = createAction(
  MetaInfoResourceActionType.LOAD_ROLES_SUCCESS,
  props<{ result: MetaInfoResource[] }>()
);

export const loadPermissionResources = createAction(
  MetaInfoResourceActionType.LOAD_PERMISSIONS
);

export const loadPermissionResourcesSuccess = createAction(
  MetaInfoResourceActionType.LOAD_PERMISSIONS_SUCCESS,
  props<{ result: MetaInfoResource[] }>()
);

export const loadSelectedAppInfoResources = createAction(
  MetaInfoResourceActionType.LOAD_SELECTED_APP_INFO_RESOURCES
);

export const loadSelectedAppInfoResourcesSuccess = createAction(
  MetaInfoResourceActionType.LOAD_SELECTED_APP_INFOS_RESOURCES_SUCCESS,
  props<{ payload: MetaInfoResource[] }>()
);

export const loadSelectedAppInfoResource = createAction(
  MetaInfoResourceActionType.LOAD_SELECTED_APP_INFO_RESOURCE,
  props<{ selectedResource: MetaInfoResource }>()
);
