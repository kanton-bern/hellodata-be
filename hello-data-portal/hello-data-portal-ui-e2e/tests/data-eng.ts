import {BrowserContext, FrameLocator, Page} from '@playwright/test';
import {AirflowVersion, expect, navigate, openMenu, signIn, User} from './fixtures';
import {exec} from './docker';
import {BUSINESS_DOMAIN_NAME, CONTEXT_KEY, SHOWCASE_DAG_ID} from './instance';

// The Data Eng. menu: Orchestration (Airflow embedded in the portal), DWH Viewer (CloudBeaver in a new tab) and
// Filebrowser (SFTPGo in a new tab). They need the Airflow, CloudBeaver and SFTPGo services of the compose stack.
// The portal embeds Airflow 2 and Airflow 3, each with its own Orchestration page; the Airflow helpers take the version
// under test (the airflowVersion fixture of the Playwright project).

export const DATA_ENG_MENU = 'Data Eng.';
export {CONTEXT_KEY};
export const DATA_DOMAIN_FOLDER = '/Default Data Domain';

// DAG that is always in the dags folder of the default data domain, the data domain key is part of its id (see
// SHOWCASE_DAG_ID in instance.ts)
export const SHOWCASE_DAG = SHOWCASE_DAG_ID;

/** What differs between the Airflow versions in the portal. */
export type Airflow = {
  version: AirflowVersion;
  /** The module type of its resources (the DAGs in /api/pipelines). */
  moduleType: 'AIRFLOW' | 'AIRFLOW3';
  /** The Data Eng. menu entry and the breadcrumb of its Orchestration page. */
  entry: string;
  /** The url of its Orchestration page. */
  path: string;
  /** Its dags folder on the shared storage of the portal api; Airflow 3's is mounted by compose-airflow3.yaml. */
  dagsDir: string;
};

const AIRFLOWS: Record<AirflowVersion, Airflow> = {
  '2': {version: '2', moduleType: 'AIRFLOW', entry: 'Orchestration', path: 'embedded-orchestration',
    dagsDir: '/storage/airflow/dags'},
  '3': {version: '3', moduleType: 'AIRFLOW3', entry: 'Orchestration (Airflow3)', path: 'embedded-orchestration-airflow3',
    dagsDir: '/storage/airflow3/dags'},
};

export function airflow(version: AirflowVersion): Airflow {
  return AIRFLOWS[version];
}

/**
 * The end of the url the portal loads into the Airflow iframe, for the DAG list or the page of a DAG. Airflow 2 is
 * opened through its Keycloak login with the target page appended (/home, /dags/<id>), Airflow 3 through its logout
 * with the target page url-encoded in ?next= (%2Fdags, %2Fdags%2F<id>).
 */
export function orchestrationSrc(version: AirflowVersion, dagId?: string): RegExp {
  if (version === '3') {
    return new RegExp(`\\?next=${encodeURIComponent(dagId ? `/dags/${dagId}` : '/dags')}$`);
  }
  return new RegExp(dagId ? `/dags/${dagId}$` : '/home$');
}

// the airflow sidecar publishes DAGs to the portal on a fixed interval (HD_AIRFLOW_PUBLISH_INTERVAL_MINUTES, 1 minute in
// the e2e workflow) and Airflow looks for new DAG files every HD_AIRFLOW_DAG_DIR_LIST_INTERVAL_SECONDS (30 there)
export const PIPELINE_SYNC_TIMEOUT = 4 * 60_000;
// SFTPGo users and their data domain folders are created by its sidecar after the portal published the users
export const FILEBROWSER_SYNC_TIMEOUT = 3 * 60_000;

/** The Airflow or CloudBeaver page embedded in the portal. */
export function subsystemFrame(page: Page): FrameLocator {
  return page.frameLocator('app-subsystem-iframe iframe');
}

