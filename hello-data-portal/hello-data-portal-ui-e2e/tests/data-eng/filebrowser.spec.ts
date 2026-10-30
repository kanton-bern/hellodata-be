import {
  ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {
  createFolder,
  DATA_DOMAIN_FOLDER,
  deleteFolder,
  expectNoDataEngEntry,
  expectRowAction,
  fileRow,
  FILEBROWSER_SYNC_TIMEOUT,
  openFilebrowser,
  openFolder,
  renameFolder,
} from '../data-eng';
import {withUser} from '../superset';

// HELLODATA-2004: the Filebrowser is SFTPGo, which shows each data domain the user has a role in as a folder.
// Data domain admins may do everything there, editors may create and rename but not delete, viewers may only read.
test.describe('Filebrowser', () => {
  const RUN_ID = Date.now().toString(36);

  for (const user of [ADMIN, DATA_DOMAIN_ADMIN]) {
    test(`${user.username} creates, renames and deletes a folder`, async ({page}) => {
      test.setTimeout(2 * FILEBROWSER_SYNC_TIMEOUT);
      const folder = `e2e-folder-${RUN_ID}-${user === ADMIN ? 'admin' : 'dd-admin'}`;
      await openPortalAs(page, user.username, user.password);
      const filebrowser = await openFilebrowser(page);
      await openFolder(filebrowser, DATA_DOMAIN_FOLDER);

      await createFolder(filebrowser, folder);
      await renameFolder(filebrowser, folder, `${folder}-renamed`);
      // the folder is stored in the data domain's bucket, so it is still there after opening the folder again
      await openFolder(filebrowser, DATA_DOMAIN_FOLDER);
      await expect(fileRow(filebrowser, `${folder}-renamed`)).toBeVisible();
      await deleteFolder(filebrowser, `${folder}-renamed`);
    });
  }

  test(`${DATA_DOMAIN_EDITOR.username} creates and renames a folder, but cannot delete it`, async ({page, browser}) => {
    test.setTimeout(3 * FILEBROWSER_SYNC_TIMEOUT);
    const folder = `e2e-folder-${RUN_ID}-editor`;
    await openPortalAs(page, DATA_DOMAIN_EDITOR.username, DATA_DOMAIN_EDITOR.password);
    const filebrowser = await openFilebrowser(page);
    await openFolder(filebrowser, DATA_DOMAIN_FOLDER);
    try {
      await createFolder(filebrowser, folder);
      await renameFolder(filebrowser, folder, `${folder}-renamed`);
      await expectRowAction(filebrowser, `${folder}-renamed`, 'delete', false);
    } finally {
      // the editor cannot clean up, so the data domain admin deletes what is left
      await withUser(browser, DATA_DOMAIN_ADMIN, async adminPage => {
        const adminFilebrowser = await openFilebrowser(adminPage);
        await openFolder(adminFilebrowser, DATA_DOMAIN_FOLDER);
        for (const name of [folder, `${folder}-renamed`]) {
          if (await fileRow(adminFilebrowser, name).count() > 0) {
            await deleteFolder(adminFilebrowser, name);
          }
        }
      });
    }
  });

  // the Filebrowser needs the DATA_FILEBROWSER permission, which viewers and business specialists do not have
  for (const user of [DATA_DOMAIN_BUSINESS_SPECIALIST, DATA_DOMAIN_VIEWER]) {
    test(`${user.username} has no Filebrowser`, async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectNoDataEngEntry(page, 'Filebrowser');
    });
  }
});
