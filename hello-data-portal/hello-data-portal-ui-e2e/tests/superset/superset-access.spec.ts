import {
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expandMenuGroup,
  expect,
  openMenu,
  openPortalAs,
  test,
} from '../fixtures';
import {changeDashboardAccessAsAdmin, DATA_DOMAIN, expectDashboardListed, openSuperset, SHOWCASE_DASHBOARD} from '../superset';

// HELLODATA-798: data domain admins and editors open the Superset instance of their data domain from the Dashboards
// menu and are logged in there with their portal user
for (const {role, user} of [
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
]) {
  test(`${role} opens the Superset instance of its data domain`, async ({page}) => {
    await openPortalAs(page, user.username, user.password);
    const superset = await openSuperset(page);
    await expect(superset.getByRole('button', {name: 'Dashboards', exact: true})).toBeVisible();
    // the welcome page lists the dashboards of the instance
    await expect(superset.getByText(SHOWCASE_DASHBOARD, {exact: true}).first()).toBeVisible();
  });
}

for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  test.describe(role, () => {
    // with access to a dashboard the menu has an entry for the data domain, where the link would be
    test.beforeAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    test('has no link to the Superset instance', async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDashboardListed(page, true);
      const drawer = await openMenu(page, 'Dashboards');
      await expandMenuGroup(drawer, DATA_DOMAIN);
      await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toBeVisible();
      await expect(drawer.getByRole('button', {name: 'Open Superset Instance', exact: true})).toHaveCount(0);
    });
  });
}
