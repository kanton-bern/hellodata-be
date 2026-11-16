import {expect, test} from '../fixtures';
import {SHOWCASE_DASHBOARD, SUPERSET_SYNC_TIMEOUT} from '../superset';
import {
  addSupersetRole,
  BATCH_TIMEOUT,
  batchEmail,
  deleteBatchUsers,
  ensureSupersetRoles,
  expectSupersetRoles,
  importCsv,
  portalDashboards,
  portalRoles,
  portalUser,
  RLS_A,
  RLS_B,
  showcaseDashboardRole,
  toCsv,
} from '../batch-import';

// HELLODATA-2073 / HELLODATA-2074: users that already exist are changed to match the csv file. Roles that are not in
// the file are removed, in the portal and in Superset. Each test continues with the user the previous one left.
test.describe('Batch import - existing users', () => {
  test.describe.configure({mode: 'serial'});

  const user = batchEmail('existing');
  let dashboardRole: string;

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + BATCH_TIMEOUT);
    dashboardRole = showcaseDashboardRole();
    await ensureSupersetRoles([RLS_A, RLS_B]);
    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole, RLS_A]}]));
    await expectSupersetRoles(user, ['BI_VIEWER', dashboardRole, RLS_A]);
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [user]);
  });

  // a second import for an existing viewer lists another RLS role; the viewer ends up with the new RLS role only,
  // the dashboard role stays and the showcase dashboard is still assigned in the portal
  test('replaces the RLS roles of a viewer with the ones of the file', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole, RLS_B]}]));

    await expectSupersetRoles(user, ['BI_VIEWER', dashboardRole, RLS_B]);
    expect(portalDashboards(user)).toEqual([SHOWCASE_DASHBOARD]);
  });

  // an RLS role added directly in Superset and the dashboard role missing in the next file are both removed;
  // the viewer keeps only the RLS role of the file and has no portal dashboards left
  test('removes roles that were given in Superset directly and dashboards that are no longer in the file', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    addSupersetRole(user, RLS_A);
    await expectSupersetRoles(user, ['BI_VIEWER', dashboardRole, RLS_A, RLS_B]);

    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_B]}]));

    await expectSupersetRoles(user, ['BI_VIEWER', RLS_B]);
    expect(portalDashboards(user)).toEqual([]);
  });

  // the file turns the viewer into a data domain editor; the portal role changes and in Superset the user gets
  // BI_EDITOR and sql_lab instead of the viewer roles
  test('changes the data domain role and drops the roles only a viewer has', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_EDITOR'}]));

    expect(portalRoles(user)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_EDITOR'});
    await expectSupersetRoles(user, ['BI_EDITOR', 'sql_lab']);
  });

  // the role NONE takes all data domain access away; the user stays enabled in the portal and has only BI_NO_ACCESS
  // in Superset
  test('takes all access to the data domain away with the role NONE', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: user, dataDomainRole: 'NONE'}]));

    expect(portalRoles(user)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'NONE'});
    expect(portalUser(user)?.enabled).toBe(true);
    await expectSupersetRoles(user, ['BI_NO_ACCESS']);
  });

  // a file makes the user HelloDATA admin (superuser, Admin and BI_ADMIN roles in Superset); a following file makes the
  // user a plain viewer again and the superuser flag and the admin roles are gone
  test('promotes a user to HelloDATA admin and back', async () => {
    test.setTimeout(3 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: user, businessDomainRole: 'HELLODATA_ADMIN', dataDomainRole: 'DATA_DOMAIN_ADMIN'}]));

    expect(portalUser(user)?.superuser).toBe(true);
    await expectSupersetRoles(user, ['Admin', 'BI_ADMIN', 'BI_EDITOR', 'sql_lab']);

    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole]}]));

    expect(portalUser(user)?.superuser).toBe(false);
    expect(portalRoles(user)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_VIEWER'});
    await expectSupersetRoles(user, ['BI_VIEWER', dashboardRole]);
  });
});
