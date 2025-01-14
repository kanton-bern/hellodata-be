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

FROM apache/airflow:slim-2.8.1-python3.10

ENV AIRFLOW_VERSION=2.8.1
ENV PYTHON_VERSION=3.10

## Change container image to root user to allow for global
## installation of software
USER root

RUN apt-get update && apt-get install -y --no-install-recommends \
         vim nano gdal-bin \
  # deps for liquibase
  && apt-get install --no-install-recommends -y openjdk-17-jre-headless gnupg unzip postgresql-client \
  && apt-get autoremove -yqq --purge \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

## Install needed airflow dependencies
USER airflow
RUN python -m pip install --upgrade pip
RUN pip install --upgrade pip  \
  &&  pip install --no-cache-dir psycopg2-binary Redis authlib==0.15.5 \
  &&  pip install --no-cache-dir "apache-airflow[celery,postgres,kubernetes,statsd,mssql]==${AIRFLOW_VERSION}" --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-${AIRFLOW_VERSION}/constraints-${PYTHON_VERSION}.txt" \
  &&  pip install --no-cache-dir virtualenv

# dbt dependencies
RUN pip install --no-cache-dir dbt-core==1.5.0 dbt-postgres==1.5.0

# add additional python requirements
RUN pip install --no-cache-dir pyaxis pygdaltools pyctuator

# add sftp provider
RUN pip --no-cache-dir install apache-airflow-providers-sftp

#USER root
# Copy config files to container's $AIRFLOW_HOME
#COPY webserver_config.py $AIRFLOW_HOME/webserver_config.py
#COPY user_auth.py $AIRFLOW_HOME/user_auth.py
COPY airflow.cfg $AIRFLOW_HOME/airflow.cfg

#COPY settings.py /home/airflow/.local/lib/python3.10/site-packages/airflow/settings.py


##################### Add Liquibase to manage custom database scripts ######################
USER root

# install drivers to take screenshots
RUN apt-get update && \
    apt-get install -y dos2unix && \
    # deps for airflow
    apt-get install --no-install-recommends -y wget

# Make /liquibase directory and change owner to superset
RUN mkdir /liquibase && chown airflow /liquibase
WORKDIR /liquibase

# Symbolic link will be broken until later
RUN ln -s /liquibase/liquibase /usr/local/bin/liquibase

# Set LIQUIBASE_HOME environment variable
ENV LIQUIBASE_HOME=/liquibase

# Latest Liquibase Release Version
ARG LIQUIBASE_VERSION=4.21.1
# Download, verify, extract liquibase
ARG LB_SHA256=C04542865E5ECE8B7B1EE9BD6BEAEFC5315E350620288D6AC1A2D32C3B1F7D8B
RUN set -x &&  \
    wget -q -O liquibase-${LIQUIBASE_VERSION}.tar.gz "https://github.com/liquibase/liquibase/releases/download/v${LIQUIBASE_VERSION}/liquibase-${LIQUIBASE_VERSION}.tar.gz"
RUN echo "$LB_SHA256 liquibase-${LIQUIBASE_VERSION}.tar.gz" | sha256sum -c -
RUN tar -xzf liquibase-${LIQUIBASE_VERSION}.tar.gz


# add liquibase migrations
COPY --chown=airflow:root ./db/changelog /db/changelog
COPY --chown=airflow:root ./db/wait-and-migrate.sh /db/wait-and-migrate.sh
COPY --chown=airflow:root ./entrypoint.sh /app/entrypoint.sh

RUN chmod a+x /db/*.sh
RUN chmod a+x /app/*.sh

#Change line-endings for liquibase-file from Windows to Unix and cleanup
RUN dos2unix /db/wait-and-migrate.sh && apt-get --purge remove -y dos2unix && rm -rf /var/lib/apt/lists/*

# Switching back to using the `airflow` user
USER airflow
WORKDIR /app
ENTRYPOINT ["/usr/bin/dumb-init", "--", "/app/entrypoint.sh"]
