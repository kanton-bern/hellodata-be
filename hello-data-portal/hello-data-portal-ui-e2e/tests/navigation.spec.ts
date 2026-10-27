import {ADMIN, errorToasts, expect, navigate, openPortalAs, test} from './fixtures';

// which menu entries each role sees is covered by roles.spec.ts
test.describe('Navigation', () => {
  const adminPages = [
    {item: 'User management', url: /\/user-management$/, heading: 'Users management'},
    {item: 'Portal role management', url: /\/roles-management$/, heading: 'Portal roles management'},
    {item: 'Notifications', url: /\/announcements-management$/, heading: 'Notifications management'},
    {item: 'FAQ management', url: /\/faq-management$/, heading: 'FAQ management'},
    {item: 'Documentation management', url: /\/documentation-management$/, heading: 'Documentation management'},
  ];

  for (const {item, url, heading} of adminPages) {
    test(`administration menu opens ${item}`, async ({adminPage: page}) => {
      await navigate(page, 'Administration', item);
      await expect(page).toHaveURL(url);
      await expect(page.getByRole('heading', {name: heading, exact: true})).toBeVisible();
    });
  }

  test('home page shows the overview panels', async ({adminPage: page}) => {
    const main = page.locator('#mainContentDiv');
    for (const panel of ['Dashboards', 'External dashboards', 'Data Marts', 'Lineage', 'FAQ', 'Documentation']) {
      await expect(main.getByText(panel, {exact: true}).first()).toBeVisible();
    }
  });

  test('home page loads the lineage docs without errors', async ({page}) => {
    // the docs url is published by the dbt-docs sidecar, so the request starts some time after the page loaded
    const docsRequest = page.waitForRequest(/\/api\/projects-docs$/, {timeout: 45_000});
    await openPortalAs(page, ADMIN.username, ADMIN.password);
    const response = await (await docsRequest).response();
    expect(response?.ok(), 'GET .../api/projects-docs failed').toBe(true);
    await expect(errorToasts(page)).toHaveCount(0);
  });
});
