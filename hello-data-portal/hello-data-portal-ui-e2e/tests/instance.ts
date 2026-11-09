// The names that differ between HelloDATA installations. Each one is read from an environment variable and defaults to
// the docker compose stack (compose-override.yaml), so a plain `npm test` keeps running against compose. For another
// installation put them in a local env file (see "Running against another installation" in the README).

function env(name: string, composeDefault: string) {
  return process.env[name] || composeDefault;
}

// the keys of the two data domains, part of urls, DAG ids, DAG folders and the ids of permission checkboxes
export const CONTEXT_KEY = env('E2E_CONTEXT_KEY', 'Default_Data_Domain');
export const EXTRA_CONTEXT_KEY = env('E2E_EXTRA_CONTEXT_KEY', 'Extra_Data_Domain');

// the business domain, its name prefixes the CloudBeaver folder of the data domain connections
export const BUSINESS_DOMAIN_NAME = env('E2E_BUSINESS_DOMAIN_NAME', 'HelloDATA Product Development');

// the e-mail domain of the example users the portal api creates (create-example-users profile)
export const EXAMPLE_USERS_EMAIL_DOMAIN = env('E2E_EXAMPLE_USERS_EMAIL_DOMAIN', 'hellodata.ch');
export const BUSINESS_DOMAIN_ADMIN_USERNAME = env('E2E_BUSINESS_DOMAIN_ADMIN_USERNAME',
  `hellodata-product-development-admin@${EXAMPLE_USERS_EMAIL_DOMAIN}`);

// the databases the tests prepare data in or check results in (see docker.ts)
export const PORTAL_DB = env('E2E_PORTAL_DB', 'hd_metainfo');
export const SUPERSET_DB = env('E2E_SUPERSET_DB', 'superset_default_data_domain');

// the Airflow the installation runs: 2 (the compose default) or 3 (compose-airflow3.yaml, HelloDATA 1.7 Helm charts).
// The portal has a menu entry and a page for each (Orchestration, Orchestration (Airflow3)), the specs test this one.
export const AIRFLOW_VERSION = env('E2E_AIRFLOW_VERSION', '2');
if (!['2', '3'].includes(AIRFLOW_VERSION)) {
  throw new Error(`E2E_AIRFLOW_VERSION must be 2 or 3, not ${AIRFLOW_VERSION}`);
}
export const AIRFLOW3 = AIRFLOW_VERSION === '3';

// a DAG of the default data domain that is always there: the showcase DAG of Airflow 2, the e2e DAG of
// compose-airflow3.yaml for Airflow 3
export const SHOWCASE_DAG_ID = env('E2E_SHOWCASE_DAG', `hello_world_${CONTEXT_KEY}`);
