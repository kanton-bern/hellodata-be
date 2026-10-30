import {defineConfig, devices} from '@playwright/test';

const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './tests',
  // the specs share one portal instance and one admin user, so keep them serial
  fullyParallel: false,
  workers: 1,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  timeout: 60_000,
  expect: {timeout: 15_000},
  reporter: isCI
    ? [['list'], ['html', {open: 'never'}], ['junit', {outputFile: 'test-results/junit.xml'}]]
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
});
