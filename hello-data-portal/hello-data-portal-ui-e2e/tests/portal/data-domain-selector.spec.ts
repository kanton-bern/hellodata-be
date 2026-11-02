import {Page} from '@playwright/test';
import {ADMIN, DATA_DOMAIN_ADMIN, expect, openMenu, openPortalAs, test, User} from '../fixtures';
import {DATA_DOMAIN, dashboardRow, expectDashboardListed, SHOWCASE_DASHBOARD} from '../superset';

const ALL_DATA_DOMAINS = 'All Data Domains';
// the second data domain of the e2e installation (compose-override.yaml); it has no Superset and therefore no dashboards
const EXTRA_DATA_DOMAIN = 'Extra Data Domain';

/** The name of the selected data domain in the top bar. */
function selectedDataDomain(page: Page) {
  return page.locator('.data-domain-selector .data-domain-name');
}

/** The entries of the data domain menu, which lists the data domains of the user except the selected one. */
function dataDomainEntry(page: Page, name: string) {
  return page.getByRole('menuitem', {name, exact: true});
}

async function openDataDomainMenu(page: Page) {
  await selectedDataDomain(page).click();
  await expect(page.getByRole('menuitem').first()).toBeVisible();
}

async function selectDataDomain(page: Page, name: string) {
  await openDataDomainMenu(page);
  await dataDomainEntry(page, name).click();
  await expect(selectedDataDomain(page)).toHaveText(name);
}

/** Logs a user in; a new browser context has no stored selection, so the portal starts with all data domains. */
async function openPortalWithAllDataDomains(page: Page, user: User) {
  await openPortalAs(page, user.username, user.password);
  await expect(selectedDataDomain(page)).toHaveText(ALL_DATA_DOMAINS);
}

test.describe('Data domain selector', () => {
  test('admin chooses between all data domains', async ({page}) => {
    await openPortalWithAllDataDomains(page, ADMIN);
    await openDataDomainMenu(page);
    await expect(dataDomainEntry(page, DATA_DOMAIN)).toBeVisible();
    await expect(dataDomainEntry(page, EXTRA_DATA_DOMAIN)).toBeVisible();
    // the selected entry is shown in the top bar, not in the menu
    await expect(dataDomainEntry(page, ALL_DATA_DOMAINS)).toHaveCount(0);

    await dataDomainEntry(page, DATA_DOMAIN).click();
    await expect(selectedDataDomain(page)).toHaveText(DATA_DOMAIN);
    await openDataDomainMenu(page);
    await expect(dataDomainEntry(page, ALL_DATA_DOMAINS)).toBeVisible();
    await expect(dataDomainEntry(page, EXTRA_DATA_DOMAIN)).toBeVisible();
    await expect(dataDomainEntry(page, DATA_DOMAIN)).toHaveCount(0);
  });

  test(`${DATA_DOMAIN_ADMIN.username} chooses only its own data domain`, async ({page}) => {
    await openPortalWithAllDataDomains(page, DATA_DOMAIN_ADMIN);
    await openDataDomainMenu(page);
    await expect(dataDomainEntry(page, DATA_DOMAIN)).toBeVisible();
    await expect(dataDomainEntry(page, EXTRA_DATA_DOMAIN)).toHaveCount(0);
  });

  test('the selected data domain filters the dashboards', async ({page}) => {
    await openPortalWithAllDataDomains(page, ADMIN);
    await expectDashboardListed(page, true);
    let drawer = await openMenu(page, 'Dashboards');
    await expect(drawer.getByRole('button', {name: DATA_DOMAIN, exact: true})).toBeVisible();
    // an open menu drawer takes the first click outside of it, so start the selection on a freshly loaded page
    await page.reload();

    // the extra data domain has no dashboards, so neither the list nor the menu shows the ones of the default data domain
    await selectDataDomain(page, EXTRA_DATA_DOMAIN);
    await expect(page.getByText('No dashboards found', {exact: true})).toBeVisible();
    await expect(dashboardRow(page)).toHaveCount(0);
    drawer = await openMenu(page, 'Dashboards');
    await expect(drawer.getByRole('button', {name: 'List', exact: true})).toBeVisible();
    await expect(drawer.getByRole('button', {name: DATA_DOMAIN, exact: true})).toHaveCount(0);
    await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toHaveCount(0);
    await page.reload();

    await selectDataDomain(page, DATA_DOMAIN);
    await expect(dashboardRow(page)).toBeVisible();
  });

  test('the selected data domain is kept after a reload', async ({page}) => {
    await openPortalWithAllDataDomains(page, DATA_DOMAIN_ADMIN);
    await selectDataDomain(page, DATA_DOMAIN);
    await page.reload();
    await expect(selectedDataDomain(page)).toHaveText(DATA_DOMAIN);

    await selectDataDomain(page, ALL_DATA_DOMAINS);
    await page.reload();
    await expect(selectedDataDomain(page)).toHaveText(ALL_DATA_DOMAINS);
  });
});
