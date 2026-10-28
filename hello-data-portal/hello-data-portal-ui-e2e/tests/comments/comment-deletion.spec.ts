import {DATA_DOMAIN_EDITOR, DATA_DOMAIN_VIEWER, expect, openPortalAs, test} from '../fixtures';
import {
  addComment,
  addPublishedComment,
  changeCommentPermissionAsAdmin,
  commentEntry,
  deleteComment,
  deleteCommentIfPresent,
  editComment,
  filterComments,
  openComments,
  showDetails,
} from '../comments';
import {withUser} from '../superset';

// the HELLODATA admin reviews, the editor writes and the viewer reads
const WRITER = DATA_DOMAIN_EDITOR;
const READER = DATA_DOMAIN_VIEWER;

test.describe('Delete comments', () => {
  test.describe.configure({timeout: 3 * 60_000});

  test.beforeAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, WRITER, 'write');
    await changeCommentPermissionAsAdmin(browser, READER, 'read', true);
  });

  test.afterAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, WRITER, 'none');
    await changeCommentPermissionAsAdmin(browser, READER, 'none', false);
  });

  // HELLODATA-2932
  test('an author deletes its own draft', async ({page}) => {
    const text = `e2e draft to delete ${Date.now()}`;
    await openPortalAs(page, WRITER.username, WRITER.password);
    const feed = await openComments(page);
    const entry = commentEntry(feed, text);
    try {
      await addComment(feed, text);
      // a draft without a published version is deleted entirely
      await deleteComment(entry, 'e2e: written by mistake');
      await expect(entry).toHaveCount(0);

      // the author still finds it among the deleted comments
      await filterComments(feed, 'Status', 'Deleted');
      await expect(entry.locator('.deleted-label')).toBeVisible();
      await expect(entry.locator('.delete-btn')).toBeDisabled();
    } finally {
      await filterComments(feed, 'Status', 'All (active)');
      await deleteCommentIfPresent(feed, text);
    }
  });

  // HELLODATA-2932
  test('a reviewer deletes the active version and the last published one is shown again', async ({adminPage, browser}) => {
    const token = `e2e versioned ${Date.now()}`;
    const feed = await openComments(adminPage);
    const entry = commentEntry(feed, token);
    try {
      await addPublishedComment(feed, `${token} first`);

      await test.step('only the author and reviewers may delete a comment', async () => {
        for (const user of [WRITER, READER]) {
          await withUser(browser, user, async otherPage => {
            const otherEntry = commentEntry(await openComments(otherPage), token);
            await expect(otherEntry, user.username).toBeVisible();
            await expect(otherEntry.locator('.delete-btn'), user.username).toBeDisabled();
          });
        }
      });

      // editing a published comment creates a new draft version
      await editComment(entry, `${token} second`);
      await expect(entry.locator('.draft-badge')).toBeVisible();
      await expect(entry.locator('.version-badge')).toHaveText('v2');

      await deleteComment(entry, 'e2e: the change is not needed');
      await expect(entry.locator('p.comment-text')).toHaveText(`${token} first`);
      await expect(entry.locator('.draft-badge')).toHaveCount(0);
    } finally {
      await deleteCommentIfPresent(feed, token);
    }
  });

  // HELLODATA-2931
  test('a reviewer deletes a comment with all its versions', async ({adminPage}) => {
    const token = `e2e delete all versions ${Date.now()}`;
    const reason = 'e2e: the comment is outdated';
    const feed = await openComments(adminPage);
    const entry = commentEntry(feed, token);
    try {
      await addPublishedComment(feed, `${token} first`);
      await editComment(entry, `${token} second`);

      await deleteComment(entry, reason, true);
      // unlike deleting the active version, no published version is left
      await expect(entry).toHaveCount(0);

      await filterComments(feed, 'Status', 'Deleted');
      await expect(entry.locator('.deleted-label')).toBeVisible();
      await showDetails(entry);
      await expect(entry.locator('.comment-details')).toContainText(reason);
      const history = entry.locator('.history-item');
      await expect(history.filter({hasText: `${token} first`})).toHaveCount(1);
      await expect(history.filter({hasText: `${token} second`})).toHaveCount(1);
    } finally {
      await filterComments(feed, 'Status', 'All (active)');
      await deleteCommentIfPresent(feed, token);
    }
  });
});
