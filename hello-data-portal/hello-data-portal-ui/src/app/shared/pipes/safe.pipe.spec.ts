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

import {SafePipe} from './safe.pipe';
import {DomSanitizer} from '@angular/platform-browser';
import {beforeEach, describe, expect, it, jest} from "@jest/globals";
import {TestBed} from "@angular/core/testing";


describe('SafePipe', () => {
  let pipe: SafePipe;
  let sanitizer: DomSanitizer;

  beforeEach(() => {
    sanitizer = {
      bypassSecurityTrustHtml: jest.fn(),
      bypassSecurityTrustStyle: jest.fn(),
      bypassSecurityTrustScript: jest.fn(),
      bypassSecurityTrustUrl: jest.fn(),
      bypassSecurityTrustResourceUrl: jest.fn(),
    } as unknown as DomSanitizer;

    TestBed.configureTestingModule({
      providers: [{provide: DomSanitizer, useValue: sanitizer}],
    });

    TestBed.runInInjectionContext(() => {
      pipe = new SafePipe(); // ✅ now inside an injection context
    });
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should call bypassSecurityTrustHtml for type "html"', () => {
    const value = '<div>HTML Content</div>';

    pipe.transform(value, 'html');

    expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith(value);
  });

  it('should call bypassSecurityTrustStyle for type "style"', () => {
    const value = 'color: red;';

    pipe.transform(value, 'style');

    expect(sanitizer.bypassSecurityTrustStyle).toHaveBeenCalledWith(value);
  });

  it('should call bypassSecurityTrustScript for type "script"', () => {
    const value = 'alert("Hello World!");';

    pipe.transform(value, 'script');

    expect(sanitizer.bypassSecurityTrustScript).toHaveBeenCalledWith(value);
  });

  it('should call bypassSecurityTrustUrl for type "url"', () => {
    const value = 'https://example.com';

    pipe.transform(value, 'url');

    expect(sanitizer.bypassSecurityTrustUrl).toHaveBeenCalledWith(value);
  });

  it('should call bypassSecurityTrustResourceUrl for type "resourceUrl"', () => {
    const value = 'https://example.com/resource';

    pipe.transform(value, 'resourceUrl');

    expect(sanitizer.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith(value);
  });

  it('should throw an error for an invalid type', () => {
    const value = 'Invalid Value';
    const invalidType = 'invalidType';

    expect(() => pipe.transform(value, invalidType)).toThrow(`Invalid safe type specified: ${invalidType}`);
  });
});
