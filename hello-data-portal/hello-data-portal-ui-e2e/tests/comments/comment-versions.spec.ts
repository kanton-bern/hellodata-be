import {BUSINESS_DOMAIN_ADMIN, DATA_DOMAIN_EDITOR, expect, openPortalAs, test} from '../fixtures';
import {
  addPublishedComment,
  changeCommentPermissionAsAdmin,
  commentEntry,
  commentResponse,
  deleteCommentIfPresent,
  editComment,
  openComments,
  publish,
  sendForReview,
  showDetails,
} from '../comments';
import {withUser} from '../superset';

const REVIEWER = BUSINESS_DOMAIN_ADMIN;
const WRITER = DATA_DOMAIN_EDITOR;

// HELLODATA-2947
test.describe('Restore a comment version', () => {
  test.describe.configure({timeout: 3 * 60_000});

  test.beforeAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, WRITER, 'write');
  });

  test.afterAll(async ({browser}) => {
    await changeCommentPermissionAsAdmin(browser, WRITER, 'none');
  });

  test('a reviewer restores an earlier published version', async ({page, browser}) => {
    const token = `e2e restore ${Date.now()}`;
    await openPortalAs(page, REVIEWER.username, REVIEWER.password);
    const feed = await openComments(page);
    const entry = commentEntry(feed, token);
    try {
      await addPublishedComment(feed, `${token} first`);
      await editComment(entry, `${token} second`);
      await sendForReview(entry);
      await publish(entry);
      await expect(entry.locator('.version-badge')).toHaveText('v2');

      await test.step('a writer who is not the author sees neither details nor versions', async () => {
        await withUser(browser, WRITER, async writerPage => {
          const writerEntry = commentEntry(await openComments(writerPage), token);
          await expect(writerEntry.locator('p.comment-text')).toHaveText(`${token} second`);
          await expect(writerEntry.locator('.info-btn')).toBeDisabled();
          await expect(writerEntry.locator('.restore-btn')).toHaveCount(0);
        });
      });

      await showDetails(entry);
      const first = entry.locator('.history-item').filter({hasText: `${token} first`});
      const second = entry.locator('.history-item').filter({hasText: `${token} second`});
      await expect(second.locator('.active-badge')).toBeVisible();
      // only published versions other than the active one can be restored
      await expect(second.locator('.restore-btn')).toHaveCount(0);

      await first.locator('.restore-btn').click();
      const restored = commentResponse(page, 'POST', '/restore/1');
      const dialog = page.locator('.p-confirmdialog').filter({visible: true});
      await dialog.getByRole('button', {name: 'Restore Version', exact: true}).click();
      await restored;

      await expect(entry.locator('p.comment-text')).toHaveText(`${token} first`);
      await expect(entry.locator('.version-badge')).toHaveCount(0);
      // the restored version is the active one, the other stays in the history
      await expect(first.locator('.active-badge')).toBeVisible();
      await expect(second).toHaveCount(1);
    } finally {
      await deleteCommentIfPresent(feed, token);
    }
  });
});
