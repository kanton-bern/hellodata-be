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

import {Component, viewChild} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {describe, expect, it} from '@jest/globals';
import {ClipWatchDirective} from './clip-watch.directive';

@Component({
  standalone: true,
  imports: [ClipWatchDirective],
  template: `
    <div class="tile" appClipWatch #clip="clipWatch"><div class="text">Text</div></div>
    @if (clip.clipped()) {
      <span class="warning">cut off</span>
    }
  `,
})
class HostComponent {
  clip = viewChild.required(ClipWatchDirective);
}

/** jsdom does no layout: give an element the vertical extent a browser would. */
function placeAt(el: Element, top: number, bottom: number): void {
  el.getBoundingClientRect = () => ({top, bottom, left: 0, right: 100, width: 100, height: bottom - top, x: 0, y: top}) as DOMRect;
}

describe('ClipWatchDirective', () => {
  const setup = (textBottom: number) => {
    const fixture = TestBed.createComponent(HostComponent);
    const el: HTMLElement = fixture.nativeElement;
    // Set before the first change detection: the directive checks once its view is ready.
    fixture.detectChanges(false);
    placeAt(el.querySelector('.tile')!, 0, 100);
    placeAt(el.querySelector('.text')!, 8, textBottom);
    fixture.componentInstance.clip().check();
    fixture.detectChanges();
    return {fixture, el};
  };

  it('shows no warning while the text fits the tile', () => {
    const {fixture, el} = setup(90);
    expect(fixture.componentInstance.clip().clipped()).toBe(false);
    expect(el.querySelector('.warning')).toBeNull();
  });

  it('warns when the text runs past the bottom of the tile', () => {
    const {fixture, el} = setup(140);
    expect(fixture.componentInstance.clip().clipped()).toBe(true);
    expect(el.querySelector('.warning')).not.toBeNull();
  });

  it('clears the warning once the text fits again', () => {
    const {fixture, el} = setup(140);
    placeAt(el.querySelector('.text')!, 8, 60);
    fixture.componentInstance.clip().check();
    fixture.detectChanges();
    expect(el.querySelector('.warning')).toBeNull();
  });
});
