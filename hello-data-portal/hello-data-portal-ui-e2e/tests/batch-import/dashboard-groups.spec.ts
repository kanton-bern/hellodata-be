import {expect, test} from '../fixtures';
import {SUPERSET_SYNC_TIMEOUT} from '../superset';
import {
  BATCH_TIMEOUT,
  batchEmail,
  createDashboardGroup,
  dashboardGroupMembers,
  deleteBatchUsers,
  deleteDashboardGroup,
  ensureSupersetRoles,
  expectSupersetRoles,
  importCsv,
  portalDashboards,
  RLS_A,
  showcaseDashboardRole,
  toCsv,
} from '../batch-import';

// HELLODATA-2073 / HELLODATA-2074: the dashboardGroup column puts viewers and business specialists into dashboard groups.
// Like the roles, the groups of a data domain are the ones of the file: a group that is no longer listed is left.
test.describe('Batch import - dashboard groups', () => {
  const user = batchEmail('grouped');
  const group = `E2E batch group ${Date.now().toString(36)}`;
  let dashboardRole: string;

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + 30_000);
    dashboardRole = showcaseDashboardRole();
    await ensureSupersetRoles([RLS_A]);
    createDashboardGroup(group);
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [user]);
    deleteDashboardGroup(group);
  });

  test('adds a viewer to the groups of the file and removes it from groups no longer listed', async () => {
    test.setTimeout(3 * BATCH_TIMEOUT);
    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_A], dashboardGroups: [group]}]));

    expect(dashboardGroupMembers(group)).toEqual([user]);
    // the dashboard comes from the group, not from a D_ role in the file
    expect(portalDashboards(user)).toEqual([]);
    await expectSupersetRoles(user, ['BI_VIEWER', dashboardRole, RLS_A]);

    await importCsv(toCsv([{email: user, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_A]}]));

    expect(dashboardGroupMembers(group)).toEqual([]);
    await expectSupersetRoles(user, ['BI_VIEWER', RLS_A]);
  });
});
