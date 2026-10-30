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
import {changeCommentPermissionAsAdmin, CONTEXT_KEY, domainCommentsMenuEntry} from '../comments';
import {DATA_DOMAIN, withUser} from '../superset';

async function expectDomainCommentsPage(page: Page) {
  await (await domainCommentsMenuEntry(page)).click();
  await expect(page).toHaveURL(new RegExp(`/my-dashboards/comments/${CONTEXT_KEY}`));
  await expect(page.getByRole('heading', {name: `Domain Comments: ${DATA_DOMAIN}`})).toBeVisible();
  await expect(page.getByRole('columnheader', {name: 'Dashboard'})).toBeVisible();
}

// HELLODATA-4072: the domain-wide comments overview is a review tool
test.describe('All Dashboards Comments menu', () => {
  // a user's dashboards show up only after the sync with Superset
  test.describe.configure({timeout: 3 * 60_000});

  for (const {role, user} of [
    {role: 'HELLODATA_ADMIN', user: ADMIN},
    {role: 'BUSINESS_DOMAIN_ADMIN', user: BUSINESS_DOMAIN_ADMIN},
    {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  ]) {
    test(`is available to ${role}`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDomainCommentsPage(page);
    });
  }

  for (const {role, user, dashboardAccess} of [
    {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
    {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER, dashboardAccess: true},
    {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST, dashboardAccess: true},
  ] as {role: string, user: User, dashboardAccess?: boolean}[]) {
    test.describe(role, () => {
      test.describe.configure({timeout: 3 * 60_000});

      test.afterAll(async ({browser}) => {
        await changeCommentPermissionAsAdmin(browser, user, 'none', dashboardAccess ? false : undefined);
      });

      test('gets the menu only with the review permission', async ({browser}) => {
        await test.step('write: no menu', async () => {
          await changeCommentPermissionAsAdmin(browser, user, 'write', dashboardAccess);
          await withUser(browser, user, async page => {
            await expect(await domainCommentsMenuEntry(page)).toHaveCount(0);
          });
        });

        await test.step('review: menu and overview page', async () => {
          await changeCommentPermissionAsAdmin(browser, user, 'review');
          await withUser(browser, user, expectDomainCommentsPage);
        });
      });
    });
  }
});
