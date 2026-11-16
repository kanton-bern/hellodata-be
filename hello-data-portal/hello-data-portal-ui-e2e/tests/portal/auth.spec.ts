import {ADMIN, dismissToasts, expect, openPortalAs, signIn, test} from '../fixtures';

// login with Keycloak, a wrong password and logout of the portal
test.describe('Authentication', () => {
  // the admin opens the portal, logs in on the Keycloak form and lands on the home page, where the top bar shows
  // the admin's email
  test('admin logs in through Keycloak and lands on the home page', async ({page}) => {
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    await expect(page.locator('#topbar-right-group')).toContainText(ADMIN.email);
  });

  // the admin logs in with a wrong password; Keycloak shows an error and keeps the login form
  test('wrong password is rejected by Keycloak', async ({page}) => {
    await page.goto('');
    await signIn(page, ADMIN.username, 'definitely-wrong');
    await expect(page.locator('#input-error, .kc-feedback-text, [id*=error]').first()).toBeVisible();
    await expect(page.locator('#kc-login')).toBeVisible();
  });

  // the admin logs out from the user menu and sees the login form; opening a portal url afterwards leads to the
  // login form again, so the session is gone
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
