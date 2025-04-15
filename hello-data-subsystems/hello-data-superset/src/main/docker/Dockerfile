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

FROM bedag/hellodata-superset:5.0-HD.rc1

COPY ./assets/images/* /app/superset/static/assets/images/

# Switching to root to install the required packages
USER root

# install drivers to take screenshots
RUN apt-get update && \
    # deps for superset
    apt-get install --no-install-recommends -y firefox-esr wget gcc g++ && \
    # deps for liquibase
    apt-get install --no-install-recommends -y openjdk-17-jre-headless ca-certificates-java gnupg unzip postgresql-client

ENV GECKODRIVER_VERSION=0.34.0
RUN wget -q https://github.com/mozilla/geckodriver/releases/download/v${GECKODRIVER_VERSION}/geckodriver-v${GECKODRIVER_VERSION}-linux64.tar.gz && \
    tar -x geckodriver -zf geckodriver-v${GECKODRIVER_VERSION}-linux64.tar.gz -O > /usr/bin/geckodriver && \
    chmod 755 /usr/bin/geckodriver && \
    rm geckodriver-v${GECKODRIVER_VERSION}-linux64.tar.gz

# Install requirements for superset
RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && uv pip install --no-cache authlib==1.5.2 psycopg2-binary==2.9.10 redis==5.2.1 jmespath==1.0.1 gevent

# install additional drivers
# Find which driver you need based on the analytics database you want to connect to here:
# https://superset.apache.org/installation.html#database-dependencies
# SqlServer
RUN uv pip install pymssql
# psycopg2
RUN uv pip install psycopg2
# oracle
RUN uv pip install cx_Oracle
# statsd
RUN uv pip install statsd

# Flower is a web based tool for monitoring the Celery cluster
RUN uv pip install flower

# Monitoring service with https://github.com/SolarEdgeTech/pyctuator
RUN uv pip install pyctuator

# CSP https://superset.apache.org/docs/security/
# https://pypi.org/project/flask-talisman/
RUN uv pip install flask-talisman

# CORS
RUN uv pip install flask-cors

##################### Add Liquibase to manage custom database scripts ######################
# Make /liquibase directory and change owner to superset
RUN mkdir /liquibase && chown superset /liquibase
WORKDIR /liquibase

# Symbolic link will be broken until later
RUN ln -s /liquibase/liquibase /usr/local/bin/liquibase

# Set LIQUIBASE_HOME environment variable
ENV LIQUIBASE_HOME=/liquibase

# Latest Liquibase Release Version
ARG LIQUIBASE_VERSION=4.25.1
# Download, verify, extract liquibase
ARG LB_SHA256=8B2B7AA8EC755D4EE52FA0210CD2A244FD16ED695FC4A72245562950776D2A56
RUN set -x &&  \
    wget -q -O liquibase-${LIQUIBASE_VERSION}.tar.gz "https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.tar.gz"
RUN echo "$LB_SHA256 liquibase-${LIQUIBASE_VERSION}.tar.gz" | sha256sum -c -
RUN tar -xzf liquibase-${LIQUIBASE_VERSION}.tar.gz


# add liquibase migrations
COPY --chown=superset ./db/changelog /db/changelog
COPY --chown=superset ./wait-and-migrate.sh /db/wait-and-migrate.sh
COPY --chown=superset ./entrypoint.sh /db/entrypoint.sh
#RUN set -xe \
#    && chmod +x /db/wait-and-migrate.sh \
#    && chown -R superset:superset /db

RUN chmod a+x /db/*.sh

# Switching back to using the `superset` user
USER superset
WORKDIR /app

CMD ["/bin/sh", "-c", "/db/entrypoint.sh"]
