# hello-data-airflow3 — Airflow 3 image (flavored for HelloDATA)

The Airflow **3** counterpart of `hello-data-airflow`, built to run **side by side** with the
Airflow 2.8 image during the migration. Produces image `bedag/hello-data-airflow3`.

## What differs from `hello-data-airflow` (2.8)
- **Base image:** `apache/airflow:slim-3.2.2-python3.12` (was `slim-2.8.1-python3.10`).
- **Providers:** `apache-airflow[celery,postgres,cncf.kubernetes,statsd,mssql,fab]` + `apache-airflow-providers-standard` (Airflow 3 renamed the k8s extra to `cncf.kubernetes` and split FAB / core operators into providers).
- **No `02_triggers.sql`.** Per-data-domain DAG access is now done natively by a cluster policy — see `airflow_local_settings.py` (`dag_policy`) + FAB `access_control` sync. The other Liquibase changelogs (`00/01/03/04`) are unchanged.
- **`webserver_config.py` rewritten** for the FAB auth manager (`apache-airflow-providers-fab`): OAuth to Keycloak with a **public client + PKCE** (no secret), **auto-redirect** to Keycloak (portal-iframe friendly), `AUTH_ROLES_MAPPING`, dynamic `DD_<domain>` pass-through, and the `hd_base` base role.
- **`airflow_local_settings.py` added** (baked into `$AIRFLOW_HOME/config/`) — the `dag_policy`.
- **`user_auth.py` removed** — the Airflow-2 FAB REST auth backend is gone; the sidecar uses the Airflow-3 REST API v2 + JWT instead.
- Reused unchanged: `entrypoint.sh` (non-root `airflow`, migrate → stage standard DAGs → exec stock entrypoint — it passes `"$@"` so it works for `api-server`/`dag-processor` too), the Liquibase install + `wait-and-migrate.sh`, dbt/GDAL/private-plugin layers, `AF_OPERATOR` role (`01_...sql`).

## Required environment (set by the deployment/Helm, not baked)
- **Its own metadata database** — this image MUST point at a **separate** Postgres DB from Airflow 2 (`DB_HOST/DB_NAME/DB_PORT/DB_USER/DB_PASS` for Liquibase; `AIRFLOW__DATABASE__SQL_ALCHEMY_CONN` for Airflow). The two Airflows share no state.
- **Keycloak:** `KEYCLOAK_BASE_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` (public client).
- **Airflow 3 config** (via env, like the PoC): `AIRFLOW__CORE__AUTH_MANAGER=airflow.providers.fab.auth_manager.fab_auth_manager.FabAuthManager`, `AIRFLOW__API__BASE_URL`, `AIRFLOW__FAB__ENABLE_PROXY_FIX=True`, `AIRFLOW__API_AUTH__JWT_SECRET`, `AIRFLOW__API__SECRET_KEY`, executor.
- Components run as `airflow api-server` and `airflow dag-processor` (+ scheduler/triggerer/workers) via the container command.

## TODO before production use
- **Private HelloDATA plugins** (`hellodata_be_airflow_pod_operator_params`, `hellodata_be_dag_logs`) are Airflow-2 builds — publish/point at Airflow-3-compatible versions.
- **dbt** pinned to `1.8.2` for py3.12 — verify the build; bump if needed.
- **`airflow.cfg`** is the seeded 2.8 template; most keys are overridden by env at deploy, but review `[core] auth_manager`, `[api]`, executor, and drop `[webserver]`-only keys.
- **Standard DAG** `hellodata-standard-dags/monitoring_dag.py` uses the ORM (`DagModel`/`DagRun`) — verify against the Airflow-3 ORM.
