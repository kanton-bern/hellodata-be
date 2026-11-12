import {Page} from '@playwright/test';
import {DATA_DOMAIN_VIEWER, expect, navigate, openPortalAs, test} from '../fixtures';

const row = (page: Page, text: string) => page.getByRole('row').filter({hasText: text});
const messageEditor = (page: Page) => page.locator('.ql-editor:visible').first();
const publishedCheckbox = (page: Page) => page.locator('p-checkbox#published');
const publishedInput = (page: Page) => page.locator('input#published');
const save = (page: Page) => page.getByRole('button', {name: 'Save', exact: true});

// A published announcement opens a modal popup for every user of the portal, the admin who published it included. It
// covers the page and blocks every click, so it is closed as soon as it gets in the way of an action. The viewer of the
// second test does not use it, since it checks the popup.
function closeAnnouncementPopup(page: Page) {
  return page.addLocatorHandler(
    page.getByRole('dialog').filter({hasText: 'Do not show again'}),
    () => page.keyboard.press('Escape'));
}

async function openList(page: Page) {
  await navigate(page, 'Administration', 'Notifications');
  await expect(page).toHaveURL(/\/announcements-management$/);
}

// the page of the published announcements lists only the published ones
async function openPublishedPage(page: Page) {
  await page.goto('published-announcements');
  await expect(page).toHaveURL(/\/published-announcements$/);
}

// creates an announcement in the default language, the form opens on that tab, which is the only required one
async function createAnnouncement(page: Page, message: string, published: boolean) {
  await openList(page);
  await page.getByRole('button', {name: 'Create', exact: true}).click();
  await expect(page).toHaveURL(/\/announcements-management\/create$/);
  // the form is rebuilt once the languages are loaded, which can wipe what was typed before
  await expect(async () => {
    await messageEditor(page).fill(message);
    await expect(save(page)).toBeEnabled({timeout: 1_000});
    await expect(messageEditor(page)).toContainText(message, {timeout: 1_000});
  }).toPass();
  if (published) {
    await publishedCheckbox(page).click();
    await expect(publishedInput(page)).toBeChecked();
  }
  await save(page).click();
  await expect(page).toHaveURL(/\/announcements-management$/);
  await expect(row(page, message)).toHaveCount(1);
}

// deletes an announcement of the management list, the dialog asks for a confirmation
async function deleteAnnouncement(page: Page, message: string) {
  // the buttons of a row are details, edit and delete
  await row(page, message).getByRole('button').nth(2).click();
  await page.getByRole('dialog').getByRole('button', {name: 'Delete', exact: true}).click();
  await expect(row(page, message)).toHaveCount(0);
}

// A test that fails half way must not leave its announcements behind: a published one would open its popup for every
// user in all the specs that follow.
async function deleteLeftovers(page: Page, token: string) {
  await openList(page);
  while (await row(page, token).count() > 0) {
    await deleteAnnouncement(page, token);
  }
}

// the admin manages the notifications (announcements) of the portal
test.describe('Announcements management', () => {
  // the admin creates a published announcement, edits its text, unpublishes it and deletes it; after each step the list
  // and the page of the published announcements show the change
  test('admin creates, edits, unpublishes and deletes an announcement', async ({adminPage: page}) => {
    const id = Date.now();
    // single tokens, since the rich text editor stores spaces of replaced content as &nbsp;
    const created = `created-${id}`;
    const edited = `edited-${id}`;
    await closeAnnouncementPopup(page);

    try {
      await createAnnouncement(page, created, true);

      await openPublishedPage(page);
      await expect(page.getByText(created)).toBeVisible();

      // edit
      await openList(page);
      await row(page, created).getByRole('button').nth(1).click();
      await expect(page).toHaveURL(/\/announcements-management\/edit\//);
      // wait for the stored announcement to be loaded into the form, otherwise it overwrites our change
      await expect(messageEditor(page)).toContainText(created);
      await expect(publishedInput(page)).toBeChecked();
      await messageEditor(page).fill(edited);
      await save(page).click();
      await expect(page).toHaveURL(/\/announcements-management$/);
      await expect(row(page, edited)).toHaveCount(1);
      await expect(row(page, created)).toHaveCount(0);
      // the details of the row show the message per language
      await row(page, edited).getByRole('button').first().click();
      await expect(page.locator('.announcement-translations-table').getByText(edited)).toBeVisible();

      await openPublishedPage(page);
      await expect(page.getByText(edited)).toBeVisible();
      await expect(page.getByText(created)).toHaveCount(0);

      // unpublish - it stays in the management list, but is no longer shown to the users
      await openList(page);
      await row(page, edited).getByRole('button').nth(1).click();
      await expect(page).toHaveURL(/\/announcements-management\/edit\//);
      await expect(messageEditor(page)).toContainText(edited);
      await expect(publishedInput(page)).toBeChecked();
      await publishedCheckbox(page).click();
      await expect(publishedInput(page)).not.toBeChecked();
      await save(page).click();
      await expect(page).toHaveURL(/\/announcements-management$/);
      await expect(row(page, edited)).toHaveCount(1);

      await openPublishedPage(page);
      await expect(page.getByText(edited)).toHaveCount(0);

      // delete
      await openList(page);
      await deleteAnnouncement(page, edited);
    } finally {
      await deleteLeftovers(page, String(id));
    }
  });

  // a published announcement reaches the users: a popup opens for them in the portal, they can hide it for good, and
  // it stays on the page of the published announcements; an unpublished one is not shown to them
  test('a viewer sees the published announcement and not the unpublished one', async ({adminPage: admin, browser}) => {
    const id = Date.now();
    const visible = `visible-${id}`;
    const hidden = `hidden-${id}`;

    await closeAnnouncementPopup(admin);

    try {
      await createAnnouncement(admin, visible, true);
      await createAnnouncement(admin, hidden, false);

      const viewer = await browser.newPage();
      try {
        // the portal opens the popup on its own, the unpublished announcement is not in it
        await openPortalAs(viewer, DATA_DOMAIN_VIEWER.username, DATA_DOMAIN_VIEWER.password);
        const popup = viewer.getByRole('dialog');
        await expect(popup.getByText(visible)).toBeVisible();
        await expect(popup.getByText(hidden)).toHaveCount(0);

        // "Do not show again" hides it from the popup for this user, also after a reload
        await popup.getByText('Do not show again').click();
        await viewer.keyboard.press('Escape');
        await expect(popup).toHaveCount(0);
        await viewer.reload();
        await expect(viewer.locator('#userMenu')).toBeVisible();
        // the popup opens after a short delay, so give it the time before checking that it stays away
        await viewer.waitForTimeout(2_000);
        await expect(viewer.getByRole('dialog')).toHaveCount(0);

        // the page of the published announcements still lists it, but not the unpublished one
        await openPublishedPage(viewer);
        await expect(viewer.getByText(visible)).toBeVisible();
        await expect(viewer.getByText(hidden)).toHaveCount(0);
      } finally {
        await viewer.close();
      }
    } finally {
      await deleteLeftovers(admin, String(id));
    }
  });
});
