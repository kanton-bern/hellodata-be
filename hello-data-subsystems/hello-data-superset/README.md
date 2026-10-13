# Superset with a flavor of HelloDATA

## What's the difference to the official superset docker image?

- Base image: `bedag/hellodata-superset:6-1-HD-rc1` (HelloDATA fork of Apache Superset 6.1, built from the `6.1-HD.rc1` branch of the superset-fork repo)
- Added `firefox/geckodriver` to be able to create dashboard screenshots and send reports
- Added `flower` to monitor the celery cluster.
- Added custom database drivers
- Contains `liquibase` to manage the custom HelloDATA sql scripts

## Contains following database drivers:
  - PostgreSQL
  - MySql
  - SqlServer
  - Oracle
  - Elasticsearch
  - SAP HANA
  - Snowflake
  - google sheets

## Liquibase
To run liquibase database migrations, call:
```shell
/db/wait-and-migrate.sh
```
This will run the `liquibase update` command.