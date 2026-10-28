import {expect, navigate, test} from '../fixtures';

test.describe('FAQ management', () => {
  test('admin creates, edits and deletes a FAQ', async ({adminPage: page}) => {
    const id = Date.now();
    const title = `E2E FAQ ${id}`;
    // single tokens, since the rich text editor stores spaces of replaced content as &nbsp;
    const created = `created-${id}`;
    const edited = `edited-${id}`;
    // the title is only entered in the default language, so other languages show it with a fallback note
    const row = page.getByRole('row').filter({hasText: title});

    await navigate(page, 'Administration', 'FAQ management');
    await expect(page).toHaveURL(/\/faq-management$/);

    const titleInput = page.locator('#title:visible');
    const messageEditor = page.locator('#faqMessage:visible .ql-editor');
    const save = page.getByRole('button', {name: 'Save', exact: true});

    // create - the form opens on the default language tab, which is the only required one
    await page.locator('#createFaqButton').click();
    await expect(page).toHaveURL(/\/faq-management\/create$/);
    // the form is rebuilt once the languages are loaded, which can wipe what was typed before
    await expect(async () => {
      await titleInput.fill(title);
      await messageEditor.fill(created);
      await expect(save).toBeEnabled({timeout: 1_000});
      await expect(titleInput).toHaveValue(title, {timeout: 1_000});
    }).toPass();
    await save.click();
    await expect(page).toHaveURL(/\/faq-management$/);
    await expect(row).toHaveCount(1);

    // edit
    await row.locator('[id^="editFaq_"] button').click();
    await expect(page).toHaveURL(/\/faq-management\/edit\//);
    // wait for the stored FAQ to be loaded into the form, otherwise it overwrites our change
    await expect(messageEditor).toContainText(created);
    await messageEditor.fill(edited);
    await save.click();
    await expect(page).toHaveURL(/\/faq-management$/);
    await row.getByRole('button').first().click();
    await expect(page.getByText(edited)).toBeVisible();

    // delete
    await row.locator('[id^="deleteFaq_"] button').click();
    await page.locator('#deleteFaqButton button').click();
    await expect(row).toHaveCount(0);
  });
});
