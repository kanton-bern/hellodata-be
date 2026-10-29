import {Browser, BrowserContext, FrameLocator, Page} from '@playwright/test';
import {ADMIN, dismissToasts, expandMenuGroup, expect, navigate, openMenu, openPortalAs, User} from './fixtures';

// the Superset instance of the default data domain and the dashboard its database init script creates
export const DATA_DOMAIN = 'Default Data Domain';
export const SHOWCASE_DASHBOARD = 'HD-Showcase Tierstatistik';
export const SHOWCASE_DASHBOARD_SLUG = 'tierstatistik';

// the superset sidecar publishes dashboards, users and roles to the portal on a fixed interval
// (HD_SUPERSET_PUBLISH_INTERVAL_MINUTES, 1 minute in the e2e workflow), so changes made in Superset need some time
export const SUPERSET_SYNC_TIMEOUT = 3 * 60_000;
// access granted in the portal reaches the user's dashboard list after the next sync with Superset
export const ACCESS_SYNC_TIMEOUT = 90_000;

/** The row of a dashboard in a PrimeNG table (dashboard list, import-export page). */
export function dashboardRow(page: Page, title = SHOWCASE_DASHBOARD) {
  return page.getByRole('row').filter({has: page.getByRole('cell', {name: title, exact: true})});
}

/** Reloads the dashboard list until the dashboard is (or is no longer) listed. */
export async function expectDashboardListed(page: Page, listed: boolean, timeout = ACCESS_SYNC_TIMEOUT, title = SHOWCASE_DASHBOARD) {
  await expect(async () => {
    // the table shows "No dashboards found" until the dashboards are loaded, so wait for them first
    const loaded = page.waitForResponse(r => r.url().endsWith('/superset/my-dashboards') && r.ok());
    await page.goto('my-dashboards');
    await loaded;
    const row = dashboardRow(page, title);
    await expect(row.or(page.getByText('No dashboards found', {exact: true}))).toBeVisible();
    await expect(row).toHaveCount(listed ? 1 : 0);
  }).toPass({timeout, intervals: [5_000, 10_000]});
}

/** Opens a dashboard from the Dashboards menu and returns the embedded Superset dashboard once it is rendered. */
export async function openDashboard(page: Page, title = SHOWCASE_DASHBOARD): Promise<FrameLocator> {
  await navigate(page, 'Dashboards', title, DATA_DOMAIN);
  await expect(page).toHaveURL(/\/my-dashboards\/detail\//);
  const frame = page.frameLocator('app-subsystem-iframe iframe');
  // the dashboard header shows the title, the charts are loaded afterwards
  await expect(frame.getByText(title, {exact: true}).first()).toBeVisible({timeout: 60_000});
  return frame;
}

/** Opens the Superset instance of the data domain from the Dashboards menu, as a logged in user, in a new tab. */
export async function openSuperset(page: Page): Promise<Page> {
  const drawer = await openMenu(page, 'Dashboards');
  await expandMenuGroup(drawer, DATA_DOMAIN);
  // the portal opens it with window.open(..., 'noopener'), so it is not reported as a popup of this page
  const supersetPage = page.context().waitForEvent('page');
  await drawer.getByRole('button', {name: 'Open Superset Instance', exact: true}).click();
  const superset = await supersetPage;
  await expect(superset).toHaveURL(/\/superset\/welcome\//, {timeout: 60_000});
  return superset;
}

/** Publishes a dashboard or turns it into a draft, with the status button of the dashboard header in Superset. */
export async function setDashboardPublished(superset: Page, published: boolean, slug = SHOWCASE_DASHBOARD_SLUG) {
  await superset.goto(new URL(`/superset/dashboard/${slug}/`, superset.url()).toString());
  // the accessible name starts with the name of the icon, e.g. "circle-check Published"
  const current = superset.getByRole('button', {name: published ? /Draft$/ : /Published$/});
  const wanted = superset.getByRole('button', {name: published ? /Published$/ : /Draft$/});
  await expect(current.or(wanted)).toBeVisible({timeout: 60_000});
  if (await current.isVisible()) {
    await current.click();
  }
  await expect(wanted).toBeVisible();
}

/** Opens the edit page of a user in the admin's user management. */
export async function openUserEdit(page: Page, user: User) {
  await navigate(page, 'Administration', 'User management');
  // the table shows 10 users per page, so filter it in case other tests added users
  await page.getByPlaceholder('Search').fill(user.username);
  const userRow = page.getByRole('row').filter({has: page.getByRole('cell', {name: user.username, exact: true})});
  // the actions are icon buttons without a name: edit is the first one
  await userRow.locator('button').first().click();
  await expect(page).toHaveURL(/\/user-management\/edit\//);
}

/** Saves the user edit page: roles, dashboards and comment permissions are saved together. */
export async function saveUserEdit(page: Page) {
  const saved = page.waitForResponse(r => r.url().endsWith('/context-roles') && r.request().method() === 'PATCH');
  await page.getByRole('button', {name: 'Save', exact: true}).first().click();
  expect((await saved).ok(), 'saving the user failed').toBe(true);
  await dismissToasts(page);
}

/** Gives a viewer or business specialist access to a dashboard (or takes it away) in the admin's user management. */
export async function setDashboardAccess(page: Page, user: User, granted: boolean, title = SHOWCASE_DASHBOARD) {
  await openUserEdit(page, user);
  const dashboard = page.getByRole('checkbox', {name: title, exact: true});
  await expect(dashboard).toBeVisible();
  if (await dashboard.isChecked() === granted) {
    return;
  }
  await dashboard.setChecked(granted);
  await saveUserEdit(page);
}

/** Logs the admin in in a separate browser context and changes the dashboard access of a user. */
export async function changeDashboardAccessAsAdmin(browser: Browser, user: User, granted: boolean) {
  await withUser(browser, ADMIN, page => setDashboardAccess(page, user, granted));
}

/** Runs the callback with another user logged in, in a browser context of its own. */
export async function withUser<T>(browser: Browser, user: User, callback: (page: Page, context: BrowserContext) => Promise<T>): Promise<T> {
  const context = await browser.newContext();
  try {
    const page = await context.newPage();
    await openPortalAs(page, user.username, user.password);
    return await callback(page, context);
  } finally {
    await context.close();
  }
}
