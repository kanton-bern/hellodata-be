import {Page} from '@playwright/test';
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

const CONTEXT_KEY = 'Default_Data_Domain';

const ALL = ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN', 'DATA_DOMAIN_ADMIN', 'DATA_DOMAIN_EDITOR',
  'DATA_DOMAIN_BUSINESS_SPECIALIST', 'DATA_DOMAIN_VIEWER'];

// pages no other spec opens, with the roles whose default permissions (SystemDefaultPortalRoleName) satisfy the
// requiredPermissions of their route in app-routing.module.ts; every other role is sent to the forbidden page
const PAGES: {url: string, permission: string, allowed: string[]}[] = [
  {url: 'published-announcements', permission: 'DASHBOARDS', allowed: ALL},
  {url: 'external-dashboards', permission: '-', allowed: ALL},
  {
    url: 'embedded-dm-viewer', permission: 'DATA_MARTS',
    allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN', 'DATA_DOMAIN_ADMIN', 'DATA_DOMAIN_EDITOR', 'DATA_DOMAIN_BUSINESS_SPECIALIST'],
  },
  {
    url: `advanced-analytics-viewer/${CONTEXT_KEY}`, permission: 'DATA_JUPYTER',
    allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN', 'DATA_DOMAIN_ADMIN'],
  },
  {url: 'external-dashboards/create', permission: 'EXTERNAL_DASHBOARDS_MANAGEMENT', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'dashboard-access/list', permission: 'DASHBOARD_ACCESS', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: `dashboard-groups/list/${CONTEXT_KEY}`, permission: 'DASHBOARD_GROUPS_MANAGEMENT', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'dashboard-import-export', permission: 'DASHBOARD_IMPORT_EXPORT', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'users-overview', permission: 'USERS_OVERVIEW', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'subsystem-users', permission: 'USER_MANAGEMENT', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'bulk-assignments', permission: 'USER_MANAGEMENT', allowed: ['HELLODATA_ADMIN', 'BUSINESS_DOMAIN_ADMIN']},
  {url: 'workspaces', permission: 'WORKSPACES', allowed: ['HELLODATA_ADMIN']},
  {url: `queries/list/${CONTEXT_KEY}`, permission: 'QUERIES', allowed: ['HELLODATA_ADMIN']},
];

const USERS: {role: string, user: User}[] = [
  {role: 'HELLODATA_ADMIN', user: ADMIN},
  {role: 'BUSINESS_DOMAIN_ADMIN', user: BUSINESS_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
];

/** The component the router rendered: the page itself, or app-not-allowed when the PermissionsGuard refused it. */
function routedComponent(page: Page) {
  return page.locator('#mainContentDiv > router-outlet + *');
}

test.describe('Page access by url', () => {
  for (const {role, user} of USERS) {
    test.describe(role, () => {
      // one login per role, the tests only open urls
      test.describe.configure({mode: 'serial'});
      let page: Page;

      test.beforeAll(async ({browser}) => {
        page = await browser.newPage();
        await openPortalAs(page, user.username, user.password);
      });

      test.afterAll(async () => {
        await page.close();
      });

      for (const {url, permission, allowed} of PAGES) {
        const isAllowed = allowed.includes(role);
        test(`${isAllowed ? 'opens' : 'is not allowed to open'} ${url} (${permission})`, async () => {
          await page.goto(url);
          await expect(routedComponent(page)).toHaveCount(1);
          if (isAllowed) {
            await expect(page).toHaveURL(new RegExp(`/${url}$`));
            await expect(page.locator('app-not-allowed')).toHaveCount(0);
          } else {
            await expect(page).toHaveURL(/\/forbidden$/);
            await expect(page.locator('app-not-allowed')).toHaveCount(1);
          }
        });
      }
    });
  }
});
