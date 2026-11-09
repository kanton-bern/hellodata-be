import {expect, Locator, Page, test as base} from '@playwright/test';
import {BUSINESS_DOMAIN_ADMIN_USERNAME, EXAMPLE_USERS_EMAIL_DOMAIN} from './instance';

export const ADMIN = {
  username: process.env.E2E_ADMIN_USERNAME ?? 'admin',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'admin',
  email: process.env.E2E_ADMIN_EMAIL ?? 'admin@hellodata.ch',
};

export type User = {username: string, password: string};

// example users created by the portal api's create-example-users profile (see ExampleUsersInitializer),
// for the business domain and the default data domain (the e-mail domain and the business domain admin differ per
// installation, see instance.ts)
export const BUSINESS_DOMAIN_ADMIN: User = {username: BUSINESS_DOMAIN_ADMIN_USERNAME, password: 'admin'};
export const DATA_DOMAIN_ADMIN: User = {username: `default-data-domain-admin@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'admin'};
export const DATA_DOMAIN_EDITOR: User = {username: `default-data-domain-editor@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'editor'};
export const DATA_DOMAIN_BUSINESS_SPECIALIST: User = {username: `default-data-domain-business_specialist@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'specialist'};
export const DATA_DOMAIN_VIEWER: User = {username: `default-data-domain-viewer@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'viewer'};
// the second data domain exists only in the e2e installation (compose-override.yaml), which creates its example users too
export const EXTRA_DATA_DOMAIN_ADMIN: User = {username: `extra-data-domain-admin@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'admin'};
export const EXTRA_DATA_DOMAIN_VIEWER: User = {username: `extra-data-domain-viewer@${EXAMPLE_USERS_EMAIL_DOMAIN}`, password: 'viewer'};

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
  await selectLanguage(page, 'EN');
}

/** Selects a language in the top bar (EN, DE, FR, ...), which the portal saves for the user. */
export async function selectLanguage(page: Page, language: string) {
  const link = page.locator('#topbar-language-selector a', {hasText: language});
  if (!(await link.getAttribute('class'))?.includes('selected-language')) {
    // the portal saves the language of the user, wait for it so a reload keeps it
    const saved = page.waitForResponse(r => r.url().includes('/set-selected-lang/') && r.request().method() === 'PATCH');
    await link.click();
    expect((await saved).ok(), `selecting ${language} failed`).toBe(true);
  }
  await expect(link).toHaveClass(/selected-language/);
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

/**
 * Expands a collapsible group of the sub navigation drawer (e.g. a data domain in the Dashboards menu) and returns the
 * group. Each data domain with Superset has its own Open Superset Instance and All Dashboards Comments entries, so look
 * them up in the group.
 */
export async function expandMenuGroup(drawer: Locator, group: string): Promise<Locator> {
  const header = drawer.getByRole('button', {name: group, exact: true});
  if (await header.getAttribute('aria-expanded') !== 'true') {
    await header.click();
  }
  await expect(header).toHaveAttribute('aria-expanded', 'true');
  // the inner locator of has is matched inside each group, so it must not start at the drawer
  return drawer.locator('.nav-drawer__group').filter({has: drawer.page().getByRole('button', {name: group, exact: true})});
}

/** Opens a first-level entry of the left sidebar and clicks one of its sub entries, optionally inside a group. */
export async function navigate(page: Page, menu: string, item: string, group?: string) {
  const drawer = await openMenu(page, menu);
  const parent = group ? await expandMenuGroup(drawer, group) : drawer;
  await parent.getByRole('button', {name: item, exact: true}).click();
}

/** Picks an option of a PrimeNG select, its options are appended to the body. */
export async function selectOption(select: Locator, option: string) {
  // the accessible name of an option is its untranslated label (e.g. "@deleted"), so match the shown text
  const shown = new RegExp(`^\\s*${option.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`);
  const optionEntry = select.page().getByRole('option').filter({hasText: shown});
  // a click on the select while the overlay of the previous selection still closes can close the new one right away,
  // so open it again until the option can be picked
  await expect(async () => {
    if (!await optionEntry.isVisible()) {
      await select.click();
    }
    await optionEntry.click({timeout: 2_000});
  }).toPass({timeout: 20_000});
  await expect(select).toContainText(option);
}

type Fixtures = {
  /** A page with the admin user logged in, on the home page. */
  adminPage: Page;
};

/** The Airflow versions the portal can embed: 2 (Orchestration) and 3 (Orchestration (Airflow3)). */
export type AirflowVersion = '2' | '3';

export type WorkerFixtures = {
  /**
   * The Airflow the Data Eng. specs test, set per Playwright project (playwright.config.ts): the project airflow2 runs the
   * whole suite, airflow3 only the specs tagged @airflow.
   */
  airflowVersion: AirflowVersion;
};

export const test = base.extend<Fixtures, WorkerFixtures>({
  airflowVersion: ['2', {option: true, scope: 'worker'}],
  adminPage: async ({page}, use) => {
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    await use(page);
  },
});

export {expect};
