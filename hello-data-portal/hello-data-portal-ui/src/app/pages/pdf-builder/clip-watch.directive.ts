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

import {AfterViewInit, Directive, ElementRef, OnDestroy, inject, signal} from "@angular/core";
import {isCutOff} from "./pdf-canvas";

/**
 * Watches a box that clips its content (overflow: hidden, like a PDF grid tile) and exposes whether any
 * of its children would be cut off (see isCutOff). Re-checked whenever the box or a child changes size
 * (tile resized, text edited, canvas rescaled, web font loaded).
 */
@Directive({
  selector: '[appClipWatch]',
  exportAs: 'clipWatch',
  standalone: true,
})
export class ClipWatchDirective implements AfterViewInit, OnDestroy {
  private host: HTMLElement = inject(ElementRef<HTMLElement>).nativeElement;
  private resizeObserver: ResizeObserver | null = null;
  private mutationObserver: MutationObserver | null = null;

  readonly clipped = signal(false);

  ngAfterViewInit(): void {
    this.check();
    if (typeof ResizeObserver === 'undefined') {
      return;
    }
    this.resizeObserver = new ResizeObserver(() => this.check());
    this.observeBoxes();
    // A child swapped by the template (e.g. empty placeholder -> text) needs observing too.
    this.mutationObserver = new MutationObserver(() => this.observeBoxes());
    this.mutationObserver.observe(this.host, {childList: true});
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.mutationObserver?.disconnect();
  }

  check(): void {
    this.clipped.set(Array.from(this.host.children).some(child => isCutOff(this.host, child)));
  }

  private observeBoxes(): void {
    this.resizeObserver!.disconnect();
    this.resizeObserver!.observe(this.host);
    Array.from(this.host.children).forEach(child => this.resizeObserver!.observe(child));
    this.check();
  }
}
