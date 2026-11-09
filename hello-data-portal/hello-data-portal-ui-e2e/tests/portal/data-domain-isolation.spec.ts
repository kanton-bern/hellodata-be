import {
  ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_VIEWER,
  expect,
  EXTRA_DATA_DOMAIN_ADMIN,
  EXTRA_DATA_DOMAIN_VIEWER,
  openMenu,
  openPortalAs,
  test,
  User,
} from '../fixtures';
import {
  addDag,
  cloudbeaverConnection,
  CONTEXT_KEY,
  DM_CONNECTION,
  DWH_CONNECTION,
  expandConnectionsFolder,
  expectPipelines,
  EXTRA_DM_CONNECTION,
  EXTRA_DWH_CONNECTION,
  openDataMartsViewer,
  PIPELINE_SYNC_TIMEOUT,
  removeDag,
  routeCloudbeaver,
  SHOWCASE_DAG,
} from '../data-eng';
import {DATA_DOMAIN, dashboardRow, expectDashboardListed, SHOWCASE_DASHBOARD} from '../superset';
import {EXTRA_CONTEXT_KEY} from '../instance';

const EXTRA_DATA_DOMAIN = 'Extra Data Domain';

// A user with a role in one data domain sees nothing of another one: neither in the data domain selector, nor its
// dashboards, its DAGs or its CloudBeaver connections. The e2e installation has the Default Data Domain with Superset and
// the Extra Data Domain without (compose-override.yaml), and both share Airflow and CloudBeaver.
test.describe('Data domain isolation', () => {
  const DOMAINS: {name: string, admin: User, ownConnections: string[], foreignConnections: string[],
    foreignDataDomain: string}[] = [
    {
      name: DATA_DOMAIN, admin: DATA_DOMAIN_ADMIN,
      ownConnections: [DM_CONNECTION, DWH_CONNECTION], foreignConnections: [EXTRA_DM_CONNECTION, EXTRA_DWH_CONNECTION],
      foreignDataDomain: EXTRA_DATA_DOMAIN,
    },
    {
      name: EXTRA_DATA_DOMAIN, admin: EXTRA_DATA_DOMAIN_ADMIN,
      ownConnections: [EXTRA_DM_CONNECTION, EXTRA_DWH_CONNECTION], foreignConnections: [DM_CONNECTION, DWH_CONNECTION],
      foreignDataDomain: DATA_DOMAIN,
    },
  ];

  for (const {name, admin, ownConnections, foreignConnections, foreignDataDomain} of DOMAINS) {
    // checks that the admin of this data domain sees only its own data and nothing of the other one
    test.describe(name, () => {
      // the data domain admin opens the data domain selector; it offers its own data domain but not the other one
      test(`${admin.username} cannot choose the ${foreignDataDomain}`, async ({page}) => {
        await openPortalAs(page, admin.username, admin.password);
        await page.locator('.data-domain-selector .data-domain-name').click();
        await expect(page.getByRole('menuitem', {name, exact: true})).toBeVisible();
        await expect(page.getByRole('menuitem', {name: foreignDataDomain, exact: true})).toHaveCount(0);
      });

      // the data domain admin opens the Data Marts viewer (CloudBeaver); the connections of its data domain are listed,
      // the ones of the other data domain are not
      test(`${admin.username} sees only the CloudBeaver connections of its data domain`, async ({page}) => {
        await openPortalAs(page, admin.username, admin.password);
        await routeCloudbeaver(page.context(), new URL(page.url()).origin);
        const cloudbeaver = await openDataMartsViewer(page);
        await expandConnectionsFolder(cloudbeaver);
        for (const connection of ownConnections) {
          await expect(cloudbeaverConnection(cloudbeaver, connection), connection).toHaveCount(1);
        }
        for (const connection of foreignConnections) {
          await expect(cloudbeaverConnection(cloudbeaver, connection), connection).toHaveCount(0);
        }
      });
    });
  }

  // only the Default Data Domain has Superset, so its dashboards must not leak into the other one
  test(`${EXTRA_DATA_DOMAIN_ADMIN.username} sees no dashboards of the ${DATA_DOMAIN}`, async ({page}) => {
    await openPortalAs(page, EXTRA_DATA_DOMAIN_ADMIN.username, EXTRA_DATA_DOMAIN_ADMIN.password);
    await expectDashboardListed(page, false, 10_000);
    await expect(dashboardRow(page)).toHaveCount(0);
    const drawer = await openMenu(page, 'Dashboards');
    await expect(drawer.getByRole('button', {name: DATA_DOMAIN, exact: true})).toHaveCount(0);
    await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toHaveCount(0);
  });
});

