import {BrowserContext, FrameLocator, Page} from '@playwright/test';
import {expect, navigate, openMenu, signIn, User} from './fixtures';
import {exec} from './docker';

// The Data Eng. menu: Orchestration (Airflow embedded in the portal), DWH Viewer (CloudBeaver in a new tab) and
// Filebrowser (SFTPGo in a new tab). They need the Airflow, CloudBeaver and SFTPGo services of the compose stack.

export const DATA_ENG_MENU = 'Data Eng.';
export const CONTEXT_KEY = 'Default_Data_Domain';
export const DATA_DOMAIN_FOLDER = '/Default Data Domain';

// DAGs the showcase copies into the dags folder of the default data domain, the data domain key is part of their id
export const SHOWCASE_DAG = `hello_world_${CONTEXT_KEY}`;

// the airflow sidecar publishes DAGs to the portal on a fixed interval (HD_AIRFLOW_PUBLISH_INTERVAL_MINUTES, 1 minute in
// the e2e workflow) and Airflow looks for new DAG files every HD_AIRFLOW_DAG_DIR_LIST_INTERVAL_SECONDS (30 there)
export const PIPELINE_SYNC_TIMEOUT = 4 * 60_000;
// SFTPGo users and their data domain folders are created by its sidecar after the portal published the users
export const FILEBROWSER_SYNC_TIMEOUT = 3 * 60_000;

const DAGS_DIR = '/storage/airflow/dags';

/** The Airflow or CloudBeaver page embedded in the portal. */
export function subsystemFrame(page: Page): FrameLocator {
  return page.frameLocator('app-subsystem-iframe iframe');
}

