import {Page} from '@playwright/test';
import {ADMIN, BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_BUSINESS_SPECIALIST, dismissToasts, expect, navigate, openPortalAs, signIn, test, User} from '../fixtures';
import {withUser} from '../superset';

// the business specialist has the fewest dependencies in the other specs (no comments, no dashboards by default)
const USER = DATA_DOMAIN_BUSINESS_SPECIALIST;

/** Disables or enables a user from the actions of the user management list, as admin. */
async function setUserEnabled(page: Page, user: User, enabled: boolean) {
  await navigate(page, 'Administration', 'User management');
  await page.getByPlaceholder('Search').fill(user.username);
  const userRow = page.getByRole('row').filter({has: page.getByRole('cell', {name: user.username, exact: true})});
  // the fifth column shows whether the user is enabled; a previous run may have left it in the wanted state
  await expect(userRow).toHaveCount(1);
  if (await userRow.getByRole('cell').nth(4).locator('.text-success').count() === (enabled ? 1 : 0)) {
    return;
  }
  // the actions are icon buttons without a name: edit, disable or enable, delete
  const action = userRow.locator('button').nth(1);
  const label = enabled ? 'Enable' : 'Disable';
  await action.click();
  const dialog = page.getByRole('alertdialog').or(page.getByRole('dialog')).filter({hasText: `${label} user`});
  const saved = page.waitForResponse(r => r.url().endsWith(enabled ? '/enable' : '/disable') && r.request().method() === 'PATCH');
  await dialog.getByRole('button', {name: label, exact: true}).click();
  expect((await saved).ok(), `${label.toLowerCase()} failed`).toBe(true);
  await dismissToasts(page);
}

/** Logs the disabled user in; without permissions the portal may not offer the language selector of openPortalAs. */
async function openPortalAsDisabledUser(page: Page) {
  await page.goto('');
  await signIn(page, USER.username, USER.password);
  await expect(page).toHaveURL(/\/home$/);
  await expect(page.locator('#userMenu')).toBeVisible();
}

function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function dataMartsMenu(page: Page) {
  return page.locator('#appSidebar').getByRole('button', {name: 'Data Marts', exact: true});
}

function permissionMissing(page: Page) {
  return page.locator('.permission-error');
}

// HELLODATA-4156: a user disabled in the portal can still log in (the Keycloak realms are shared), but gets no
// permissions at all and sees only the admins to contact
test.describe('Disabled user', () => {
  test.describe.configure({mode: 'serial'});

  test.beforeAll(async ({browser}) => {
    await withUser(browser, ADMIN, page => setUserEnabled(page, USER, false));
  });

  test.afterAll(async ({browser}) => {
    await withUser(browser, ADMIN, page => setUserEnabled(page, USER, true));
  });

  // the disabled user logs in and gets no permissions: the page lists the admins to contact; there is no
  // dashboards panel and no Data Marts menu entry
  test('sees only the list of admins to contact and no menu entries of its role', async ({page}) => {
    await openPortalAsDisabledUser(page);
    await expect(permissionMissing(page)).toBeVisible();
    // the portal lists the enabled business domain admins, or the HelloDATA admins when there is none
    // (UserService.findHelloDataAdminUsers), so either one is a valid contact
    const admins = [ADMIN.email, BUSINESS_DOMAIN_ADMIN.username];
    await expect(permissionMissing(page).getByRole('listitem').filter({hasText: new RegExp(admins.map(escapeRegExp).join('|'))})
      .first()).toBeVisible();
    // none of the panels of a user with a data domain role
    await expect(page.locator('app-dashboards')).toHaveCount(0);
    // a business specialist normally has the DataMarts Viewer (DATA_MARTS)
    await expect(dataMartsMenu(page)).toHaveCount(0);
  });

  // the disabled user opens pages by url (data marts viewer, announcements); each time it is sent back to the home page
  // with the missing permissions notice
  test('is sent back to the home page when it opens a page by url', async ({page}) => {
    await openPortalAsDisabledUser(page);
    for (const url of ['embedded-dm-viewer', 'published-announcements']) {
      await page.goto(url);
      await expect(page, url).toHaveURL(/\/home$/);
      await expect(permissionMissing(page), url).toBeVisible();
    }
  });

  // the admin enables the user again; after the next login the user sees its dashboards panel and the Data Marts menu,
  // and the missing permissions notice is gone
  test('gets its access back when the admin enables it again', async ({page, browser}) => {
    await withUser(browser, ADMIN, adminPage => setUserEnabled(adminPage, USER, true));
    await openPortalAs(page, USER.username, USER.password);
    await expect(page.locator('app-dashboards')).toBeVisible();
    await expect(permissionMissing(page)).toHaveCount(0);
    await expect(dataMartsMenu(page)).toBeVisible();
  });
});
