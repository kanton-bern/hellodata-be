import {expect, Page, test as base} from '@playwright/test';

export const ADMIN = {
  username: process.env.E2E_ADMIN_USERNAME ?? 'admin',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'admin',
  email: process.env.E2E_ADMIN_EMAIL ?? 'admin@hellodata.ch',
};

/** Fills the Keycloak login form the portal redirects to. */
export async function signIn(page: Page, username: string, password: string) {
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();
}

/** Opens the portal, logs in through Keycloak and waits until the home page is rendered in English. */
export async function openPortalAs(page: Page, username: string, password: string) {
  await page.goto('');
  await signIn(page, username, password);
  await expect(page).toHaveURL(/\/home$/);
  await expect(page.locator('#userMenu')).toBeVisible();
  // the selected language is a per-user setting, so a previous run may have left another one active
  const english = page.locator('#topbar-language-selector a', {hasText: 'EN'});
  if (!(await english.getAttribute('class'))?.includes('selected-language')) {
    await english.click();
  }
  await expect(english).toHaveClass(/selected-language/);
}

/** Error notifications shown by the portal (PrimeNG toast). */
export function errorToasts(page: Page) {
  return page.locator('.p-toast-message-error');
}

/** Closes all open notifications, since they can cover the top bar. */
export async function dismissToasts(page: Page) {
  const close = page.locator('.p-toast-close-button');
  for (let i = 0; i < 5 && await close.count() > 0; i++) {
    // a toast may fade out on its own while we try to close it
    await close.first().click({timeout: 2_000}).catch(() => undefined);
  }
}

/** Opens a first-level entry of the left sidebar and returns its sub navigation drawer. */
export async function openMenu(page: Page, menu: string) {
  await page.locator('#appSidebar').getByRole('button', {name: menu, exact: true}).click();
  const drawer = page.getByRole('navigation', {name: 'Sub navigation'});
  await expect(drawer).toBeVisible();
  return drawer;
}

/** Opens a first-level entry of the left sidebar and clicks one of its sub entries. */
export async function navigate(page: Page, menu: string, item: string) {
  const drawer = await openMenu(page, menu);
  await drawer.getByRole('button', {name: item, exact: true}).click();
}

type Fixtures = {
  /** A page with the admin user logged in, on the home page. */
  adminPage: Page;
};

export const test = base.extend<Fixtures>({
  adminPage: async ({page}, use) => {
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    await use(page);
  },
});

export {expect};
