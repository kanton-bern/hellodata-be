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

import {Component} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from '@jest/globals';
import {ExternalLinkDirective} from './external-link.directive';

@Component({
  standalone: true,
  imports: [ExternalLinkDirective],
  template: `
    <a href="https://example.com" appExternalLink id="with-icon">Example</a>
    <a href="https://example.com" appExternalLink [appExternalLinkShowIcon]="false" id="without-icon">No Icon</a>
  `
})
class TestHostComponent {}

describe('ExternalLinkDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should set target="_blank"', () => {
    const link = fixture.nativeElement.querySelector('#with-icon') as HTMLAnchorElement;
    expect(link.getAttribute('target')).toBe('_blank');
  });

  it('should set rel="noopener noreferrer"', () => {
    const link = fixture.nativeElement.querySelector('#with-icon') as HTMLAnchorElement;
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('should append an external-link icon by default', () => {
    const link = fixture.nativeElement.querySelector('#with-icon') as HTMLAnchorElement;
    const icon = link.querySelector('i.fa-up-right-from-square');
    expect(icon).toBeTruthy();
    expect(icon?.getAttribute('aria-hidden')).toBe('true');
  });

  it('should not append icon when appExternalLinkShowIcon is false', () => {
    const link = fixture.nativeElement.querySelector('#without-icon') as HTMLAnchorElement;
    const icon = link.querySelector('i.fa-up-right-from-square');
    expect(icon).toBeFalsy();
  });

  it('should still set target and rel when icon is hidden', () => {
    const link = fixture.nativeElement.querySelector('#without-icon') as HTMLAnchorElement;
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });
});
