import {expect, test} from '../fixtures';
import {SUPERSET_SYNC_TIMEOUT} from '../superset';
import {
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

// HELLODATA-2073 / HELLODATA-2074: the rows are processed from top to bottom, so when a file has several rows for the
// same user, the end result is the one of its last row. The current state of an existing user counts like a row above
// the first one of the file.
test.describe('Batch import - last record wins', () => {
  const newUser = batchEmail('last-new');
  const demoted = batchEmail('last-demoted');
  const existing = batchEmail('last-existing');
  let dashboardRole: string;

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + 30_000);
    dashboardRole = showcaseDashboardRole();
    await ensureSupersetRoles([RLS_A, RLS_B]);
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [newUser, demoted, existing]);
  });

  test('keeps the data domain and Superset roles of the last row of a new user', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([
      {email: newUser, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole, RLS_A]},
      {email: newUser, dataDomainRole: 'DATA_DOMAIN_ADMIN'},
      {email: newUser, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_B]},
    ]));

    expect(portalRoles(newUser)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_VIEWER'});
    expect(portalDashboards(newUser)).toEqual([]);
    await expectSupersetRoles(newUser, ['BI_VIEWER', RLS_B]);
  });

  test('keeps the business domain role of the last row', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(toCsv([
      {email: demoted, businessDomainRole: 'HELLODATA_ADMIN', dataDomainRole: 'DATA_DOMAIN_ADMIN'},
      {email: demoted, dataDomainRole: 'DATA_DOMAIN_EDITOR'},
    ]));

    expect(portalUser(demoted)?.superuser).toBe(false);
    expect(portalRoles(demoted)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_EDITOR'});
    await expectSupersetRoles(demoted, ['BI_EDITOR', 'sql_lab']);
  });

  test('ends an existing user with the last row, whatever it had before', async () => {
    test.setTimeout(3 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: existing, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [dashboardRole, RLS_A]}]));
    await expectSupersetRoles(existing, ['BI_VIEWER', dashboardRole, RLS_A]);

    await importCsv(toCsv([
      {email: existing, dataDomainRole: 'DATA_DOMAIN_EDITOR'},
      {email: existing, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_B]},
    ]));

    expect(portalRoles(existing)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_VIEWER'});
    expect(portalDashboards(existing)).toEqual([]);
    await expectSupersetRoles(existing, ['BI_VIEWER', RLS_B]);
  });
});
