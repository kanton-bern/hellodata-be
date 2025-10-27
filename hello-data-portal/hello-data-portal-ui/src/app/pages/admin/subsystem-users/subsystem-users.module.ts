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
import {TranslocoModule} from "@jsverse/transloco";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ButtonModule} from "primeng/button";
import {EditorModule} from "primeng/editor";
import {RippleModule} from "primeng/ripple";
import {SharedModule} from "primeng/api";
import {TableModule} from "primeng/table";
import {ToolbarModule} from "primeng/toolbar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {CheckboxModule} from "primeng/checkbox";
import {TooltipModule} from "primeng/tooltip";
import {SubsystemUsersComponent} from "./subsystem-users.component";
import {AccordionModule} from "primeng/accordion";
import {InputNumberModule} from "primeng/inputnumber";
import {TextareaModule} from 'primeng/textarea';
import {InputTextModule} from "primeng/inputtext";
import {InputSwitchModule} from "primeng/inputswitch";
import {InputMaskModule} from "primeng/inputmask";
import {AutoFocusModule} from "primeng/autofocus";
import {TagModule} from "primeng/tag";


@NgModule({
  declarations: [SubsystemUsersComponent],
  imports: [
    CommonModule,
    TranslocoModule,
    ReactiveFormsModule,
    ButtonModule,
    EditorModule,
    RippleModule,
    SharedModule,
    TableModule,
    ToolbarModule,
    FormsModule,
    ConfirmDialogModule,
    CheckboxModule,
    TooltipModule,
    AccordionModule,
    InputMaskModule,
    InputSwitchModule,
    InputTextModule,
    TextareaModule,
    InputNumberModule,
    AutoFocusModule,
    TagModule,

  ]
})
export class SubsystemUsersModule {
}