/** Opens the Orchestration page of an Airflow and returns the embedded Airflow once it shows a DAG of the data domain. */
export async function openOrchestration(page: Page, version: AirflowVersion): Promise<FrameLocator> {
  const {entry, path} = airflow(version);
  await navigate(page, DATA_ENG_MENU, entry);
  await expect(page).toHaveURL(new RegExp(`/${path}$`));
  const frame = subsystemFrame(page);
  // the iframe logs in to Airflow through Keycloak first and then opens the DAG list
  await expect(frame.getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible({timeout: 90_000});
  return frame;
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
function dagFile(version: AirflowVersion, dagId: string) {
  if (version === '3') {
    // Airflow 3 moved DAG to the task SDK and dropped schedule_interval
    return [
      'from datetime import datetime',
      'from airflow.sdk import DAG',
      '',
      `dag = DAG('${dagId}', description='e2e test DAG', schedule=None, start_date=datetime(2024, 1, 1), catchup=False)`,
      '',
    ].join('\n');
  }
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
export function addDag(version: AirflowVersion, contextKey: string, dagId: string) {
  const file = `${airflow(version).dagsDir}/${contextKey}/${dagId}.py`;
  exec('hello-data-portal-api', ['sh', '-c', 'mkdir -p "$(dirname "$1")" && cat > "$1.tmp" && mv "$1.tmp" "$1"', 'sh', file],
    {input: dagFile(version, dagId), user: '0'});
}

/** Removes a DAG written by addDag, and the data domain folder if it is empty then. */
export function removeDag(version: AirflowVersion, contextKey: string, dagId: string) {
  exec('hello-data-portal-api', ['sh', '-c', 'rm -f "$1/$2.py" && rmdir "$1" 2>/dev/null || true', 'sh',
    `${airflow(version).dagsDir}/${contextKey}`, dagId], {user: '0'});
}

/**
 * Reloads the portal and returns the ids of the DAGs of one Airflow the api reports to the current user (status panel
 * of the summary). The api lists the DAGs of all Airflows together, the module type tells them apart (a pipeline
 * without one comes from an Airflow 2 sidecar older than Airflow 3).
 */
export async function pipelineIds(page: Page, version: AirflowVersion): Promise<string[]> {
  // the pipelines are loaded once after the login, so reload to get the current ones
  const loaded = page.waitForResponse(r => r.url().endsWith('/api/pipelines') && r.request().method() === 'GET');
  await page.goto('summary');
  const response = await loaded;
  expect(response.ok(), 'GET .../api/pipelines failed').toBe(true);
  const {moduleType} = airflow(version);
  return (await response.json() as {id: string, moduleType?: string}[])
    .filter(pipeline => (pipeline.moduleType ?? 'AIRFLOW') === moduleType)
    .map(pipeline => pipeline.id);
}

/**
 * Reloads the portal until the api reports the expected DAGs of an Airflow (and none of the unexpected ones) to the
 * current user.
 */
export async function expectPipelines(page: Page, version: AirflowVersion,
                                      {listed = [], notListed = []}: {listed?: string[], notListed?: string[]},
                                      timeout = PIPELINE_SYNC_TIMEOUT) {
  // toPass reports only its timeout, so keep the last answer of the api for the error
  let ids: string[] = [];
  await expect(async () => {
    ids = await pipelineIds(page, version);
    expect(ids).toEqual(expect.arrayContaining(listed));
    for (const id of notListed) {
      expect(ids, `${id} must not be listed`).not.toContain(id);
    }
  }).toPass({timeout, intervals: [10_000, 15_000]}).catch(error => {
    throw new Error(`expected the Airflow ${version} DAGs ${listed.join(', ')} and none of ${notListed.join(', ') || '-'}, `
      + `the api reported: ${ids.join(', ') || 'none'}\n${error.message}`);
  });
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
    try {
      const response = await route.fetch({url: CLOUDBEAVER_URL + url.pathname + url.search});
      await route.fulfill({response});
    } catch (error) {
      // CloudBeaver cancels requests it no longer needs (and the page may be closed at the end of a test); the route
      // of such a request cannot be fulfilled any more, which must not fail the test
      if (!/disposed|closed|cancel/i.test(String(error))) {
        throw error;
      }
    }
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
  // the new tab starts at the portal root with ?redirectTo=..., goes through Keycloak and then opens the viewer; wait for
  // either end of that redirect chain instead of checking a page that is still navigating
  const login = viewer.locator('#kc-login');
  await expect(login.or(viewer.locator('#data-warehouse-component'))).toBeVisible({timeout: 60_000});
  if (await login.isVisible()) {
    await signIn(viewer, user.username, user.password);
    // after a login in the tab the portal can open home instead of the redirectTo page, open the viewer then
    await expect(viewer).toHaveURL(/\/(home|data-warehouse-viewer)$/, {timeout: 30_000});
    if (!viewer.url().endsWith('/data-warehouse-viewer')) {
      await viewer.goto('data-warehouse-viewer');
    }
  }
  await expect(viewer).toHaveURL(/\/data-warehouse-viewer$/, {timeout: 30_000});
  return viewer;
}

// the folder of initial-data-sources.conf with the DataMarts and DWH connections of the data domains
export const CLOUDBEAVER_FOLDER = `${BUSINESS_DOMAIN_NAME} DBs`;
export const DM_CONNECTION = 'Default Data Domain DataMarts';
export const DWH_CONNECTION = 'Default Data Domain DWH';
export const EXTRA_DM_CONNECTION = 'Extra Data Domain DataMarts';
export const EXTRA_DWH_CONNECTION = 'Extra Data Domain DWH';

/** Opens Data Marts -> DataMarts Viewer, which embeds CloudBeaver in the portal, and returns CloudBeaver. */
export async function openDataMartsViewer(page: Page): Promise<FrameLocator> {
  await navigate(page, 'Data Marts', 'DataMarts Viewer');
  await expect(page).toHaveURL(/\/embedded-dm-viewer$/);
  return subsystemFrame(page);
}

/**
 * Waits until CloudBeaver shows the shared connections folder and expands it. Right after the session of another user
 * ended, CloudBeaver can fail to load the connections ("datasource registry is null after init"); its Retry button
 * loads them again.
 */
export async function expandConnectionsFolder(cloudbeaver: FrameLocator) {
  const folder = cloudbeaver.locator(`[title="${CLOUDBEAVER_FOLDER}"]`);
  await expect(async () => {
    const retry = cloudbeaver.getByRole('button', {name: /^retry$/i});
    if (await retry.count() > 0) {
      await retry.first().click();
    }
    await expect(folder).toBeVisible({timeout: 10_000});
  }).toPass({timeout: 90_000, intervals: [2_000, 5_000]});
  // the row of a navigator node holds its Expand button next to its name; nested elements come later in the document,
  // so the last element with both is the row itself (and not the navigator, which holds the other folders too)
  const expand = cloudbeaver.getByRole('button', {name: 'Expand', exact: true});
  const row = cloudbeaver.locator('div').filter({has: folder}).filter({has: expand}).last();
  await row.getByRole('button', {name: 'Expand', exact: true}).click();
}

/** A connection in the CloudBeaver navigator, by its name. */
export function cloudbeaverConnection(cloudbeaver: FrameLocator, name: string) {
  return cloudbeaver.locator(`[title="Name: ${name}"]`);
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
    // the data domain folder appears once the sidecar added the user to the group of the data domain; SFTPGo hides the
    // file list of an empty folder and shows an upload hint instead
    // (the hidden list stays in the page, so only the visible one of both counts)
    await expect(filebrowser.locator('#file_manager_list')
      .or(filebrowser.getByText('This directory is empty', {exact: false}))
      .filter({visible: true}).first()).toBeVisible({timeout: 10_000});
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
  // SFTPGo asks in its own bootstrap modal (ModalAlert in base.html), not in a SweetAlert dialog
  await filebrowser.locator('#modal_alert_ok').click();
  await expect(fileRow(filebrowser, name)).toHaveCount(0);
}

/** Whether the actions menu of a row offers an entry. */
export async function expectRowAction(filebrowser: Page, name: string, action: string, offered: boolean) {
  await openRowActions(filebrowser, name);
  await expect(rowAction(filebrowser, 'rename').or(rowAction(filebrowser, 'move_or_copy')).first()).toBeVisible();
  await expect(rowAction(filebrowser, action)).toHaveCount(offered ? 1 : 0);
  await filebrowser.keyboard.press('Escape');
}
