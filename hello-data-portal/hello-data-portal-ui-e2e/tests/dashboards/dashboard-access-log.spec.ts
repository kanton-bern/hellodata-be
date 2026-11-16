import {Page} from '@playwright/test';
import {DATA_DOMAIN_EDITOR, expect, test} from '../fixtures';
import {openDashboard, SHOWCASE_DASHBOARD, withUser} from '../superset';

// the portal sidecar copies the dashboard views from the logs of Superset (HELLO_DATA_SYNCHRONIZE_DASHBOARD_ACCESS_IN_MINUTES,
// 1 minute in compose-override.yaml instead of 60), after an initial delay of 2 minutes
const ACCESS_LOG_TIMEOUT = 5 * 60_000;

async function accessLogRow(page: Page, username: string) {
  await page.goto('dashboard-access/list');
  await page.locator('#dashboard_access_table').getByPlaceholder('Search').fill(username);
  return page.getByRole('row').filter({has: page.getByRole('cell', {name: username, exact: true})})
    .filter({has: page.getByRole('cell', {name: SHOWCASE_DASHBOARD, exact: true})});
}

// HELLODATA admins see who opened which dashboard of a data domain, and when, in Administration -> Dashboard access
test.describe('Dashboard access log', () => {
  // an editor opens a dashboard, then the admin finds a row with that user and dashboard in the dashboard access list
  // (the list is filled by a sync, so the test retries for a few minutes)
  test('lists a user who opened a dashboard', async ({adminPage: page, browser}) => {
    test.setTimeout(ACCESS_LOG_TIMEOUT + 2 * 60_000);
    await withUser(browser, DATA_DOMAIN_EDITOR, async editorPage => {
      await openDashboard(editorPage);
    });

    await expect(async () => {
      await expect(await accessLogRow(page, DATA_DOMAIN_EDITOR.username)).not.toHaveCount(0);
    }).toPass({timeout: ACCESS_LOG_TIMEOUT, intervals: [15_000, 30_000]});
  });
});
