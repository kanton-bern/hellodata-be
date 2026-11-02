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
import {expectNoDataEngEntry, openDwhViewer, routeCloudbeaver} from '../data-eng';

// HELLODATA-855
test.describe('DWH Viewer', () => {
  for (const user of [ADMIN, BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_ADMIN]) {
    test(`${user.username} opens CloudBeaver logged in`, async ({page}) => {
      // The gateway sends the HELLODATA_ADMIN role as CloudBeaver team ADMIN, but on a fresh compose database the admin
      // team is called admin (CloudBeaver lower-cases the teams of initial-data.conf), so CloudBeaver cannot save the
      // team of the user and shows its own login instead.
      test.fixme(user === ADMIN, 'CloudBeaver has no ADMIN team on a fresh compose database');
      await openPortalAs(page, user.username, user.password);
      await routeCloudbeaver(page.context(), new URL(page.url()).origin);

      const viewer = await openDwhViewer(page, user);
      const iframe = viewer.locator('#data-warehouse-component iframe');
      await expect(iframe).toHaveAttribute('src', /\/cloudbeaver\/$/);
      // the gateway logs the portal user in (reverse proxy authentication); CloudBeaver has no anonymous access, so it
      // shows its navigator only to a logged-in user, with the shared DWH connection of the data domains if there is one
      // (a fresh compose installation has none), and without the portal user it would ask for its own login
      const cloudbeaver = viewer.frameLocator('#data-warehouse-component iframe');
      await expect(cloudbeaver.getByText('Shared', {exact: true})
        .or(cloudbeaver.getByText('No Connections', {exact: false})).first()).toBeVisible({timeout: 60_000});
      await expect(cloudbeaver.getByText('User password', {exact: true})).toHaveCount(0);
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
