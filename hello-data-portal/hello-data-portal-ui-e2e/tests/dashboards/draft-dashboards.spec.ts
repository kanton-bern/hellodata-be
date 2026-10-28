import {DATA_DOMAIN_EDITOR, DATA_DOMAIN_VIEWER, expect, openMenu, openPortalAs, test} from '../fixtures';
import {
  changeDashboardAccessAsAdmin,
  expectDashboardListed,
  openSuperset,
  setDashboardPublished,
  SHOWCASE_DASHBOARD,
  SUPERSET_SYNC_TIMEOUT,
  withUser,
} from '../superset';

// HELLODATA-701: a dashboard turned into a draft in Superset disappears from the Dashboards menu and list
test.describe('Draft dashboards', () => {
  // the portal sees the draft only after the superset sidecar published it, then the dashboard is published again
  test.describe.configure({timeout: 3 * SUPERSET_SYNC_TIMEOUT});

  test.beforeAll(async ({browser}) => {
    await changeDashboardAccessAsAdmin(browser, DATA_DOMAIN_VIEWER, true);
  });

  test.afterAll(async ({browser}) => {
    await changeDashboardAccessAsAdmin(browser, DATA_DOMAIN_VIEWER, false);
  });

  test('are not available to a viewer', async ({page, browser}) => {
    await openPortalAs(page, DATA_DOMAIN_VIEWER.username, DATA_DOMAIN_VIEWER.password);
    await expectDashboardListed(page, true);

    await withUser(browser, DATA_DOMAIN_EDITOR, async editorPage => setDashboardPublished(await openSuperset(editorPage), false));
    try {
      await expectDashboardListed(page, false, SUPERSET_SYNC_TIMEOUT);
      const drawer = await openMenu(page, 'Dashboards');
      await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toHaveCount(0);
    } finally {
      await withUser(browser, DATA_DOMAIN_EDITOR, async editorPage => setDashboardPublished(await openSuperset(editorPage), true));
      // the other tests need the published dashboard
      await expectDashboardListed(page, true, SUPERSET_SYNC_TIMEOUT);
    }
  });
});
