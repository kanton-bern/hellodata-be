import {writeFile} from 'fs/promises';
import {Page} from '@playwright/test';
import {expect, navigate, test} from '../fixtures';
import {dashboardRow, expectDashboardListed} from '../superset';

/**
 * Exports the dashboard from the import-export page and returns the zip. The portal opens the Superset export url in a
 * new tab with 'noopener', which Playwright does not report as a download, so the test fetches the same url with the
 * session of the browser context.
 */
async function exportDashboard(page: Page) {
  await navigate(page, 'Administration', 'Dashboards import-export');
  await expect(page).toHaveURL(/\/dashboard-import-export$/);
  await dashboardRow(page).getByRole('checkbox').check();
  const exportRequest = page.context().waitForEvent('request', r => r.url().includes('/api/v1/dashboard/export'));
  await page.getByRole('button', {name: 'Export', exact: true}).click();
  const response = await page.context().request.get((await exportRequest).url());
  expect(response.ok(), 'Superset export failed').toBe(true);
  expect(response.headers()['content-type']).toBe('application/zip');
  expect(response.headers()['content-disposition']).toMatch(/attachment; filename=dashboard_export_.*\.zip/);
  return response.body();
}

// HELLODATA-1318 and HELLODATA-1319
test.describe('Dashboard export and import', () => {
  test('exports a dashboard as zip', async ({adminPage: page}) => {
    const zip = await exportDashboard(page);
    // a zip file starts with "PK"
    expect(zip.subarray(0, 2).toString()).toBe('PK');
  });

  test('imports an exported dashboard', async ({adminPage: page}, testInfo) => {
    const file = testInfo.outputPath('dashboard_export.zip');
    await writeFile(file, await exportDashboard(page));

    await page.getByRole('button', {name: 'Import', exact: true}).click();
    const upload = page.locator('p-fileupload');
    await upload.locator('input[type=file]').first().setInputFiles(file);
    // without "Remove orphaned charts and datasets", so nothing else of the data domain is touched
    await expect(page.getByRole('checkbox', {name: 'Remove orphaned charts and datasets'})).not.toBeChecked();
    const uploaded = page.waitForResponse(r => r.url().includes('/superset/upload-dashboards/') && r.request().method() === 'POST');
    await upload.getByRole('button', {name: 'Upload', exact: true}).click();
    expect((await uploaded).ok(), 'dashboard upload failed').toBe(true);

    // the import overwrites the existing dashboard, which stays available
    await expectDashboardListed(page, true);
  });
});
