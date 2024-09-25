#
# Copyright © 2024, Kanton Bern
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

FROM bedag/hello-data-portal-ui

LABEL MAINTAINER="HelloDATA Team"

RUN mkdir -p /app/config

ARG HELLO_DATA_AUTH_SERVER_URL

RUN echo "window.environment = { \
            production: false, \
            portalApi: 'http://localhost:8081/api', \
            docsApi: 'http://localhost:8086/dbt-docs', \
            debugInfoEnabled: true, \
            authConfig: { \
              logLevel: 1, \
              authority: 'http://localhost:38080/realms/hellodata', \
              redirectUrl: 'http://localhost:8080/app/', \
              postLogoutRedirectUri: 'http://localhost:8080/app/', \
              clientId: 'frontend-client' \
            }, \
            domainNamespace: 'hellodata', \
            baseDomain: 'localhost', \
            deploymentEnvironment: { \
              name: 'PREVIEW', \
              headerColor: 'rgb(9,106,232)' \
            }, \
            locale: 'de-CH', \
            subSystemsConfig: { \
              airflow: {protocol: 'http://', host: 'localhost', domain: ':28080'}, \
              dbtDocs: {protocol: 'http://', host: 'localhost', domain: ':8086/dbt-docs'}, \
              dmViewer: {protocol: 'http://', host: 'localhost', domain: ':8087/cloudbeaver/'}, \
              dwhViewer: {protocol: 'http://', host: 'localhost', domain: ':8087/cloudbeaver/'}, \
              advancedAnalyticsViewer: {protocol: 'http://', host: 'localhost', domain: ':8088'}, \
              monitoringStatus: {protocol: 'http://', host: 'localhost', domain: ':5099'}, \
              devToolsMailbox: {protocol: 'http://', host: 'localhost', domain: ':8001'}, \
              devToolsFileBrowser: {protocol: 'http://', host: 'localhost', domain: ':8090'} \
            }, \
            footerConfig: { \
              openSourceDataPlatformUrl: 'https://kanton-bern.github.io/hellodata-be', \
              licenseUrl: 'https://github.com/kanton-bern/hellodata-be/blob/main/LICENSE', \
              githubUrl: 'https://github.com/kanton-bern/hellodata-be', \
              versionLink: 'https://github.com/kanton-bern/hellodata-be/releases/' \
            } \
          }" > /app/config/environment.js

RUN chown -R nginx: /app

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
