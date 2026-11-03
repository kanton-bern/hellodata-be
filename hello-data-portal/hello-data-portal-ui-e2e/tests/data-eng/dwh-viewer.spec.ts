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
  User,
} from '../fixtures';
import {
  cloudbeaverConnection,
  DM_CONNECTION,
  DWH_CONNECTION,
  expandConnectionsFolder,
  expectNoDataEngEntry,
  openDataMartsViewer,
  openDwhViewer,
  routeCloudbeaver,
} from '../data-eng';

// What each role gets in CloudBeaver: the DWH Viewer needs the portal permission DATA_DWH, the DataMarts Viewer
// DATA_MARTS (both embed the same CloudBeaver). Inside CloudBeaver the sidecar gives HELLODATA_ADMIN the admin team,
// DATA_DOMAIN_ADMIN the DWH and DataMarts teams of the data domain, and editors and business specialists only the
// DataMarts team. Business domain admins are data domain admins of every data domain.
const ROLES: {role: string, user: User, dwhViewer: boolean, connections: string[], cloudbeaverAdmin: boolean}[] = [
  {role: 'HELLODATA_ADMIN', user: ADMIN, dwhViewer: true, connections: [DM_CONNECTION, DWH_CONNECTION], cloudbeaverAdmin: true},
  {role: 'BUSINESS_DOMAIN_ADMIN', user: BUSINESS_DOMAIN_ADMIN, dwhViewer: true, connections: [DM_CONNECTION, DWH_CONNECTION], cloudbeaverAdmin: false},
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN, dwhViewer: true, connections: [DM_CONNECTION, DWH_CONNECTION], cloudbeaverAdmin: false},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR, dwhViewer: false, connections: [DM_CONNECTION], cloudbeaverAdmin: false},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST, dwhViewer: false, connections: [DM_CONNECTION], cloudbeaverAdmin: false},
];

const ALL_CONNECTIONS = [DM_CONNECTION, DWH_CONNECTION];

test.describe('DWH Viewer', () => {
  for (const {role, user, dwhViewer, connections, cloudbeaverAdmin} of ROLES) {
    test.describe(role, () => {
      test.beforeEach(async ({page}) => {
        await openPortalAs(page, user.username, user.password);
        await routeCloudbeaver(page.context(), new URL(page.url()).origin);
      });

      if (dwhViewer) {
        // HELLODATA-855
        test('opens CloudBeaver logged in from the DWH Viewer', async ({page}) => {
          const viewer = await openDwhViewer(page, user);
          const iframe = viewer.locator('#data-warehouse-component iframe');
          await expect(iframe).toHaveAttribute('src', /\/cloudbeaver\/$/);
          // the gateway logs the portal user in (reverse proxy authentication); CloudBeaver has no anonymous access, so
          // it shows its navigator only to a logged-in user, and without the portal user it would ask for its own login
          const cloudbeaver = viewer.frameLocator('#data-warehouse-component iframe');
          await expandConnectionsFolder(cloudbeaver);
          await expect(cloudbeaver.getByText('User password', {exact: true})).toHaveCount(0);
        });
      } else {
        test('has no DWH Viewer', async ({page}) => {
          await expectNoDataEngEntry(page, 'DWH Viewer');
          await page.goto('data-warehouse-viewer');
          await expect(page).toHaveURL(/\/forbidden$/);
        });
      }

      test('sees only the connections of its role in the DataMarts Viewer', async ({page}) => {
        const cloudbeaver = await openDataMartsViewer(page);
        await expandConnectionsFolder(cloudbeaver);
        for (const connection of ALL_CONNECTIONS) {
          await expect(cloudbeaverConnection(cloudbeaver, connection), connection)
            .toHaveCount(connections.includes(connection) ? 1 : 0);
        }
      });

      test(`${cloudbeaverAdmin ? 'can' : 'cannot'} create connections in CloudBeaver`, async ({page}) => {
        const cloudbeaver = await openDataMartsViewer(page);
        await expandConnectionsFolder(cloudbeaver);
        // the admin team of CloudBeaver has the admin permission, which adds the connection management to the navigator
        await expect(cloudbeaver.locator('[title="New Connection"]')).toHaveCount(cloudbeaverAdmin ? 1 : 0);
      });
    });
  }

  // the viewer has neither DATA_DWH nor DATA_MARTS (page-access.spec.ts checks the urls of both)
  test.describe('DATA_DOMAIN_VIEWER', () => {
    test('has no DWH Viewer', async ({page}) => {
      await openPortalAs(page, DATA_DOMAIN_VIEWER.username, DATA_DOMAIN_VIEWER.password);
      await expectNoDataEngEntry(page, 'DWH Viewer');
      await page.goto('data-warehouse-viewer');
      await expect(page).toHaveURL(/\/forbidden$/);
    });
  });
});
