import {expect, test} from '../fixtures';
import {
  addComment,
  commentEntry,
  deleteCommentIfPresent,
  filterComments,
  openComments,
  publish,
  sendForReview,
} from '../comments';

// HELLODATA-2945
test.describe('Comments filtering', () => {
  // a user's dashboards show up only after the sync with Superset
  test.describe.configure({timeout: 3 * 60_000});

  test('filters the comments by status, quarter, tag and year', async ({adminPage}) => {
    const id = Date.now();
    // tags are lower case and at most 10 characters long
    const tag = `e2e${id % 1_000_000}`;
    const draftText = `e2e draft comment ${id}`;
    const publishedText = `e2e published comment ${id}`;
    const feed = await openComments(adminPage);
    const draft = commentEntry(feed, draftText);
    const published = commentEntry(feed, publishedText);
    try {
      // the relevant quarter is set by hand here, without it the quarter of the creation date is used
      await addComment(feed, draftText, {tags: [tag], quarter: 'Q1'});
      const entry = await addComment(feed, publishedText, {quarter: 'Q2'});
      await sendForReview(entry);
      await publish(entry);

      await test.step('status', async () => {
        await filterComments(feed, 'Status', 'Draft');
        await expect(draft).toBeVisible();
        await expect(published).toHaveCount(0);
        await filterComments(feed, 'Status', 'Published');
        await expect(published).toBeVisible();
        await expect(draft).toHaveCount(0);
        await filterComments(feed, 'Status', 'All (active)');
        await expect(draft).toBeVisible();
        await expect(published).toBeVisible();
      });

      await test.step('quarter', async () => {
        await filterComments(feed, 'Quarter', 'Q1');
        await expect(draft).toBeVisible();
        await expect(published).toHaveCount(0);
        await filterComments(feed, 'Quarter', 'Q2');
        await expect(published).toBeVisible();
        await expect(draft).toHaveCount(0);
        await filterComments(feed, 'Quarter', 'All');
      });

      await test.step('tag', async () => {
        await filterComments(feed, 'Tag', tag);
        await expect(draft).toBeVisible();
        await expect(draft.locator('app-tag-chip')).toContainText(tag);
        await expect(published).toHaveCount(0);
        await filterComments(feed, 'Tag', 'All');
      });

      await test.step('year', async () => {
        await filterComments(feed, 'Year', String(new Date().getFullYear()));
        await expect(draft).toBeVisible();
        await expect(published).toBeVisible();
        await filterComments(feed, 'Year', 'All');
      });
    } finally {
      await deleteCommentIfPresent(feed, draftText);
      await deleteCommentIfPresent(feed, publishedText);
    }
  });
});
