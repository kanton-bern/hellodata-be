# HelloDATA Portal UI E2E Tests

Basic regression suite for the portal, written with [Playwright](https://playwright.dev). It runs against the portal,
the default data domain's Superset, Airflow, CloudBeaver and SFTPGo of the
[docker compose installation](../../hello-data-deployment/docker-compose/README.md) and logs in through the real
Keycloak login page.

The specs are grouped by feature:

| Folder                  | Covers                                                                                       |
|-------------------------|----------------------------------------------------------------------------------------------|
| `tests/portal/`         | Login, logout, navigation, the home page, the data domain selector and the isolation of the data domains (selector, DAGs, CloudBeaver connections, dashboards) |
| `tests/administration/` | Users (invite, role, delete), portal roles, FAQ management, which pages each role may open by url and users disabled in the portal |
| `tests/dashboards/`     | Superset dashboards in the portal: access, drafts, filters and metadata per role, dashboard groups and the dashboard access log |
| `tests/superset/`       | The Superset instance itself: opening it from the portal, dashboard export/import, databases, its UI in the portal language |
| `tests/comments/`       | Dashboard comments: permissions per role, review workflow, filtering, deletion, versions     |
| `tests/batch-import/`   | Batch import of users and roles from csv files: new users, existing users, last record wins, dashboard groups, file validation |
| `tests/data-eng/`       | The Data Eng. menu: Orchestration (Airflow) and its breadcrumb, DAG status per data domain, DWH and DataMarts Viewer (CloudBeaver connections per role), Filebrowser (SFTPGo) |

Every test and `test.describe` has a comment right above it that says in plain words what it does and what it
expects. Scenarios from Jira name their ticket (e.g. `HELLODATA-687`) on the last line of that comment. The published
report shows both (see [Published reports](#published-reports)), so keep the comment directly above the call - not
above a surrounding `for` loop - when adding a test.

## Running locally

Start the portal, the default data domain's Superset and their dependencies (a separate compose project, so your own
local data is not touched):

`minio/minio` and `minio/mc` are no longer published on Docker Hub, so add the e2e override (it uses the bitnami
builds for SFTPGo's S3 storage, and gives the portal the second data domain `Extra Data Domain` the data domain selector
tests need) to the compose files first:

```bash
cd hello-data-deployment/docker-compose
export COMPOSE_FILE="$(grep '^COMPOSE_FILE=' .env | cut -d= -f2):../../hello-data-portal/hello-data-portal-ui-e2e/compose-override.yaml"
HD_SUPERSET_PUBLISH_INTERVAL_MINUTES=1 HD_DBT_DOCS_PUBLISH_INTERVAL_MINUTES=1 HD_BATCH_USERS_SCAN_INTERVAL_SECONDS=5 \
HD_AIRFLOW_DAG_DIR_LIST_INTERVAL_SECONDS=30 HD_AIRFLOW_PUBLISH_INTERVAL_MINUTES=1 HD_SFTPGO_PUBLISH_INTERVAL_MINUTES=1 \
  docker compose -p hellodata-e2e up -d --wait hello-data-portal-ui hello-data-superset-sidecar-default-data-domain \
  airflow-webserver airflow-scheduler hello-data-airflow-sidecar hello-data-sftpgo-sidecar
```

Use `HD_VERSION=<tag>` in front of the command to test a specific release candidate instead of `latest`.
The variables shorten waits that only matter for the tests (all of them default to the normal values):

- `HD_SUPERSET_PUBLISH_INTERVAL_MINUTES=1` makes the Superset sidecar publish dashboards, users and roles to the portal
  every minute instead of every 10 minutes; the draft dashboard test waits for that.
- `HD_DBT_DOCS_PUBLISH_INTERVAL_MINUTES=1` does the same for the dbt docs sidecar.
- `HD_BATCH_USERS_SCAN_INTERVAL_SECONDS=5` makes the portal api look for batch import csv files every 5 seconds instead
  of every 30; every batch import test waits for that.
- `HD_AIRFLOW_DAG_DIR_LIST_INTERVAL_SECONDS=30` makes Airflow look for new DAG files every 30 seconds instead of every 5
  minutes, and `HD_AIRFLOW_PUBLISH_INTERVAL_MINUTES=1` makes the Airflow sidecar publish the DAGs every minute; the
  pipeline status test adds DAGs and waits for both.
- `HD_SFTPGO_PUBLISH_INTERVAL_MINUTES=1` makes the SFTPGo sidecar publish its instance (the Filebrowser menu entry)
  every minute.

Airflow and SFTPGo need more memory than the rest of the subset; leave them out of the command when only other specs
are run.

On a fresh stack the sidecars may publish their resources before the portal sidecar is listening. They republish on
their interval, so the lineage docs test can fail until then. Wait for the dbt docs app info and the
Superset dashboards first (this is what the CI workflow does, the query returns 4 when the dbt docs, Superset, Airflow
and SFTPGo resources are there):

```bash
docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "select count(distinct module_type) from resource where (kind = 'hellodata/AppInfo' and module_type in ('DBT_DOCS', 'SFTPGO'))
   or (kind = 'hellodata/Dashboards' and module_type = 'SUPERSET') or (kind = 'hellodata/Pipelines' and module_type = 'AIRFLOW')"
```

The portal sidecar can also miss the context roles the portal api publishes for the example users at startup. Those users
then have no portal roles, see an empty menu and fail `tests/administration/roles.spec.ts`. Ask the api for a new
synchronization of all users (it runs within 30 seconds) and wait until this query returns 0, as the CI workflow does:

```bash
docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "update user_sync_lock set status = 'STARTED', modified_date = now()"
docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "select count(*) from user_ u where not exists (select 1 from user_portal_role p where p.user_id = u.id)
   and exists (select 1 from user_context_role c join role r on r.id = c.role_id where c.user_id = u.id and r.name <> 'NONE')"
```

The Extra Data Domain has no subsystem, so no sidecar publishes it and the CloudBeaver sidecar never creates its teams
(`EXTRA_DATA_DOMAIN_READ_DM`, `EXTRA_DATA_DOMAIN_READ_DWH`): its users then see no connections in the DataMarts Viewer.
Publish a copy of the dbt docs app info for it, as a sidecar of that data domain would (this is what the CI workflow
does; the copy keeps the dbt docs url, and the SFTPGo sidecar creates the groups of the data domain as well):

```bash
appinfo=$(docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "select metainfo from resource where kind = 'hellodata/AppInfo' and module_type = 'DBT_DOCS' limit 1")
message=$(jq -c '.instanceName += "-extra-data-domain"
  | .businessContextInfo.subContext += {key: "Extra_Data_Domain", name: "Extra Data Domain", extra: true}' <<< "$appinfo")
docker compose -p hellodata-e2e run --rm -T --entrypoint nats nats-box --server nats:4222 pub publish_resources.app_info "$message"
```

Install and run the tests:

```bash
cd hello-data-portal/hello-data-portal-ui-e2e
npm ci
npx playwright install chromium
npx playwright test          # headless
npx playwright test --ui     # interactive runner
npx playwright test tests/data-eng   # one folder only
E2E_BROWSER_CHANNEL=msedge npx playwright test   # in Microsoft Edge (install it with: npx playwright install msedge)
npx playwright show-report   # open the last HTML report
```

Stop and remove the test stack afterwards:

```bash
cd hello-data-deployment/docker-compose
docker compose -p hellodata-e2e down --volumes
```

## Configuration

| Variable              | Default                                   | Description                                         |
|-----------------------|-------------------------------------------|-----------------------------------------------------|
| `E2E_BASE_URL`        | `http://localhost:8080/app`               | Portal URL                                          |
| `E2E_ADMIN_USERNAME`  | `admin`                                   | Keycloak admin user used by most tests              |
| `E2E_ADMIN_PASSWORD`  | `admin`                                   | Its password                                        |
| `E2E_ADMIN_EMAIL`     | `admin@hellodata.ch`                      | Its email, shown in the top bar and profile         |
| `E2E_COMPOSE_PROJECT` | `$COMPOSE_PROJECT_NAME` or `hellodata-e2e` | Compose project the tests that read the databases or write files (batch import, pipelines, users, dashboard groups) run `docker exec` in; `hellodata` for the everyday stack |
| `E2E_BROWSER_CHANNEL` | empty (Chromium bundled with Playwright)  | Browser channel, e.g. `msedge` for Microsoft Edge |
| `E2E_CLOUDBEAVER_URL` | `http://localhost:8087` if `E2E_BASE_URL` is not set, else empty | CloudBeaver gateway the DWH Viewer tests route `<portal>/cloudbeaver/` to; empty when the installation routes it itself (ingress) |

The other users are the example users created by the portal api's `create-example-users` profile (see
`ExampleUsersInitializer`). `tests/administration/roles.spec.ts` logs in as each of them and checks the menus and pages of its role:

| Role                              | User                                                   | Password     |
|-----------------------------------|--------------------------------------------------------|--------------|
| `HELLODATA_ADMIN`                 | `admin`                                                | `admin`      |
| `BUSINESS_DOMAIN_ADMIN`           | `hellodata-product-development-admin@hellodata.ch`     | `admin`      |
| `DATA_DOMAIN_ADMIN`               | `default-data-domain-admin@hellodata.ch`               | `admin`      |
| `DATA_DOMAIN_EDITOR`              | `default-data-domain-editor@hellodata.ch`              | `editor`     |
| `DATA_DOMAIN_BUSINESS_SPECIALIST` | `default-data-domain-business_specialist@hellodata.ch` | `specialist` |
| `DATA_DOMAIN_VIEWER`              | `default-data-domain-viewer@hellodata.ch`              | `viewer`     |

The example users are created only once per data domain, so an existing installation that was started before the
business specialist was added does not get that user. Use a fresh stack (`down --volumes`) to run the role tests.

Pointing `E2E_BASE_URL` at another installation (e.g. a port-forwarded cluster) works as long as the Keycloak client
accepts that URL as redirect URI.

## CI

`.github/workflows/e2e.yml` runs the suite for every release candidate tag (`*.rc*`) right after `maven.yml` pushed the
images, and every working night (Monday to Friday, 02:00 UTC) against the images of `develop` (`latest`) in Chromium,
so a fresh report of `latest` is on the reports page every morning. Schedules only run from the default branch. It can also be started manually on any branch (Actions -> "E2E tests (docker compose)" -> Run workflow) with
the image tag to test and the browser (Chromium or Microsoft Edge). The tests run from the selected branch, the images come from the given tag. Every push publishes
images: `develop` as `latest`, and gitflow branches as `<name after the prefix>-SNAPSHOT` (e.g. `feature/my-change` ->
`my-change-SNAPSHOT`), so a branch can be tested once its `maven.yml` run has finished. The HTML report, traces of
failed tests and the compose logs are uploaded as a workflow artifact.

### Published reports

Every run is also summarized and published as a web page, so it can be shared without downloading the artifact:

1. `Record image digests` writes `images.json`, the digests of the images that were pulled. Tags like `latest` or
   `*-SNAPSHOT` move, so the digests show which images were really tested.
2. `Summarize test results` runs `scripts/summarize.mjs`. It reads the JSON report of Playwright
   (`test-results/results.json`, CI only) and writes `summary.json`: the version, browser, commit, images and every test
   with its feature folder, description, `test.step()` titles, status (passed, failed, flaky, skipped), retries, error
   and Jira tickets. The description and the tickets come from the comment right above a test or its `test.describe`;
   each test also links to its actions (clicks, navigations, assertions) in the full Playwright report. The same summary is shown on the
   page of the workflow run. When the stack did not start, the summary has the status `error` and no tests.
3. The `publish` job runs `scripts/build-reports-site.mjs` in the `e2e/` folder of the `gh-pages` branch, which GitHub
   Pages serve next to the docs at https://kanton-bern.github.io/hellodata-be/e2e/ (linked in the docs under More):
   - `e2e/` - the list of tested versions, last tested first, with the latest result per browser
   - `e2e/<version>/` - the runs of a version and what its latest run tested, e.g.
     https://kanton-bern.github.io/hellodata-be/e2e/e2e-playwright-SNAPSHOT/
   - `e2e/<version>/<browser>/<run id>-<attempt>/` - what was tested in a run and the result
   - `e2e/<version>/<browser>/<run id>-<attempt>/report/` - the full Playwright HTML report

   The pages are rebuilt from the `summary.json` files every time, and only the last 3 runs per version and browser
   and the last 30 versions are kept. Compose logs are not published, they stay in the workflow artifact.

`.github/workflows/deploy_docs.yml` also writes to `gh-pages`: it replaces the docs and keeps `e2e/`. Both use
`.github/scripts/update-gh-pages.sh`, which pushes the branch as a single commit without history (so old reports do not
stay in every clone of the repository) and runs the change again when the other workflow pushed in the meantime.

The reports are public. They contain screenshots, videos and traces of the test stack (example users only).

To try the scripts locally after a run (`CI=1` adds the JSON reporter):

```bash
CI=1 npx playwright test tests/portal
HD_VERSION=local node scripts/summarize.mjs --out summary.json
node scripts/build-reports-site.mjs --site /tmp/e2e-site/e2e --summary summary.json --report playwright-report
open /tmp/e2e-site/e2e/index.html
```

## Writing tests

- Import `test` and `expect` from `./fixtures`, not from `@playwright/test`, so the custom fixtures are available.
- Ask for the `adminPage` fixture to start logged in as admin on the home page. Use the plain `page` fixture when the
  test controls the login itself (other user, wrong password, or listening for requests made during login).
- Every test gets a fresh browser context and logs in on its own, so tests do not share a session.
- `tests/fixtures.ts` also provides helpers: `openPortalAs()` (Keycloak login, waits for the home page and switches to
  English), `signIn()`, `selectOption()` (picks an option of a PrimeNG select), `selectLanguage()` (switches the language in the top bar and waits until the portal saved it),
  `navigate()` / `openMenu()` / `expandMenuGroup()` for the left sidebar, `errorToasts()` and
  `dismissToasts()`.
- `tests/superset.ts` has the helpers for dashboards and Superset: `changeDashboardAccessAsAdmin()` (gives a viewer or
  business specialist access to the showcase dashboard in user management), `expectDashboardListed()` (reloads the
  dashboard list until the sync with Superset is done), `openDashboard()` (returns the embedded dashboard as a frame),
  `openSuperset()` (opens the Superset instance in a new tab), `setDashboardPublished()` and `withUser()` (runs a step
  as another user in its own browser context).
- `tests/comments.ts` has the helpers for dashboard comments: `changeCommentPermissionAsAdmin()` (sets the read, write
  or review permission of a user in the default data domain, optionally together with the dashboard access),
  `openComments()` (opens the showcase dashboard and its comments panel), `addComment()`, `sendForReview()`,
  `publish()`, `decline()`, `editComment()`, `deleteComment()`, `filterComments()` and `showDetails()`.
- `tests/batch-import.ts` has the helpers for the batch import. The import has no UI: the portal api picks up csv files
  from `/storage/batch-users-processing` every 30 seconds (5 with the command above). `importCsv()` / `importInvalidCsv()` copy a file into the
  portal api container with `docker exec` and wait for the result in `users-output.log`. The result is checked in the
  databases: `portalRoles()`, `portalDashboards()`, `portalCommentPermission()` in the portal and `supersetRoles()` /
  `expectSupersetRoles()` in Superset (`ab_user`, `ab_user_role`, `ab_role`). `createDashboardGroup()`,
  `dashboardGroupMembers()` and `deleteDashboardGroup()` work on the dashboard groups of the portal. So these tests need
  the docker CLI and the compose stack on the machine that runs Playwright. They create the Superset roles `RLS_E2E_A`
  and `RLS_E2E_B` once and keep them, and delete the users and groups they created in `afterAll`.
- `tests/data-eng.ts` has the helpers for the Data Eng. menu: `openOrchestration()` (the embedded Airflow),
  `breadcrumb()`, `addDag()` / `removeDag()` (write a DAG file into the dags folder of a data domain with `docker exec`),
  `pipelineIds()` / `expectPipelines()` (the DAGs `/api/pipelines` reports to the logged in user), `openDwhViewer()` and
  `routeCloudbeaver()` (the portal embeds `<portal>/cloudbeaver/`, which only an ingress serves, so the tests route it to
  the gateway on port 8087; CloudBeaver's event web socket cannot be routed to another port and is answered by a silent
  mock), `openDataMartsViewer()` (CloudBeaver embedded in the portal), `expandConnectionsFolder()` (expands the shared
  connections folder, and presses CloudBeaver's Retry when it failed to load the connections of a new session) and
  `cloudbeaverConnection()`, `openFilebrowser()`, `openFolder()`, `createFolder()`, `renameFolder()`, `deleteFolder()` for SFTPGo and
  `expectNoDataEngEntry()`. `tests/docker.ts` has the `docker exec` and SQL helpers shared with the batch import.
- Only the admin roles (`HELLODATA_ADMIN`, `BUSINESS_DOMAIN_ADMIN`, `DATA_DOMAIN_ADMIN`) can comment by default. Comment
  tests give the other roles a comment permission in `beforeAll` and set it back to none in `afterAll`. A reviewer does
  not see the drafts of other users, so a comment is cleaned up by its author.
- Viewers and business specialists see no dashboard until an admin gives them access. Tests that need it give it in
  `beforeAll` and take it away in `afterAll`.
- Prefer roles and element ids over CSS structure, and use Playwright's auto-waiting assertions instead of fixed waits.
- Tests share one portal instance, so data created by a test must have a unique name and be cleaned up by that test.
