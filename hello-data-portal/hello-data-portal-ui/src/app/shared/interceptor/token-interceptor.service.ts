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

import {inject, Injectable} from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from "@angular/common/http";
import {BehaviorSubject, map, Observable, switchMap, throwError} from "rxjs";
import {catchError, filter, take} from "rxjs/operators";
import {AuthService} from "../services";
import {environment} from "../../../environments/environment";
import {OidcSecurityService} from "angular-auth-oidc-client";

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  private readonly authService = inject(AuthService);
  private readonly oidcSecurityService = inject(OidcSecurityService);

  private isRefreshing = false;
  private readonly refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return this.authService.accessToken.pipe(
      switchMap(access_token => {
        if (access_token && !req.url.includes(environment.authConfig.authority)) {
          req = req.clone({
            setHeaders: {
              Authorization: `Bearer ${access_token}`
            }
          });
        }
        return next.handle(req).pipe(
          map((event: HttpEvent<any>) => {
            if (event instanceof HttpResponse && req.url.includes(environment.authConfig.authority + '/protocol/openid-connect/token')) {
              console.debug("creating an auth cookie for a domain: ." + environment.baseDomain);
              document.cookie = 'auth.access_token=' + event.body.access_token + '; path=/; domain=.' + environment.baseDomain + '; secure;';
            }
            return event;
          }),
          catchError((error: HttpErrorResponse) => this.handleError(error, req, next))
        );
      })
    );
  }

  private handleError(error: HttpErrorResponse, req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip auth-related URLs
    if (req.url.includes(environment.authConfig.authority)) {
      return throwError(() => error);
    }

    // Handle status 0 (network error / connection refused) - just propagate so showError can display a toast
    if (error.status === 0) {
      console.warn('[TokenInterceptor] Network error (status 0) for:', req.url);
      return throwError(() => error);
    }

    // Handle 401 Unauthorized
    if (error.status === 401) {
      return this.handle401Error(req, next, error);
    }

    // Handle 403 Forbidden - user is authenticated but not authorized
    if (error.status === 403) {
      console.warn('Access forbidden for resource:', req.url);
      return throwError(() => error);
    }

    return throwError(() => error);
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler, originalError: HttpErrorResponse): Observable<HttpEvent<any>> {
    if (this.isRefreshing) {
      // Another request is already refreshing the token, wait for it
      console.debug('[TokenInterceptor] Waiting for ongoing token refresh');
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addTokenToRequest(req, token)))
      );
    } else {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      console.debug('[TokenInterceptor] Attempting token refresh for request:', req.url);

      // Try to refresh the token using OIDC library
      return this.oidcSecurityService.forceRefreshSession().pipe(
        switchMap((result) => {
          this.isRefreshing = false;
          console.debug('[TokenInterceptor] Token refresh result:', result?.isAuthenticated ? 'authenticated' : 'not authenticated');

          if (result?.isAuthenticated) {
            // Token refresh successful, get new token and retry request
            return this.authService.accessToken.pipe(
              take(1),
              switchMap(newToken => {
                console.debug('[TokenInterceptor] Retrying request with new token');
                this.refreshTokenSubject.next(newToken);
                return next.handle(this.addTokenToRequest(req, newToken));
              })
            );
          } else {
            // Refresh failed, redirect to login
            console.warn('[TokenInterceptor] Token refresh failed (not authenticated), redirecting to login. Original URL:', req.url);
            this.redirectToLogin();
            return throwError(() => originalError);
          }
        }),
        catchError((refreshError) => {
          this.isRefreshing = false;
          console.warn('[TokenInterceptor] Token refresh error, redirecting to login. Error:', refreshError, 'Original URL:', req.url);
          this.redirectToLogin();
          return throwError(() => originalError);
        })
      );
    }
  }

  private addTokenToRequest(req: HttpRequest<any>, token: string | null): HttpRequest<any> {
    if (token) {
      return req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    return req;
  }

  private redirectToLogin(): void {
    // Clear any stale auth state and redirect to login
    this.oidcSecurityService.logoffLocal();
    this.authService.doLogin();
  }
}
