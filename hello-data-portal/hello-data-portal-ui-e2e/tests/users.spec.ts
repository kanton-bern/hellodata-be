import {ADMIN, BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER, expect, navigate, test} from './fixtures';

// the menus and pages each role may open are covered by roles.spec.ts
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
    for (const email of [ADMIN.email, BUSINESS_DOMAIN_ADMIN.username, DATA_DOMAIN_VIEWER.username, DATA_DOMAIN_BUSINESS_SPECIALIST.username]) {
      await expect(page.getByRole('cell', {name: email, exact: true})).toBeVisible();
    }
  });
});
