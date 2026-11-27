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

import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {TestBed} from '@angular/core/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {provideMockStore} from '@ngrx/store/testing';
import {firstValueFrom, Observable, of, ReplaySubject, throwError, toArray} from 'rxjs';
import {Action} from '@ngrx/store';
import {AuthEffects} from './auth.effects';
import {login, loginServiceUnavailable} from './auth.action';
import {AuthService, LoginServiceProbeService} from '../../shared/services';
import {UsersManagementService} from '../users-management/users-management.service';
import {TranslateService} from '../../shared/services/translate.service';
import {CloudbeaverService} from './cloudbeaver.service';
import {CloudbeaverSessionService} from '../../shared/services/cloudbeaver-session.service';
import {WindowManagementService} from '../../shared/services/window-management.service';

describe('AuthEffects login$', () => {
  const wellKnownUrl = 'https://sso.example.com/realms/hd/.well-known/openid-configuration';
  let actions$: ReplaySubject<Action>;
  let authService: { doLogin: jest.Mock<() => Observable<void>> };
  let probe: { wellKnownUrl: string, probe: jest.Mock<() => Promise<'refused' | 'unreachable'>> };
  let effects: AuthEffects;

  beforeEach(() => {
    actions$ = new ReplaySubject<Action>();
    authService = {doLogin: jest.fn()};
    probe = {wellKnownUrl, probe: jest.fn()};
    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        provideMockStore(),
        {provide: AuthService, useValue: authService},
        {provide: LoginServiceProbeService, useValue: probe},
        {provide: UsersManagementService, useValue: {}},
        {provide: TranslateService, useValue: {}},
        {provide: CloudbeaverService, useValue: {}},
        {provide: CloudbeaverSessionService, useValue: {}},
        {provide: WindowManagementService, useValue: {}}
      ]
    });
    effects = TestBed.inject(AuthEffects);
  });

  it('dispatches nothing when the login redirect starts', async () => {
    authService.doLogin.mockReturnValue(of(undefined));
    actions$.next(login());
    actions$.complete();

    await expect(firstValueFrom(effects.login$.pipe(toArray()))).resolves.toEqual([]);
    expect(probe.probe).not.toHaveBeenCalled();
  });

  it('reports the probed reason when the login service configuration cannot be loaded', async () => {
    authService.doLogin.mockReturnValue(throwError(() => new Error('[object Object]')));
    probe.probe.mockResolvedValue('refused');
    actions$.next(login());

    const action = await firstValueFrom(effects.login$);

    expect(action).toEqual(loginServiceUnavailable({
      info: {reachability: 'refused', loginServiceUrl: wellKnownUrl, occurredAt: expect.any(String) as unknown as string}
    }));
  });
});
