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
import {HomeComponent} from "./home.component";
import {DashboardsComponent} from './dashboards/dashboards.component';
import {TranslocoModule} from "@ngneat/transloco";
import {FaqComponent} from './faq/faq.component';
import {RouterLink} from "@angular/router";
import {HdCommonModule} from "../../hd-common.module";
import {AccordionModule} from "primeng/accordion";
import {EditorModule} from "primeng/editor";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {TableModule} from "primeng/table";
import {InputTextModule} from "primeng/inputtext";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {ToolbarModule} from "primeng/toolbar";
import {DialogModule} from "primeng/dialog";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {SummaryModule} from "../../shared/components";
import {TooltipModule} from "primeng/tooltip";
import {LineageComponent} from "./lineage/lineage.component";
import {DropdownModule} from "primeng/dropdown";
import {DmComponent} from "./datamarts/dm.component";
import {ExternalComponent} from "./external/external.component";
import {HomeDocumentationComponent} from "./documentation/home-documentation.component";
import {DataViewModule} from "primeng/dataview";
import {FieldsetModule} from "primeng/fieldset";
import {ScrollPanelModule} from "primeng/scrollpanel";
import {SidebarModule} from "primeng/sidebar";
import {BadgeModule} from "primeng/badge";
import {AdminInitModule} from "../../shared/components/admin-init/admin-init.component";


@NgModule({
  declarations: [HomeComponent, DashboardsComponent, FaqComponent, LineageComponent, DmComponent, ExternalComponent, HomeDocumentationComponent],
  imports: [
    CommonModule,
    TranslocoModule,
    RouterLink,
    HdCommonModule,
    AccordionModule,
    EditorModule,
    FormsModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    ToolbarModule,
    DialogModule,
    ConfirmDialogModule,
    SummaryModule,
    TooltipModule,
    DropdownModule,
    ReactiveFormsModule,
    DataViewModule,
    FieldsetModule,
    ScrollPanelModule,
    SidebarModule,
    BadgeModule,
    AdminInitModule
  ]
})
export class HomeModule {
}
