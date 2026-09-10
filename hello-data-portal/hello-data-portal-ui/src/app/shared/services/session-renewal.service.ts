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

import {inject, Injectable, NgZone} from '@angular/core';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {EMPTY, switchMap, take} from 'rxjs';

/**
 * Proactively refreshes the OIDC access token when the tab regains focus/visibility.
 *
 * Browsers throttle (and eventually suspend) timers on backgrounded tabs, so the
 * angular-auth-oidc-client silent-renew timer - scheduled shortly before the access
 * token expires - often does not fire while the user is on another tab or app. With
 * the shipped Keycloak realm the access token lives 5 min but the SSO session stays
 * alive for 30 min of idle, so a token that expired in the background can still be
 * refreshed silently once the user returns.
 *
 * Renewing here, on focus and BEFORE the periodic pollers (profile / CloudBeaver
 * session) fire, turns the common "away 5-30 min" case into a silent refresh instead
 * of a 401 that the interceptor would recover from by redirecting to /home. It does
 * NOT help once the SSO session itself has expired (away > 30 min) - a full re-login
 * is unavoidable then; that is a Keycloak ssoSessionIdleTimeout matter, not client.
 */
@Injectable({providedIn: 'root'})
export class SessionRenewalService {
  private readonly oidcSecurityService = inject(OidcSecurityService);
  private readonly zone = inject(NgZone);

  // Refresh when the access token expires within this window on focus.
  private static readonly RENEW_WITHIN_SECONDS = 60;
  // Guard against hammering the token endpoint when the user flips tabs quickly.
  private static readonly MIN_INTERVAL_MS = 15000;

  private started = false;
  private lastAttemptMs = 0;

  start(): void {
    if (this.started || typeof document === 'undefined') {
      return;
    }
    this.started = true;
    // These listeners are pure DOM plumbing; keep them out of Angular's zone so an
    // idle tab-switch doesn't schedule change detection. We re-enter the zone only
    // when we actually trigger a refresh.
    this.zone.runOutsideAngular(() => {
      document.addEventListener('visibilitychange', this.onFocusOrVisible);
      window.addEventListener('focus', this.onFocusOrVisible);
    });
  }

  private readonly onFocusOrVisible = (): void => {
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') {
      return;
    }
    const now = Date.now();
    if (now - this.lastAttemptMs < SessionRenewalService.MIN_INTERVAL_MS) {
      return;
    }
    this.lastAttemptMs = now;

    this.oidcSecurityService.isAuthenticated().pipe(
      take(1),
      switchMap(isAuthenticated => {
        // Logged out: let the guard / interceptor drive the (re-)login instead.
        if (!isAuthenticated) {
          return EMPTY;
        }
        return this.oidcSecurityService.getPayloadFromAccessToken().pipe(take(1));
      })
    ).subscribe({
      next: payload => {
        const expSeconds: number | undefined = payload?.exp;
        const secondsLeft = expSeconds ? expSeconds - Math.floor(Date.now() / 1000) : 0;
        if (secondsLeft > SessionRenewalService.RENEW_WITHIN_SECONDS) {
          return; // token is still fresh, nothing to do
        }
        this.zone.run(() => {
          this.oidcSecurityService.forceRefreshSession().pipe(take(1)).subscribe({
            next: result => console.debug('[SessionRenewal] refreshed on focus, authenticated:', result?.isAuthenticated),
            error: e => console.warn('[SessionRenewal] focus refresh failed (will re-auth on next request):', e)
          });
        });
      },
      error: e => console.debug('[SessionRenewal] could not evaluate token on focus', e)
    });
  };
}
