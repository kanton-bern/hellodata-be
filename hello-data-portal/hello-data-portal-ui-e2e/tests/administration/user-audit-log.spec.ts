import {Page} from '@playwright/test';
import {CONTEXT_KEY} from '../instance';
import {
  ADMIN,
  BUSINESS_DOMAIN_ADMIN,
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  dismissToasts,
  expect,
  navigate,
  openPortalAs,
  selectOption,
  test,
  User,
} from '../fixtures';
import {DATA_DOMAIN, openUserEdit, saveUserEdit, SHOWCASE_DASHBOARD, withUser} from '../superset';
import {BATCH_TIMEOUT, deleteBatchUsers, deleteDashboardGroup, importCsv, portalRoles, portalUser, toCsv} from '../batch-import';
import {bulkAssign, expectResult} from '../bulk-assignments';
import {
  AuditRow,
  auditRows,
  changeCard,
  changeCards,
  changeLabels,
  expandHistoryRow,
  historyDetails,
  deleteAuditRows,
  expectHistoryRow,
  getAuditLog,
  historyPaginator,
  historyPanel,
  historyRow,
  historyRows,
  openHistory,
  portalApiOf,
  portalUserId,
} from '../user-audit';

// the English labels of the History panel (en.json, "@History action ..." and "@History source ...")
const ACTION = {
  USER_CREATED: 'User created',
  USER_DELETED: 'User deleted',
  USER_ENABLED: 'User enabled',
  USER_DISABLED: 'User disabled',
  ROLES_CHANGED: 'Roles changed',
  DASHBOARD_GROUP_MEMBERSHIP_CHANGED: 'Dashboard group membership changed',
} as const;
const SOURCE = {MANUAL: 'Manual', BATCH: 'Batch', BULK_ASSIGNMENT: 'Bulk assignment'} as const;
// the table of the History panel shows 20 entries per page
const HISTORY_PAGE_SIZE = 20;

function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** The newest audit row of an action and source, failing if there is none. */
function newestRow(rows: AuditRow[], action: string, source: string) {
  const row = rows.find(r => r.action === action && r.source === source);
  expect(row, `no ${action} entry from ${source} in ${JSON.stringify(rows.map(r => `${r.action}/${r.source}`))}`).toBeDefined();
  return row!;
}

/** The change of a field (and data domain) in an audit row. */
function change(row: AuditRow, field: string, contextKey: string | null = null) {
  return row.changes.find(c => c.field === field && c.contextKey === contextKey);
}

/** Clicks Disable or Enable in the toolbar of the user edit page and confirms the dialog. */
async function setEnabledOnEditPage(page: Page, enabled: boolean) {
  const label = enabled ? 'Enable' : 'Disable';
  // the edit page has the toolbar above and below the form
  await page.getByRole('button', {name: label, exact: true}).first().click();
  const dialog = page.getByRole('alertdialog').or(page.getByRole('dialog')).filter({hasText: `${label} user`});
  const saved = page.waitForResponse(r => r.url().endsWith(enabled ? '/enable' : '/disable') && r.request().method() === 'PATCH');
  await dialog.getByRole('button', {name: label, exact: true}).click();
  expect((await saved).ok(), `${label.toLowerCase()} failed`).toBe(true);
  await dismissToasts(page);
}

function dashboardGroupRow(page: Page, name: string) {
  return page.getByRole('row').filter({has: page.getByRole('cell', {name, exact: true})});
}

