import {
  BUSINESS_DOMAIN_ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {breadcrumb, expectNoDataEngEntry, openOrchestration, SHOWCASE_DAG, subsystemFrame} from '../data-eng';

// the Orchestration page (Airflow) and who may open it
test.describe('Orchestration', () => {
  // the admin opens Orchestration; Airflow is shown with the DAG of the data domain in the list
  // HELLODATA-755
  test('admin opens Airflow with the DAGs of the data domain', async ({adminPage: page}) => {
    const airflow = await openOrchestration(page);
    await expect(page.locator('app-subsystem-iframe iframe')).toHaveAttribute('src', /\/home$/);
    await expect(airflow.getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible();
  });

  // HELLODATA-755
  for (const user of [BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_ADMIN]) {
    // the user opens Orchestration and Airflow is shown
    // HELLODATA-755
    test(`${user.username} opens Airflow with the DAGs of the data domain`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await openOrchestration(page);
    });
  }

  // HELLODATA-755: Orchestration needs the DATA_ENG permission, which only the admin roles have
  for (const user of [DATA_DOMAIN_EDITOR, DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER]) {
    // the user has no Orchestration entry in the menu and its page redirects to the forbidden page
    // HELLODATA-755
    test(`${user.username} has no orchestration`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectNoDataEngEntry(page, 'Orchestration');
      await page.goto('embedded-orchestration');
      await expect(page).toHaveURL(/\/forbidden$/);
    });
  }

  // the admin opens Orchestration and the breadcrumb shows Orchestration
  // HELLODATA-848
  test('breadcrumb shows Orchestration on the DAG list', async ({adminPage: page}) => {
    await openOrchestration(page);
    await expect(breadcrumb(page).getByText('Orchestration', {exact: true})).toBeVisible();
  });

  // the admin opens the details of a DAG; the breadcrumb Orchestration leads back to the DAG list of Airflow
  // HELLODATA-848
  test('breadcrumb leads from a DAG back to the DAG list', async ({adminPage: page}) => {
    // the details of a DAG are opened from the pipelines of a workspace (Monitoring -> Workspaces)
    await page.goto(`embedded-orchestration/details/${SHOWCASE_DAG}`);
    const iframe = page.locator('app-subsystem-iframe iframe');
    await expect(iframe).toHaveAttribute('src', new RegExp(`/dags/${SHOWCASE_DAG}$`));
    await expect(subsystemFrame(page).getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible({timeout: 90_000});

    const crumb = breadcrumb(page).getByText('Orchestration', {exact: true});
    await expect(crumb).toBeVisible();
    await crumb.click();
    await expect(page).toHaveURL(/\/embedded-orchestration$/);
    await expect(iframe).toHaveAttribute('src', /\/home$/);
    await expect(breadcrumb(page).getByText('Orchestration', {exact: true})).toBeVisible();
  });
});
