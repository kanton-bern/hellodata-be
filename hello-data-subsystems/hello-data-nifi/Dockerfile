FROM apache/nifi:2.3.0

COPY ./postgresql-42.7.5.jar /opt/nifi/nifi-current/lib/postgresql-42.7.5.jar
COPY ./mssql-jdbc-12.8.1.jre11.jar /opt/nifi/nifi-current/lib/mssql-jdbc-12.8.1.jre11.jar

# Prepare the flowfile directory
RUN mkdir -p /opt/nifi/nifi-current/flowfile_config
RUN chown -R nifi:nifi /opt/nifi/nifi-current/flowfile_config

USER root
COPY start.sh /opt/nifi/scripts/start.sh
RUN chown -R nifi:nifi /opt/nifi/scripts/start.sh
RUN chmod +x /opt/nifi/scripts/start.sh

COPY state-management.xml /opt/nifi/nifi-current/conf/state-management.xml
RUN chown nifi:nifi /opt/nifi/nifi-current/conf/state-management.xml
USER nifi