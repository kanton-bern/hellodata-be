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
  portalApi: 'http://hello-data-portal-api:8081/api',
  docsApi: 'http://localhost:8081',
  debugInfoEnabled: true,
  authConfig: {
    logLevel: LogLevel.Debug,
    authority: 'http://hellodata-portal-keycloak:8080/realms/hellodata',
    redirectUrl: 'http://hellodata-portal-ui:80/',
    postLogoutRedirectUri: 'http://hellodata-portal-ui:80/',
    clientId: 'frontend-client',
    scope: 'openid profile email'
  },
  deploymentEnvironment: {
    name: 'DEV'
  },
  locale: 'de-CH',
  domainNamespace: 'hellodata-local',
  baseDomain: 'cluster.local',
  subSystemsConfig: {
    airflow: {protocol: 'http://', host: 'airflow', domain: '.cluster.local'},
    dbtDocs: {protocol: 'http://', host: 'dbt-docs', domain: '.cluster.local'},
    dmViewer: {protocol: 'http://', host: 'dm-db', domain: '.cluster.local'},
    dwhViewer: {protocol: 'http://', host: 'dwh-db', domain: '.cluster.local'},
    filebrowser: {protocol: 'http://', host: 'fs', domain: '.cluster.local'},
    advancedAnalyticsViewer: {protocol: 'http://', host: 'jupyterhub', domain: '.cluster.local'},
    monitoringStatus: {protocol: 'http://', host: 'status', domain: '.cluster.local'},
    devToolsMailbox: {protocol: 'http://', host: 'mb', domain: '.cluster.local'},
    devToolsFileBrowser: {protocol: 'http://', host: 'fs', domain: '.cluster.local'}
  },
  footerConfig: {
    openSourceDataPlatformUrl: 'https://kanton-bern.github.io/hellodata-be',
    licenseUrl: 'https://github.com/kanton-bern/hellodata-be/blob/main/LICENSE',
    githubUrl: 'https://github.com/kanton-bern/hellodata-be',
    versionLink: 'https://github.com/kanton-bern/hellodata-be/releases/'
  },
  matomoConfig: {
    enabled: false,
    siteId: 1, // <-- Replace with your real Site ID
    trackerUrl: 'http://localhost:8081/' // <-- Replace with your real Matomo URL
  }
};
