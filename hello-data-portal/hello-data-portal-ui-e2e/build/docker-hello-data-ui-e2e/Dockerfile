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

##### Stage 1

FROM node:16.14.2 as builder

WORKDIR /build_ui

# Copy project files to the docker image
COPY . .

# install angular/cli globally (latest version, change this to the version you are using)
RUN yarn global add @angular/cli@15.2.0

# if you prefer npm, replace the above command with
# RUN npm install @angular/cli@latest -g

# install packages
RUN yarn install

# FOR NPM
# npm install


# Build Angular Application in Production
RUN yarn build -- --configuration e2e

RUN ls -la /build_ui

#### STAGE 2
#### Deploying the application
FROM registry.mgmtbi.ch/docker_base/nginx:1

LABEL MAINTAINER="DAP Team"


COPY --from=builder /build_ui/dist/hellodata-portal-ui /app

COPY --from=builder /build_ui/build/docker/nginx.conf /etc/nginx/nginx.conf

RUN chown -R nginx: /app

EXPOSE 80

# run nginx under tini's supervision
# -s is not necessary, since tini will run as PID 1
ENTRYPOINT ["tini", "--"]

CMD ["nginx", "-g", "daemon off;"]
