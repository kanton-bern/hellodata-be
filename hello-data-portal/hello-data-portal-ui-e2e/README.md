# HelloDATA Portal UI E2E Tests

Basic regression suite for the portal, written with [Playwright](https://playwright.dev). It runs against the portal
subset of the [docker compose installation](../../hello-data-deployment/docker-compose/README.md) and logs in through
the real Keycloak login page.

## Running locally

Start the portal and its dependencies (a separate compose project, so your own local data is not touched):

```bash
cd hello-data-deployment/docker-compose
docker compose -p hellodata-e2e up -d --wait hello-data-portal-ui
```

Use `HD_VERSION=<tag>` in front of the command to test a specific release candidate instead of `latest`.

On a fresh stack the sidecars may publish their resources before the portal sidecar is listening. They republish
every 10 minutes, so the lineage docs test can fail until then. Wait for the dbt docs app info first (this is what the
CI workflow does):

```bash
docker compose -p hellodata-e2e exec -T postgres psql -U postgres -d hd_metainfo -tAc \
  "select count(*) from resource where kind = 'hellodata/AppInfo' and module_type = 'DBT_DOCS'"
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
| `E2E_VIEWER_USERNAME` | `default-data-domain-viewer@hellodata.ch` | Example data domain viewer (no administration menu) |
| `E2E_VIEWER_PASSWORD` | `viewer`                                  | Its password                                        |

Pointing `E2E_BASE_URL` at another installation (e.g. a port-forwarded cluster) works as long as the Keycloak client
accepts that URL as redirect URI.

## CI

`.github/workflows/e2e.yml` runs the suite for every release candidate tag (`*.rc*`) right after `maven.yml` pushed the
images, and can be started manually (Actions -> "E2E tests (docker compose)") for any image tag. The HTML report,
traces of failed tests and the compose logs are uploaded as a workflow artifact.

## Writing tests

- Import `test` and `expect` from `./fixtures`, not from `@playwright/test`, so the custom fixtures are available.
- Ask for the `adminPage` fixture to start logged in as admin on the home page. Use the plain `page` fixture when the
  test controls the login itself (other user, wrong password, or listening for requests made during login).
- Every test gets a fresh browser context and logs in on its own, so tests do not share a session.
- `tests/fixtures.ts` also provides helpers: `openPortalAs()` (Keycloak login, waits for the home page and switches to
  English), `signIn()`, `navigate()` / `openMenu()` for the left sidebar, `errorToasts()` and `dismissToasts()`.
- Prefer roles and element ids over CSS structure, and use Playwright's auto-waiting assertions instead of fixed waits.
- Tests share one portal instance, so data created by a test must have a unique name and be cleaned up by that test.
