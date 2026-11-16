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
import {airflow, breadcrumb, expectNoDataEngEntry, openOrchestration, orchestrationSrc, SHOWCASE_DAG, subsystemFrame} from '../data-eng';

// the Orchestration page and who may open it: Orchestration for Airflow 2, Orchestration (Airflow3) for Airflow 3 (the
// Airflow of the Playwright project, the @airflow tag runs it for each)
test.describe('Orchestration', {tag: '@airflow'}, () => {
  // the admin opens Orchestration; Airflow is shown with the DAG of the data domain in the list
  // HELLODATA-755
  test('admin opens Airflow with the DAGs of the data domain', async ({adminPage: page, airflowVersion}) => {
    const frame = await openOrchestration(page, airflowVersion);
    await expect(page.locator('app-subsystem-iframe iframe')).toHaveAttribute('src', orchestrationSrc(airflowVersion));
    await expect(frame.getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible();
  });

  // HELLODATA-755
  for (const user of [BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_ADMIN]) {
    // the user opens Orchestration and Airflow is shown
    // HELLODATA-755
    test(`${user.username} opens Airflow with the DAGs of the data domain`, async ({page, airflowVersion}) => {
      await openPortalAs(page, user.username, user.password);
      await openOrchestration(page, airflowVersion);
    });
  }

  // HELLODATA-755: Orchestration needs the DATA_ENG permission, which only the admin roles have
  for (const user of [DATA_DOMAIN_EDITOR, DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER]) {
    // the user has no Orchestration entry in the menu and its page redirects to the forbidden page
    // HELLODATA-755
    test(`${user.username} has no orchestration`, async ({page, airflowVersion}) => {
      const {entry, path} = airflow(airflowVersion);
      await openPortalAs(page, user.username, user.password);
      await expectNoDataEngEntry(page, entry);
      await page.goto(path);
      await expect(page).toHaveURL(/\/forbidden$/);
    });
  }

  // the admin opens Orchestration and the breadcrumb shows Orchestration
  // HELLODATA-848
  test('breadcrumb shows Orchestration on the DAG list', async ({adminPage: page, airflowVersion}) => {
    await openOrchestration(page, airflowVersion);
    await expect(breadcrumb(page).getByText(airflow(airflowVersion).entry, {exact: true})).toBeVisible();
  });

  // the admin opens the details of a DAG; the breadcrumb Orchestration leads back to the DAG list of Airflow
  // HELLODATA-848
  test('breadcrumb leads from a DAG back to the DAG list', async ({adminPage: page, airflowVersion}) => {
    const {entry, path} = airflow(airflowVersion);
    // the details of a DAG are opened from the pipelines of a workspace (Monitoring -> Workspaces)
    await page.goto(`${path}/details/${SHOWCASE_DAG}`);
    const iframe = page.locator('app-subsystem-iframe iframe');
    await expect(iframe).toHaveAttribute('src', orchestrationSrc(airflowVersion, SHOWCASE_DAG));
    await expect(subsystemFrame(page).getByText(SHOWCASE_DAG, {exact: true}).first()).toBeVisible({timeout: 90_000});

    const crumb = breadcrumb(page).getByText(entry, {exact: true});
    await expect(crumb).toBeVisible();
    await crumb.click();
    await expect(page).toHaveURL(new RegExp(`/${path}$`));
    await expect(iframe).toHaveAttribute('src', orchestrationSrc(airflowVersion));
    await expect(breadcrumb(page).getByText(entry, {exact: true})).toBeVisible();
  });
});
