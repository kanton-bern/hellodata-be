import {Page} from '@playwright/test';
import {expect, navigate, selectOption, test} from '../fixtures';
import {DATA_DOMAIN, SHOWCASE_DASHBOARD, SUPERSET_SYNC_TIMEOUT} from '../superset';
import {
  BATCH_TIMEOUT,
  batchEmail,
  CONTEXT_KEY,
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

type Assignment = {
  role: string,
  // the comment permissions to tick, the portal gives none to a viewer by default
  comments?: ('read' | 'write' | 'review')[],
  dashboards?: string[],
  rlsRoles?: string[],
};

// Walks through the four steps of the bulk assignments wizard (users, data domains, roles, summary), applies the
// assignment to the default data domain and returns once the result panel is shown.
async function bulkAssign(page: Page, emails: string[], assignment: Assignment) {
  await navigate(page, 'Administration', 'Bulk assignments');
  await expect(page).toHaveURL(/\/bulk-assignments$/);
  const next = page.getByRole('button', {name: 'Next', exact: true});

  // step 1 - the users are searched one by one, the selection is kept while the search changes
  const search = page.getByPlaceholder('Search');
  const selectedCount = page.locator('.selected-count');
  for (const [i, email] of emails.entries()) {
    await search.fill(email);
    const card = page.locator('.user-card').filter({hasText: email});
    await expect(card).toHaveCount(1);
    await card.click();
    await expect(selectedCount).toHaveText(new RegExp(`:\\s*${i + 1}$`));
  }
  await next.click();

  // step 2 - only the default data domain
  await page.locator('.domain-item').filter({hasText: DATA_DOMAIN}).click();
  await expect(page.locator('.selected-count')).toHaveText(/:\s*1$/);
  await next.click();

  // step 3 - the role of the domain with its comment permissions and, for a viewer, its dashboards and RLS groups
  await selectOption(page.locator(`p-select#roleSelect-${CONTEXT_KEY}`), assignment.role);
  for (const permission of assignment.comments ?? []) {
    await page.locator(`label[for="${permission}Comments-${CONTEXT_KEY}"]`).click();
    await expect(page.locator(`input#${permission}Comments-${CONTEXT_KEY}`)).toBeChecked();
  }
  for (const dashboard of assignment.dashboards ?? []) {
    await page.locator('.dashboard-selection label').filter({hasText: dashboard}).click();
  }
  for (const rlsRole of assignment.rlsRoles ?? []) {
    await page.locator(`label[for="rls-${CONTEXT_KEY}-${rlsRole}"]`).click();
  }
  await next.click();

  // step 4 - the summary lists the users and the data domain, applying it asks for a confirmation
  for (const email of emails) {
    await expect(page.locator('.flow-user-list').getByText(email)).toBeVisible();
  }
  await expect(page.locator('.domain-header')).toContainText(DATA_DOMAIN);
  await page.getByRole('button', {name: 'Apply Assignments', exact: true}).click();
  await page.locator('.p-confirmdialog').getByRole('button', {name: 'Yes', exact: true}).click();
  await expect(page.locator('.result-panel')).toBeVisible({timeout: BATCH_TIMEOUT});
}

// the result panel counts the users that were updated, skipped and failed
async function expectResult(page: Page, counts: {updated: number, skipped: number, failed: number}) {
  const value = (kind: string) => page.locator(`.result-item.${kind} .result-value`);
  await expect(value('success')).toHaveText(String(counts.updated));
  await expect(value('warning')).toHaveText(String(counts.skipped));
  await expect(value('error')).toHaveText(String(counts.failed));
}

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
