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

import {Injectable, NgModule} from '@angular/core';
import {AuthModule, OpenIdConfiguration, StsConfigLoader, StsConfigStaticLoader} from 'angular-auth-oidc-client';
import {environment} from "../../environments/environment";

@Injectable({providedIn: 'root'})
export class AuthConfigService {
  getConfig(): OpenIdConfiguration {
    return {
      triggerAuthorizationResultEvent: true,
      postLoginRoute: '/home',
      forbiddenRoute: '/forbidden',
      unauthorizedRoute: '/unauthorized',
      logLevel: environment.authConfig.logLevel,
      historyCleanupOff: false,
      authority: environment.authConfig.authority,
      redirectUrl: `${environment.authConfig.redirectUrl}/callback`,
      postLogoutRedirectUri: environment.authConfig.postLogoutRedirectUri,
      clientId: environment.authConfig.clientId,
      scope: 'openid profile email offline_access',
      responseType: 'code',
      silentRenew: true,
      silentRenewUrl: `${window.location.origin}/silent-renew.html`,
      renewTimeBeforeTokenExpiresInSeconds: 30,
      useRefreshToken: true,
      ignoreNonceAfterRefresh: true,
      //kc_idp_hint: make automatic redirect to keycloak adfs provider:
      customParamsAuthRequest: {"kc_idp_hint": "adfs"}
    };
  }
}

const authFactory = (configService: AuthConfigService) => {
  const config = configService.getConfig();
  return new StsConfigStaticLoader(config);
};

@NgModule({
  imports: [
    AuthModule.forRoot({
      loader: {
        provide: StsConfigLoader,
        useFactory: authFactory,
        deps: [AuthConfigService],
      },
    }),
  ],
  exports: [AuthModule],
})
export class AuthConfigModule {
}
