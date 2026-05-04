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

import {AfterViewInit, Directive, ElementRef, inject, input, Renderer2} from '@angular/core';

/**
 * Directive that marks a link as external.
 * Automatically sets target="_blank", rel="noopener noreferrer",
 * and appends a visual external-link icon.
 *
 * Usage:
 *   <a href="https://example.com" appExternalLink>Example</a>
 *   <a href="https://example.com" appExternalLink [appExternalLinkShowIcon]="false">No icon</a>
 */
@Directive({
  selector: 'a[appExternalLink]',
  standalone: true
})
export class ExternalLinkDirective implements AfterViewInit {
  private readonly el = inject(ElementRef);
  private readonly renderer = inject(Renderer2);

  appExternalLinkShowIcon = input(true);

  ngAfterViewInit(): void {
    const anchor = this.el.nativeElement as HTMLAnchorElement;

    this.renderer.setAttribute(anchor, 'target', '_blank');
    this.renderer.setAttribute(anchor, 'rel', 'noopener noreferrer');

    if (this.appExternalLinkShowIcon()) {
      const icon = this.renderer.createElement('i');
      this.renderer.addClass(icon, 'fa-solid');
      this.renderer.addClass(icon, 'fa-up-right-from-square');
      this.renderer.setStyle(icon, 'margin-left', '0.35em');
      this.renderer.setStyle(icon, 'font-size', '0.8em');
      this.renderer.setAttribute(icon, 'aria-hidden', 'true');
      this.renderer.appendChild(anchor, icon);
    }
  }
}
