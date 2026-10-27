import {ADMIN, expect, navigate, openPortalAs, test} from './fixtures';

// example users created by the portal api's create-example-users profile (see application.yml)
const VIEWER = {
  username: process.env.E2E_VIEWER_USERNAME ?? 'default-data-domain-viewer@hellodata.ch',
  password: process.env.E2E_VIEWER_PASSWORD ?? 'viewer',
};

test.describe('Users and roles', () => {
  test('profile shows the roles of the logged in user', async ({adminPage: page}) => {
    await page.locator('#userMenu').click();
    await page.getByRole('menuitem', {name: 'Profile'}).click();
    await expect(page).toHaveURL(/\/profile$/);
    const main = page.locator('#mainContentDiv');
    await expect(main).toContainText(ADMIN.email);
    await expect(main.getByRole('cell', {name: 'HELLODATA ADMIN'})).toBeVisible();
  });

  test('user management lists the admin and the example users', async ({adminPage: page}) => {
    await navigate(page, 'Administration', 'User management');
    await expect(page).toHaveURL(/\/user-management$/);
    await expect(page.getByRole('cell', {name: ADMIN.email, exact: true})).toBeVisible();
    await expect(page.getByRole('cell', {name: VIEWER.username, exact: true})).toBeVisible();
  });

  test('data domain viewer has no administration menu', async ({page}) => {
    await openPortalAs(page, VIEWER.username, VIEWER.password);
    await expect(page.locator('#topbar-right-group')).toContainText(VIEWER.username);
    // wait for the home page content, so the menu is built before asserting what is missing
    await expect(page.locator('#mainContentDiv').getByText('FAQ', {exact: true})).toBeVisible();
    const sidebar = page.locator('#appSidebar');
    await expect(sidebar.getByRole('button', {name: 'Administration', exact: true})).toHaveCount(0);
    await expect(sidebar.getByRole('button', {name: 'DevTools', exact: true})).toHaveCount(0);
  });
});
