import {
  ADMIN,
  BUSINESS_DOMAIN_ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openMenu,
  openPortalAs,
  test,
  User,
} from '../fixtures';

const MAIN_MENUS = ['Dashboards', 'Lineage', 'Data Marts', 'Data Eng.', 'Administration', 'Monitoring', 'DevTools'];
const ADMIN_ITEMS = ['User management', 'RLS Groups', 'Portal role management', 'Notifications', 'FAQ management',
  'Documentation management'];

// what each role may see, derived from the permissions in SystemDefaultPortalRoleName
const ROLES: {role: string, user: User, menus: string[], adminItems: string[]}[] = [
  {
    role: 'HELLODATA_ADMIN',
    user: ADMIN,
    menus: MAIN_MENUS,
    adminItems: ADMIN_ITEMS,
  },
  {
    role: 'BUSINESS_DOMAIN_ADMIN',
    user: BUSINESS_DOMAIN_ADMIN,
    menus: ['Dashboards', 'Lineage', 'Data Marts', 'Data Eng.', 'Administration'],
    adminItems: ['User management', 'RLS Groups', 'Notifications', 'FAQ management', 'Documentation management'],
  },
  {
    role: 'DATA_DOMAIN_ADMIN',
    user: DATA_DOMAIN_ADMIN,
    // RLS_ROLES_MANAGEMENT: names the RLS groups of its data domains (HELLODATA-1477)
    menus: ['Dashboards', 'Lineage', 'Data Marts', 'Data Eng.', 'Administration'],
    adminItems: ['RLS Groups'],
  },
  {
    role: 'DATA_DOMAIN_EDITOR',
    user: DATA_DOMAIN_EDITOR,
    menus: ['Dashboards', 'Lineage', 'Data Marts', 'Data Eng.'],
    adminItems: [],
  },
  {
    role: 'DATA_DOMAIN_BUSINESS_SPECIALIST',
    user: DATA_DOMAIN_BUSINESS_SPECIALIST,
    menus: ['Dashboards', 'Lineage', 'Data Marts'],
    adminItems: [],
  },
  {
    role: 'DATA_DOMAIN_VIEWER',
    user: DATA_DOMAIN_VIEWER,
    menus: ['Dashboards', 'Lineage'],
    adminItems: [],
  },
];

// which menu entries and pages each portal role can see
test.describe('Roles', () => {
  for (const {role, user, menus, adminItems} of ROLES) {
    // a user of this role after logging in
    test.describe(role, () => {
      test.beforeEach(async ({page}) => {
        await openPortalAs(page, user.username, user.password);
        // wait for the home page content, so the menu is built before asserting what is missing
        await expect(page.locator('#mainContentDiv').getByText('FAQ', {exact: true})).toBeVisible();
      });

      // the user sees exactly the main menu entries of its role in the sidebar and none of the others
      test('sees only the main menu entries of its role', async ({page}) => {
        const sidebar = page.locator('#appSidebar');
        for (const menu of MAIN_MENUS) {
          await expect(sidebar.getByRole('button', {name: menu, exact: true}),
            `${menu} menu`).toHaveCount(menus.includes(menu) ? 1 : 0);
        }
      });

      if (adminItems.length > 0) {
        // the user opens the Administration menu and sees exactly the entries allowed for its role
        test('sees only the administration entries of its role', async ({page}) => {
          const drawer = await openMenu(page, 'Administration');
          for (const item of ADMIN_ITEMS) {
            await expect(drawer.getByRole('button', {name: item, exact: true}),
              `${item} entry`).toHaveCount(adminItems.includes(item) ? 1 : 0);
          }
        });
      }

      if (!adminItems.includes('User management')) {
        // the user opens the user management by url and is sent to the forbidden page without seeing the users heading
        test('is not allowed to open user management by url', async ({page}) => {
          await page.goto('user-management');
          await expect(page).toHaveURL(/\/forbidden$/);
          await expect(page.getByRole('heading', {name: 'Users management', exact: true})).toHaveCount(0);
        });
      }
    });
  }
});
