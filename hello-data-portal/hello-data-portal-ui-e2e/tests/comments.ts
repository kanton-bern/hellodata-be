import {Browser, Locator, Page} from '@playwright/test';
import {ADMIN, dismissToasts, expandMenuGroup, expect, openMenu, User} from './fixtures';
import {DATA_DOMAIN, expectDashboardListed, openDashboard, openUserEdit, saveUserEdit, setDashboardAccess, SHOWCASE_DASHBOARD, withUser} from './superset';

// the context key of the default data domain, used in the ids of the comment permission checkboxes
export const CONTEXT_KEY = 'Default_Data_Domain';
export const DOMAIN_COMMENTS = 'All Dashboards Comments';

/**
 * The comment permission of a user in a data domain. Each level includes the ones before it:
 * read - sees published comments, write - also writes comments, review - also publishes, declines and restores them.
 */
export type CommentPermission = 'none' | 'read' | 'write' | 'review';
const LEVELS = ['read', 'write', 'review'] as const;

/** The checkbox of a comment permission level on the user edit page. */
export function commentPermissionCheckbox(page: Page, level: typeof LEVELS[number]) {
  return page.locator(`#${level}-${CONTEXT_KEY}`);
}

/** Sets the comment permission of a user in the default data domain, in the admin's user management. */
export async function setCommentPermission(page: Page, user: User, permission: CommentPermission) {
  await openUserEdit(page, user);
  await expect(commentPermissionCheckbox(page, 'read')).toBeAttached();
  const wanted = LEVELS.slice(0, LEVELS.indexOf(permission as typeof LEVELS[number]) + 1);
  const current: typeof LEVELS[number][] = [];
  for (const level of LEVELS) {
    if (await commentPermissionCheckbox(page, level).isChecked()) {
      current.push(level);
    }
  }
  if (current.join() === wanted.join()) {
    return;
  }
  // a level is disabled while a higher one is checked, so clear them from the top
  for (const level of [...current].reverse()) {
    await commentPermissionCheckbox(page, level).click();
  }
  // checking a level also checks the ones below it
  if (permission !== 'none') {
    await commentPermissionCheckbox(page, permission).click();
  }
  for (const level of LEVELS) {
    await expect(commentPermissionCheckbox(page, level)).toBeChecked({checked: wanted.includes(level)});
  }
  await saveUserEdit(page);
}

/**
 * Logs the admin in in a separate browser context and sets the comment permission of a user. Viewers and business
 * specialists also need access to the showcase dashboard to see its comments, pass dashboardAccess for them.
 */
export async function changeCommentPermissionAsAdmin(browser: Browser, user: User, permission: CommentPermission, dashboardAccess?: boolean) {
  await withUser(browser, ADMIN, async page => {
    if (dashboardAccess !== undefined) {
      await setDashboardAccess(page, user, dashboardAccess);
    }
    await setCommentPermission(page, user, permission);
  });
}

/** The comments panel next to an embedded dashboard, only there for users with at least read permission. */
export function commentsPanel(page: Page) {
  return page.locator('app-embed-my-dashboard .comments-panel');
}

/** Opens the showcase dashboard and its comments panel, returns the comments feed. */
export async function openComments(page: Page): Promise<Locator> {
  // the dashboard is in the menu only after the user's access was synced with Superset
  await expectDashboardListed(page, true);
  await openDashboard(page);
  // the closed panel is a vertical "Comments" tab on the right of the dashboard, the hidden feed has the same title
  await commentsPanel(page).getByText('Comments', {exact: true}).filter({visible: true}).click();
  const feed = page.locator('app-comments-feed');
  await expect(feed.getByRole('heading', {name: 'Comments', exact: true})).toBeVisible();
  return feed;
}

/** The textarea for a new comment, only there for users with write permission. */
export function newCommentInput(feed: Locator) {
  return feed.locator('.comment-form textarea');
}

/** A comment of the feed, found by a text that all its versions contain. */
export function commentEntry(feed: Locator, text: string) {
  return feed.locator('app-comment-entry').filter({has: feed.page().locator('p.comment-text', {hasText: text})});
}

/** Waits for a request of the comment api of the default data domain, e.g. commentResponse(page, 'POST', '/publish'). */
export async function commentResponse(page: Page, method: string, action = '') {
  const url = new RegExp(`/dashboards/${CONTEXT_KEY}/\\d+/comments(/[^/]+)?${action}$`);
  const response = await page.waitForResponse(r => url.test(new URL(r.url()).pathname) && r.request().method() === method);
  expect(response.ok(), `${method} ${action || 'comment'} failed`).toBe(true);
  return response;
}

/** Picks an option of a PrimeNG select, its options are appended to the body. */
async function selectOption(select: Locator, option: string) {
  await select.click();
  // the accessible name of an option is its untranslated label (e.g. "@deleted"), so match the shown text
  const shown = new RegExp(`^\\s*${option.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`);
  await select.page().getByRole('option').filter({hasText: shown}).click();
}

/** Writes a new comment, optionally with tags and a relevant quarter (Q1 - Q4), and returns its entry. */
export async function addComment(feed: Locator, text: string, options: {tags?: string[], quarter?: string} = {}) {
  const page = feed.page();
  const form = feed.locator('.comment-form');
  await newCommentInput(feed).fill(text);
  if (options.tags?.length) {
    // the action buttons have no name: tags is the first one, the pointer url the second
    await form.locator('button.action-toggle').first().click();
    const dialog = page.getByRole('dialog', {name: 'Add tags'});
    const tagInput = dialog.getByRole('combobox');
    for (const tag of options.tags) {
      await tagInput.fill(tag);
      await tagInput.press('Enter');
    }
    await dialog.getByRole('button', {name: 'Done', exact: true}).click();
    await expect(dialog).toBeHidden();
  }
  if (options.quarter) {
    await selectOption(form.locator('p-select.quarter-select'), options.quarter);
  }
  const created = commentResponse(page, 'POST');
  await form.getByRole('button', {name: 'Add', exact: true}).click();
  await created;
  const entry = commentEntry(feed, text);
  await expect(entry).toBeVisible();
  return entry;
}

