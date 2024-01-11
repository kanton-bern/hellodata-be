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

import {TruncatePipe} from './truncate.pipe';
import {beforeEach, describe, expect, it} from "@jest/globals";

describe('TruncatePipe', () => {
  let pipe: TruncatePipe;

  beforeEach(() => {
    pipe = new TruncatePipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should truncate a string with default parameters', () => {
    const input = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.';
    const expectedOutput = 'Lorem ipsum dolor sit ame...';

    const result = pipe.transform(input);

    expect(result).toEqual(expectedOutput);
  });

  it('should truncate a string with a custom limit', () => {
    const input = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.';
    const customLimit = 10;
    const expectedOutput = 'Lorem ipsu...';

    const result = pipe.transform(input, customLimit);

    expect(result).toEqual(expectedOutput);
  });

  it('should truncate a string with complete words', () => {
    const input = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.';
    const customLimit = 20;
    const completeWords = true;
    const expectedOutput = 'Lorem ipsum dolor...';

    const result = pipe.transform(input, customLimit, completeWords);

    expect(result).toEqual(expectedOutput);
  });

  it('should truncate a string with a custom ellipsis', () => {
    const input = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit.';
    const customEllipsis = '... (Read More)';
    const expectedOutput = 'Lorem ipsum dolor sit ame... (Read More)';

    const result = pipe.transform(input, undefined, undefined, customEllipsis);

    expect(result).toEqual(expectedOutput);
  });

  it('should not truncate a string if limit is greater than or equal to string length', () => {
    const input = 'Short string.';
    const customLimit = 20;

    const result = pipe.transform(input, customLimit);

    expect(result).toEqual(input);
  });

});
