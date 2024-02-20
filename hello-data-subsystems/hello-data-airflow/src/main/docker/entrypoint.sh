#!/bin/bash

# Run wait and migrate on each start and then the entrypoint command for the airflow
# https://airflow.apache.org/docs/docker-stack/entrypoint.html#entrypoint-commands
exec /db/wait-and-migrate.sh & /entrypoint "${@}"
