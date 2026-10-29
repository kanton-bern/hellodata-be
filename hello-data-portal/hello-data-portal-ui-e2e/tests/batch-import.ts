import {execFileSync} from 'node:child_process';
import {Browser, Page} from '@playwright/test';
import {ADMIN, dismissToasts, expect, navigate} from './fixtures';
import {SHOWCASE_DASHBOARD_SLUG, SUPERSET_SYNC_TIMEOUT, withUser} from './superset';

// The batch import has no UI: the portal api scans a folder for *.csv files every 30 seconds
// (hello-data.batch-users-file), processes them and writes its progress to users-output.log in the same folder.
// The helpers below drop the files into the portal api container and check the result in the databases of the portal
// and of Superset, so these tests need the docker CLI and the docker compose stack on the machine running Playwright.

const COMPOSE_PROJECT = process.env.E2E_COMPOSE_PROJECT ?? process.env.COMPOSE_PROJECT_NAME ?? 'hellodata-e2e';
const BATCH_DIR = '/storage/batch-users-processing';
const BATCH_LOG = `${BATCH_DIR}/users-output.log`;
const PORTAL_DB = 'hd_metainfo';
const SUPERSET_DB = 'superset_default_data_domain';

export const CONTEXT_KEY = 'Default_Data_Domain';
export const CSV_HEADER = 'email;businessDomainRole;context;dataDomainRole;supersetRole;dashboardGroup';
// the scan interval (30s), creating the users and the sync with Superset over NATS
export const BATCH_TIMEOUT = 3 * 60_000;

// RLS roles are plain Superset roles, the tests create them once and keep them (see ensureSupersetRoles)
export const RLS_A = 'RLS_E2E_A';
export const RLS_B = 'RLS_E2E_B';

export type CsvRow = {
  email: string,
  businessDomainRole?: string,
  context?: string,
  dataDomainRole: string,
  supersetRoles?: string[],
  dashboardGroups?: string[],
};

/** A unique email per test run, so a user is new to the portal and to Superset when the test starts. */
export function batchEmail(name: string) {
  return `batch-${name}.${RUN_ID}@hellodata.ch`;
}
const RUN_ID = Date.now().toString(36);

/** Builds a batch import file: semicolons between columns, commas between the values of a cell. */
export function toCsv(rows: CsvRow[], {header = CSV_HEADER, bom = false} = {}) {
  const lines = rows.map(row => [
    row.email,
    row.businessDomainRole ?? 'NONE',
    row.context ?? CONTEXT_KEY,
    row.dataDomainRole,
    (row.supersetRoles ?? []).join(','),
    (row.dashboardGroups ?? []).join(','),
  ].join(';'));
  // files saved by Excel start with a byte order mark
  return (bom ? '﻿' : '') + [header, ...lines].join('\n') + '\n';
}

const containers = new Map<string, string>();

/** The id of the running container of a compose service. */
function container(service: string) {
  if (!containers.has(service)) {
    const id = execFileSync('docker', ['ps', '-q',
      '--filter', `label=com.docker.compose.project=${COMPOSE_PROJECT}`,
      '--filter', `label=com.docker.compose.service=${service}`], {encoding: 'utf8'}).trim().split('\n')[0];
    if (!id) {
      throw new Error(`service ${service} of compose project ${COMPOSE_PROJECT} is not running (set E2E_COMPOSE_PROJECT)`);
    }
    containers.set(service, id);
  }
  return containers.get(service)!;
}

function exec(service: string, args: string[], input?: string) {
  return execFileSync('docker', ['exec', '-i', container(service), ...args], {encoding: 'utf8', input});
}

/** Runs a query in the postgres container and returns its rows, each as a list of column values. */
export function sql(database: string, query: string): string[][] {
  const out = exec('postgres', ['psql', '-U', 'postgres', '-d', database, '-v', 'ON_ERROR_STOP=1', '-tA', '-F', '|', '-c', query]);
  return out.split('\n').filter(line => line.length > 0).map(line => line.split('|'));
}

/** Quotes a value for a SQL statement. */
function literal(value: string) {
  return `'${value.replaceAll("'", "''")}'`;
}

/** Copies a csv file into the batch folder. It is written under another name first, so the scan never reads half a file. */
function dropCsv(content: string) {
  const file = `${BATCH_DIR}/batchprocessing_user_roles_e2e_${Date.now()}.csv`;
  exec('hello-data-portal-api', ['sh', '-c', 'mkdir -p "$(dirname "$1")" && cat > "$1.tmp" && mv "$1.tmp" "$1"', 'sh', file], content);
  return file;
}

