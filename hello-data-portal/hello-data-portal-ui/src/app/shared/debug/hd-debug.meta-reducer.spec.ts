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

import {afterEach, describe, expect, it, jest} from '@jest/globals';
import {hdDebugMetaReducer} from './hd-debug.meta-reducer';

describe('hdDebugMetaReducer', () => {
  const reducer = jest.fn((state: any, _action: any) => ({...state, touched: true}));

  afterEach(() => {
    delete window.hdDebug;
    reducer.mockClear();
  });

  it('passes through to the wrapped reducer when diagnostics are disabled', () => {
    const result = hdDebugMetaReducer(reducer)({a: 1}, {type: '[AUTH] Check auth'});
    expect(result).toEqual({a: 1, touched: true});
  });

  it('logs only the action type when diagnostics are enabled', () => {
    const log = jest.fn();
    window.hdDebug = {log};
    const action = {type: '[AUTH] Login complete', accessToken: 'secret'} as any;

    hdDebugMetaReducer(reducer)({}, action);

    expect(log).toHaveBeenCalledWith('action', '[AUTH] Login complete');
    expect(reducer).toHaveBeenCalledWith({}, action);
  });
});