// The DAGs of a data domain are listed only to its users, in the Airflow of the Playwright project (the @airflow tag
// runs it for each Airflow): each data domain gets a DAG of its own, and the default data domain has the showcase DAG.
test.describe('Data domain isolation of the DAGs', {tag: '@airflow'}, () => {
  // the status panel shows the first 20 characters of a DAG id, so keep them short
  const RUN_ID = Date.now().toString(36);
  const DEFAULT_DAG = `e2e_dd_${RUN_ID}`;
  const EXTRA_DAG = `e2e_xd_${RUN_ID}`;

  test.beforeAll(async ({browser, airflowVersion}) => {
    test.setTimeout(PIPELINE_SYNC_TIMEOUT + 60_000);
    addDag(airflowVersion, CONTEXT_KEY, DEFAULT_DAG);
    addDag(airflowVersion, EXTRA_CONTEXT_KEY, EXTRA_DAG);
    // the admin sees the DAGs of every data domain, so this waits until both are published
    const page = await browser.newPage();
    try {
      await openPortalAs(page, ADMIN.username, ADMIN.password);
      await expectPipelines(page, airflowVersion, {listed: [DEFAULT_DAG, EXTRA_DAG]});
    } finally {
      await page.close();
    }
  });

  test.afterAll(({airflowVersion}) => {
    removeDag(airflowVersion, CONTEXT_KEY, DEFAULT_DAG);
    removeDag(airflowVersion, EXTRA_CONTEXT_KEY, EXTRA_DAG);
  });

  const DOMAINS: {name: string, admin: User, viewer: User, ownDags: string[], foreignDags: string[]}[] = [
    {
      name: DATA_DOMAIN, admin: DATA_DOMAIN_ADMIN, viewer: DATA_DOMAIN_VIEWER,
      ownDags: [SHOWCASE_DAG, DEFAULT_DAG], foreignDags: [EXTRA_DAG],
    },
    {
      name: EXTRA_DATA_DOMAIN, admin: EXTRA_DATA_DOMAIN_ADMIN, viewer: EXTRA_DATA_DOMAIN_VIEWER,
      ownDags: [EXTRA_DAG], foreignDags: [SHOWCASE_DAG, DEFAULT_DAG],
    },
  ];

  for (const {name, admin, viewer, ownDags, foreignDags} of DOMAINS) {
    // checks that the admin and the viewer of this data domain see only its own DAGs and none of the other one
    test.describe(name, () => {
      // the data domain admin opens the pipelines status; the DAGs of its data domain are listed, not of the other
      // data domain
      test(`${admin.username} sees only the DAGs of its data domain`, async ({page, airflowVersion}) => {
        // the DAGs were published before (beforeAll), but the Airflow sidecar republishes them every minute
        test.setTimeout(PIPELINE_SYNC_TIMEOUT + 60_000);
        await openPortalAs(page, admin.username, admin.password);
        await expectPipelines(page, airflowVersion, {listed: ownDags, notListed: foreignDags});
      });

      // the data domain viewer opens the pipelines status; the DAGs of its data domain are listed, not of the other
      // data domain
      test(`${viewer.username} sees only the DAGs of its data domain`, async ({page, airflowVersion}) => {
        // the DAGs were published before (beforeAll), but the Airflow sidecar republishes them every minute
        test.setTimeout(PIPELINE_SYNC_TIMEOUT + 60_000);
        await openPortalAs(page, viewer.username, viewer.password);
        await expectPipelines(page, airflowVersion, {listed: ownDags, notListed: foreignDags});
      });
    });
  }
});
