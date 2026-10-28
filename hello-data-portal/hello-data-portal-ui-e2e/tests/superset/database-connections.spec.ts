import {Page} from '@playwright/test';
import {expect, test} from '../fixtures';
import {openSuperset} from '../superset';

// the postgres service of the docker compose installation, as reached from the Superset container
const DWH = {
  host: process.env.E2E_DWH_HOST ?? 'postgres',
  port: process.env.E2E_DWH_PORT ?? '5432',
  database: process.env.E2E_DWH_DATABASE ?? 'hellodata_product_development_default_data_domain_dwh',
  username: process.env.E2E_DWH_USERNAME ?? 'postgres',
  password: process.env.E2E_DWH_PASSWORD ?? 'postgres',
};

async function openDatabaseList(superset: Page) {
  await superset.goto(new URL('/databaseview/list/', superset.url()).toString());
  await expect(superset.getByRole('button', {name: 'Database'})).toBeVisible();
}

const NAME_PREFIX = 'e2e-dwh-';

function databaseRow(superset: Page, name: string | RegExp) {
  return superset.getByRole('row').filter({has: superset.getByRole('cell', {name, exact: true})});
}

/** Deletes the database connections whose name matches, e.g. the ones left behind by an aborted run. */
async function deleteDatabases(superset: Page, name: string | RegExp) {
  await openDatabaseList(superset);
  const rows = databaseRow(superset, name);
  for (let left = await rows.count(); left > 0; left--) {
    const row = rows.first();
    // the row actions only appear on hover
    await row.hover();
    await row.getByRole('button', {name: 'trash'}).click();
    const dialog = superset.getByRole('dialog');
    await dialog.getByRole('textbox').fill('DELETE');
    await dialog.getByRole('button', {name: 'Delete', exact: true}).click();
    await expect(rows).toHaveCount(left - 1);
  }
}

// HELLODATA-1325
test('admin adds a new database connection in Superset', async ({adminPage: page}) => {
  const superset = await openSuperset(page);
  await deleteDatabases(superset, new RegExp(`^${NAME_PREFIX}`));
  const name = `${NAME_PREFIX}${Date.now()}`;
  try {
    await superset.getByRole('button', {name: 'Database'}).click();
    const dialog = superset.getByRole('dialog');
    await expect(dialog).toContainText('Connect a database');

    await dialog.getByText('PostgreSQL', {exact: true}).click();
    await dialog.locator('input[name="host"]').fill(DWH.host);
    await dialog.locator('input[name="port"]').fill(DWH.port);
    await dialog.locator('input[name="database"]').fill(DWH.database);
    await dialog.locator('input[name="username"]').fill(DWH.username);
    await dialog.locator('input[name="password"]').fill(DWH.password);
    await dialog.locator('input[name="database_name"]').fill(name);
    // Superset tests the connection before it saves it
    await dialog.getByRole('button', {name: 'Connect', exact: true}).click();
    await dialog.getByRole('button', {name: 'Finish', exact: true}).click();
    await expect(dialog).toBeHidden();

    await openDatabaseList(superset);
    await expect(databaseRow(superset, name)).toBeVisible();
  } finally {
    await deleteDatabases(superset, name);
  }
});
