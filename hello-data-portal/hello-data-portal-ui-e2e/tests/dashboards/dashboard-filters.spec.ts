import {
  DATA_DOMAIN_ADMIN,
  DATA_DOMAIN_BUSINESS_SPECIALIST,
  DATA_DOMAIN_EDITOR,
  DATA_DOMAIN_VIEWER,
  expect,
  openPortalAs,
  test,
} from '../fixtures';
import {FrameLocator} from '@playwright/test';
import {changeDashboardAccessAsAdmin, expectDashboardListed, openDashboard} from '../superset';

// the embedded Superset dashboard shows the edit button and the filter bar settings (gear) only to users who may change it

/** The settings icon of the filter bar: "gear" up to Superset 5, "setting" since Superset 6. */
function filterBarSettings(dashboard: FrameLocator) {
  return dashboard.getByRole('img', {name: /^(gear|setting)$/});
}

// HELLODATA-703
for (const {role, user} of [
  {role: 'DATA_DOMAIN_ADMIN', user: DATA_DOMAIN_ADMIN},
  {role: 'DATA_DOMAIN_EDITOR', user: DATA_DOMAIN_EDITOR},
]) {
  // admins and editors may change the embedded dashboard and its filters
  test.describe(`Dashboard filters - ${role}`, () => {
    test.beforeEach(async ({page}) => {
      await openPortalAs(page, user.username, user.password);
    });

    // a dashboard admin or editor opens a dashboard and edits it: the Save button appears, then Discard leaves the edit
    // mode
    test('can edit the dashboard', async ({page}) => {
      const dashboard = await openDashboard(page);
      await dashboard.getByRole('button', {name: 'Edit dashboard', exact: true}).click();
      await expect(dashboard.getByRole('button', {name: 'Save', exact: true})).toBeVisible();
      // leave the edit mode without changing the shared dashboard
      await dashboard.getByRole('button', {name: 'Discard', exact: true}).click();
      await expect(dashboard.getByRole('button', {name: 'Edit dashboard', exact: true})).toBeVisible();
    });

    // a dashboard admin or editor opens the filter settings (gear) of a dashboard and sees the dialog to add and edit
    // filters
    test('can add and edit filters', async ({page}) => {
      const dashboard = await openDashboard(page);
      await filterBarSettings(dashboard).click();
      // the settings open a menu whose first entry opens the dialog: "Add or edit filters" up to Superset 5, "Add or
      // edit filters and controls" since Superset 6
      await dashboard.getByRole('menuitem', {name: /^Add or edit filters/}).click();

      const filters = dashboard.getByRole('dialog').filter({hasText: /Add (and|or) edit filters/});
      await expect(filters).toBeVisible();
      await expect(filters.getByRole('button', {name: /Add filter/i}).first()).toBeVisible();
      await expect(filters.getByRole('button', {name: 'Save', exact: true})).toBeEnabled();
      await filters.getByRole('button', {name: 'Cancel', exact: true}).click();
      await expect(filters).toBeHidden();
    });
  });
}

// HELLODATA-702
for (const {role, user} of [
  {role: 'DATA_DOMAIN_VIEWER', user: DATA_DOMAIN_VIEWER},
  {role: 'DATA_DOMAIN_BUSINESS_SPECIALIST', user: DATA_DOMAIN_BUSINESS_SPECIALIST},
]) {
  // viewers and business specialists with dashboard access may use the filters but not change them
  test.describe(`Dashboard filters - ${role}`, () => {
    test.beforeAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, true);
    });

    test.afterAll(async ({browser}) => {
      await changeDashboardAccessAsAdmin(browser, user, false);
    });

    // a viewer or business specialist opens a dashboard: the filters can be applied, but there is no Edit dashboard
    // button and no filter settings gear
    test('cannot edit the dashboard or its filters', async ({page}) => {
      await openPortalAs(page, user.username, user.password);
      await expectDashboardListed(page, true);
      const dashboard = await openDashboard(page);
      // the filter bar itself can be used, only its configuration is missing
      await expect(dashboard.getByRole('button', {name: 'Apply filters'})).toBeVisible();
      await expect(dashboard.getByRole('button', {name: 'Edit dashboard', exact: true})).toHaveCount(0);
      await expect(filterBarSettings(dashboard)).toHaveCount(0);
    });
  });
}