// HELLODATA-4194: every change of a user's access is written to the user audit log, which HelloDATA admins and
// business domain admins read in the History panel of the user edit page. The tests follow one invited user through
// its life (invite, manual role, disable and enable, dashboard group, batch import, bulk assignment, deletion) and check
// each entry in the panel, in the portal database and in the api.
test.describe('User audit log', () => {
  test.describe.configure({mode: 'serial'});
  const RUN_ID = Date.now().toString(36);
  const EMAIL = `e2e-audit.${RUN_ID}@hellodata.ch`;
  const TARGET: User = {username: EMAIL, password: ''};
  const GROUP = `e2e-audit-group-${RUN_ID}`;
  let userId: string;

  test.afterAll(async ({browser}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    // a test that failed half way leaves the group and the user behind
    deleteDashboardGroup(GROUP);
    await deleteBatchUsers(browser, [EMAIL]);
    if (userId) {
      deleteAuditRows(userId);
    }
  });

  // the admin invites a new user; the audit log gets a "User created" entry by the admin with the new user's e-mail,
  // enabled flag and superuser flag, and the History panel of the edit page the portal opens lists it without them
  test('inviting a user writes a "User created" entry', async ({adminPage: page}) => {
    await navigate(page, 'Administration', 'User management');
    await page.locator('#user input').fill(EMAIL);
    await page.getByRole('option').filter({hasText: EMAIL}).click();
    await expect(page.locator('#firstName')).not.toHaveValue('');
    await page.getByRole('button', {name: 'Set permissions', exact: true}).click();
    await expect(page).toHaveURL(/\/user-management\/edit\//);

    userId = portalUserId(EMAIL)!;
    expect(userId, 'the invited user is not in the portal').toBeDefined();
    const rows = auditRows(userId);
    expect(rows.filter(r => r.action === 'USER_CREATED')).toHaveLength(1);
    const created = newestRow(rows, 'USER_CREATED', 'MANUAL');
    expect(created).toMatchObject({actorEmail: ADMIN.email, targetEmail: EMAIL});
    expect(change(created, 'email')).toMatchObject({before: null, after: EMAIL});
    expect(change(created, 'enabled')).toMatchObject({before: null, after: true});
    expect(change(created, 'superuser')).toMatchObject({before: null, after: false});
    // the creation is the first entry of the user
    expect(rows.at(-1)!.action).toBe('USER_CREATED');

    await openHistory(page);
    await expect(historyRows(page)).toHaveCount(rows.length);
    await expectHistoryRow(historyRow(page, ACTION.USER_CREATED, SOURCE.MANUAL), {
      who: ADMIN.email,
      source: SOURCE.MANUAL,
      action: ACTION.USER_CREATED,
    });
    // the initial values of a new user are obvious, the panel shows the entry without its changes (the log keeps them)
    await expect(historyRow(page, ACTION.USER_CREATED, SOURCE.MANUAL).getByRole('cell').nth(5)).toHaveText('');
  });

  // the admin gives the user the viewer role and saves; the open History panel shows the new "Roles changed" entry on
  // top right away (no reload), with the role in the data domain before and after
  test('saving a role writes a "Roles changed" entry and refreshes the panel', async ({adminPage: page}) => {
    await openUserEdit(page, TARGET);
    await openHistory(page);
    const before = auditRows(userId).length;
    await expect(historyRows(page)).toHaveCount(before);

    await selectOption(page.locator(`p-select#dataDomain-${CONTEXT_KEY}`), 'DATA DOMAIN VIEWER');
    await saveUserEdit(page);
    expect(portalRoles(EMAIL).dataDomainRole).toBe('DATA_DOMAIN_VIEWER');

    const rows = auditRows(userId);
    expect(rows).toHaveLength(before + 1);
    expect(rows[0]).toMatchObject({action: 'ROLES_CHANGED', source: 'MANUAL', actorEmail: ADMIN.email, targetEmail: EMAIL});
    expect(change(rows[0], 'role', CONTEXT_KEY)?.after).toBe('DATA_DOMAIN_VIEWER');

    await expect(historyRows(page)).toHaveCount(before + 1);
    await expectHistoryRow(historyRows(page).first(), {
      who: ADMIN.email,
      source: SOURCE.MANUAL,
      action: ACTION.ROLES_CHANGED,
      changes: [{domain: DATA_DOMAIN, line: /^Role: .+ -> DATA_DOMAIN_VIEWER$/}],
    });
  });

  // the admin disables the user and enables it again on its edit page; each action writes its own entry with the
  // enabled flag before and after, and the panel shows both
  test('disabling and enabling write "User disabled" and "User enabled" entries', async ({adminPage: page}) => {
    await openUserEdit(page, TARGET);
    const before = auditRows(userId).length;

    await setEnabledOnEditPage(page, false);
    expect(portalUser(EMAIL)?.enabled).toBe(false);
    await openHistory(page);
    await expectHistoryRow(historyRows(page).first(), {
      who: ADMIN.email, source: SOURCE.MANUAL, action: ACTION.USER_DISABLED, changes: ['Enabled: true -> false'],
    });

    await setEnabledOnEditPage(page, true);
    expect(portalUser(EMAIL)?.enabled).toBe(true);
    await openHistory(page);
    await expectHistoryRow(historyRows(page).first(), {
      who: ADMIN.email, source: SOURCE.MANUAL, action: ACTION.USER_ENABLED, changes: ['Enabled: false -> true'],
    });

    const rows = auditRows(userId);
    expect(rows).toHaveLength(before + 2);
    expect(rows.slice(0, 2).map(r => r.action)).toEqual(['USER_ENABLED', 'USER_DISABLED']);
    expect(change(rows[1], 'enabled')).toMatchObject({before: true, after: false});
    expect(change(rows[0], 'enabled')).toMatchObject({before: false, after: true});
  });

  // the admin creates a dashboard group with the user as member and deletes it again; both write a "Dashboard group
  // membership changed" entry for the user, with the group's name in the data domain
  test('joining and leaving a dashboard group write membership entries', async ({adminPage: page}) => {
    await page.goto(`dashboard-groups/list/${CONTEXT_KEY}`);
    await expect(page.locator('#dashboardGroupsTable')).toBeVisible();
    await page.getByRole('button', {name: 'Create', exact: true}).click();
    await page.locator('input#name').fill(GROUP);
    await page.getByRole('checkbox', {name: SHOWCASE_DASHBOARD, exact: true}).check();
    await page.getByRole('tab', {name: /^Users/}).click();
    await page.getByRole('tabpanel', {name: /^Users/}).getByPlaceholder('Search').fill(EMAIL);
    await page.getByRole('checkbox', {name: new RegExp(escapeRegExp(EMAIL))}).check();
    await expect(page.getByRole('tab', {name: 'Users (1)'})).toBeVisible();
    const created = page.waitForResponse(r => r.url().endsWith('/dashboard-groups') && r.request().method() === 'POST');
    await page.getByRole('button', {name: 'Save', exact: true}).click();
    expect((await created).ok(), 'saving the dashboard group failed').toBe(true);
    await dismissToasts(page);

    let rows = auditRows(userId);
    expect(rows[0]).toMatchObject({action: 'DASHBOARD_GROUP_MEMBERSHIP_CHANGED', source: 'MANUAL', actorEmail: ADMIN.email, targetEmail: EMAIL});
    expect(rows[0].changes).toEqual([{field: 'dashboardGroups', contextKey: CONTEXT_KEY, before: null, after: GROUP}]);

    await page.goto(`dashboard-groups/list/${CONTEXT_KEY}`);
    await dashboardGroupRow(page, GROUP).locator('button.p-button-danger').click();
    const deleted = page.waitForResponse(r => r.url().includes('/dashboard-groups/') && r.request().method() === 'DELETE');
    await page.locator('.p-confirmdialog').getByRole('button', {name: 'Delete', exact: true}).click();
    expect((await deleted).ok(), 'deleting the dashboard group failed').toBe(true);
    await expect(dashboardGroupRow(page, GROUP)).toHaveCount(0);

    rows = auditRows(userId);
    expect(rows[0].action).toBe('DASHBOARD_GROUP_MEMBERSHIP_CHANGED');
    expect(rows[0].changes).toEqual([{field: 'dashboardGroups', contextKey: CONTEXT_KEY, before: GROUP, after: null}]);

    // the panel translates the field and shows the name of the data domain
    await openUserEdit(page, TARGET);
    await openHistory(page);
    const membership = historyRows(page).filter({has: page.getByRole('cell', {name: ACTION.DASHBOARD_GROUP_MEMBERSHIP_CHANGED, exact: true})});
    await expect(membership).toHaveCount(2);
    await expectHistoryRow(membership.nth(0), {
      who: ADMIN.email, source: SOURCE.MANUAL, action: ACTION.DASHBOARD_GROUP_MEMBERSHIP_CHANGED, context: DATA_DOMAIN,
      changes: [`Dashboard groups [${DATA_DOMAIN}]: ${GROUP} -> -`],
    });
    await expectHistoryRow(membership.nth(1), {
      who: ADMIN.email, source: SOURCE.MANUAL, action: ACTION.DASHBOARD_GROUP_MEMBERSHIP_CHANGED, context: DATA_DOMAIN,
      changes: [`Dashboard groups [${DATA_DOMAIN}]: - -> ${GROUP}`],
    });
  });

  // a batch import changes the user's role; the entry has the source Batch and no actor (the import runs as a job), so
  // the panel shows the source as who; importing the same role again changes nothing and writes no entry
  test('a batch import writes a "Roles changed" entry from the batch, and nothing when nothing changes', async ({adminPage: page}) => {
    test.setTimeout(3 * BATCH_TIMEOUT);
    const csv = toCsv([{email: EMAIL, dataDomainRole: 'DATA_DOMAIN_BUSINESS_SPECIALIST'}]);
    const before = auditRows(userId).length;
    await importCsv(csv);
    expect(portalRoles(EMAIL).dataDomainRole).toBe('DATA_DOMAIN_BUSINESS_SPECIALIST');

    let rows = auditRows(userId);
    expect(rows.length).toBeGreaterThan(before);
    const batch = newestRow(rows, 'ROLES_CHANGED', 'BATCH');
    expect(batch).toMatchObject({actorEmail: '', targetEmail: EMAIL});
    expect(change(batch, 'role', CONTEXT_KEY)).toMatchObject({before: 'DATA_DOMAIN_VIEWER', after: 'DATA_DOMAIN_BUSINESS_SPECIALIST'});

    const afterFirstImport = rows.length;
    await importCsv(csv);
    rows = auditRows(userId);
    expect(rows, 'an import without a change must not write an entry').toHaveLength(afterFirstImport);

    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expectHistoryRow(historyRow(page, ACTION.ROLES_CHANGED, SOURCE.BATCH), {
      who: SOURCE.BATCH,
      source: SOURCE.BATCH,
      action: ACTION.ROLES_CHANGED,
      changes: [`Role [${DATA_DOMAIN}]: DATA_DOMAIN_VIEWER -> DATA_DOMAIN_BUSINESS_SPECIALIST`],
    });
  });

  // the admin gives the user the editor role in the bulk assignments wizard; the entry has the source Bulk assignment
  // and the admin as actor
  test('a bulk assignment writes a "Roles changed" entry from the bulk assignment', async ({adminPage: page}) => {
    test.setTimeout(2 * BATCH_TIMEOUT);
    await bulkAssign(page, [EMAIL], {role: 'DATA_DOMAIN_EDITOR'});
    await expectResult(page, {updated: 1, skipped: 0, failed: 0});
    expect(portalRoles(EMAIL).dataDomainRole).toBe('DATA_DOMAIN_EDITOR');

    const bulk = newestRow(auditRows(userId), 'ROLES_CHANGED', 'BULK_ASSIGNMENT');
    expect(bulk).toMatchObject({actorEmail: ADMIN.email, targetEmail: EMAIL});
    expect(change(bulk, 'role', CONTEXT_KEY)).toMatchObject({before: 'DATA_DOMAIN_BUSINESS_SPECIALIST', after: 'DATA_DOMAIN_EDITOR'});

    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expectHistoryRow(historyRow(page, ACTION.ROLES_CHANGED, SOURCE.BULK_ASSIGNMENT), {
      who: ADMIN.email,
      source: SOURCE.BULK_ASSIGNMENT,
      action: ACTION.ROLES_CHANGED,
      changes: [`Role [${DATA_DOMAIN}]: DATA_DOMAIN_BUSINESS_SPECIALIST -> DATA_DOMAIN_EDITOR`],
    });
  });

  // like the user overview, a History row shows the changes without a domain and the names of the domains it changed;
  // its chevron opens one card per domain (the domain as its heading, its role first), "Expand all" opens all of them
  // (even with the nine data domains some installations have); what changed is bold and how it changed is not; the table
  // pages with a paginator while it has entries and shows the empty message without one when it has none
  test('the History shows the domain changes in expandable cards and hides the paginator without entries', async ({adminPage: page}) => {
    const rows = auditRows(userId);
    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expect(historyRows(page)).toHaveCount(Math.min(rows.length, HISTORY_PAGE_SIZE));
    await expect(historyPaginator(page)).toBeVisible();

    const bulk = historyRow(page, ACTION.ROLES_CHANGED, SOURCE.BULK_ASSIGNMENT);
    await expect(bulk.locator('.change-domains')).toContainText(DATA_DOMAIN);
    const details = await expandHistoryRow(bulk);
    await expect(bulk.locator('.change-domains')).toHaveCount(0);
    const role = changeLabels(changeCard(details, DATA_DOMAIN)).first();
    await expect(role).toHaveText('Role');
    expect(Number(await role.evaluate(e => getComputedStyle(e).fontWeight))).toBeGreaterThanOrEqual(600);
    await expect(role.locator('xpath=..')).toHaveText('Role: DATA_DOMAIN_BUSINESS_SPECIALIST -> DATA_DOMAIN_EDITOR');
    // one card per domain, each with its role first
    const headings = await changeCards(details).locator('.change-domain').allTextContents();
    expect(new Set(headings).size, `each domain in one card: ${headings}`).toBe(headings.length);
    for (const heading of headings) {
      const labels = await changeLabels(changeCard(details, heading)).allTextContents();
      expect(labels.indexOf('Role'), `the role first in ${heading}: ${labels}`).toBeLessThanOrEqual(0);
    }
    await bulk.locator('.change-toggle button').click();
    await expect(historyDetails(bulk)).toHaveCount(0);

    // an entry without domain changes has no chevron, its changes are in the row
    const enabled = historyRows(page).filter({has: page.getByRole('cell', {name: ACTION.USER_ENABLED, exact: true})}).first();
    await expect(changeLabels(enabled)).toHaveText(['Enabled']);
    await expect(enabled.locator('.change-toggle')).toHaveCount(0);

    // every user the tests create has entries, so an empty log is served by a stub of the api
    await page.route(url => url.pathname.endsWith(`/users/${userId}/audit`),
      route => route.fulfill({json: {content: [], totalElements: 0, totalPages: 0, number: 0, size: HISTORY_PAGE_SIZE}}));
    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expect(historyRows(page)).toHaveText(['No history entries']);
    await expect(historyPaginator(page)).toHaveCount(0);
    await page.unrouteAll();

    // an entry with nine data domains: the names in the row, nine cards on "Expand all", which the page remembers
    const entry = {
      id: '00000000-0000-0000-0000-000000000001', occurredAt: new Date().toISOString(), actorId: null, actorEmail: ADMIN.email,
      targetUserId: userId, targetEmail: EMAIL, action: 'ROLES_CHANGED', source: 'BULK_ASSIGNMENT', contextKey: null, contextName: null,
      changes: Array.from({length: 9}, (_, i) => ({
        field: 'role', contextKey: `dd${i}`, contextName: `Data domain ${i + 1}`, before: 'NONE', after: 'DATA_DOMAIN_VIEWER',
      })),
    };
    await page.route(url => url.pathname.endsWith(`/users/${userId}/audit`),
      route => route.fulfill({json: {content: [entry], totalElements: 1, totalPages: 1, number: 0, size: HISTORY_PAGE_SIZE}}));
    await openUserEdit(page, TARGET);
    await openHistory(page);
    const many = historyRows(page).first();
    await expect(many.locator('.change-domains')).toHaveText(Array.from({length: 9}, (_, i) => `Data domain ${i + 1}`).join(', '));
    await page.locator('#userAuditExpandAll').click();
    await expect(changeCards(historyDetails(many))).toHaveCount(9);
    await expect(page.locator('#userAuditExpandAll')).toHaveText('Collapse all');
    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expect(changeCards(historyDetails(historyRows(page).first()))).toHaveCount(9);
    await page.locator('#userAuditExpandAll').click();
    await expect(historyDetails(historyRows(page).first())).toHaveCount(0);
    await page.unrouteAll();
  });

  // the search above the History table asks the api for the matching entries of all pages: here by the translated label
  // of a source (the database holds BULK_ASSIGNMENT), a text no entry has shows the empty message, Clear shows all again
  test('the History can be searched', async ({adminPage: page}) => {
    const rows = auditRows(userId);
    const bulk = rows.filter(row => row.source === 'BULK_ASSIGNMENT');
    expect(bulk.length, 'the bulk assignment test wrote an entry').toBeGreaterThan(0);
    await openUserEdit(page, TARGET);
    await openHistory(page);
    await expect(historyRows(page)).toHaveCount(Math.min(rows.length, HISTORY_PAGE_SIZE));

    const searched = page.waitForResponse(r => r.url().includes(`/users/${userId}/audit`) && r.url().includes('search=') && r.url().includes('codes=BULK_ASSIGNMENT'));
    await page.locator('#userAuditSearch').fill(SOURCE.BULK_ASSIGNMENT);
    expect((await searched).ok()).toBe(true);
    await expect(historyRows(page)).toHaveCount(bulk.length);
    for (const row of await historyRows(page).all()) {
      await expect(row.getByRole('cell').nth(2)).toHaveText(SOURCE.BULK_ASSIGNMENT);
    }

    await page.locator('#userAuditSearch').fill('no entry contains this');
    await expect(historyRows(page)).toHaveText(['No history entries']);
    await expect(historyPaginator(page)).toHaveCount(0);

    await page.locator('#userAuditSearchClear').click();
    await expect(page.locator('#userAuditSearch')).toHaveValue('');
    await expect(historyRows(page)).toHaveCount(Math.min(rows.length, HISTORY_PAGE_SIZE));
  });

  // a business domain admin has the USER_MANAGEMENT permission too: it sees the same History panel and may read the
  // audit log through the api
  test('a business domain admin sees the History panel and may read the log through the api', async ({browser}) => {
    const rows = auditRows(userId);
    await withUser(browser, BUSINESS_DOMAIN_ADMIN, async page => {
      await openUserEdit(page, TARGET);
      await openHistory(page);
      await expect(historyRows(page)).toHaveCount(Math.min(rows.length, HISTORY_PAGE_SIZE));
      await expect(historyRows(page).first().getByRole('cell').nth(3)).toHaveText(ACTION[rows[0].action as keyof typeof ACTION]);

      const api = await portalApiOf(page);
      const response = await getAuditLog(page.request, api, userId);
      expect(response.status()).toBe(200);
      expect((await response.json()).totalElements).toBe(rows.length);
    });
  });

  // the api returns the entries newest first, pages them and caps the page size at 100; an unknown user has an empty
  // log, and a request without a token is refused
  test('the api pages the log newest first and refuses anonymous requests', async ({adminPage: page, request}) => {
    const rows = auditRows(userId);
    const api = await portalApiOf(page);

    const all = await getAuditLog(page.request, api, userId, {size: 1000});
    expect(all.status()).toBe(200);
    const body = await all.json();
    expect(body.size, 'the page size is capped at 100').toBe(100);
    expect(body.totalElements).toBe(rows.length);
    expect(body.content.map((e: any) => e.action)).toEqual(rows.map(r => r.action));
    for (const entry of body.content) {
      expect(entry).toMatchObject({targetUserId: userId, targetEmail: EMAIL});
    }
    const times = body.content.map((e: any) => Date.parse(e.occurredAt));
    expect(times).toEqual([...times].sort((a, b) => b - a));
    // the context of a membership entry is resolved to the name of the data domain
    const membership = body.content.find((e: any) => e.action === 'DASHBOARD_GROUP_MEMBERSHIP_CHANGED');
    expect(membership).toMatchObject({contextKey: CONTEXT_KEY, contextName: DATA_DOMAIN});
    expect(membership.changes[0]).toMatchObject({field: 'dashboardGroups', contextKey: CONTEXT_KEY, contextName: DATA_DOMAIN});

    const second = await (await getAuditLog(page.request, api, userId, {page: 1, size: 1})).json();
    expect(second).toMatchObject({number: 1, size: 1, totalElements: rows.length});
    expect(second.content.map((e: any) => e.id)).toEqual([body.content[1].id]);

    // out of range values are clamped instead of failing
    const clamped = await (await getAuditLog(page.request, api, userId, {page: -1, size: 0})).json();
    expect(clamped).toMatchObject({number: 0, size: 1});

    const unknown = await getAuditLog(page.request, api, crypto.randomUUID());
    expect(unknown.status()).toBe(200);
    expect((await unknown.json()).totalElements).toBe(0);

    // the request fixture shares nothing with the logged in page: no token, no cookies
    const anonymous = await getAuditLog(request, api, userId, {anonymous: true});
    expect(anonymous.status()).toBe(401);
  });

  // the admin deletes the user; the audit log gets a "User deleted" entry and keeps the whole history of the user,
  // which the admin can still read through the api
  test('deleting the user writes a "User deleted" entry and keeps the history', async ({adminPage: page}) => {
    const before = auditRows(userId).length;
    await navigate(page, 'Administration', 'User management');
    await page.getByPlaceholder('Search').fill(EMAIL);
    const userRow = page.getByRole('row').filter({has: page.getByRole('cell', {name: EMAIL, exact: true})});
    await expect(userRow).toHaveCount(1);
    await userRow.locator('button.p-button-danger').click();
    await page.locator('.p-confirmdialog').getByRole('button', {name: 'Delete', exact: true}).click();
    await expect(userRow).toHaveCount(0);
    await dismissToasts(page);
    expect(portalUser(EMAIL)).toBeUndefined();

    const rows = auditRows(userId);
    expect(rows).toHaveLength(before + 1);
    expect(rows[0]).toMatchObject({action: 'USER_DELETED', source: 'MANUAL', actorEmail: ADMIN.email, targetEmail: EMAIL});

    const api = await portalApiOf(page);
    const response = await getAuditLog(page.request, api, userId, {size: 100});
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.totalElements).toBe(rows.length);
    // the whole life of the user, from its deletion back to its creation
    expect(body.content[0].action).toBe('USER_DELETED');
    expect(body.content.at(-1).action).toBe('USER_CREATED');
  });
});

