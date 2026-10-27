import {ADMIN, dismissToasts, expect, openPortalAs, signIn, test} from './fixtures';

test.describe('Authentication', () => {
  test('admin logs in through Keycloak and lands on the home page', async ({page}) => {
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    await expect(page.locator('#topbar-right-group')).toContainText(ADMIN.email);
  });

  test('wrong password is rejected by Keycloak', async ({page}) => {
    await page.goto('');
    await signIn(page, ADMIN.username, 'definitely-wrong');
    await expect(page.locator('#input-error, .kc-feedback-text, [id*=error]').first()).toBeVisible();
    await expect(page.locator('#kc-login')).toBeVisible();
  });

  test('logout ends the session', async ({page}) => {
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    // home page errors are covered by navigation.spec.ts - here they would only hide the user menu
    await page.waitForLoadState('networkidle');
    await dismissToasts(page);
    await page.locator('#userMenu').click();
    await page.getByRole('menuitem', {name: 'Logout'}).click();
    await expect(page.locator('#kc-login')).toBeVisible();
    // the Keycloak session is gone too, so any portal url leads back to the login form
    await page.goto('home');
    await expect(page.locator('#kc-login')).toBeVisible();
  });
});
