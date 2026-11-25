import {expect, test} from '../fixtures';
import {SHOWCASE_DASHBOARD, SUPERSET_SYNC_TIMEOUT} from '../superset';
import {bulkAssign, expectResult} from '../bulk-assignments';
import {
  BATCH_TIMEOUT,
  batchEmail,
  deleteBatchUsers,
  ensureSupersetRoles,
  expectSupersetRoles,
  importCsv,
  portalCommentPermission,
  portalDashboards,
  portalRoles,
  RLS_A,
  showcaseDashboardRole,
  toCsv,
} from '../batch-import';

// An admin gives the same role in a data domain to several existing users at once in the bulk assignments wizard. The
// users are created by a batch import first, which only imports users with a role, so the editors start as viewers.
test.describe('Bulk assignments', () => {
  // the last test takes back the role the first one gave
  test.describe.configure({mode: 'serial'});
  const editorA = batchEmail('bulk-editor-a');
  const editorB = batchEmail('bulk-editor-b');
  const viewer = batchEmail('bulk-viewer');
  let dashboardRole: string;

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + 2 * BATCH_TIMEOUT);
    dashboardRole = showcaseDashboardRole();
    await ensureSupersetRoles([RLS_A]);
    await importCsv(toCsv([
      {email: editorA, dataDomainRole: 'DATA_DOMAIN_VIEWER'},
      {email: editorB, dataDomainRole: 'DATA_DOMAIN_VIEWER'},
      {email: viewer, dataDomainRole: 'DATA_DOMAIN_EDITOR'},
    ]));
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [editorA, editorB, viewer]);
  });

  // two viewers become data domain editors in one run; the portal stores the role and Superset has the
  // BI_EDITOR and sql_lab roles for both
  test('gives two users the data domain editor role', async ({adminPage: page}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    expect(portalRoles(editorA).dataDomainRole).toBe('DATA_DOMAIN_VIEWER');

    await bulkAssign(page, [editorA, editorB], {role: 'DATA_DOMAIN_EDITOR'});
    await expectResult(page, {updated: 2, skipped: 0, failed: 0});

    for (const email of [editorA, editorB]) {
      expect(portalRoles(email).dataDomainRole).toBe('DATA_DOMAIN_EDITOR');
      await expectSupersetRoles(email, ['BI_EDITOR', 'sql_lab']);
    }
  });

  // a viewer gets a dashboard, an RLS group and the read permission for comments; the portal stores the dashboard and
  // the permission and Superset has the BI_VIEWER, dashboard and RLS roles
  test('gives a user the viewer role with a dashboard, an RLS group and comment permission', async ({adminPage: page}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);

    await bulkAssign(page, [viewer], {
      role: 'DATA_DOMAIN_VIEWER', comments: ['read'], dashboards: [SHOWCASE_DASHBOARD], rlsRoles: [RLS_A],
    });
    await expectResult(page, {updated: 1, skipped: 0, failed: 0});

    expect(portalRoles(viewer).dataDomainRole).toBe('DATA_DOMAIN_VIEWER');
    expect(portalDashboards(viewer)).toEqual([SHOWCASE_DASHBOARD]);
    expect(portalCommentPermission(viewer)).toBe('read');
    await expectSupersetRoles(viewer, ['BI_VIEWER', dashboardRole, RLS_A]);
  });

  // the role NONE takes the access to the data domain away again
  test('takes the role away with NONE', async ({adminPage: page}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);

    await bulkAssign(page, [editorA, editorB], {role: 'NONE'});
    await expectResult(page, {updated: 2, skipped: 0, failed: 0});

    for (const email of [editorA, editorB]) {
      expect(portalRoles(email).dataDomainRole).toBe('NONE');
    }
  });
});
