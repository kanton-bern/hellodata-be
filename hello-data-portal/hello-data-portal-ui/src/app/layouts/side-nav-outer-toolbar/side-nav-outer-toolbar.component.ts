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

import {Component, Input, NgModule} from '@angular/core';
import {HeaderModule, SummaryModule} from '../../shared/components';
import {CommonModule} from '@angular/common';

import {RouterLink, RouterOutlet} from '@angular/router';
import {ScrollPanelModule} from "primeng/scrollpanel";
import {Store} from "@ngrx/store";
import {AppState} from "../../store/app/app.state";
import {Observable, tap} from "rxjs";
import {selectNavItems} from "../../store/menu/menu.selector";
import {TranslocoModule} from "@ngneat/transloco";
import {TooltipModule} from "primeng/tooltip";
import {DividerModule} from "primeng/divider";
import {ToastModule} from "primeng/toast";
import {ScrollTopModule} from "primeng/scrolltop";
import {UnsavedChangesModule} from "../../shared/components/unsaved-changes-dialog/unsaved-changes-dialog.component";
import {selectPublishedAndFilteredAnnouncements} from "../../store/announcement/announcement.selector";
import {selectCurrentUserPermissionsLoaded} from "../../store/auth/auth.selector";

@Component({
  selector: 'app-side-nav-outer-toolbar',
  templateUrl: './side-nav-outer-toolbar.component.html',
  styleUrls: ['./side-nav-outer-toolbar.component.scss']
})
export class SideNavOuterToolbarComponent {

  @Input()
  title!: string;
  navItems$: Observable<any[]>;
  publishedAnnouncements$: Observable<any>
  selectCurrentUserPermissionsLoaded$: Observable<boolean>
  height = 8;

  constructor(private store: Store<AppState>) {
    this.navItems$ = this.store.select(selectNavItems);
    this.publishedAnnouncements$ = this.store.select(selectPublishedAndFilteredAnnouncements).pipe(tap(value => {
        this.height = 8 + (value.length * 4);
      }
    ));
    this.selectCurrentUserPermissionsLoaded$ = this.store.select(selectCurrentUserPermissionsLoaded);
  }

}

@NgModule({
  imports: [HeaderModule,
    CommonModule,
    RouterOutlet,
    ScrollPanelModule,
    RouterLink,
    TranslocoModule,
    TooltipModule,
    DividerModule,
    SummaryModule,
    ToastModule,
    ScrollTopModule,
    UnsavedChangesModule],
  exports: [SideNavOuterToolbarComponent],
  declarations: [SideNavOuterToolbarComponent]
})
export class SideNavOuterToolbarModule {
}
