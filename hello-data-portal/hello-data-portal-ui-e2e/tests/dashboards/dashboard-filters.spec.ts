import {
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {changeDashboardAccessAsAdmin, expectDashboardListed, openDashboard} from '../superset';

// the embedded Superset dashboard shows the edit button and the filter bar settings (gear) only to users who may change it

// HELLODATA-703
for (const {role, user} of [
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
]) {
  test.describe(`Dashboard filters - ${role}`, () => {
    test.beforeEach(async ({page}) => {
      await openPortalAs(page, user.username, user.password);
    });

    test('can edit the dashboard', async ({page}) => {
      const dashboard = await openDashboard(page);
      await dashboard.getByRole('button', {name: 'Edit dashboard', exact: true}).click();
      await expect(dashboard.getByRole('button', {name: 'Save', exact: true})).toBeVisible();
      // leave the edit mode without changing the shared dashboard
      await dashboard.getByRole('button', {name: 'Discard', exact: true}).click();
      await expect(dashboard.getByRole('button', {name: 'Edit dashboard', exact: true})).toBeVisible();
    });

    test('can add and edit filters', async ({page}) => {
      const dashboard = await openDashboard(page);
      await dashboard.getByRole('img', {name: 'gear'}).click();
      await dashboard.getByRole('menuitem', {name: 'Add or edit filters', exact: true}).click();

      const filters = dashboard.getByRole('dialog').filter({hasText: 'Add and edit filters'});
      await expect(filters).toBeVisible();
      await expect(filters.getByRole('button', {name: 'Add Filter'})).toBeVisible();
      await expect(filters.getByRole('button', {name: 'Save', exact: true})).toBeEnabled();
      await filters.getByRole('button', {name: 'Cancel', exact: true}).click();
      await expect(filters).toBeHidden();
    });
  });
}

// HELLODATA-702
for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  test.describe(`Dashboard filters - ${role}`, () => {
    test.beforeAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    test('cannot edit the dashboard or its filters', async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDashboardListed(page, true);
      const dashboard = await openDashboard(page);
      // the filter bar itself can be used, only its configuration is missing
      await expect(dashboard.getByRole('button', {name: 'Apply filters'})).toBeVisible();
      await expect(dashboard.getByRole('button', {name: 'Edit dashboard', exact: true})).toHaveCount(0);
      await expect(dashboard.getByRole('img', {name: 'gear'})).toHaveCount(0);
    });
  });
}
