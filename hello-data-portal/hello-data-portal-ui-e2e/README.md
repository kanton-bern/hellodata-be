# HelloDATA Portal UI E2E Tests

Basic regression suite for the portal, written with [Playwright](https://playwright.dev). It runs against the portal
and the default data domain's Superset of the
[docker compose installation](../../hello-data-deployment/docker-compose/README.md) and logs in through the real
Keycloak login page.

The specs are grouped by feature:

| Folder                  | Covers                                                                                       |
|-------------------------|----------------------------------------------------------------------------------------------|
| `tests/portal/`         | Login, logout, navigation and the home page                                                  |
| `tests/administration/` | Users, portal roles and FAQ management                                                       |
| `tests/dashboards/`     | Superset dashboards in the portal: access, drafts, filters and metadata per role              |
| `tests/superset/`       | The Superset instance itself: opening it from the portal, dashboard export/import, databases |
| `tests/comments/`       | Dashboard comments: permissions per role, review workflow, filtering, deletion, versions     |
| `tests/batch-import/`   | Batch import of users and roles from csv files: new users, existing users, last record wins, dashboard groups, file validation |

Scenarios from Jira name their ticket (e.g. `HELLODATA-687`) in a comment above the test.

## Running locally

Start the portal, the default data domain's Superset and their dependencies (a separate compose project, so your own
local data is not touched):

```bash
cd hello-data-deployment/docker-compose
HD_SUPERSET_PUBLISH_INTERVAL_MINUTES=1 docker compose -p hellodata-e2e up -d --wait \
  hello-data-portal-ui hello-data-superset-sidecar-default-data-domain
```

Use `HD_VERSION=<tag>` in front of the command to test a specific release candidate instead of `latest`.
`HD_SUPERSET_PUBLISH_INTERVAL_MINUTES=1` makes the Superset sidecar publish dashboards, users and roles to the portal
every minute instead of every 10 minutes; the draft dashboard test waits for that.

On a fresh stack the sidecars may publish their resources before the portal sidecar is listening. The dbt docs sidecar
republishes every 10 minutes, so the lineage docs test can fail until then. Wait for the dbt docs app info and the
Superset dashboards first (this is what the CI workflow does, the query returns 2 when both are there):

```bash
docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "select count(distinct module_type) from resource where (kind = 'hellodata/AppInfo' and module_type = 'DBT_DOCS')
   or (kind = 'hellodata/Dashboards' and module_type = 'SUPERSET')"
```

Install and run the tests:

```bash
cd hello-data-portal/hello-data-portal-ui-e2e
npm ci
npx playwright install chromium
npx playwright test          # headless
npx playwright test --ui     # interactive runner
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
| `E2E_DWH_HOST`        | `postgres`                                | Database the Superset connection test connects to, as reached from Superset |
| `E2E_DWH_PORT`        | `5432`                                    | Its port                                            |
| `E2E_DWH_DATABASE`    | `hellodata_product_development_default_data_domain_dwh` | Its database name                     |
| `E2E_DWH_USERNAME`    | `postgres`                                | Its user                                            |
| `E2E_DWH_PASSWORD`    | `postgres`                                | Its password                                        |
| `E2E_COMPOSE_PROJECT` | `$COMPOSE_PROJECT_NAME` or `hellodata-e2e` | Compose project the batch import tests run `docker exec` in |

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
images. It can also be started manually on any branch (Actions -> "E2E tests (docker compose)" -> Run workflow) with
the image tag to test. The tests run from the selected branch, the images come from the given tag. Every push publishes
images: `develop` as `latest`, and gitflow branches as `<name after the prefix>-SNAPSHOT` (e.g. `feature/my-change` ->
`my-change-SNAPSHOT`), so a branch can be tested once its `maven.yml` run has finished. The HTML report, traces of
failed tests and the compose logs are uploaded as a workflow artifact.

## Writing tests

- Import `test` and `expect` from `./fixtures`, not from `@playwright/test`, so the custom fixtures are available.
- Ask for the `adminPage` fixture to start logged in as admin on the home page. Use the plain `page` fixture when the
  test controls the login itself (other user, wrong password, or listening for requests made during login).
- Every test gets a fresh browser context and logs in on its own, so tests do not share a session.
- `tests/fixtures.ts` also provides helpers: `openPortalAs()` (Keycloak login, waits for the home page and switches to
  English), `signIn()`, `navigate()` / `openMenu()` / `expandMenuGroup()` for the left sidebar, `errorToasts()` and
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
  from `/storage/batch-users-processing` every 30 seconds. `importCsv()` / `importInvalidCsv()` copy a file into the
  portal api container with `docker exec` and wait for the result in `users-output.log`. The result is checked in the
  databases: `portalRoles()`, `portalDashboards()`, `portalCommentPermission()` in the portal and `supersetRoles()` /
  `expectSupersetRoles()` in Superset (`ab_user`, `ab_user_role`, `ab_role`). `createDashboardGroup()`,
  `dashboardGroupMembers()` and `deleteDashboardGroup()` work on the dashboard groups of the portal. So these tests need
  the docker CLI and the compose stack on the machine that runs Playwright. They create the Superset roles `RLS_E2E_A`
  and `RLS_E2E_B` once and keep them, and delete the users and groups they created in `afterAll`.
- Only the admin roles (`HELLODATA_ADMIN`, `BUSINESS_DOMAIN_ADMIN`, `DATA_DOMAIN_ADMIN`) can comment by default. Comment
  tests give the other roles a comment permission in `beforeAll` and set it back to none in `afterAll`. A reviewer does
  not see the drafts of other users, so a comment is cleaned up by its author.
- Viewers and business specialists see no dashboard until an admin gives them access. Tests that need it give it in
  `beforeAll` and take it away in `afterAll`.
- Prefer roles and element ids over CSS structure, and use Playwright's auto-waiting assertions instead of fixed waits.
- Tests share one portal instance, so data created by a test must have a unique name and be cleaned up by that test.
