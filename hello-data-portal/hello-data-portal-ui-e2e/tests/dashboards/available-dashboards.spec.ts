import {DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER, expect, openMenu, openPortalAs, test} from '../fixtures';
import {changeDashboardAccessAsAdmin, expectDashboardListed, openDashboard, SHOWCASE_DASHBOARD} from '../superset';

// HELLODATA-687: viewers and business specialists only see the dashboards an admin gave them access to
for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  test.describe(`Available dashboards - ${role}`, () => {
    // both tests change the access of the same user
    test.describe.configure({mode: 'serial'});

    test.beforeEach(async ({page}) => {
      await openPortalAs(page, user.username, user.password);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    test('sees no dashboard without access', async ({page, browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
      await expectDashboardListed(page, false);
      const drawer = await openMenu(page, 'Dashboards');
      await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toHaveCount(0);
      // only data engineers get the link to the Superset instance
      await expect(drawer.getByRole('button', {name: 'Open Superset Instance', exact: true})).toHaveCount(0);
    });

    test('sees and opens a dashboard after the admin gave access', async ({page, browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
      await expectDashboardListed(page, true);
      // opens it from the Dashboards menu, below its data domain
      const dashboard = await openDashboard(page);
      await expect(dashboard.getByText('Aktueller Bestand').first()).toBeVisible();
    });
  });
}
