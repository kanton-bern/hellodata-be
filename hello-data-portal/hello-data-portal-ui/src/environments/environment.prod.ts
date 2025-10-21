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

import {Environment} from "./env.model";
import {LogLevel} from "angular-auth-oidc-client";

// augment the Window interface to include the Environment
// see https://mariusschulz.com/blog/declaring-global-variables-in-typescript#augmenting-the-window-interface
declare global {
  interface Window {
    environment: Environment
  }
}
export const environment: Environment = window.environment != undefined ? window.environment : {
  production: false,
  portalApi: '--configure me--',
  docsApi: '--configure me--',
  debugInfoEnabled: true,
  authConfig: {
    logLevel: LogLevel.Debug,
    authority: '--configure me--',
    redirectUrl: '--configure me--',
    postLogoutRedirectUri: '--configure me--',
    clientId: '--configure me--',
    scope: 'openid profile email'
  },
  deploymentEnvironment: {
    name: 'PROD'
  },
  locale: 'de-CH',
  domainNamespace: '--configure me--',
  baseDomain: '--configure me--',
  subSystemsConfig: {
    airflow: {protocol: 'http://', host: 'airflow', domain: '--configure me--'},
    dbtDocs: {protocol: 'http://', host: 'dbt-docs', domain: '--configure me--'},
    dmViewer: {protocol: 'http://', host: 'dm-db', domain: '--configure me--'},
    dwhViewer: {protocol: 'http://', host: 'dwh-db', domain: '--configure me--'},
    filebrowser: {protocol: 'http://', host: 'fs', domain: '--configure me--'},
    advancedAnalyticsViewer: {protocol: 'http://', host: 'jupyterhub', domain: '--configure me--'},
    devToolsMailbox: {protocol: 'http://', host: 'mb', domain: '--configure me--'},
    devToolsFileBrowser: {protocol: 'http://', host: 'fs', domain: '--configure me--'}
  },
  footerConfig: {
    openSourceDataPlatformUrl: '--configure me--',
    licenseUrl: '--configure me--',
    githubUrl: '--configure me--',
    versionLink: '--configure me--'
  },
  matomoConfig: {
    enabled: false,
    siteId: 1, // <-- Replace with your real Site ID
    trackerUrl: 'http://localhost:8081/' // <-- Replace with your real Matomo URL
  }
};
