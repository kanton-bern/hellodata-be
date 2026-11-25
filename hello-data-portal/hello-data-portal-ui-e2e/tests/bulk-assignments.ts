import {Page} from '@playwright/test';
import {expect, navigate, selectOption} from './fixtures';
import {DATA_DOMAIN} from './superset';
import {BATCH_TIMEOUT, CONTEXT_KEY} from './batch-import';

// The bulk assignments wizard of the user management, used by the bulk assignment specs and by the user audit log spec.

export type Assignment = {
  role: string,
  // the comment permissions to tick, the portal gives none to a viewer by default
  comments?: ('read' | 'write' | 'review')[],
  dashboards?: string[],
  rlsRoles?: string[],
};

// Walks through the four steps of the bulk assignments wizard (users, data domains, roles, summary), applies the
// assignment to the default data domain and returns once the result panel is shown.
export async function bulkAssign(page: Page, emails: string[], assignment: Assignment) {
  // the wizard is not in the menu, the user management has a button for it
  await navigate(page, 'Administration', 'User management');
  await page.getByRole('button', {name: 'Bulk assignments', exact: true}).click();
  await expect(page).toHaveURL(/\/bulk-assignments$/);
  const next = page.getByRole('button', {name: 'Next', exact: true});

  // step 1 - the users are searched one by one, the selection is kept while the search changes
  // each step has its own counter; while the stepper moves on, the counters of both steps are in the page
  const selectedUsers = page.locator('.selected-count').filter({hasText: /^Selected users:/});
  const selectedDomains = page.locator('.selected-count').filter({hasText: /^Selected data domains:/});
  const search = page.getByPlaceholder('Search');
  for (const [i, email] of emails.entries()) {
    await search.fill(email);
    const card = page.locator('.user-card').filter({hasText: email});
    await expect(card).toHaveCount(1);
    await card.click();
    await expect(selectedUsers).toHaveText(new RegExp(`:\\s*${i + 1}$`));
  }
  await next.click();

  // step 2 - only the default data domain; wait until step 1 is gone, a click during the transition is lost
  await expect(selectedUsers).toHaveCount(0);
  await expect(selectedDomains).toBeVisible();
  await page.locator('.domain-item').filter({hasText: DATA_DOMAIN}).click();
  await expect(selectedDomains).toHaveText(/:\s*1$/);
  await next.click();
  await expect(selectedDomains).toHaveCount(0);

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
export async function expectResult(page: Page, counts: {updated: number, skipped: number, failed: number}) {
  const value = (kind: string) => page.locator(`.result-item.${kind} .result-value`);
  await expect(value('success')).toHaveText(String(counts.updated));
  await expect(value('warning')).toHaveText(String(counts.skipped));
  await expect(value('error')).toHaveText(String(counts.failed));
}
