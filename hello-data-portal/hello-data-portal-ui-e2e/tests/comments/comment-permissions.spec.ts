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
  changeCommentPermissionAsAdmin,
  commentPermissionCheckbox,
  commentsPanel,
  newCommentInput,
  openComments,
  setCommentPermission,
} from '../comments';
import {openDashboard, openUserEdit, withUser} from '../superset';

// the admins get read, write and review in their data domains by default (DashboardCommentPermissionService)
const FULL_ACCESS_BY_DEFAULT: {role: string, user: User}[] = [
  {role: 'HELLODATA_ADMIN', user: ADMIN},
  {role: 'BUSINESS_DOMAIN_ADMIN', user: BUSINESS_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
];

// every other role gets no comment permission until an admin grants it; viewers and business specialists also need
// access to the dashboard
const NO_ACCESS_BY_DEFAULT: {role: string, user: User, dashboardAccess?: boolean}[] = [
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER, dashboardAccess: true},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST, dashboardAccess: true},
];

// HELLODATA-4070
test.describe('Commenting permissions', () => {
  for (const {role, user} of FULL_ACCESS_BY_DEFAULT) {
    test(`${role} can read and write comments`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      const feed = await openComments(page);
      await expect(newCommentInput(feed)).toBeVisible();
      // reviewers can export and import the comments of a dashboard
      await expect(feed.locator('.export-btn')).toBeVisible();
    });
  }

  for (const {role, user, dashboardAccess} of NO_ACCESS_BY_DEFAULT) {
    test.describe(role, () => {
      test.beforeAll(async ({browser}) => {
        await changeCommentPermissionAsAdmin(browser, user, 'none', dashboardAccess);
      });

      test.afterAll(async ({browser}) => {
        await changeCommentPermissionAsAdmin(browser, user, 'none', dashboardAccess ? false : undefined);
      });

      test('sees no comments without a comment permission', async ({page}) => {
        await openPortalAs(page, user.username, user.password);
        await openDashboard(page);
        await expect(commentsPanel(page)).toHaveCount(0);
      });
    });
  }
});

// HELLODATA-4071
test.describe('Edit commenting permissions', () => {
  test('review includes write and read', async ({adminPage}) => {
    await openUserEdit(adminPage, DATA_DOMAIN_EDITOR);
    const [read, write, review] = (['read', 'write', 'review'] as const).map(level => commentPermissionCheckbox(adminPage, level));
    await expect(review).toBeEnabled();
    await review.click();
    await expect(review).toBeChecked();
    await expect(write).toBeChecked();
    await expect(read).toBeChecked();
    // the lower levels can only be taken away after review
    await expect(write).toBeDisabled();
    await expect(read).toBeDisabled();
    // nothing is saved, the editor keeps its permission
  });

  test('the comment permissions of a business domain admin cannot be changed', async ({adminPage}) => {
    await openUserEdit(adminPage, BUSINESS_DOMAIN_ADMIN);
    for (const level of ['read', 'write', 'review'] as const) {
      const checkbox = commentPermissionCheckbox(adminPage, level);
      await expect(checkbox, level).toBeChecked();
      await expect(checkbox, level).toBeDisabled();
    }
  });

  for (const {role, user, dashboardAccess} of NO_ACCESS_BY_DEFAULT) {
    test.describe(role, () => {
      // every step changes the permission as admin and logs the user in again
      test.describe.configure({timeout: 4 * 60_000});

      test.beforeAll(async ({browser}) => {
        if (dashboardAccess) {
          await changeCommentPermissionAsAdmin(browser, user, 'none', true);
        }
      });

      test.afterAll(async ({browser}) => {
        await changeCommentPermissionAsAdmin(browser, user, 'none', dashboardAccess ? false : undefined);
      });

      test('gets the comment access an admin grants', async ({browser}) => {
        await test.step('read: sees the comments, cannot write', async () => {
          await changeCommentPermissionAsAdmin(browser, user, 'read');
          await withUser(browser, user, async page => {
            const feed = await openComments(page);
            await expect(newCommentInput(feed)).toHaveCount(0);
          });
        });

        await test.step('write: can write comments, cannot export them', async () => {
          await changeCommentPermissionAsAdmin(browser, user, 'write');
          await withUser(browser, user, async page => {
            const feed = await openComments(page);
            await expect(newCommentInput(feed)).toBeVisible();
            await expect(feed.locator('.export-btn')).toHaveCount(0);
          });
        });

        await test.step('review: can also export the comments', async () => {
          await changeCommentPermissionAsAdmin(browser, user, 'review');
          await withUser(browser, user, async page => {
            const feed = await openComments(page);
            await expect(newCommentInput(feed)).toBeVisible();
            await expect(feed.locator('.export-btn')).toBeVisible();
          });
        });

        await test.step('none: the comments panel is gone again', async () => {
          await withUser(browser, ADMIN, page => setCommentPermission(page, user, 'none'));
          await withUser(browser, user, async page => {
            await openDashboard(page);
            await expect(commentsPanel(page)).toHaveCount(0);
          });
        });
      });
    });
  }
});
