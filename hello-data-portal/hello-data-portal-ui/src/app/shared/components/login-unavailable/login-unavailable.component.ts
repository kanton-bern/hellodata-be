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

import {ChangeDetectionStrategy, Component, computed, inject, input, signal} from '@angular/core';
import {Store} from "@ngrx/store";
import {TranslocoPipe} from "@jsverse/transloco";
import {Button} from "primeng/button";
import {AppState} from "../../../store/app/app.state";
import {LoginUnavailableInfo} from "../../../store/auth/auth.state";
import {login} from "../../../store/auth/auth.action";
import {AppInfoService} from "../../services";
import {ExternalLinkDirective} from "../../directives/external-link.directive";

/**
 * Full-screen notice shown when the portal cannot start the Keycloak login, e.g. because a
 * firewall in front of Keycloak blocks this device. Lets the user retry, open the login service
 * directly (which shows the real block page with its error id) and copy support information.
 */
@Component({
  selector: 'app-login-unavailable',
  templateUrl: './login-unavailable.component.html',
  styleUrls: ['./login-unavailable.component.scss'],
  imports: [TranslocoPipe, Button, ExternalLinkDirective],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginUnavailableComponent {
  private readonly store = inject<Store<AppState>>(Store);
  private readonly appInfo = inject(AppInfoService);

  readonly info = input.required<LoginUnavailableInfo>();
  readonly copied = signal(false);

  readonly reasonKey = computed(() => this.info().reachability === 'refused'
    ? '@Login service refused'
    : '@Login service unreachable');

  readonly supportInfo = computed(() => {
    const info = this.info();
    return [
      `time: ${info.occurredAt}`,
      `portal: ${window.location.href}`,
      `login service: ${info.loginServiceUrl}`,
      `result: ${info.reachability}`,
      `online: ${navigator.onLine}`,
      `build: ${this.buildInfo()}`,
      `userAgent: ${navigator.userAgent}`
    ].join('\n');
  });

  // Build info comes from /actuator/info and is still missing when that call did not finish
  private buildInfo(): string {
    const buildInfo = this.appInfo.buildInfo;
    return buildInfo && !buildInfo.includes('undefined') ? buildInfo : 'unknown';
  }

  retry(): void {
    this.copied.set(false);
    this.store.dispatch(login());
  }

  async copySupportInfo(): Promise<void> {
    const text = this.supportInfo();
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // Clipboard API is missing or denied (e.g. older or managed mobile browsers)
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }
    this.copied.set(true);
  }
}
