import {Browser} from '@playwright/test';
import {DATA_DOMAIN_ADMIN, DATA_DOMAIN_EDITOR, DATA_DOMAIN_VIEWER, expect, openPortalAs, test} from '../fixtures';
import {
  addComment,
  changeCommentPermissionAsAdmin,
  commentEntry,
  decline,
  deleteCommentIfPresent,
  openComments,
  publish,
  sendForReview,
} from '../comments';
import {withUser} from '../superset';

// the editor writes comments, the viewer only reads them and the data domain admin reviews them
const AUTHOR = DATA_DOMAIN_EDITOR;
const READER = DATA_DOMAIN_VIEWER;
const REVIEWER = DATA_DOMAIN_ADMIN;

test.describe('Comment review', () => {
  // the tests switch between the author, the reviewer and the reader, each in its own browser context
  test.describe.configure({timeout: 4 * 60_000});

  test.beforeAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, AUTHOR, 'write');
    await changeCommentPermissionAsAdmin(browser, READER, 'read', true);
  });

  test.afterAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, AUTHOR, 'none');
    await changeCommentPermissionAsAdmin(browser, READER, 'none', false);
  });

  /** Writes a comment as the author and sends it for review. */
  async function submitForReview(browser: Browser, text: string) {
    await withUser(browser, AUTHOR, async page => sendForReview(await addComment(await openComments(page), text)));
  }

  /** The author can always delete its own comment, whatever state a failed test left it in. */
  async function cleanUp(browser: Browser, text: string) {
    await withUser(browser, AUTHOR, async page => deleteCommentIfPresent(await openComments(page), text));
  }

  // HELLODATA-2933
  test('an author sends a draft for review', async ({page, browser}) => {
    const text = `e2e draft for review ${Date.now()}`;
    await openPortalAs(page, AUTHOR.username, AUTHOR.password);
    const feed = await openComments(page);
    try {
      const entry = await addComment(feed, text);
      await expect(entry.locator('.draft-badge')).toBeVisible();
      // a writer cannot publish, not even its own comment
      await expect(entry.locator('.publish-btn')).toBeDisabled();

      await test.step('a draft is only visible to its author', async () => {
        for (const user of [READER, REVIEWER]) {
          await withUser(browser, user, async otherPage => {
            await expect(commentEntry(await openComments(otherPage), text), user.username).toHaveCount(0);
          });
        }
      });

      await sendForReview(entry);
      // a comment waiting for review is locked for its author
      await expect(entry.locator('.edit-btn')).toBeDisabled();
      await expect(entry.locator('.send-review-btn')).toBeDisabled();

      await test.step('the reviewer sees the comment and can publish or decline it', async () => {
        await withUser(browser, REVIEWER, async reviewerPage => {
          const reviewerEntry = commentEntry(await openComments(reviewerPage), text);
          await expect(reviewerEntry.locator('.ready-review-badge')).toBeVisible();
          await expect(reviewerEntry.locator('.publish-btn')).toBeEnabled();
          await expect(reviewerEntry.locator('.decline-btn')).toBeEnabled();
        });
      });

      await test.step('the reader still does not see it', async () => {
        await withUser(browser, READER, async readerPage => {
          await expect(commentEntry(await openComments(readerPage), text)).toHaveCount(0);
        });
      });
    } finally {
      await deleteCommentIfPresent(feed, text);
    }
  });

  // HELLODATA-2946
  test('a reviewer publishes a comment', async ({page, browser}) => {
    const text = `e2e comment to publish ${Date.now()}`;
    try {
      await submitForReview(browser, text);
      await openPortalAs(page, REVIEWER.username, REVIEWER.password);
      const entry = commentEntry(await openComments(page), text);
      await publish(entry);
      await expect(entry.locator('.draft-badge, .ready-review-badge, .declined-badge')).toHaveCount(0);

      await test.step('the reader sees the published comment', async () => {
        await withUser(browser, READER, async readerPage => {
          const readerEntry = commentEntry(await openComments(readerPage), text);
          await expect(readerEntry).toBeVisible();
          // read only: all actions are disabled
          for (const action of ['.edit-btn', '.publish-btn', '.delete-btn']) {
            await expect(readerEntry.locator(action), action).toBeDisabled();
          }
        });
      });
    } finally {
      await cleanUp(browser, text);
    }
  });

  // HELLODATA-2946
  test('a reviewer declines a comment with a reason', async ({page, browser}) => {
    const text = `e2e comment to decline ${Date.now()}`;
    const reason = 'e2e: please add the source of the numbers';
    try {
      await submitForReview(browser, text);
      await openPortalAs(page, REVIEWER.username, REVIEWER.password);
      const entry = commentEntry(await openComments(page), text);
      await expect(entry.locator('.decline-btn')).toBeEnabled();
      await decline(entry, reason);

      await test.step('the author sees the decline reason and can edit the comment again', async () => {
        await withUser(browser, AUTHOR, async authorPage => {
          const authorEntry = commentEntry(await openComments(authorPage), text);
          await expect(authorEntry.locator('.decline-reason-banner')).toContainText(reason);
          await expect(authorEntry.locator('.edit-btn')).toBeEnabled();
        });
      });

      await test.step('the reader does not see the declined comment', async () => {
        await withUser(browser, READER, async readerPage => {
          await expect(commentEntry(await openComments(readerPage), text)).toHaveCount(0);
        });
      });
    } finally {
      await cleanUp(browser, text);
    }
  });
});
