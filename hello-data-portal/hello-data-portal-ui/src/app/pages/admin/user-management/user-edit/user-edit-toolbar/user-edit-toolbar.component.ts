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

import {Component, input, output} from "@angular/core";
import {Toolbar} from "primeng/toolbar";
import {Button} from "primeng/button";
import {Tooltip} from "primeng/tooltip";
import {Ripple} from "primeng/ripple";
import {TranslocoPipe} from "@jsverse/transloco";
import {User} from "../../../../../store/users-management/users-management.model";
import {ICON_REGISTRY} from '../../../../../shared/icons';

@Component({
  selector: 'app-user-edit-toolbar',
  standalone: true,
  template: `
    <p-toolbar>
      <div class="p-toolbar-group-start">
        @if (!user().superuser && user().enabled) {
          <p-button (click)="disableClicked.emit(user())" (keydown.enter)="disableClicked.emit(user())"
                    (keydown.space)="disableClicked.emit(user())"
                    [icon]="icons.ACTION_CANCEL.class" pRipple
                    [label]="'@Disable' | transloco"
                    severity="danger"/>
        }
      </div>
      <div class="p-toolbar-group-end">
        @if (!user().superuser && !user().enabled) {
          <p-button (click)="enableClicked.emit(user())" (keydown.enter)="enableClicked.emit(user())"
                    (keydown.space)="enableClicked.emit(user())"
                    [icon]="icons.ACTION_CREATE.class" pRipple
                    [label]="'@Enable' | transloco"
                    class="mr-2"/>
        }
        <p-button (click)="cancelClicked.emit()" (keydown.enter)="cancelClicked.emit()"
                  (keydown.space)="cancelClicked.emit()"
                  class="mr-2"
                  [label]="'@Cancel' | transloco"
                  severity="secondary" pRipple/>
        <p-button (click)="saveClicked.emit()" (keydown.enter)="saveClicked.emit()" (keydown.space)="saveClicked.emit()"
                  [disabled]="saveDisabled()"
                  [loading]="saveLoading()"
                  [label]="'@Save' | transloco"
                  [icon]="icons.ACTION_SAVE.class"
                  pRipple/>
      </div>
    </p-toolbar>
  `,
  imports: [Toolbar, Button, Tooltip, Ripple, TranslocoPipe]
})
export class UserEditToolbarComponent {
  protected readonly icons = ICON_REGISTRY;
  readonly user = input.required<User>();
  readonly saveDisabled = input<boolean>(false);
  readonly saveLoading = input<boolean>(false);

  readonly cancelClicked = output<void>();
  readonly saveClicked = output<void>();
  readonly disableClicked = output<User>();
  readonly enableClicked = output<User>();
}
