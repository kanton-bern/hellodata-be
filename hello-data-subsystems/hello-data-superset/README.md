# Superset with a flavor of HelloDATA

## What's the difference to the official superset docker image?

- Base image: `apache/superset:2.1.0`
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