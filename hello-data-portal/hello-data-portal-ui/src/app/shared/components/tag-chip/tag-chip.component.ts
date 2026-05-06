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

import {Component, input, output} from '@angular/core';
import {ICON_REGISTRY} from '../../icons';
import {NgClass} from '@angular/common';

/**
 * Shared chip component for user-generated comment tags.
 *
 * Use this component wherever a dashboard-comment tag needs to be rendered —
 * either as a read-only display chip or as an editable chip with a remove button.
 * Do NOT use this for system-generated label badges (roles, statuses, durations, etc.);
 * those belong to PrimeNG <p-tag> with an appropriate severity.
 *
 * Inputs:
 *   tag       - the tag string to display (required)
 *   removable - when true renders a remove (×) button; defaults to false
 *   size      - 'sm' renders a compact chip for tight layouts; defaults to 'md'
 *
 * Output:
 *   removed   - emitted when the user clicks/activates the remove button
 */
@Component({
  imports: [NgClass],
  selector: 'app-tag-chip',
  standalone: true,
  template: `
    <span class="tag-chip"
          [class.tag-chip--removable]="removable()"
          [class.tag-chip--sm]="size() === 'sm'">
      <i [ngClass]="icons.CONTENT_TAG.class"></i>
      <span>{{ tag() }}</span>
      @if (removable()) {
        <button type="button"
                class="tag-chip__remove"
                (click)="removed.emit()"
                (keydown.enter)="removed.emit()"
                (keydown.space)="removed.emit()">
          <i [ngClass]="icons.ACTION_CLOSE.class"></i>
        </button>
      }
    </span>
  `,
  styleUrls: ['./tag-chip.component.scss']
})
export class TagChipComponent {
  protected readonly icons = ICON_REGISTRY;
  tag = input.required<string>();
  removable = input(false);
  size = input<'sm' | 'md'>('md');
  removed = output<void>();
}