function csvExists(file: string) {
  return exec('hello-data-portal-api', ['sh', '-c', 'test -f "$1" && echo yes || true', 'sh', file]).trim() === 'yes';
}

function removeCsv(file: string) {
  exec('hello-data-portal-api', ['rm', '-f', file]);
}

/** The number of lines in the batch log, to read only what was logged afterwards. */
function batchLogLength() {
  return batchLogSince(0).split('\n').length - 1;
}

function batchLogSince(line: number) {
  const log = exec('hello-data-portal-api', ['sh', '-c', 'cat "$1" 2>/dev/null || true', 'sh', BATCH_LOG]);
  return log.split('\n').slice(line).join('\n');
}

/** Polls the batch log until it matches one of the patterns and returns the log written since `line`. */
async function waitForBatchLog(line: number, patterns: RegExp[]) {
  const deadline = Date.now() + BATCH_TIMEOUT;
  for (; ;) {
    const log = batchLogSince(line);
    if (patterns.some(pattern => pattern.test(log))) {
      return log;
    }
    if (Date.now() > deadline) {
      throw new Error(`the batch import did not log ${patterns.join(' or ')} within ${BATCH_TIMEOUT / 1000}s, it logged:\n${log}`);
    }
    await new Promise(resolve => setTimeout(resolve, 2_000));
  }
}

/** Drops a csv file into the batch folder and waits until the portal has processed all of its users. */
export async function importCsv(content: string) {
  const mark = batchLogLength();
  const file = dropCsv(content);
  try {
    const log = await waitForBatchLog(mark, [/Processed \d+ users successfully/, /Error inviting users/]);
    expect(log, 'the batch import failed').not.toContain('Error inviting users');
    expect(csvExists(file), 'a processed file is removed from the batch folder').toBe(false);
    return log;
  } finally {
    removeCsv(file);
  }
}

/** Drops a csv file that has to be rejected and returns the error the portal logged for it. */
export async function importInvalidCsv(content: string) {
  const mark = batchLogLength();
  const file = dropCsv(content);
  try {
    const log = await waitForBatchLog(mark, [/Error inviting users/, /Processed \d+ users successfully/]);
    expect(log, 'the file was imported although it is invalid').not.toMatch(/Processed \d+ users successfully/);
    // a rejected file stays in the folder (and is read again on every scan) until someone fixes or removes it
    expect(csvExists(file), 'a rejected file is left in the batch folder').toBe(true);
    return log;
  } finally {
    removeCsv(file);
  }
}

/** The user in the portal database, undefined if the portal does not know it. */
export function portalUser(email: string) {
  const [row] = sql(PORTAL_DB, `select enabled, superuser from user_ where lower(email) = lower(${literal(email)})`);
  return row && {enabled: row[0] === 't', superuser: row[1] === 't'};
}

/** The business domain role and the role in the default data domain of a user, as stored in the portal. */
export function portalRoles(email: string) {
  const rows = sql(PORTAL_DB, `
      select c.type, r.name
      from user_context_role ucr
               join user_ u on u.id = ucr.user_id
               join role r on r.id = ucr.role_id
               join context c on c.context_key = ucr.context_key
      where lower(u.email) = lower(${literal(email)})
        and (c.type = 'BUSINESS_DOMAIN' or c.context_key = ${literal(CONTEXT_KEY)})`);
  return {
    businessDomainRole: rows.find(([type]) => type === 'BUSINESS_DOMAIN')?.[1],
    dataDomainRole: rows.find(([type]) => type === 'DATA_DOMAIN')?.[1],
  };
}

/** The dashboards of the default data domain a viewer or business specialist was given in the portal. */
export function portalDashboards(email: string) {
  return sql(PORTAL_DB, `
      select d.dashboard_title
      from user_selected_dashboard d
               join user_ u on u.id = d.user_id
      where lower(u.email) = lower(${literal(email)})
        and d.context_key = ${literal(CONTEXT_KEY)}
      order by 1`).map(([title]) => title);
}

/** The comment permission of a user in the default data domain, e.g. "read,write,review" or "" for none. */
export function portalCommentPermission(email: string) {
  const [row] = sql(PORTAL_DB, `
      select p.read_comments, p.write_comments, p.review_comments
      from dashboard_comment_permission p
               join user_ u on u.id = p.user_id
      where lower(u.email) = lower(${literal(email)})
        and p.context_key = ${literal(CONTEXT_KEY)}`);
  return ['read', 'write', 'review'].filter((_, i) => row?.[i] === 't').join(',');
}

