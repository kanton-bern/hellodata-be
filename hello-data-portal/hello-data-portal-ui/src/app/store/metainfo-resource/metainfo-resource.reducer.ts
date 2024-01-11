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

import {initialMetaInfoResourceState, MetaInfoResourceState} from "./metainfo-resource.state";
import {MetaInfoResourceActions, MetaInfoResourceActionType} from "./metainfo-resource.action";

export const metaInfoResourceReducer = (
  state = initialMetaInfoResourceState,
  action: MetaInfoResourceActions
): MetaInfoResourceState => {
  switch (action.type) {
    case MetaInfoResourceActionType.LOAD_APP_INFOS_SUCCESS: {
      return {
        ...state,
        appInfos: action.result
      };
    }

    case MetaInfoResourceActionType.LOAD_SELECTED_APP_INFO_RESOURCES: {
      return {
        ...state,
        selectedAppInfoResources: [],
        selectedAppInfoResource: null
      };
    }

    case MetaInfoResourceActionType.LOAD_SELECTED_APP_INFO_RESOURCE: {
      return {
        ...state,
        selectedAppInfoResource: action.selectedResource
      };
    }

    case MetaInfoResourceActionType.LOAD_SELECTED_APP_INFOS_RESOURCES_SUCCESS: {
      return {
        ...state,
        selectedAppInfoResources: action.payload
      };
    }

    case MetaInfoResourceActionType.LOAD_ROLES_SUCCESS: {
      return {
        ...state,
        roles: action.result
      };
    }

    case MetaInfoResourceActionType.LOAD_PERMISSIONS_SUCCESS: {
      return {
        ...state,
        permissions: action.result
      };
    }

    default:
      return state;
  }
};