/** Opens Data Eng. -> Orchestration and returns the embedded Airflow once it shows a DAG of the data domain. */
export async function openOrchestration(page: Page): Promise<FrameLocator> {
  await navigate(page, DATA_ENG_MENU, 'Orchestration');
  await expect(page).toHaveURL(/\/embedded-orchestration$/);
  const airflow = subsystemFrame(page);
  // the iframe logs in to Airflow through Keycloak first and then opens the DAG list
  await expect(airflow.getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible({timeout: 90_000});
  return airflow;
}

/** The breadcrumb of the portal header. */
export function breadcrumb(page: Page) {
  return page.locator('app-breadcrumb');
}

/** Checks that the logged in user has no entry in the Data Eng. menu, which is hidden when no entry is left. */
export async function expectNoDataEngEntry(page: Page, entry: string) {
  // wait for the home page content, so the menu is built before asserting what is missing
  await expect(page.locator('#mainContentDiv').getByText('FAQ', {exact: true})).toBeVisible();
  const dataEng = page.locator('#appSidebar').getByRole('button', {name: DATA_ENG_MENU, exact: true});
  if (await dataEng.count() > 0) {
    const drawer = await openMenu(page, DATA_ENG_MENU);
    await expect(drawer.getByRole('button', {name: entry, exact: true})).toHaveCount(0);
  }
}

/** A DAG file without tasks, enough for Airflow to list it and for the sidecar to publish it. */
function dagFile(dagId: string) {
  return [
    'from datetime import datetime',
    'from airflow import DAG',
    '',
    `dag = DAG('${dagId}', description='e2e test DAG', schedule_interval=None, start_date=datetime(2024, 1, 1), catchup=False)`,
    '',
  ].join('\n');
}

/**
 * Writes a DAG into the dags folder of a data domain (the folder name is the data domain key). The portal shows a DAG
 * only to users with a role in the data domain whose key is part of the file location.
 * The dags folder belongs to root, so the file is written as root in a container that mounts the shared storage.
 */
export function addDag(contextKey: string, dagId: string) {
  const file = `${DAGS_DIR}/${contextKey}/${dagId}.py`;
  exec('hello-data-portal-api', ['sh', '-c', 'mkdir -p "$(dirname "$1")" && cat > "$1.tmp" && mv "$1.tmp" "$1"', 'sh', file],
    {input: dagFile(dagId), user: '0'});
}

/** Removes a DAG written by addDag, and the data domain folder if it is empty then. */
export function removeDag(contextKey: string, dagId: string) {
  exec('hello-data-portal-api', ['sh', '-c', 'rm -f "$1/$2.py" && rmdir "$1" 2>/dev/null || true', 'sh', `${DAGS_DIR}/${contextKey}`, dagId],
    {user: '0'});
}

/** Reloads the portal and returns the ids of the DAGs the api reports to the current user (status panel of the summary). */
export async function pipelineIds(page: Page): Promise<string[]> {
  // the pipelines are loaded once after the login, so reload to get the current ones
  const loaded = page.waitForResponse(r => r.url().endsWith('/api/pipelines') && r.request().method() === 'GET');
  await page.goto('summary');
  const response = await loaded;
  expect(response.ok(), 'GET .../api/pipelines failed').toBe(true);
  return (await response.json() as {id: string}[]).map(pipeline => pipeline.id);
}

/** Reloads the portal until the api reports the expected DAGs (and none of the unexpected ones) to the current user. */
export async function expectPipelines(page: Page, {listed = [], notListed = []}: {listed?: string[], notListed?: string[]},
                                      timeout = PIPELINE_SYNC_TIMEOUT) {
  await expect(async () => {
    const ids = await pipelineIds(page);
    expect(ids).toEqual(expect.arrayContaining(listed));
    for (const id of notListed) {
      expect(ids, `${id} must not be listed`).not.toContain(id);
    }
  }).toPass({timeout, intervals: [10_000, 15_000]});
}

// The DWH Viewer embeds <portal origin>/cloudbeaver/, which an ingress routes to the CloudBeaver gateway on Kubernetes.
// The portal container of the compose stack serves only /app, so the tests route those requests to the gateway
// themselves. Set E2E_CLOUDBEAVER_URL to an empty value when the portal installation routes them on its own.
const CLOUDBEAVER_URL = process.env.E2E_CLOUDBEAVER_URL ?? (process.env.E2E_BASE_URL ? '' : 'http://localhost:8087');

/** Routes the CloudBeaver requests of the portal to the gateway of the compose stack (see CLOUDBEAVER_URL). */
export async function routeCloudbeaver(context: BrowserContext, portalOrigin: string) {
  if (!CLOUDBEAVER_URL) {
    return;
  }
  const prefix = `${portalOrigin}/cloudbeaver/`;
  await context.route(url => url.href.startsWith(prefix), async route => {
    const url = new URL(route.request().url());
    // the gateway reads the portal's access token from the auth.access_token cookie, which is sent to every port of the host
    const response = await route.fetch({url: CLOUDBEAVER_URL + url.pathname + url.search});
    await route.fulfill({response});
  });
  // a web socket cannot be routed to another port, so CloudBeaver's event socket is answered by a silent mock
  await context.routeWebSocket(url => url.href.startsWith(prefix.replace(/^http/, 'ws')), () => undefined);
}

/** Signs in on the Keycloak login form if a new tab shows it: on http the tab does not reuse the Keycloak session. */
async function signInIfAsked(tab: Page, user: User) {
  if (await tab.locator('#kc-login').isVisible()) {
    await signIn(tab, user.username, user.password);
  }
}

/** Opens Data Eng. -> DWH Viewer, which the portal opens in a new tab, and returns that tab once it shows the viewer. */
export async function openDwhViewer(page: Page, user: User): Promise<Page> {
  const drawer = await openMenu(page, DATA_ENG_MENU);
  const viewerPage = page.context().waitForEvent('page');
  await drawer.getByRole('button', {name: 'DWH Viewer', exact: true}).click();
  const viewer = await viewerPage;
  // the new tab starts at the portal root with ?redirectTo=..., logs in and then opens the viewer
  await expect(async () => {
    await signInIfAsked(viewer, user);
    await expect(viewer).toHaveURL(/\/data-warehouse-viewer$/, {timeout: 5_000});
  }).toPass({timeout: 60_000, intervals: [1_000, 2_000]});
  return viewer;
}

/** Opens Data Eng. -> Filebrowser, which the portal opens in a new tab, and returns that tab once SFTPGo logged the user in. */
export async function openFilebrowser(page: Page, user: User): Promise<Page> {
  const drawer = await openMenu(page, DATA_ENG_MENU);
  const filebrowserPage = page.context().waitForEvent('page');
  const loginRequest = page.context().waitForEvent('request', r => r.url().includes('/web/client/oidclogin'));
  await drawer.getByRole('button', {name: 'Filebrowser', exact: true}).click();
  const filebrowser = await filebrowserPage;
  const loginUrl = (await loginRequest).url();
  // SFTPGo logs in through Keycloak, but knows the user only after its sidecar created it
  await expect(async () => {
    await signInIfAsked(filebrowser, user);
    if (await filebrowser.locator('#kc-login').count() === 0 && !/\/web\/client\/files/.test(filebrowser.url())) {
      await filebrowser.goto(loginUrl);
      await signInIfAsked(filebrowser, user);
    }
    await expect(filebrowser).toHaveURL(/\/web\/client\/files/, {timeout: 10_000});
  }).toPass({timeout: FILEBROWSER_SYNC_TIMEOUT, intervals: [5_000, 10_000]});
  return filebrowser;
}

/** Opens a folder in the SFTPGo web client and waits until its content is listed. */
export async function openFolder(filebrowser: Page, path: string) {
  await expect(async () => {
    await filebrowser.goto(new URL(`/web/client/files?path=${encodeURIComponent(path)}`, filebrowser.url()).toString());
    // the data domain folder appears once the sidecar added the user to the group of the data domain
    await expect(filebrowser.locator('#file_manager_list')).toBeVisible({timeout: 10_000});
  }).toPass({timeout: FILEBROWSER_SYNC_TIMEOUT, intervals: [10_000]});
}

/** The row of a file or folder in the current SFTPGo folder. */
export function fileRow(filebrowser: Page, name: string) {
  return filebrowser.locator('#file_manager_list tbody tr')
    .filter({has: filebrowser.getByRole('link', {name, exact: true})});
}

export async function createFolder(filebrowser: Page, name: string) {
  await filebrowser.locator('#id_create_dir_button').click();
  await filebrowser.locator('#file_manager_new_folder_input').fill(name);
  await filebrowser.locator('#file_manager_add_folder').click();
  await expect(fileRow(filebrowser, name)).toBeVisible();
}

/** Opens the actions menu of a row (the "..." button). */
async function openRowActions(filebrowser: Page, name: string) {
  await fileRow(filebrowser, name).locator('button[data-kt-menu-trigger]').click();
}

/** The entry of the open actions menu, e.g. 'rename' or 'delete'. */
function rowAction(filebrowser: Page, action: string) {
  return filebrowser.locator(`[data-kt-filemanager-table-action="${action}"]:visible`);
}

export async function renameFolder(filebrowser: Page, name: string, newName: string) {
  await openRowActions(filebrowser, name);
  await rowAction(filebrowser, 'rename').click();
  await filebrowser.locator('#rename_new_name').fill(newName);
  await filebrowser.locator('#id_do_rename_button').click();
  await expect(fileRow(filebrowser, newName)).toBeVisible();
  await expect(fileRow(filebrowser, name)).toHaveCount(0);
}

export async function deleteFolder(filebrowser: Page, name: string) {
  await openRowActions(filebrowser, name);
  await rowAction(filebrowser, 'delete').click();
  await filebrowser.locator('.swal2-confirm').click();
  await expect(fileRow(filebrowser, name)).toHaveCount(0);
}

/** Whether the actions menu of a row offers an entry. */
export async function expectRowAction(filebrowser: Page, name: string, action: string, offered: boolean) {
  await openRowActions(filebrowser, name);
  await expect(rowAction(filebrowser, 'rename').or(rowAction(filebrowser, 'move_or_copy')).first()).toBeVisible();
  await expect(rowAction(filebrowser, action)).toHaveCount(offered ? 1 : 0);
  await filebrowser.keyboard.press('Escape');
}
