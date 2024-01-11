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

import {ContainsPipe} from './contains.pipe';
import {beforeEach, describe, expect, it} from "@jest/globals";

describe('ContainsPipe', () => {
  let pipe: ContainsPipe;

  beforeEach(() => {
    pipe = new ContainsPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should return true if the array contains the given value', () => {
    const inputArray = ['apple', 'banana', 'cherry', 'date'];
    const searchTerm = 'banana';

    const result = pipe.transform(inputArray, searchTerm);

    expect(result).toBe(true);
  });

  it('should return false if the array does not contain the given value', () => {
    const inputArray = ['apple', 'banana', 'cherry', 'date'];
    const searchTerm = 'grape';

    const result = pipe.transform(inputArray, searchTerm);

    expect(result).toBe(false);
  });

  it('should return false for an empty array', () => {
    const inputArray: string[] = [];
    const searchTerm = 'apple';

    const result = pipe.transform(inputArray, searchTerm);

    expect(result).toBe(false);
  });

  it('should return true if the array contains multiple occurrences of the given value', () => {
    const inputArray = ['apple', 'banana', 'cherry', 'date', 'apple'];
    const searchTerm = 'apple';

    const result = pipe.transform(inputArray, searchTerm);

    expect(result).toBe(true);
  });

  it('should return true if the array contains numbers as strings', () => {
    const inputArray = ['123', '456', '789'];
    const searchTerm = '456';

    const result = pipe.transform(inputArray, searchTerm);

    expect(result).toBe(true);
  });
});