// The audit log shows who changed whose access, so only users with USER_MANAGEMENT may read it. Every data domain role
// is refused: it cannot read the log of another user nor its own through the api, and the user edit page with the
// History panel sends it to the forbidden page.
test.describe('User audit log access', () => {
  const REFUSED: {role: string, user: User}[] = [
    {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
    {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
    {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
    {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  ];

  for (const {role, user} of REFUSED) {
    test.describe(role, () => {
      // one login per role
      test.describe.configure({mode: 'serial'});
      let page: Page;
      // the HelloDATA admin, whose log has entries on every installation
      let otherUserId: string;
      let ownId: string;

      test.beforeAll(async ({browser}) => {
        otherUserId = portalUserId(ADMIN.email)!;
        ownId = portalUserId(user.username)!;
        expect(otherUserId, `${ADMIN.email} is not in the portal`).toBeDefined();
        expect(ownId, `${user.username} is not in the portal`).toBeDefined();
        page = await browser.newPage();
        await openPortalAs(page, user.username, user.password);
      });

      test.afterAll(async () => {
        await page.close();
      });

      // GET /users/{id}/audit of another user with the token of this user is refused with 403 and returns no entries
      test('cannot read the audit log of another user through the api', async () => {
        const api = await portalApiOf(page);
        const response = await getAuditLog(page.request, api, otherUserId);
        expect(response.status()).toBe(403);
        expect(await response.text()).not.toContain('"occurredAt"');
      });

      // the user cannot read its own audit log either: the History is for the user management only
      test('cannot read its own audit log through the api', async () => {
        const api = await portalApiOf(page);
        const response = await getAuditLog(page.request, api, ownId);
        expect(response.status()).toBe(403);
        expect(await response.text()).not.toContain('"occurredAt"');
      });

      // opening the user edit page by url sends the user to the forbidden page, without a History panel and without any
      // request for the audit log
      test('is sent to the forbidden page instead of the History panel', async () => {
        const auditRequests: string[] = [];
        const onRequest = (r: {url(): string}) => {
          if (/\/users\/[^/]+\/audit(\?|$)/.test(r.url())) {
            auditRequests.push(r.url());
          }
        };
        page.on('request', onRequest);
        try {
          await page.goto(`user-management/edit/${otherUserId}`);
          await expect(page).toHaveURL(/\/forbidden$/);
          await expect(page.locator('app-not-allowed')).toHaveCount(1);
          await expect(historyPanel(page)).toHaveCount(0);
          expect(auditRequests, 'the portal requested the audit log for a user without USER_MANAGEMENT').toEqual([]);
        } finally {
          page.off('request', onRequest);
        }
      });
    });
  }
});
