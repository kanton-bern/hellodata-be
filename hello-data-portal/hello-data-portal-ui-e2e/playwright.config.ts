import {defineConfig, devices} from '@playwright/test';
import type {AirflowVersion, WorkerFixtures} from './tests/fixtures';

const isCI = !!process.env.CI;

// The Airflows the installation runs, e.g. "2,3" for the e2e stack with compose-airflow3.yaml. Each one is a project:
// the first runs the whole suite, the others only the specs tagged @airflow (Orchestration, pipeline status, isolation
// of the DAGs), so the rest of the suite runs once.
const AIRFLOW_VERSIONS = (process.env.E2E_AIRFLOW_VERSIONS || '2').split(',').map(version => version.trim());
for (const version of AIRFLOW_VERSIONS) {
  if (version !== '2' && version !== '3') {
    throw new Error(`E2E_AIRFLOW_VERSIONS must list 2 and/or 3 (e.g. "2,3"), not ${process.env.E2E_AIRFLOW_VERSIONS}`);
  }
}

export default defineConfig<{}, WorkerFixtures>({
  testDir: './tests',
  // the specs share one portal instance and one admin user, so keep them serial
  fullyParallel: false,
  workers: 1,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  timeout: 60_000,
  expect: {timeout: 15_000},
  reporter: isCI
    // results.json is the input of scripts/summarize.mjs, which builds the summary of the published report
    ? [['list'], ['html', {open: 'never'}], ['junit', {outputFile: 'test-results/junit.xml'}],
      ['json', {outputFile: 'test-results/results.json'}]]
    : [['list'], ['html', {open: 'never'}]],
  use: {
    ...devices['Desktop Chrome'],
    // e.g. 'msedge' to run the suite in Microsoft Edge instead of the bundled Chromium
    channel: process.env.E2E_BROWSER_CHANNEL || undefined,
    // trailing slash matters: page.goto('home') resolves to <base>/home
    baseURL: (process.env.E2E_BASE_URL ?? 'http://localhost:8080/app').replace(/\/?$/, '/'),
    viewport: {width: 1600, height: 1000},
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: AIRFLOW_VERSIONS.map((version, index) => ({
    name: `airflow${version}`,
    use: {airflowVersion: version as AirflowVersion},
    grep: index === 0 ? undefined : /@airflow/,
  })),
});
