import {Page} from '@playwright/test';
import {ADMIN, DATA_DOMAIN_BUSINESS_SPECIALIST, expect, signIn, test} from '../fixtures';
import {openUserEdit, withUser} from '../superset';
import {portalUser} from '../batch-import';
import {enabledInKeycloak, setEnabledInKeycloak} from '../keycloak';

// the business specialist has the fewest dependencies in the other specs (no comments, no dashboards by default)
const USER = DATA_DOMAIN_BUSINESS_SPECIALIST;

function authProviderWarning(page: Page) {
  return page.locator('#disabled-in-auth-provider-warning');
}

// HELLODATA-4194: the portal never changes users in the central authentication service (Keycloak), and since then the
// Keycloak sync no longer copies the enabled flag from there either. A user disabled only in Keycloak stays enabled in
// the portal but cannot log in, so the user edit page warns about it.
test.describe('User disabled in the central authentication service', () => {
  test.describe.configure({mode: 'serial'});

  test.beforeAll(() => {
    setEnabledInKeycloak(USER.username, false);
  });

  test.afterAll(() => {
    setEnabledInKeycloak(USER.username, true);
  });

  // the admin opens the edit page of the user disabled in Keycloak: the user is still enabled in the portal and the page
  // shows the warning
  test('the user edit page warns that the user is disabled there', async ({adminPage: page}) => {
    expect(enabledInKeycloak(USER.username)).toBe(false);
    expect(portalUser(USER.username)?.enabled).toBe(true);
    await openUserEdit(page, USER);
    await expect(authProviderWarning(page)).toBeVisible();
    await expect(authProviderWarning(page)).toContainText('disabled in the central authentication service');
  });

  // Keycloak refuses the login of the user, whatever the portal says
  test('the user cannot log in', async ({page}) => {
    await page.goto('');
    await signIn(page, USER.username, USER.password);
    await expect(page.getByText(/account is disabled/i)).toBeVisible();
    await expect(page).not.toHaveURL(/\/home$/);
  });

  // once the user is enabled in Keycloak again the warning is gone
  test('the warning is gone once the user is enabled there again', async ({browser}) => {
    setEnabledInKeycloak(USER.username, true);
    await withUser(browser, ADMIN, async page => {
      await openUserEdit(page, USER);
      // the warning is rendered together with the user data, so wait for the user before checking it is not there
      await expect(page.locator('#email')).toHaveValue(USER.username);
      await expect(authProviderWarning(page)).toHaveCount(0);
    });
  });
});
