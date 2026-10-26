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
import {Router} from '@angular/router';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {Observable, shareReplay, switchMap, throwError, timeout} from 'rxjs';
import {catchError, finalize, take} from 'rxjs/operators';
import {AuthService} from './auth.service';
import {SubsystemLogoutService} from './subsystem-logout.service';

/**
 * Single place that refreshes the OIDC session and, when that is no longer possible, sends the
 * user back to login. Shared by the TokenInterceptor (401 recovery) and SessionRenewalService
 * (tab wake), so they never run two refreshes at once - Keycloak rotates the refresh token and
 * would reject the second one, forcing a spurious logout.
 *
 * The refresh runs detached from whoever asked for it (shareReplay without refCount): a caller
 * that unsubscribes early - e.g. a poller whose effect uses switchMap, or a tab that went to
 * sleep mid-request - can no longer leave a refresh "in flight" forever. That used to park
 * every later 401 indefinitely, with no refresh and no redirect to login.
 */
@Injectable({providedIn: 'root'})
export class SessionRefreshService {
  private readonly authService = inject(AuthService);
  private readonly oidcSecurityService = inject(OidcSecurityService);
  private readonly router = inject(Router);
  private readonly subsystemLogoutService = inject(SubsystemLogoutService);

  private static readonly REDIRECT_TO_KEY = 'redirectTo';
  // A refresh that has not answered by then (frozen tab, dead network) counts as failed.
  static readonly REFRESH_TIMEOUT_MS = 30000;

  private refreshInFlight$: Observable<string | null> | null = null;
  private isRedirectingToLogin = false;

  /**
   * Refreshes the session (joining a refresh already in flight) and emits the new access token.
   * Errors - after redirecting to login - when the session can no longer be refreshed.
   */
  refresh(): Observable<string | null> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }
    const refresh$: Observable<string | null> = this.oidcSecurityService.forceRefreshSession().pipe(
      take(1),
      timeout(SessionRefreshService.REFRESH_TIMEOUT_MS),
      switchMap(result => {
        console.debug('[SessionRefresh] Token refresh result:', result?.isAuthenticated ? 'authenticated' : 'not authenticated');
        if (result?.isAuthenticated) {
          return this.authService.accessToken.pipe(take(1));
        }
        return throwError(() => new Error('Session refresh returned not authenticated'));
      }),
      catchError(error => {
        console.warn('[SessionRefresh] Token refresh failed, redirecting to login. Error:', error);
        this.redirectToLogin();
        return throwError(() => error);
      }),
      finalize(() => {
        if (this.refreshInFlight$ === refresh$) {
          this.refreshInFlight$ = null;
        }
      }),
      shareReplay({bufferSize: 1, refCount: false})
    );
    this.refreshInFlight$ = refresh$;
    return refresh$;
  }

  redirectToLogin(): void {
    if (this.isRedirectingToLogin) {
      return; // Prevent multiple concurrent login redirects
    }
    this.isRedirectingToLogin = true;
    // Remember where the user was so we can bring them back after re-authentication,
    // instead of always dumping them on /home. Without this, a token expiry while the
    // user sits in a subsystem iframe (CloudBeaver / Airflow / Superset) kicks them out
    // to the portal home page. The AppComponent restores this from sessionStorage once
    // auth completes. Note: only the Angular route is preserved, not the subsystem's
    // internal (cross-origin) iframe location.
    this.rememberCurrentRouteForRedirect();
    console.warn('[SessionRefresh] Clearing local auth state and navigating to /home for re-authentication');
    // Clear stale local auth state. The AutoLoginPartialRoutesGuard on /home
    // will trigger the OIDC authorize flow properly.
    this.oidcSecurityService.logoffLocal();
    // HELLODATA-4146: the portal session is gone (Keycloak refused the refresh), so also end the
    // sessions of the subsystems opened from this tab - otherwise their tabs stay logged in.
    // Navigate once their logout calls finished or timed out; the navigation itself still runs
    // in a later task to avoid triggering it during change detection.
    this.subsystemLogoutService.logoutOpenedSubsystems()
      .catch(e => console.debug('[SessionRefresh] subsystem logout failed', e))
      .finally(() => {
        this.isRedirectingToLogin = false;
        this.router.navigate(['/home']);
      });
  }

  private rememberCurrentRouteForRedirect(): void {
    try {
      const currentUrl = this.router.url;
      // Skip routes that would create a redirect loop or aren't worth restoring.
      if (!currentUrl || currentUrl === '/' || currentUrl.startsWith('/home') || currentUrl.includes('/callback')) {
        return;
      }
      // Strip the leading slash: the AppComponent restore path dispatches
      // navigate({url}) which router.navigate([url]) accepts either way, and the
      // existing redirectTo links (menu.state/service) use the slash-less form.
      const redirectTo = currentUrl.startsWith('/') ? currentUrl.substring(1) : currentUrl;
      sessionStorage.setItem(SessionRefreshService.REDIRECT_TO_KEY, redirectTo);
      console.debug('[SessionRefresh] Saved redirect target for post-login restore:', redirectTo);
    } catch (e) {
      console.debug('[SessionRefresh] Could not persist redirect target', e);
    }
  }
}
