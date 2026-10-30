import {expect, navigate, test} from '../fixtures';
import {SHOWCASE_DASHBOARD, SUPERSET_SYNC_TIMEOUT} from '../superset';
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
  portalUser,
  RLS_A,
  RLS_B,
  showcaseDashboardRole,
  toCsv,
} from '../batch-import';

// HELLODATA-2073 (roles in the subsystems) and HELLODATA-2074 (roles in HelloDATA): users of the csv file that do not
// exist yet are added to the portal and to Superset, with the roles of the file
test.describe('Batch import - new users', () => {
  const viewer = batchEmail('new-viewer');
  const specialist = batchEmail('new-specialist');
  const editor = batchEmail('new-editor');
  const dataDomainAdmin = batchEmail('new-dd-admin');
  const businessDomainAdmin = batchEmail('new-bd-admin');
  const hellodataAdmin = batchEmail('new-hd-admin');
  let dashboardRole: string;

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + 30_000);
    dashboardRole = showcaseDashboardRole();
    await ensureSupersetRoles([RLS_A, RLS_B]);
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [viewer, specialist, editor, dataDomainAdmin, businessDomainAdmin, hellodataAdmin]);
  });

  test('adds a viewer with its dashboard and RLS roles', async ({adminPage: page}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    expect(portalUser(viewer)).toBeUndefined();

    // saved by Excel: with a byte order mark in front of the header
    await importCsv(toCsv([{email: viewer, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole, RLS_A]}], {bom: true}));

    // HelloDATA: the user exists, has the roles of the file and the dashboard of its D_ role
    expect(portalUser(viewer)).toEqual({enabled: true, superuser: false});
    expect(portalRoles(viewer)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_VIEWER'});
    expect(portalDashboards(viewer)).toEqual([SHOWCASE_DASHBOARD]);
    expect(portalCommentPermission(viewer)).toBe('');
    // Superset: BI_VIEWER comes from the data domain role, the dashboard and RLS roles from the supersetRole column
    await expectSupersetRoles(viewer, ['BI_VIEWER', dashboardRole, RLS_A]);

    await navigate(page, 'Administration', 'User management');
    await page.getByPlaceholder('Search').fill(viewer);
    await expect(page.getByRole('cell', {name: viewer, exact: true})).toBeVisible();
  });

  test('maps the data domain role of each user to the BI roles of Superset', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([
      {email: specialist, dataDomainRole: 'DATA_DOMAIN_BUSINESS_SPECIALIST', supersetRoles: [RLS_B]},
      // BI_* roles are never written in the file, and the supersetRole column only counts for viewers and specialists
      {email: editor, dataDomainRole: 'DATA_DOMAIN_EDITOR', supersetRoles: [RLS_A]},
      {email: dataDomainAdmin, dataDomainRole: 'DATA_DOMAIN_ADMIN', supersetRoles: [RLS_A]},
    ]));

    expect(portalRoles(specialist)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_BUSINESS_SPECIALIST'});
    expect(portalRoles(editor)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_EDITOR'});
    expect(portalRoles(dataDomainAdmin)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_ADMIN'});
    // only data domain admins can comment by default
    expect(portalCommentPermission(editor)).toBe('');
    expect(portalCommentPermission(dataDomainAdmin)).toBe('read,write,review');

    await expectSupersetRoles(specialist, ['BI_VIEWER', RLS_B]);
    await expectSupersetRoles(editor, ['BI_EDITOR', 'sql_lab']);
    await expectSupersetRoles(dataDomainAdmin, ['BI_ADMIN', 'BI_EDITOR', 'sql_lab']);
  });

  test('makes business domain admins data domain admins everywhere', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([
      // the data domain role of the file does not matter for a business domain role other than NONE
      {email: businessDomainAdmin, businessDomainRole: 'BUSINESS_DOMAIN_ADMIN', dataDomainRole: 'DATA_DOMAIN_VIEWER'},
      {email: hellodataAdmin, businessDomainRole: 'HELLODATA_ADMIN', dataDomainRole: 'DATA_DOMAIN_ADMIN'},
    ]));

    expect(portalUser(businessDomainAdmin)?.superuser).toBe(false);
    expect(portalRoles(businessDomainAdmin)).toEqual({businessDomainRole: 'BUSINESS_DOMAIN_ADMIN', dataDomainRole: 'DATA_DOMAIN_ADMIN'});
    expect(portalCommentPermission(businessDomainAdmin)).toBe('read,write,review');
    expect(portalUser(hellodataAdmin)?.superuser).toBe(true);
    expect(portalRoles(hellodataAdmin)).toEqual({businessDomainRole: 'HELLODATA_ADMIN', dataDomainRole: 'DATA_DOMAIN_ADMIN'});

    await expectSupersetRoles(businessDomainAdmin, ['BI_ADMIN', 'BI_EDITOR', 'sql_lab']);
    // the superuser is also Admin of Superset
    await expectSupersetRoles(hellodataAdmin, ['Admin', 'BI_ADMIN', 'BI_EDITOR', 'sql_lab']);
  });
});
