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

import {TestBed} from '@angular/core/testing';
import {Store} from '@ngrx/store';
import {Actions} from '@ngrx/effects';
import {Router} from '@angular/router';
import {of, Subject} from 'rxjs';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {CallbackComponent} from './callback.component';
import {authError, checkAuth} from '../store/auth/auth.action';

describe('CallbackComponent', () => {
  const RETRY_COUNT_KEY = 'hd_callback_retry_count';
  let actions$: Subject<any>;
  let dispatch: jest.Mock;
  let navigate: jest.Mock;

  const setup = () => {
    actions$ = new Subject<any>();
    dispatch = jest.fn();
    navigate = jest.fn();

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        CallbackComponent,
        {provide: Store, useValue: {dispatch, select: () => of(false)}},
        {provide: Actions, useValue: actions$},
        {provide: Router, useValue: {navigate}}
      ]
    });
    return TestBed.inject(CallbackComponent);
  };

  beforeEach(() => {
    sessionStorage.clear();
  });

  it('dispatches checkAuth on init', () => {
    const component = setup();
    component.ngOnInit();
    expect(dispatch).toHaveBeenCalledWith(checkAuth());
  });

  it('recovers immediately on authError instead of waiting for the timeout', () => {
    const component = setup();
    component.ngOnInit();

    // Simulate exhausted retries so retryOrFail takes the router path (no page reload in jsdom).
    sessionStorage.setItem(RETRY_COUNT_KEY, '3');

    actions$.next(authError({error: new Error('invalid_grant')}));

    expect(navigate).toHaveBeenCalledWith(['/unauthorized']);
    expect(component.hasError).toBe(true);
    expect(component.isLoading).toBe(false);
  });

  it('increments the retry counter on the first authError', () => {
    const component = setup();
    component.ngOnInit();

    // location.href assignment is a no-op in jsdom; we assert the retry bookkeeping instead.
    actions$.next(authError({error: new Error('invalid_grant')}));

    expect(sessionStorage.getItem(RETRY_COUNT_KEY)).toBe('1');
  });

  it('ignores authError once auth is no longer loading', () => {
    const component = setup();
    component.ngOnInit();
    component.isLoading = false;

    actions$.next(authError({error: new Error('late error')}));

    expect(navigate).not.toHaveBeenCalled();
    expect(sessionStorage.getItem(RETRY_COUNT_KEY)).toBeNull();
  });
});
