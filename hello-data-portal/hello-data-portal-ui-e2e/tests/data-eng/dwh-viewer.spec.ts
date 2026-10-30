import {Page, Response} from '@playwright/test';
import {
  ADMIN,
  BUSINESS_DOMAIN_ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {expectNoDataEngEntry, openDwhViewer, routeCloudbeaver, subsystemFrame} from '../data-eng';

/** Collects the GraphQL responses of CloudBeaver, to find out whether it knows the user. */
function cloudbeaverResponses(page: Page) {
  const responses: Response[] = [];
  page.context().on('response', response => {
    if (response.url().includes('/cloudbeaver/api/gql')) {
      responses.push(response);
    }
  });
  return responses;
}

/** Waits until CloudBeaver reports a logged in user: the gateway logs the portal user in (reverse proxy authentication). */
async function expectCloudbeaverUser(responses: Response[]) {
  await expect.poll(async () => {
    for (const response of responses) {
      const body = await response.text().catch(() => '');
      if (body.includes('"userId":"')) {
        return true;
      }
    }
    return false;
  }, {timeout: 60_000, message: 'CloudBeaver did not report a logged in user'}).toBe(true);
}

// HELLODATA-855
test.describe('DWH Viewer', () => {
  for (const user of [ADMIN, BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_ADMIN]) {
    test(`${user.username} opens CloudBeaver logged in`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await routeCloudbeaver(page.context(), new URL(page.url()).origin);
      const responses = cloudbeaverResponses(page);

      const viewer = await openDwhViewer(page);
      await expect(viewer.locator('#data-warehouse-component iframe')).toHaveAttribute('src', /\/cloudbeaver\/$/);
      await expectCloudbeaverUser(responses);
      // without the portal user CloudBeaver would ask for its own login
      await expect(subsystemFrame(viewer).getByText('User password', {exact: true})).toHaveCount(0);
    });
  }

  // the DWH Viewer needs the DATA_DWH permission, which only the admin roles have
  for (const user of [DATA_DOMAIN_EDITOR, DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER]) {
    test(`${user.username} has no DWH Viewer`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectNoDataEngEntry(page, 'DWH Viewer');
      await page.goto('data-warehouse-viewer');
      await expect(page).toHaveURL(/\/forbidden$/);
    });
  }
});
