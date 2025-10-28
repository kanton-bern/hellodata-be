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

import {Component, EventEmitter, NgModule, Output} from '@angular/core';
import {DrawerModule} from 'primeng/drawer';
import {ScrollPanelModule} from "primeng/scrollpanel";
import {
  AsyncPipe,
  DatePipe,
  JsonPipe,
  NgClass,
  NgForOf,
  NgIf,
  NgStyle,
  NgSwitch,
  NgSwitchCase,
  NgSwitchDefault
} from "@angular/common";
import {FieldsetModule} from "primeng/fieldset";
import {AccordionModule} from "primeng/accordion";
import {EditorModule} from "primeng/editor";
import {FormsModule} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "../../../store/app/app.state";
import {
  selectDocumentationFilterEmpty,
  selectPipelines,
  selectStorageSize
} from "../../../store/summary/summary.selector";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {Observable} from "rxjs";
import {
  selectCurrentUserPermissions,
  selectDefaultLanguage,
  selectSelectedLanguage
} from "../../../store/auth/auth.selector";
import {HdCommonModule} from "../../../hd-common.module";
import {TranslocoModule} from "@jsverse/transloco";
import {TooltipModule} from "primeng/tooltip";
import {DataViewModule} from "primeng/dataview";
import {Documentation, Pipeline, StorageMonitoringResult} from "../../../store/summary/summary.model";
import {SubscriptionsComponent} from "./subscriptions/subscriptions.component";
import {navigate} from "../../../store/app/app.action";
import {FooterModule} from "../footer/footer.component";
import {AppInfoService} from "../../services";
import {TranslateService} from "../../services/translate.service";


@Component({
  selector: 'app-summary',
  templateUrl: './summary.component.html',
  styleUrls: ['./summary.component.scss'],
  standalone: false
})
export class SummaryComponent {
  currentUserPermissions$: Observable<string[]>;
  summarySidebarVisible = false;
  @Output() rightSidebarVisible = new EventEmitter<boolean>();
  overlaySidebarVisible = false;

  pipelines$: Observable<Pipeline[]>;
  documentation$: Observable<Documentation | null>;
  storeSize$: Observable<StorageMonitoringResult | null>;
  selectedLanguage$: Observable<any>;
  defaultLanguage$: Observable<any>;

  constructor(private store: Store<AppState>, public appInfo: AppInfoService, private translateService: TranslateService) {
    this.documentation$ = store.select(selectDocumentationFilterEmpty);
    this.currentUserPermissions$ = this.store.select(selectCurrentUserPermissions);
    this.pipelines$ = this.store.select(selectPipelines);
    this.storeSize$ = this.store.select(selectStorageSize);
    this.selectedLanguage$ = store.select(selectSelectedLanguage);
    this.defaultLanguage$ = store.select(selectDefaultLanguage);
  }

  toggleSummaryPanel() {
    this.summarySidebarVisible = !this.summarySidebarVisible;
    this.rightSidebarVisible.emit(this.summarySidebarVisible);
  }

  openOverlaySidebar() {
    this.overlaySidebarVisible = true;
  }

  editDocumentation() {
    this.store.dispatch(navigate({url: 'documentation-management'}))
  }

  getText(documentation: Documentation, selectedLanguage: string, defaultLanguage: string) {
    const text = documentation.texts[selectedLanguage];
    if (!text) {
      return this.translateService.translate('@Translation not available, fallback to default', {default: defaultLanguage.slice(0, 2)?.toUpperCase()}) + '\n' + documentation.texts[defaultLanguage];
    }
    return text;
  }
}


@NgModule({
  imports: [
    DrawerModule,
    ScrollPanelModule,
    NgIf,
    NgClass,
    NgStyle,
    FieldsetModule,
    AccordionModule,
    EditorModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    AsyncPipe,
    HdCommonModule,
    TranslocoModule,
    TooltipModule,
    NgForOf,
    DataViewModule,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    JsonPipe,
    DatePipe,
    FooterModule,
  ],
  declarations: [SummaryComponent, SubscriptionsComponent],
  exports: [SummaryComponent]
})
export class SummaryModule {
}
