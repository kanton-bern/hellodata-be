import {BATCH_TIMEOUT, deleteBatchUsers, expectSupersetRoles, portalRoles, portalUser} from '../batch-import';
import {dismissToasts, expect, navigate, selectOption, test} from '../fixtures';
import {saveUserEdit} from '../superset';

const CONTEXT_KEY = 'Default_Data_Domain';

// An admin invites a user, gives it a role in a data domain and deletes it again, all in the user management. The
// compose installation looks users up locally (LocalUserLookupProvider), so any email can be invited.
test.describe('User lifecycle', () => {
  test.describe.configure({mode: 'serial'});
  const EMAIL = `e2e-invite.${Date.now().toString(36)}@hellodata.ch`;

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    // a test that failed half way leaves the user behind
    await deleteBatchUsers(browser, [EMAIL]);
  });

  test('admin invites a new user', async ({adminPage: page}) => {
    expect(portalUser(EMAIL)).toBeUndefined();
    await navigate(page, 'Administration', 'User management');
    await page.locator('#user input').fill(EMAIL);
    // the lookup suggests the user with a first and last name taken from the email
    await page.getByRole('option').filter({hasText: EMAIL}).click();
    await expect(page.locator('#firstName')).not.toHaveValue('');
    await page.getByRole('button', {name: 'Set permissions', exact: true}).click();

    // the portal opens the edit page of the new user, with no role yet
    await expect(page).toHaveURL(/\/user-management\/edit\//);
    await expect(page.locator('#email')).toHaveValue(EMAIL);
    expect(portalUser(EMAIL)).toEqual({enabled: true, superuser: false});
  });

  test('admin gives the user a role in the data domain', async ({adminPage: page}) => {
    await navigate(page, 'Administration', 'User management');
    await page.getByPlaceholder('Search').fill(EMAIL);
    const userRow = page.getByRole('row').filter({has: page.getByRole('cell', {name: EMAIL, exact: true})});
    // the actions are icon buttons without a name: edit is the first one
    await userRow.locator('button').first().click();
    await expect(page).toHaveURL(/\/user-management\/edit\//);

    await selectOption(page.locator(`p-select#dataDomain-${CONTEXT_KEY}`), 'DATA DOMAIN EDITOR');
    await saveUserEdit(page);
    expect(portalRoles(EMAIL).dataDomainRole).toBe('DATA_DOMAIN_EDITOR');
    // the portal sends the role to Superset, where a data domain editor is a BI_EDITOR with SQL Lab
    await expectSupersetRoles(EMAIL, ['BI_EDITOR', 'sql_lab']);
  });

  test('admin deletes the user', async ({adminPage: page}) => {
    await navigate(page, 'Administration', 'User management');
    await page.getByPlaceholder('Search').fill(EMAIL);
    const userRow = page.getByRole('row').filter({has: page.getByRole('cell', {name: EMAIL, exact: true})});
    await expect(userRow).toHaveCount(1);
    await userRow.locator('button.p-button-danger').click();
    await page.locator('.p-confirmdialog').getByRole('button', {name: 'Delete', exact: true}).click();
    await expect(userRow).toHaveCount(0);
    await dismissToasts(page);
    expect(portalUser(EMAIL)).toBeUndefined();
  });
});
