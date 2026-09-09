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
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import {SessionRenewalService} from './session-renewal.service';

describe('SessionRenewalService', () => {
  const nowSec = () => Math.floor(Date.now() / 1000);

  const setup = (opts: {isAuthenticated?: boolean; exp?: number} = {}) => {
    const oidcMock = {
      isAuthenticated: jest.fn().mockReturnValue(of(opts.isAuthenticated ?? true)),
      getPayloadFromAccessToken: jest.fn().mockReturnValue(of({exp: opts.exp})),
      forceRefreshSession: jest.fn().mockReturnValue(of({isAuthenticated: true}))
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        SessionRenewalService,
        {provide: OidcSecurityService, useValue: oidcMock}
      ]
    });

    const service = TestBed.inject(SessionRenewalService);
    service.start();
    return {service, oidcMock};
  };

  beforeEach(() => {
    Object.defineProperty(document, 'visibilityState', {value: 'visible', configurable: true});
  });

  const fireVisibility = () => document.dispatchEvent(new Event('visibilitychange'));

  it('refreshes when the access token is expired or about to expire', () => {
    const {oidcMock} = setup({isAuthenticated: true, exp: nowSec() - 10});
    fireVisibility();
    expect(oidcMock.forceRefreshSession).toHaveBeenCalledTimes(1);
  });

  it('does NOT refresh when the access token is still fresh', () => {
    const {oidcMock} = setup({isAuthenticated: true, exp: nowSec() + 240});
    fireVisibility();
    expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
  });

  it('does NOT refresh when the user is not authenticated', () => {
    const {oidcMock} = setup({isAuthenticated: false, exp: nowSec() - 10});
    fireVisibility();
    expect(oidcMock.getPayloadFromAccessToken).not.toHaveBeenCalled();
    expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
  });

  it('does NOT act while the tab is hidden', () => {
    const {oidcMock} = setup({isAuthenticated: true, exp: nowSec() - 10});
    Object.defineProperty(document, 'visibilityState', {value: 'hidden', configurable: true});
    fireVisibility();
    expect(oidcMock.isAuthenticated).not.toHaveBeenCalled();
    expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
  });

  it('throttles rapid focus events to a single refresh', () => {
    const {oidcMock} = setup({isAuthenticated: true, exp: nowSec() - 10});
    fireVisibility();
    fireVisibility();
    fireVisibility();
    expect(oidcMock.forceRefreshSession).toHaveBeenCalledTimes(1);
  });
});
