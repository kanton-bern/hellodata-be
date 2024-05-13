#!/bin/bash
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

# Run wait and migrate on each start and then the entrypoint command for the airflow
# https://airflow.apache.org/docs/docker-stack/entrypoint.html#entrypoint-commands

# Check if environment variables are available
if [ -n "$DB_HOST" ] && [ -n "$DB_NAME" ] && [ -n "$DB_PORT" ] && [ -n "$DB_USER" ] && [ -n "$DB_PASS" ]; then
    # Execute wait-and-migrate.sh in background
    /db/wait-and-migrate.sh &

    # Capture the process ID of wait-and-migrate.sh
    WAIT_AND_MIGRATE_PID=$!

    # Wait for wait-and-migrate.sh to finish and capture its exit code
    wait $WAIT_AND_MIGRATE_PID
    WAIT_AND_MIGRATE_EXIT_CODE=$?

    # Check if wait-and-migrate.sh failed
    if [ $WAIT_AND_MIGRATE_EXIT_CODE -ne 0 ]; then
        echo "[ENTRYPOINT]: wait-and-migrate.sh failed with exit code $WAIT_AND_MIGRATE_EXIT_CODE"
        exit $WAIT_AND_MIGRATE_EXIT_CODE
    else
        echo "[ENTRYPOINT]: wait-and-migrate.sh executed successfully"
        airflow db upgrade
        # Execute entrypoint only if wait-and-migrate.sh succeeded
        exec /entrypoint "${@}"
    fi
else
    echo "[ENTRYPOINT]: Missing one or more required environment variables (DB_HOST, DB_NAME, DB_PORT, DB_USER, DB_PASS). Skipping wait-and-migrate.sh."
    # Execute entrypoint directly
    exec /entrypoint "${@}"
fi