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

FROM bedag/hello-data-airflow

ADD conf/airflow.cfg /opt/airflow/airflow.cfg
ADD conf/webserver_config.py /opt/airflow/webserver_config.py

ADD db/02_01_add_extra_dd_connection.sql /db/changelog/changelogs/02_01_add_extra_dd_connection.sql
RUN sed -i '/<\/databaseChangeLog>/d' /db/changelog/airflow-changelog.xml

# Append custom dc changeset to the file
RUN echo '<changeSet id="0002_01" author="hellodata" runOnChange="true">' >> /db/changelog/airflow-changelog.xml \
    && echo '    <sqlFile path="changelogs/02_01_add_extra_dd_connection.sql" relativeToChangelogFile="true" splitStatements="false" />' >> /db/changelog/airflow-changelog.xml \
    && echo '</changeSet>' >> /db/changelog/airflow-changelog.xml \
    && echo '</databaseChangeLog>' >> /db/changelog/airflow-changelog.xml

RUN ls -la /db/changelog/changelogs
RUN cat /db/changelog/airflow-changelog.xml
