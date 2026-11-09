import {
  ADMIN,
  BUSINESS_DOMAIN_ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {addDag, AIRFLOW3, CONTEXT_KEY, expectPipelines, PIPELINE_SYNC_TIMEOUT, removeDag, SHOWCASE_DAG} from '../data-eng';

// HELLODATA-745: a user sees the DAG status (Status -> Data pipelines on the summary page) only of the data domains the
// user has a role in. The portal assigns a DAG to a data domain by the data domain key in the path of the DAG file.
test.describe('Pipeline status', () => {
  // the status panel shows the first 20 characters of a DAG id, so keep them short
  const RUN_ID = Date.now().toString(36);
  const OWN_DAG = `e2e_own_${RUN_ID}`;
  const FOREIGN_DAG = `e2e_xdd_${RUN_ID}`;
  // a data domain no user of the portal has a role in
  const FOREIGN_CONTEXT_KEY = 'E2E_Foreign_Data_Domain';

  test.beforeAll(async ({browser}) => {
    test.setTimeout(PIPELINE_SYNC_TIMEOUT + 60_000);
    addDag(CONTEXT_KEY, OWN_DAG);
    addDag(FOREIGN_CONTEXT_KEY, FOREIGN_DAG);
    // both DAGs are published together, so once the own one is listed the foreign one would be too
    const page = await browser.newPage();
    try {
      await openPortalAs(page, ADMIN.username, ADMIN.password);
      await expectPipelines(page, {listed: [OWN_DAG]});
    } finally {
      await page.close();
    }
  });

  test.afterAll(() => {
    removeDag(CONTEXT_KEY, OWN_DAG);
    removeDag(FOREIGN_CONTEXT_KEY, FOREIGN_DAG);
  });

  for (const user of [ADMIN, BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_ADMIN, DATA_DOMAIN_EDITOR, DATA_DOMAIN_VIEWER]) {
    // the user sees the showcase DAG and the DAG of its own data domain in the pipeline status, but not the DAG
    // of a data domain it has no role in
    test(`${user.username} sees only the DAGs of its data domain`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectPipelines(page, {listed: [SHOWCASE_DAG, OWN_DAG], notListed: [FOREIGN_DAG]}, 30_000);
    });
  }

  // the admin opens the summary page and the status panel Data pipelines lists the DAG of the data domain but not
  // the DAG of the foreign data domain; the badge next to the DAG names the Airflow it runs in (AF2 or AF3)
  test('status panel lists the DAGs of the data domain', async ({adminPage: page}) => {
    await page.goto('summary');
    const status = page.locator('#statusHeader');
    await status.getByText('Data pipelines', {exact: true}).click();
    const ownDag = status.getByText(OWN_DAG, {exact: true});
    await expect(ownDag).toBeVisible();
    await expect(status.getByText(FOREIGN_DAG, {exact: true})).toHaveCount(0);
    const row = status.locator('.grid').filter({has: ownDag});
    await expect(row.getByText(AIRFLOW3 ? 'AF3' : 'AF2', {exact: true})).toBeVisible();
  });
});
