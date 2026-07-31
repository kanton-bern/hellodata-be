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

import {inject, Injectable, NgZone, OnDestroy} from '@angular/core';
import {Subject, switchMap} from 'rxjs';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {environment} from '../../../environments/environment';

/**
 * Listens to a server-sent-events stream from the portal-api and reacts to Airflow role changes in
 * near real time. When the portal reconciles a user's Airflow roles (e.g. a data-domain admin role
 * is granted/revoked in user management), the api-server pushes an `airflow-roles-changed` event to
 * that user's open tabs. We then force an OIDC token refresh so the shared `auth.access_token`
 * cookie carries the new roles, and emit {@link airflowRolesChanged$} so an open Airflow 3 iframe
 * reloads and re-runs its cookie SSO login against the fresh roles — no more waiting for the next
 * silent token renewal.
 *
 * The connection is ref-counted via {@link connect}/{@link disconnect} so it is only held while an
 * Airflow 3 orchestration view is actually on screen.
 */
@Injectable({providedIn: 'root'})
export class AirflowSseService implements OnDestroy {
  private readonly oidc = inject(OidcSecurityService);
  private readonly zone = inject(NgZone);

  private eventSource?: EventSource;
  private refCount = 0;
  private readonly rolesChanged = new Subject<void>();
  readonly airflowRolesChanged$ = this.rolesChanged.asObservable();

  connect(): void {
    this.refCount++;
    if (this.eventSource) {
      return;
    }
    const url = `${environment.portalApi}/sse/subscribe`;
    // EventSource cannot send an Authorization header; the portal-api falls back to the
    // auth.access_token cookie (shared on the parent domain) for /sse/**. withCredentials makes
    // the browser send that cookie on this same-origin request.
    this.eventSource = new EventSource(url, {withCredentials: true});
    this.eventSource.addEventListener('airflow-roles-changed', () => this.onRolesChanged());
    this.eventSource.onerror = () => {
      // The browser reconnects EventSource automatically (and re-authenticates with a fresh cookie).
      console.debug('airflow SSE connection error - browser will auto-reconnect');
    };
  }

  disconnect(): void {
    if (this.refCount > 0) {
      this.refCount--;
    }
    if (this.refCount === 0 && this.eventSource) {
      this.eventSource.close();
      this.eventSource = undefined;
    }
  }

  private onRolesChanged(): void {
    // The EventSource callback fires outside Angular's zone; run inside it so the token refresh and
    // the downstream iframe reload trigger change detection.
    this.zone.run(() => {
      this.oidc.forceRefreshSession().pipe(
        switchMap(() => this.oidc.getAccessToken())
      ).subscribe({
        next: token => {
          if (token) {
            document.cookie = `auth.access_token=${token}; path=/; domain=.${environment.baseDomain}; secure;`;
          }
          this.rolesChanged.next();
        },
        error: err => {
          console.warn('forced token refresh after Airflow role change failed', err);
          // Still reload; the iframe re-auth will pick up whatever the current cookie holds.
          this.rolesChanged.next();
        }
      });
    });
  }

  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = undefined;
    }
    this.rolesChanged.complete();
  }
}
