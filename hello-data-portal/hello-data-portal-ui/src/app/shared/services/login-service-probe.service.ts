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

import {Injectable} from '@angular/core';
import {environment} from "../../../environments/environment";
import {LoginServiceReachability} from "../../store/auth/auth.state";

/**
 * Tells apart the two reasons why the OIDC discovery request to Keycloak can fail with status 0.
 * A cross-origin XHR hides the real response, but a 'no-cors' fetch still resolves (with an opaque
 * response) whenever the host answered at all - e.g. with a firewall 403 page - and only rejects
 * when there was no answer (DNS, TLS, VPN or offline).
 */
@Injectable({
  providedIn: 'root'
})
export class LoginServiceProbeService {
  static readonly PROBE_TIMEOUT_MS = 8000;

  get wellKnownUrl(): string {
    return environment.authConfig.authority.replace(/\/+$/, '') + '/.well-known/openid-configuration';
  }

  async probe(): Promise<LoginServiceReachability> {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), LoginServiceProbeService.PROBE_TIMEOUT_MS);
    try {
      await fetch(this.wellKnownUrl, {mode: 'no-cors', cache: 'no-store', credentials: 'omit', signal: controller.signal});
      return 'refused';
    } catch {
      return 'unreachable';
    } finally {
      clearTimeout(timeout);
    }
  }
}
