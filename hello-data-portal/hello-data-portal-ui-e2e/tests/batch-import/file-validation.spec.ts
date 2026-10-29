import {expect, test} from '../fixtures';
import {SUPERSET_SYNC_TIMEOUT} from '../superset';
import {
  BATCH_TIMEOUT,
  batchEmail,
  CONTEXT_KEY,
  deleteBatchUsers,
  ensureSupersetRoles,
  expectSupersetRoles,
  importCsv,
  importInvalidCsv,
  portalRoles,
  portalUser,
  RLS_A,
  toCsv,
} from '../batch-import';

const OLD_HEADER = 'email;businessDomainRole;context;dataDomainRole;supersetRole';
const CSV_HEADER_WITH_COMMAS = 'email,businessDomainRole,context,dataDomainRole,supersetRole,dashboardGroup';

// HELLODATA-2075: the whole file is checked before any user is touched. A file with an error is rejected as a whole,
// the error is written to users-output.log and the file is left in the folder.
test.describe('Batch import - file validation', () => {
  const valid = batchEmail('validation-valid');
  const invalid = batchEmail('validation-invalid');
  const oldFormat = batchEmail('validation-old-format');

  test.beforeAll(async () => {
    test.setTimeout(SUPERSET_SYNC_TIMEOUT + 30_000);
    await ensureSupersetRoles([RLS_A]);
  });

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await deleteBatchUsers(browser, [valid, invalid, oldFormat]);
  });

  const rejected: {name: string, csv: () => string, error: RegExp}[] = [
    {
      name: 'an unknown data domain',
      csv: () => toCsv([{email: invalid, context: 'Unknown_Data_Domain', dataDomainRole: 'DATA_DOMAIN_VIEWER'}]),
      error: /context key not found: 'Unknown_Data_Domain'/,
    },
    {
      name: 'a Superset role that does not exist in Superset',
      csv: () => toCsv([{email: invalid, dataDomainRole: 'DATA_DOMAIN_VIEWER', supersetRoles: [RLS_A, 'RLS_E2E_MISSING']}]),
      error: /not present in Superset for context 'Default_Data_Domain': \[RLS_E2E_MISSING]/,
    },
    {
      name: 'a dashboard group that does not exist',
      csv: () => toCsv([{email: invalid, dataDomainRole: 'DATA_DOMAIN_VIEWER', dashboardGroups: ['E2E missing group']}]),
      error: /dashboard group\(s\) not found in context 'Default_Data_Domain': \[E2E missing group]/,
    },
    {
      name: 'an invalid email',
      csv: () => toCsv([{email: 'not-an-email', dataDomainRole: 'DATA_DOMAIN_VIEWER'}]),
      error: /Line 2: Email is not valid not-an-email/,
    },
    {
      name: 'an unknown business domain role',
      csv: () => toCsv([{email: invalid, businessDomainRole: 'SUPERUSER', dataDomainRole: 'DATA_DOMAIN_VIEWER'}]),
      error: /Invalid business domain role name: SUPERUSER/,
    },
    {
      name: 'an unknown data domain role',
      csv: () => toCsv([{email: invalid, dataDomainRole: 'DATA_DOMAIN_OWNER'}]),
      error: /Invalid data domain role name: DATA_DOMAIN_OWNER/,
    },
    {
      name: 'an empty data domain role',
      csv: () => toCsv([{email: invalid, dataDomainRole: ''}]),
      error: /dataDomainRole is required but was empty/,
    },
    {
      name: 'columns separated by commas instead of semicolons',
      csv: () => toCsv([], {header: CSV_HEADER_WITH_COMMAS}) + `${invalid},NONE,${CONTEXT_KEY},DATA_DOMAIN_VIEWER,,\n`,
      error: /No proper headers found in the CSV file/,
    },
    {
      name: 'only a header',
      csv: () => toCsv([]),
      error: /No records found in the CSV file/,
    },
    {
      name: 'a dashboard group without the dashboardGroup column in the header',
      csv: () => toCsv([{email: invalid, dataDomainRole: 'DATA_DOMAIN_VIEWER', dashboardGroups: ['GroupA']}], {header: OLD_HEADER}),
      error: /the CSV header does not include 'dashboardGroup'/,
    },
  ];

  for (const {name, csv, error} of rejected) {
    test(`rejects a file with ${name}`, async () => {
      test.setTimeout(BATCH_TIMEOUT + 30_000);
      const log = await importInvalidCsv(csv());

      expect(log).toMatch(error);
      expect(portalUser(invalid)).toBeUndefined();
    });
  }

  test('rejects the whole file when one of its rows is invalid', async () => {
    test.setTimeout(BATCH_TIMEOUT + 30_000);
    const log = await importInvalidCsv(toCsv([
      {email: valid, dataDomainRole: 'DATA_DOMAIN_VIEWER'},
      {email: invalid, context: 'Unknown_Data_Domain', dataDomainRole: 'DATA_DOMAIN_VIEWER'},
    ]));

    expect(log).toContain(`User '${invalid}': context key not found`);
    expect(portalUser(valid)).toBeUndefined();
    expect(portalUser(invalid)).toBeUndefined();
  });

  test('accepts a file in the old format without the dashboardGroup column', async () => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await importCsv(`${OLD_HEADER}\n${oldFormat};NONE;${CONTEXT_KEY};DATA_DOMAIN_VIEWER;${RLS_A}\n`);

    expect(portalRoles(oldFormat)).toEqual({businessDomainRole: 'NONE', dataDomainRole: 'DATA_DOMAIN_VIEWER'});
    await expectSupersetRoles(oldFormat, ['BI_VIEWER', RLS_A]);
  });
});
