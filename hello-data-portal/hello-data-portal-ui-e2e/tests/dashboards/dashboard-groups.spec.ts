import {Page} from '@playwright/test';
import {deleteDashboardGroup} from '../batch-import';
import {DATA_DOMAIN_VIEWER, dismissToasts, expect, test} from '../fixtures';
import {changeDashboardAccessAsAdmin, expectDashboardListed, SHOWCASE_DASHBOARD, withUser} from '../superset';

const CONTEXT_KEY = 'Default_Data_Domain';
const USER = DATA_DOMAIN_VIEWER;

function groupRow(page: Page, name: string) {
  return page.getByRole('row').filter({has: page.getByRole('cell', {name, exact: true})});
}

async function openDashboardGroups(page: Page) {
  await page.goto(`dashboard-groups/list/${CONTEXT_KEY}`);
  await expect(page.locator('#dashboardGroupsTable')).toBeVisible();
}

// An admin groups dashboards and users of a data domain in its dashboard group page: every member of a group gets the
// dashboards of the group, and loses them again when the group is deleted
test.describe('Dashboard groups', () => {
  test.describe.configure({mode: 'serial'});
  const GROUP = `e2e-group-${Date.now().toString(36)}`;

  test.beforeAll(async ({browser}) => {
    // the viewer must get the dashboard through the group only
    await changeDashboardAccessAsAdmin(browser, USER, false);
  });

  test.afterAll(() => {
    // a test that failed half way leaves the group behind
    deleteDashboardGroup(GROUP);
  });

  // the admin creates a dashboard group with the showcase dashboard and the viewer as member, then sees it in the list
  // with the dashboard and one user
  test('admin creates a group with a dashboard and a viewer', async ({adminPage: page}) => {
    await openDashboardGroups(page);
    await page.getByRole('button', {name: 'Create', exact: true}).click();
    await page.locator('input#name').fill(GROUP);

    await page.getByRole('checkbox', {name: SHOWCASE_DASHBOARD, exact: true}).check();
    await page.getByRole('tab', {name: /^Users/}).click();
    // the panel of the Users tab by its name: an unnamed tabpanel locator can still resolve to the Dashboards panel while
    // the tabs switch, and an action keeps waiting on the element it resolved to once, which then stays hidden
    await page.getByRole('tabpanel', {name: /^Users/}).getByPlaceholder('Search').fill(USER.username);
    await page.getByRole('checkbox', {name: new RegExp(USER.username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))}).check();
    await expect(page.getByRole('tab', {name: 'Users (1)'})).toBeVisible();

    const saved = page.waitForResponse(r => r.url().endsWith('/dashboard-groups') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Save', exact: true}).click();
    expect((await saved).ok(), 'saving the dashboard group failed').toBe(true);
    await dismissToasts(page);

    await openDashboardGroups(page);
    const row = groupRow(page, GROUP);
    await expect(row).toContainText(SHOWCASE_DASHBOARD);
    await expect(row.getByRole('cell').nth(2)).toHaveText('1');
  });

  // the viewer, who has no direct access, sees the showcase dashboard through the dashboard group
  test('the viewer sees the dashboard of its group', async ({browser}) => {
    await withUser(browser, USER, page => expectDashboardListed(page, true));
  });

  // the admin deletes the dashboard group, it disappears from the list and the viewer no longer sees the dashboard
  test('admin deletes the group and the viewer loses the dashboard', async ({adminPage: page, browser}) => {
    await openDashboardGroups(page);
    const row = groupRow(page, GROUP);
    await row.locator('button.p-button-danger').click();
    const deleted = page.waitForResponse(r => r.url().includes('/dashboard-groups/') && r.request().method() === 'DELETE');
    await page.locator('.p-confirmdialog').getByRole('button', {name: 'Delete', exact: true}).click();
    expect((await deleted).ok(), 'deleting the dashboard group failed').toBe(true);
    await expect(row).toHaveCount(0);

    await withUser(browser, USER, viewerPage => expectDashboardListed(viewerPage, false));
  });
});
