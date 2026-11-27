import {APIRequestContext, Locator, Page} from '@playwright/test';
import {expect} from './fixtures';
import {sql} from './docker';
import {PORTAL_DB} from './instance';

// The user audit log (HELLODATA-4194): the portal api writes one row to user_audit_log for every change of a user's
// access (created, deleted, enabled, disabled, roles, dashboard groups) and the user edit page lists them in its History
// panel. Only users with the USER_MANAGEMENT permission (HelloDATA admins and business domain admins) may read it, through
// GET <portal api>/users/{userId}/audit.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function literal(value: string) {
  return `'${value.replaceAll("'", "''")}'`;
}

function uuid(value: string) {
  if (!UUID.test(value)) {
    throw new Error(`not a uuid: ${value}`);
  }
  return `'${value}'::uuid`;
}

/** The id of a user in the portal database, undefined if the portal does not know it. */
export function portalUserId(email: string): string | undefined {
  const [row] = sql(PORTAL_DB, `select id from user_ where lower(email) = lower(${literal(email)})`);
  return row?.[0];
}

export type AuditChange = {field: string, contextKey: string | null, before: unknown, after: unknown};

export type AuditRow = {
  action: string,
  source: string,
  actorEmail: string,
  targetEmail: string,
  changes: AuditChange[],
};

/** The audit log of a user as stored in the portal database, newest first. */
export function auditRows(userId: string): AuditRow[] {
  return sql(PORTAL_DB, `
      select action, source, coalesce(actor_email, ''), coalesce(target_email, ''), changes::text
      from user_audit_log
      where target_user_id = ${uuid(userId)}
      order by occurred_at desc`)
    .map(([action, source, actorEmail, targetEmail, changes]) => ({action, source, actorEmail, targetEmail, changes: JSON.parse(changes)}));
}

/** Removes the audit log of a user the tests created, so the log of the e2e installation does not grow with every run. */
export function deleteAuditRows(userId: string) {
  sql(PORTAL_DB, `delete from user_audit_log where target_user_id = ${uuid(userId)}`);
}

/** The portal api url (e.g. http://localhost:8081/api) and the bearer token of the user logged in on the page. */
export type PortalApi = {url: string, authorization: string};

/** Reloads the page and takes the portal api url and the token from the request for the profile of the current user. */
export async function portalApiOf(page: Page): Promise<PortalApi> {
  const profile = page.waitForRequest(r => r.url().endsWith('/users/current/profile') && !!r.headers()['authorization']);
  await page.reload();
  const request = await profile;
  return {
    url: request.url().slice(0, -'/users/current/profile'.length),
    authorization: request.headers()['authorization'],
  };
}

/** GET <portal api>/users/{userId}/audit, with the token of the given user or, without one, anonymously. */
export function getAuditLog(request: APIRequestContext, api: PortalApi, userId: string,
                            {page, size, anonymous}: {page?: number, size?: number, anonymous?: boolean} = {}) {
  const params: Record<string, number> = {};
  if (page !== undefined) {
    params.page = page;
  }
  if (size !== undefined) {
    params.size = size;
  }
  return request.get(`${api.url}/users/${userId}/audit`, {
    params,
    headers: anonymous ? {} : {Authorization: api.authorization},
    // a refused request must come back as its status, not as a redirect to the login page
    maxRedirects: 0,
  });
}

/** The paginator of the History table, hidden while it has no entries. */
export function historyPaginator(page: Page) {
  return page.locator('#userAuditTable .p-paginator');
}

/** The bold part of the change lines of a History row or domain card: what changed, e.g. "Role". */
export function changeLabels(rowOrCard: Locator) {
  return rowOrCard.locator('.change-line > strong.change-label');
}

/** The changes without a domain (e-mail, enabled ...), shown in the row itself. */
export function plainChangeLines(row: Locator) {
  return row.getByRole('cell').nth(5).locator('.change-line');
}

/** The expanded row under a History row, with one card per domain the entry changed. */
export function historyDetails(row: Locator) {
  return row.locator('xpath=following-sibling::tr[1][contains(@class, "change-details-row")]');
}

/** Opens the domain cards of a History row (the chevron, like in the user overview) unless they are open, returns them. */
export async function expandHistoryRow(row: Locator) {
  if (!await historyDetails(row).isVisible()) {
    await row.locator('.change-toggle button').click();
  }
  await expect(historyDetails(row)).toBeVisible();
  return historyDetails(row);
}

/** The domain cards of an expanded History row. */
export function changeCards(details: Locator) {
  return details.locator('section.change-card');
}

/** The card of a domain in an expanded History row, its heading is the domain name. */
export function changeCard(details: Locator, domain: string) {
  return changeCards(details).filter({has: details.page().locator('.change-domain').getByText(domain, {exact: true})});
}

/** The History panel of the user edit page. */
export function historyPanel(page: Page) {
  return page.locator('#userHistoryPanel');
}

/** The table of the History panel; its rows are the audit entries, newest first. */
export function historyTable(page: Page) {
  return page.locator('#userAuditTable');
}

/** The rows of the History table, one per entry (not the expanded rows with the domain cards). */
export function historyRows(page: Page) {
  return historyTable(page).locator('tbody tr:not(.change-details-row)');
}

/** Expands the History panel of the user edit page (it starts collapsed) and returns its table. */
export async function openHistory(page: Page) {
  await expect(historyPanel(page)).toBeVisible();
  if (!await historyTable(page).isVisible()) {
    // the toggler is a p-button host (named after the header) around the icon button, click the host
    await historyPanel(page).getByRole('button', {name: 'History', exact: true}).click();
  }
  await expect(historyTable(page)).toBeVisible();
  return historyTable(page);
}

/**
 * A change line: "Field: before -> after" for a change without a domain, "Field [Domain]: before -> after" (or a domain
 * and a line) for one in a domain, which the panel shows as "Field: before -> after" in the domain's card of the expanded
 * row.
 */
export type HistoryChange = string | {domain?: string, line: string | RegExp};

export type HistoryEntry = {
  who: string,
  source: string,
  action: string,
  context?: string,
  changes?: HistoryChange[],
};

function inDomain(change: HistoryChange): {domain?: string, line: string | RegExp} {
  if (typeof change !== 'string') {
    return change;
  }
  const match = /^(.+?) \[(.+?)]: (.*)$/s.exec(change);
  return match ? {domain: match[2], line: `${match[1]}: ${match[3]}`} : {line: change};
}

/** The columns of a History row: when, who, source, action, context, changes. */
export async function expectHistoryRow(row: Locator, entry: HistoryEntry) {
  const cells = row.getByRole('cell');
  await expect(cells.nth(0)).toHaveText(/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}$/);
  await expect(cells.nth(1)).toHaveText(entry.who);
  await expect(cells.nth(2)).toHaveText(entry.source);
  await expect(cells.nth(3)).toHaveText(entry.action);
  if (entry.context !== undefined) {
    await expect(cells.nth(4)).toHaveText(entry.context);
  }
  for (const change of entry.changes ?? []) {
    const {domain, line} = inDomain(change);
    const lines = domain ? changeCard(await expandHistoryRow(row), domain).locator('.change-line') : plainChangeLines(row);
    await expect(lines.filter({hasText: line})).toHaveCount(1);
  }
}

/** The History row of an action and source, there must be exactly one. */
export function historyRow(page: Page, action: string, source: string) {
  return historyRows(page)
    .filter({has: page.getByRole('cell', {name: action, exact: true})})
    .filter({has: page.getByRole('cell', {name: source, exact: true})});
}
