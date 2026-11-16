#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# Airflow 3 cluster policy — trigger-free per-data-domain access control.
#
# Replaces the old Postgres triggers (02_triggers.sql). At DAG parse time this
# derives the data domain from the DAG's folder and stamps `access_control` so the
# matching FAB role DD_<domain> can read/trigger only that domain's DAGs. Airflow
# then syncs the per-DAG FAB permissions natively (no ab_* table triggers).
#
import re

# Top-level folders under the dags dir that are NOT data domains.
_NON_DOMAIN = {"hellodata", "hellodata_standard_dags", "__pycache__"}


def dag_policy(dag):
    # HelloDATA layout: /opt/airflow/dags/<domain>/... (same first-folder rule the
    # old create_dag_security_entries() trigger used on dag.fileloc).
    match = re.match(r"^/opt/airflow/dags/([a-zA-Z0-9_]+)", dag.fileloc or "")
    if not match:
        return
    domain = match.group(1)
    if domain in _NON_DOMAIN:
        return
    # Airflow-3 resource-keyed access_control. Valid resources are only "DAGs" and
    # "DAG Runs"; "DAG Runs":can_create is what allows TRIGGERING a run.
    dag.access_control = {
        f"DD_{domain}": {
            "DAGs": {"can_read", "can_edit"},
            "DAG Runs": {"can_read", "can_create", "can_delete"},
        }
    }