/** The roles of a user in Superset (tables ab_user, ab_user_role and ab_role), sorted, undefined if there is no such user. */
export function supersetRoles(email: string): string[] | undefined {
  const users = sql(SUPERSET_DB, `select id from ab_user where lower(email) = lower(${literal(email)})`);
  if (users.length === 0) {
    return undefined;
  }
  return sql(SUPERSET_DB, `
      select r.name
      from ab_user_role ur
               join ab_role r on r.id = ur.role_id
      where ur.user_id = ${Number(users[0][0])}
      order by r.name`).map(([name]) => name);
}

/** Waits until a user has exactly these roles in Superset: the sidecar applies them asynchronously after the import. */
export async function expectSupersetRoles(email: string, roles: string[]) {
  await expect.poll(() => supersetRoles(email), {
    message: `Superset roles of ${email}`,
    timeout: BATCH_TIMEOUT,
    intervals: [2_000, 5_000],
  }).toEqual([...roles].sort());
}

/** Gives a user a role directly in Superset, bypassing the portal (e.g. set by hand by a Superset admin). */
export function addSupersetRole(email: string, role: string) {
  sql(SUPERSET_DB, `
      insert into ab_user_role (id, user_id, role_id)
      select nextval('ab_user_role_id_seq'), u.id, r.id
      from ab_user u, ab_role r
      where lower(u.email) = lower(${literal(email)}) and r.name = ${literal(role)}`);
}

/** The dashboard role (D_<slug of the title>_<id>) Superset created for the showcase dashboard. */
export function showcaseDashboardRole() {
  const [row] = sql(SUPERSET_DB, `
      select r.name
      from dashboards d
               join dashboard_roles dr on dr.dashboard_id = d.id
               join ab_role r on r.id = dr.role_id
      where d.slug = ${literal(SHOWCASE_DASHBOARD_SLUG)} and r.name like 'D\\_%'`);
  expect(row, 'the showcase dashboard has no dashboard role').toBeDefined();
  return row[0];
}

/**
 * Creates roles in Superset if they do not exist yet and waits until the sidecar published them to the portal:
 * the batch import rejects Superset roles it does not know.
 */
export async function ensureSupersetRoles(names: string[]) {
  for (const name of names) {
    sql(SUPERSET_DB, `
        insert into ab_role (id, name)
        select nextval('ab_role_id_seq'), ${literal(name)}
        where not exists (select 1 from ab_role where name = ${literal(name)})`);
  }
  await expect.poll(() => {
    const [row] = sql(PORTAL_DB, `
        select metainfo::text from resource
        where module_type = 'SUPERSET' and kind = 'hellodata/Roles' and context_key = ${literal(CONTEXT_KEY)}`);
    return names.filter(name => !row?.[0].includes(`"${name}"`));
  }, {message: 'Superset roles not yet published to the portal', timeout: SUPERSET_SYNC_TIMEOUT, intervals: [5_000, 10_000]}).toEqual([]);
}

/** Deletes a user in the admin's user management, together with its accounts in Keycloak and the subsystems. */
async function deleteUser(page: Page, email: string) {
  await navigate(page, 'Administration', 'User management');
  await page.getByPlaceholder('Search').fill(email);
  const row = page.getByRole('row').filter({has: page.getByRole('cell', {name: email, exact: true})});
  await expect(row).toHaveCount(1);
  await row.locator('button.p-button-danger').click();
  await page.locator('.p-confirmdialog').getByRole('button', {name: 'Delete', exact: true}).click();
  await expect(row).toHaveCount(0);
  await dismissToasts(page);
}

/**
 * Removes the users a test imported. A HelloDATA admin cannot be deleted, so all of them are first set back to no role
 * with one more import.
 */
export async function deleteBatchUsers(browser: Browser, emails: string[]) {
  const existing = emails.filter(email => portalUser(email));
  if (existing.length === 0) {
    return;
  }
  if (existing.some(email => portalUser(email)?.superuser)) {
    await importCsv(toCsv(existing.map(email => ({email, dataDomainRole: 'NONE'}))));
  }
  await withUser(browser, ADMIN, async page => {
    for (const email of existing) {
      await deleteUser(page, email);
    }
  });
}
