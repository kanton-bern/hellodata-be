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
import {Router} from '@angular/router';
import {LoginResponse, OidcSecurityService} from 'angular-auth-oidc-client';
import {NEVER, of, Subject} from 'rxjs';
import {afterEach, describe, expect, it, jest} from '@jest/globals';
import {SessionRefreshService} from './session-refresh.service';
import {AuthService} from './auth.service';
import {SubsystemLogoutService} from './subsystem-logout.service';

describe('SessionRefreshService', () => {
  const newToken = 'new-token';

  const setup = () => {
    const oidcMock = {
      forceRefreshSession: jest.fn(),
      logoffLocal: jest.fn()
    };
    const routerMock = {navigate: jest.fn(), url: '/'};
    const subsystemLogoutMock = {
      logoutOpenedSubsystems: jest.fn<() => Promise<void>>().mockResolvedValue(undefined)
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        {provide: AuthService, useValue: {accessToken: of(newToken)}},
        {provide: OidcSecurityService, useValue: oidcMock},
        {provide: Router, useValue: routerMock},
        {provide: SubsystemLogoutService, useValue: subsystemLogoutMock}
      ]
    });
    return {service: TestBed.inject(SessionRefreshService), oidcMock, routerMock};
  };

  // redirectToLogin navigates after the (mocked) subsystem logout promise chain settles.
  const flushPromises = async () => {
    for (let i = 0; i < 5; i++) {
      await Promise.resolve();
    }
  };

  afterEach(() => {
    jest.useRealTimers();
  });

  it('runs a single refresh for concurrent callers and emits the new token to all', () => {
    const {service, oidcMock} = setup();
    const refresh$ = new Subject<LoginResponse>();
    oidcMock.forceRefreshSession.mockReturnValue(refresh$);

    const tokens: (string | null)[] = [];
    service.refresh().subscribe(t => tokens.push(t));
    service.refresh().subscribe(t => tokens.push(t));
    refresh$.next({isAuthenticated: true} as LoginResponse);

    expect(oidcMock.forceRefreshSession).toHaveBeenCalledTimes(1);
    expect(tokens).toEqual([newToken, newToken]);
  });

  it('does not stay "in flight" when the caller unsubscribes before the refresh answers', () => {
    const {service, oidcMock} = setup();
    const refresh$ = new Subject<LoginResponse>();
    oidcMock.forceRefreshSession.mockReturnValue(refresh$);

    // e.g. a poller effect using switchMap cancels the request that triggered the refresh
    service.refresh().subscribe().unsubscribe();
    expect(refresh$.observed).toBe(true); // the refresh keeps running detached
    refresh$.next({isAuthenticated: true} as LoginResponse);

    // A later 401 must start a new refresh instead of parking forever.
    oidcMock.forceRefreshSession.mockReturnValue(of({isAuthenticated: true} as LoginResponse));
    const tokens: (string | null)[] = [];
    service.refresh().subscribe(t => tokens.push(t));

    expect(oidcMock.forceRefreshSession).toHaveBeenCalledTimes(2);
    expect(tokens).toEqual([newToken]);
  });

  it('redirects to login when the refresh returns not authenticated', async () => {
    const {service, oidcMock, routerMock} = setup();
    oidcMock.forceRefreshSession.mockReturnValue(of({isAuthenticated: false} as LoginResponse));

    const error = jest.fn();
    service.refresh().subscribe({error});
    await flushPromises();

    expect(error).toHaveBeenCalled();
    expect(oidcMock.logoffLocal).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('times out a refresh that never answers, redirects to login and allows a new refresh', async () => {
    jest.useFakeTimers();
    const {service, oidcMock, routerMock} = setup();
    oidcMock.forceRefreshSession.mockReturnValue(NEVER);

    const error = jest.fn();
    service.refresh().subscribe({error});
    jest.advanceTimersByTime(SessionRefreshService.REFRESH_TIMEOUT_MS);
    await flushPromises();

    expect(error).toHaveBeenCalled();
    expect(oidcMock.logoffLocal).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/home']);

    oidcMock.forceRefreshSession.mockReturnValue(of({isAuthenticated: true} as LoginResponse));
    service.refresh().subscribe();
    expect(oidcMock.forceRefreshSession).toHaveBeenCalledTimes(2);
  });

  it('remembers the current route so the user returns after re-auth', () => {
    sessionStorage.clear();
    const {service, routerMock} = setup();
    routerMock.url = '/orchestration';

    service.redirectToLogin();

    expect(sessionStorage.getItem('redirectTo')).toBe('orchestration');
  });
});
