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

import {createAction, props} from "@ngrx/store";
import {Documentation, Pipeline, StorageMonitoringResult} from "./summary.model";

export enum SummaryActionType {
  LOAD_DOCUMENTATION = '[SUMMARY] Load documentation',
  LOAD_DOCUMENTATION_SUCCESS = '[SUMMARY] Load documentation SUCCESS',
  CREATE_OR_UPDATE_DOCUMENTATION = '[SUMMARY] Create or update documentation',
  CREATE_OR_UPDATE_DOCUMENTATION_SUCCESS = '[SUMMARY] Create or update documentation SUCCESS',
  LOAD_PIPELINES = '[SUMMARY] Load pipelines',
  LOAD_PIPELINES_SUCCESS = '[SUMMARY] Load pipelines SUCCESS',
  LOAD_STORAGE_SIZE = '[SUMMARY] Load storage size',
  LOAD_STORAGE_SIZE_SUCCESS = '[SUMMARY] Load storage size SUCCESS',
}

export const loadDocumentation = createAction(
  SummaryActionType.LOAD_DOCUMENTATION
);

export const loadDocumentationSuccess = createAction(
  SummaryActionType.LOAD_DOCUMENTATION_SUCCESS,
  props<{ payload: Documentation }>()
);

export const createOrUpdateDocumentation = createAction(
  SummaryActionType.CREATE_OR_UPDATE_DOCUMENTATION,
  props<{ documentation: Documentation }>()
);

export const createOrUpdateDocumentationSuccess = createAction(
  SummaryActionType.CREATE_OR_UPDATE_DOCUMENTATION_SUCCESS
);

export const loadPipelines = createAction(
  SummaryActionType.LOAD_PIPELINES
);

export const loadPipelinesSuccess = createAction(
  SummaryActionType.LOAD_PIPELINES_SUCCESS,
  props<{ payload: Pipeline[] }>()
);

export const loadStorageSize = createAction(
  SummaryActionType.LOAD_STORAGE_SIZE
);

export const loadStorageSizeSuccess = createAction(
  SummaryActionType.LOAD_STORAGE_SIZE_SUCCESS,
  props<{ payload: StorageMonitoringResult }>()
);
