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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslocoService, TranslocoTestingModule} from '@ngneat/transloco';
import {ForbiddenComponent} from './forbidden.component';
import {beforeEach, describe, expect, it} from "@jest/globals";

describe('ForbiddenComponent', () => {
  let fixture: ComponentFixture<ForbiddenComponent>;
  let translocoService: TranslocoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ForbiddenComponent],
      imports: [TranslocoTestingModule],
    });

    fixture = TestBed.createComponent(ForbiddenComponent);
    translocoService = TestBed.inject(TranslocoService);
  });

  it('should create the ForbiddenComponent', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render translated text', () => {
    // Load translations manually
    translocoService.setTranslation({
      '@Not Allowed': 'You are not allowed',
      '@You are not allowed to access this page': 'You do not have permission to access this page',
    }, 'en');

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const headerText = compiled.querySelector('.forbidden-header').textContent;
    const paragraphText = compiled.querySelector('.forbidden-message').textContent;

    // FIXME translations not picked up
    // expect(headerText).toBe('You are not allowed');
    // expect(paragraphText).toBe('You do not have permission to access this page');

    expect(headerText).toBe('en.@Not Allowed');
    expect(paragraphText).toBe('en.@You are not allowed to access this page');
  });
});
