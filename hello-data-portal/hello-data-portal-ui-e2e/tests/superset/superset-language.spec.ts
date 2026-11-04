import {DATA_DOMAIN_EDITOR, DATA_DOMAIN_VIEWER, expect, openPortalAs, selectLanguage, test, User} from '../fixtures';
import {changeDashboardAccessAsAdmin, expectDashboardListed, openDashboard, SHOWCASE_DASHBOARD} from '../superset';

// HELLODATA-4109: the portal opens the embedded dashboard with its language (_l_), and Superset shows its UI in that
// language. Since Superset 5 the UI loads its translations from /superset/language_pack/<lang>/, which needs
// can_language_pack on Superset, so the BI roles must have it too (not only Admin, Alpha and Gamma).
const ROLES: {role: string, user: User, dashboardAccess?: boolean}[] = [
  {role: 'DATA_DOMAIN_EDITOR (BI_EDITOR)', user: DATA_DOMAIN_EDITOR},
  {role: 'DATA_DOMAIN_VIEWER (BI_VIEWER)', user: DATA_DOMAIN_VIEWER, dashboardAccess: true},
];

// the embedded Superset dashboard is shown in the language chosen in the portal, for the BI roles
test.describe('Superset language', () => {
  for (const {role, user, dashboardAccess} of ROLES) {
    // the dashboard language checked with a user of this role
    test.describe(role, () => {
      test.beforeAll(async ({browser}) => {
        if (dashboardAccess) {
          await changeDashboardAccessAsAdmin(browser, user, true);
        }
      });

      test.afterAll(async ({browser}) => {
        if (dashboardAccess) {
          await changeDashboardAccessAsAdmin(browser, user, false);
        }
      });

      // the user opens the dashboard and switches the portal to German; after reloading, Superset shows its
      // German texts (Filter anwenden, not Apply filters) and all language pack requests succeed
      test('sees the dashboard in the language of the portal', async ({page}) => {
        test.setTimeout(3 * 60_000);
        await openPortalAs(page, user.username, user.password);
        // the access a viewer gets is synced to Superset first
        await expectDashboardListed(page, true);
        // the menu entries change with the language, so open the dashboard in English and reload it in German
        await openDashboard(page);
        const dashboardUrl = page.url();
        await selectLanguage(page, 'DE');
        try {
          const languagePacks: number[] = [];
          page.on('response', response => {
            if (response.url().includes('/language_pack/')) {
              languagePacks.push(response.status());
            }
          });
          await page.goto(dashboardUrl);
          const dashboard = page.frameLocator('app-subsystem-iframe iframe');
          await expect(dashboard.getByText(SHOWCASE_DASHBOARD, {exact: true}).first()).toBeVisible({timeout: 60_000});
          // the filter bar is part of every dashboard with native filters, for viewers and editors
          await expect(dashboard.getByRole('button', {name: 'Filter anwenden'})).toBeVisible();
          await expect(dashboard.getByRole('button', {name: 'Apply filters'})).toHaveCount(0);
          // Superset 5 fetches the translations; a 403 there means the role lacks can_language_pack
          expect(languagePacks.filter(status => status !== 200), 'language pack responses').toEqual([]);
        } finally {
          await selectLanguage(page, 'EN');
        }
      });
    });
  }
});
