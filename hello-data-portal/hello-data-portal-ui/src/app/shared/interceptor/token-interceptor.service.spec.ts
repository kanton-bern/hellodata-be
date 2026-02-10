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
import {HTTP_INTERCEPTORS, HttpClient, HttpErrorResponse} from '@angular/common/http';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {LoginResponse, OidcSecurityService} from 'angular-auth-oidc-client';
import {of, throwError} from 'rxjs';
import {beforeAll, describe, expect, it, jest} from '@jest/globals';
import {TokenInterceptor} from './token-interceptor.service';
import {AuthService} from '../services';
import {environment} from '../../../environments/environment';

describe('TokenInterceptor', () => {
  const testUrl = '/api/test';
  const mockToken = 'mock-access-token';
  const newMockToken = 'new-mock-access-token';
  const mockAuthority = 'https://auth.example.com';

  // Set up a proper authority value for tests
  beforeAll(() => {
    environment.authConfig.authority = mockAuthority;
  });

  // Helper to create TestBed with specific configuration
  const setupTestBed = (tokenValue: string | null = mockToken) => {
    TestBed.resetTestingModule();

    const doLoginFn = jest.fn();
    // Use getter to match real AuthService behavior
    const authMock = {
      get accessToken() {
        return of(tokenValue);
      },
      doLogin: doLoginFn
    };

    const oidcMock = {
      forceRefreshSession: jest.fn(),
      logoffLocal: jest.fn()
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        {
          provide: HTTP_INTERCEPTORS,
          useClass: TokenInterceptor,
          multi: true
        },
        {provide: AuthService, useValue: authMock},
        {provide: OidcSecurityService, useValue: oidcMock}
      ]
    });

    const httpClient = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    return {authMock, oidcMock, httpClient, httpMock, doLoginFn};
  };

  describe('Token attachment', () => {
    it('should add Authorization header to requests', (done) => {
      const {httpClient, httpMock} = setupTestBed(mockToken);

      httpClient.get(testUrl).subscribe({
        complete: () => {
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(testUrl);
      expect(req.request.headers.has('Authorization')).toBe(true);
      expect(req.request.headers.get('Authorization')).toBe(`Bearer ${mockToken}`);
      req.flush({});
    });

    it('should not add Authorization header to auth authority requests', (done) => {
      const {httpClient, httpMock} = setupTestBed(mockToken);
      const authUrl = mockAuthority + '/some-endpoint';

      httpClient.get(authUrl).subscribe({
        complete: () => {
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(authUrl);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    });

    it('should not add Authorization header when no token is available', (done) => {
      const {httpClient, httpMock} = setupTestBed(null);

      httpClient.get(testUrl).subscribe({
        complete: () => {
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(testUrl);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    });
  });

  describe('Error handling', () => {
    it('should handle 401 error and attempt token refresh', (done) => {
      const {oidcMock, httpClient, httpMock} = setupTestBed(newMockToken);

      // Setup: forceRefreshSession returns success
      oidcMock.forceRefreshSession.mockReturnValue(
        of({isAuthenticated: true} as LoginResponse)
      );

      httpClient.get(testUrl).subscribe({
        next: () => {
          expect(oidcMock.forceRefreshSession).toHaveBeenCalled();
          httpMock.verify();
          done();
        },
        error: (err) => done(err)
      });

      // First request fails with 401
      const req = httpMock.expectOne(testUrl);
      req.flush({message: 'Unauthorized'}, {status: 401, statusText: 'Unauthorized'});

      // Retry request with new token
      const retryReq = httpMock.expectOne(testUrl);
      expect(retryReq.request.headers.get('Authorization')).toBe(`Bearer ${newMockToken}`);
      retryReq.flush({success: true});
    });

    it('should redirect to login when token refresh fails', (done) => {
      const {doLoginFn, oidcMock, httpClient, httpMock} = setupTestBed(mockToken);

      oidcMock.forceRefreshSession.mockReturnValue(
        of({isAuthenticated: false} as LoginResponse)
      );

      httpClient.get(testUrl).subscribe({
        next: () => done(new Error('Should have errored')),
        error: () => {
          setTimeout(() => {
            try {
              expect(oidcMock.forceRefreshSession).toHaveBeenCalled();
              expect(oidcMock.logoffLocal).toHaveBeenCalled();
              expect(doLoginFn).toHaveBeenCalled();
              httpMock.verify();
              done();
            } catch (e) {
              done(e as Error);
            }
          }, 0);
        }
      });

      const req = httpMock.expectOne(testUrl);
      req.flush({message: 'Unauthorized'}, {status: 401, statusText: 'Unauthorized'});
    });

    it('should redirect to login when token refresh throws error', (done) => {
      const {doLoginFn, oidcMock, httpClient, httpMock} = setupTestBed(mockToken);

      oidcMock.forceRefreshSession.mockReturnValue(
        throwError(() => new Error('Refresh failed'))
      );

      httpClient.get(testUrl).subscribe({
        next: () => done(new Error('Should have errored')),
        error: () => {
          setTimeout(() => {
            try {
              expect(oidcMock.forceRefreshSession).toHaveBeenCalled();
              expect(oidcMock.logoffLocal).toHaveBeenCalled();
              expect(doLoginFn).toHaveBeenCalled();
              httpMock.verify();
              done();
            } catch (e) {
              done(e as Error);
            }
          }, 0);
        }
      });

      const req = httpMock.expectOne(testUrl);
      req.flush({message: 'Unauthorized'}, {status: 401, statusText: 'Unauthorized'});
    });

    it('should handle status 0 (network error) same as 401', (done) => {
      const {oidcMock, httpClient, httpMock} = setupTestBed(newMockToken);

      oidcMock.forceRefreshSession.mockReturnValue(
        of({isAuthenticated: true} as LoginResponse)
      );

      httpClient.get(testUrl).subscribe({
        next: () => {
          expect(oidcMock.forceRefreshSession).toHaveBeenCalled();
          httpMock.verify();
          done();
        },
        error: (err) => done(err)
      });

      const req = httpMock.expectOne(testUrl);
      req.error(new ProgressEvent('error'), {status: 0, statusText: 'Unknown Error'});

      const retryReq = httpMock.expectOne(testUrl);
      expect(retryReq.request.headers.get('Authorization')).toBe(`Bearer ${newMockToken}`);
      retryReq.flush({success: true});
    });

    it('should pass through 403 errors without refresh attempt', (done) => {
      const {oidcMock, httpClient, httpMock} = setupTestBed(mockToken);

      httpClient.get(testUrl).subscribe({
        next: () => done(new Error('Should have errored')),
        error: (err: HttpErrorResponse) => {
          expect(err.status).toBe(403);
          expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(testUrl);
      req.flush({message: 'Forbidden'}, {status: 403, statusText: 'Forbidden'});
    });

    it('should pass through other errors without refresh attempt', (done) => {
      const {oidcMock, httpClient, httpMock} = setupTestBed(mockToken);

      httpClient.get(testUrl).subscribe({
        next: () => done(new Error('Should have errored')),
        error: (err: HttpErrorResponse) => {
          expect(err.status).toBe(500);
          expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(testUrl);
      req.flush({message: 'Server Error'}, {status: 500, statusText: 'Internal Server Error'});
    });

    it('should pass through auth-related URL errors without handling', (done) => {
      const {oidcMock, httpClient, httpMock} = setupTestBed(mockToken);
      const authUrl = mockAuthority + '/protocol/openid-connect/token';

      httpClient.get(authUrl).subscribe({
        next: () => done(new Error('Should have errored')),
        error: (err: HttpErrorResponse) => {
          expect(err.status).toBe(401);
          expect(oidcMock.forceRefreshSession).not.toHaveBeenCalled();
          httpMock.verify();
          done();
        }
      });

      const req = httpMock.expectOne(authUrl);
      req.flush({message: 'Unauthorized'}, {status: 401, statusText: 'Unauthorized'});
    });
  });
});
