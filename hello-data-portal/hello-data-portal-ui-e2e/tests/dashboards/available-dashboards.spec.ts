import {DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER, expect, openMenu, openPortalAs, test} from '../fixtures';
import {changeDashboardAccessAsAdmin, expectDashboardListed, openDashboard, SHOWCASE_DASHBOARD} from '../superset';

// HELLODATA-687: viewers and business specialists only see the dashboards an admin gave them access to
for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  // a viewer or business specialist only sees the dashboards an admin gave them access to
  test.describe(`Available dashboards - ${role}`, () => {
    // both tests change the access of the same user
    test.describe.configure({mode: 'serial'});

    test.beforeEach(async ({page}) => {
      await openPortalAs(page, user.username, user.password);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    // without dashboard access the user sees no dashboard in the Dashboards menu and no link to the Superset instance
    test('sees no dashboard without access', async ({page, browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
      await expectDashboardListed(page, false);
      const drawer = await openMenu(page, 'Dashboards');
      await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toHaveCount(0);
      // only data engineers get the link to the Superset instance
      await expect(drawer.getByRole('button', {name: 'Open Superset Instance', exact: true})).toHaveCount(0);
    });

    // after an admin gave access the user sees the dashboard in the list and menu and can open it with its content
    // visible
    test('sees and opens a dashboard after the admin gave access', async ({page, browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
      await expectDashboardListed(page, true);
      // opens it from the Dashboards menu, below its data domain
      const dashboard = await openDashboard(page);
      await expect(dashboard.getByText('Aktueller Bestand').first()).toBeVisible();
    });
  });
}
