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

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PublishedAnnouncementsWrapperComponent} from './published-announcements-wrapper/published-announcements-wrapper.component';
import {TranslocoModule} from "@jsverse/transloco";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {SharedModule} from "primeng/api";
import {ToolbarModule} from "primeng/toolbar";
import {EditorModule} from "primeng/editor";
import {FormsModule} from "@angular/forms";
import {DividerModule} from "primeng/divider";
import {TooltipModule} from "primeng/tooltip";
import {PublishedAnnouncementsPopupComponent} from "./published-announcements-popup/published-announcements-popup.component";
import {DialogModule} from 'primeng/dialog';
import {DynamicDialogModule} from 'primeng/dynamicdialog';
import {PublishedAnnouncementsPopupHeaderComponent} from "./published-announcements-popup/published-annoucements-popup-header/published-announcements-popup-header.component";
import {ToggleButtonModule} from "primeng/togglebutton";
import {TriStateCheckboxModule} from "primeng/tristatecheckbox";
import {SelectButtonModule} from "primeng/selectbutton";
import {CheckboxModule} from "primeng/checkbox";


@NgModule({
  declarations: [
    PublishedAnnouncementsWrapperComponent,
    PublishedAnnouncementsPopupComponent,
    PublishedAnnouncementsPopupHeaderComponent
  ],
  exports: [
    PublishedAnnouncementsWrapperComponent
  ],
  imports: [
    CommonModule,
    TranslocoModule,
    ButtonModule,
    RippleModule,
    SharedModule,
    ToolbarModule,
    EditorModule,
    FormsModule,
    DividerModule,
    TooltipModule,
    DialogModule,
    DynamicDialogModule,
    ToggleButtonModule,
    TriStateCheckboxModule,
    SelectButtonModule,
    CheckboxModule,
  ]
})
export class PublishedAnnouncementsModule {
}