/** Accepts the confirmation dialog of a comment action (publish, send for review, restore). */
async function confirm(page: Page, button: string) {
  const dialog = page.locator('.p-confirmdialog').filter({visible: true});
  await dialog.getByRole('button', {name: button, exact: true}).click();
  await expect(dialog).toBeHidden();
}

/** Sends a draft for review, as its author or a reviewer. */
export async function sendForReview(entry: Locator) {
  const page = entry.page();
  await entry.locator('.send-review-btn').click();
  const sent = commentResponse(page, 'POST', '/send-for-review');
  await confirm(page, 'Send for review');
  await sent;
  await expect(entry.locator('.ready-review-badge')).toBeVisible();
}

/** Publishes a comment that is ready for review, as a reviewer. */
export async function publish(entry: Locator) {
  const page = entry.page();
  await entry.locator('.publish-btn').click();
  const published = commentResponse(page, 'POST', '/publish');
  await confirm(page, 'Publish');
  await published;
  await expect(entry.locator('.ready-review-badge')).toBeHidden();
  await dismissToasts(page);
}

/** Writes a comment, sends it for review and publishes it, as a reviewer. */
export async function addPublishedComment(feed: Locator, text: string) {
  const entry = await addComment(feed, text);
  await sendForReview(entry);
  await publish(entry);
  return entry;
}

/** Declines a comment that is ready for review, as a reviewer. */
export async function decline(entry: Locator, reason: string) {
  const page = entry.page();
  await entry.locator('.decline-btn').click();
  const dialog = page.getByRole('dialog', {name: 'Decline Draft'});
  await dialog.getByLabel('Decline Reason').fill(reason);
  const declined = commentResponse(page, 'POST', '/decline');
  await dialog.getByRole('button', {name: 'Decline', exact: true}).click();
  await declined;
  await expect(entry.locator('.declined-badge')).toBeVisible();
  await dismissToasts(page);
}

/** Edits the text of a comment. A published comment gets a new draft version, a draft is changed in place. */
export async function editComment(entry: Locator, text: string) {
  const page = entry.page();
  await entry.locator('.edit-btn').click();
  const dialog = page.getByRole('dialog', {name: 'Edit comment'});
  await dialog.getByPlaceholder('Write a comment...').fill(text);
  // a published comment is cloned into a new version (POST .../clone), a draft is updated (PUT)
  const saved = page.waitForResponse(r => r.url().includes(`/dashboards/${CONTEXT_KEY}/`)
    && (r.url().endsWith('/clone') || r.request().method() === 'PUT'));
  await dialog.getByRole('button', {name: 'Save', exact: true}).click();
  expect((await saved).ok(), 'saving the comment failed').toBe(true);
  await expect(entry.locator('p.comment-text')).toHaveText(text);
  await dismissToasts(page);
}

/** Deletes the active version of a comment, or the entire comment with all its versions. */
export async function deleteComment(entry: Locator, reason: string, entire = false) {
  const page = entry.page();
  await entry.locator('.delete-btn').click();
  const dialog = page.getByRole('dialog', {name: 'Delete Comment'});
  await dialog.getByLabel('Deletion Reason').fill(reason);
  await dialog.getByLabel('Delete entire comment (all versions)').setChecked(entire);
  const deleted = commentResponse(page, 'DELETE');
  await dialog.getByRole('button', {name: 'Delete', exact: true}).click();
  await deleted;
  await expect(dialog).toBeHidden();
  await dismissToasts(page);
}

/** Deletes a comment with all its versions if it is (still) in the feed, for the cleanup of a test. */
export async function deleteCommentIfPresent(feed: Locator, text: string) {
  const entry = commentEntry(feed, text);
  if (await entry.count() > 0 && await entry.locator('.delete-btn').isEnabled()) {
    await deleteComment(entry, 'e2e cleanup', true);
  }
}

/** Selects a value of one of the filters above the comments (Year, Quarter, Status, Tag). */
export async function filterComments(feed: Locator, filter: 'Year' | 'Quarter' | 'Status' | 'Tag', option: string) {
  const group = feed.locator('.filter-group').filter({has: feed.page().getByText(filter, {exact: true})});
  await selectOption(group.locator('p-select'), option);
}

/** Expands the details of a comment: status, dates and the version history. */
export async function showDetails(entry: Locator) {
  await entry.locator('.info-btn').click();
  await expect(entry.locator('.comment-details-wrapper')).toHaveClass(/expanded/);
}

/** The "All Dashboards Comments" entry of the data domain in the Dashboards menu, only there for reviewers. */
export async function domainCommentsMenuEntry(page: Page) {
  await expectDashboardListed(page, true);
  const drawer = await openMenu(page, 'Dashboards');
  await expandMenuGroup(drawer, DATA_DOMAIN);
  // wait for the dashboards of the group, the comments entry is added together with them
  await expect(drawer.getByRole('button', {name: SHOWCASE_DASHBOARD, exact: true})).toBeVisible();
  return drawer.getByRole('button', {name: DOMAIN_COMMENTS, exact: true});
}
