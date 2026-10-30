import {Page} from '@playwright/test';
import {
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  dismissToasts,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {changeDashboardAccessAsAdmin, dashboardRow, expectDashboardListed} from '../superset';

/** The metadata of a dashboard is edited with the pencil button of its row in the dashboard list. */
function editButton(page: Page) {
  return dashboardRow(page).locator('button:has(.fa-pen-to-square)');
}

async function saveBusinessProcess(page: Page, value: string) {
  await editButton(page).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('Business Process').fill(value);
  const saved = page.waitForResponse(r => r.url().includes('/superset/dashboards/') && r.request().method() === 'PATCH');
  await dialog.getByRole('button', {name: 'Save', exact: true}).click();
  expect((await saved).ok(), 'saving the dashboard metadata failed').toBe(true);
  await expect(dialog).toBeHidden();
  await dismissToasts(page);
}

// HELLODATA-705
for (const {role, user} of [
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
]) {
  test.describe(`Dashboard metadata - ${role}`, () => {
    test('can edit the metadata of a dashboard', async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDashboardListed(page, true);
      const businessProcess = `e2e business process ${Date.now()}`;
      try {
        await saveBusinessProcess(page, businessProcess);
        await expect(dashboardRow(page).getByRole('cell', {name: businessProcess, exact: true})).toBeVisible();
        // the value is stored, not only shown
        await expectDashboardListed(page, true);
        await expect(dashboardRow(page).getByRole('cell', {name: businessProcess, exact: true})).toBeVisible();
      } finally {
        await saveBusinessProcess(page, '');
      }
    });
  });
}

// HELLODATA-704
for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  test.describe(`Dashboard metadata - ${role}`, () => {
    test.beforeAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    test('cannot edit the metadata of a dashboard', async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDashboardListed(page, true);
      // the info and open buttons are there, the edit button is hidden
      await expect(dashboardRow(page).locator('button:has(.fa-up-right-from-square)')).toBeVisible();
      await expect(editButton(page)).toBeHidden();
    });
  });
}
