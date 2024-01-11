# Aiflow with a flavor of HelloDATA

## What's the difference to the official airflow docker image?
- Base image: `apache/airflow:2.5.3` with python `3.10`
- Added `dbt-core==1.5.0`, `dbt-postgres==1.5.0`
- Added `pyaxis` `pygdaltools`