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

import { Component, Input, inject } from '@angular/core';
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {navigate} from "../../../../store/app/app.action";
import { NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';
import { TableModule } from 'primeng/table';
import { PrimeTemplate } from 'primeng/api';
import { Tag } from 'primeng/tag';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslocoPipe } from '@jsverse/transloco';

export interface PipelineInstance {
  state: "queued" | "running" | "success" | "failed";
}

export interface Pipeline {
  id: string;
  description: string;
  lastInstance: PipelineInstance;
}

@Component({
    selector: 'app-selected-workspace-pipelines',
    templateUrl: './selected-workspace-pipelines.component.html',
    styleUrls: ['./selected-workspace-pipelines.component.scss'],
    imports: [NgIf, TableModule, PrimeTemplate, Tag, NgSwitch, NgSwitchCase, NgSwitchDefault, Button, Tooltip, TranslocoPipe]
})
export class SelectedWorkspacePipelinesComponent {
  private store = inject<Store<AppState>>(Store);


  @Input()
  pipelines!: any[];

  @Input()
  instanceName!: string;

  showDetails(pipelineId: string) {
    this.store.dispatch(navigate({url: `/embedded-orchestration/details/${pipelineId}`}));
  }
}
