FROM apache/nifi:2.3.0

RUN curl --output /opt/nifi/nifi-current/lib/postgresql-42.7.5.jar https://jdbc.postgresql.org/download/postgresql-42.7.5.jar
RUN curl --output /opt/nifi/nifi-current/lib/mssql-jdbc-12.8.1.jre11.jar https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.8.1.jre11/mssql-jdbc-12.8.1.jre11.jar

# Prepare the flowfile directory
RUN mkdir -p /opt/nifi/nifi-current/flowfile_config
RUN chown -R nifi:nifi /opt/nifi/nifi-current/flowfile_config

USER root
COPY start.sh /opt/nifi/scripts/start.sh
RUN chown -R nifi:nifi /opt/nifi/scripts/start.sh
RUN chmod +x /opt/nifi/scripts/start.sh
USER nifi