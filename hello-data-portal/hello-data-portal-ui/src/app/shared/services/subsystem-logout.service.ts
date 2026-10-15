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
import {Store} from '@ngrx/store';
import {combineLatest, firstValueFrom, take} from 'rxjs';
import {AppState} from '../../store/app/app.state';
import {selectAppInfoByModuleType} from '../../store/metainfo-resource/metainfo-resource.selector';
import {
  VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY
} from '../../pages/my-dashboards/embed-my-dashboard/embed-my-dashboard.component';

/**
 * Logs the user out of the subsystems (Superset, Airflow, JupyterHub, SFTPGo) they opened from the
 * portal, by loading each logout URL in a hidden iframe - the same set the /logout page calls.
 *
 * Used when the portal session ends without an explicit logout (the Keycloak SSO session expired),
 * so subsystem tabs opened from the portal do not stay logged in after the portal session is gone.
 * Resolves once every iframe has loaded or after `timeoutMs`, whichever comes first, so callers can
 * redirect to the login afterwards without blocking on a slow or unreachable subsystem.
 */
@Injectable({providedIn: 'root'})
export class SubsystemLogoutService {
  private readonly store = inject<Store<AppState>>(Store);

  static readonly DEFAULT_TIMEOUT_MS = 1500;

  async logoutOpenedSubsystems(timeoutMs = SubsystemLogoutService.DEFAULT_TIMEOUT_MS): Promise<void> {
    const urls = await this.collectLogoutUrls();
    sessionStorage.removeItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY);
    if (urls.length === 0 || typeof document === 'undefined') {
      return;
    }
    const frames = urls.map(url => this.createHiddenFrame(url));
    const allLoaded = Promise.all(frames.map(frame => new Promise<void>(resolve => {
      frame.addEventListener('load', () => resolve(), {once: true});
      frame.addEventListener('error', () => resolve(), {once: true});
    })));
    const timeout = new Promise<void>(resolve => setTimeout(resolve, timeoutMs));
    await Promise.race([allLoaded, timeout]);
    frames.forEach(frame => frame.remove());
  }

  private async collectLogoutUrls(): Promise<string[]> {
    const urls = new Set<string>(this.visitedSubsystemLogoutUrls());
    try {
      const [airflow, airflow3, sftpgo] = await firstValueFrom(combineLatest([
        this.store.select(selectAppInfoByModuleType('AIRFLOW')),
        this.store.select(selectAppInfoByModuleType('AIRFLOW3')),
        this.store.select(selectAppInfoByModuleType('SFTPGO')),
      ]).pipe(take(1)));
      if (airflow?.length > 0) {
        urls.add(airflow[0].data.url + '/logout');
      }
      if (airflow3?.length > 0) {
        urls.add(airflow3[0].data.url + '/logout');
      }
      if (sftpgo?.length > 0) {
        urls.add(sftpgo[0].data.url + '/web/client/logout');
      }
    } catch (e) {
      console.debug('[SubsystemLogout] could not read subsystem infos from the store', e);
    }
    return Array.from(urls);
  }

  private visitedSubsystemLogoutUrls(): string[] {
    try {
      const stored = sessionStorage.getItem(VISITED_SUBSYSTEMS_SESSION_STORAGE_KEY);
      const parsed: unknown = stored ? JSON.parse(stored) : [];
      return Array.isArray(parsed) ? parsed.filter((url): url is string => typeof url === 'string') : [];
    } catch {
      return [];
    }
  }

  private createHiddenFrame(url: string): HTMLIFrameElement {
    const frame = document.createElement('iframe');
    frame.style.display = 'none';
    frame.setAttribute('aria-hidden', 'true');
    frame.tabIndex = -1;
    frame.src = url;
    document.body.appendChild(frame);
    return frame;
  }
}
