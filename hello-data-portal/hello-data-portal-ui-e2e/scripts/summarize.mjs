#!/usr/bin/env node
// Turns the Playwright JSON report into summary.json (what was tested, against which images, with which result) and
// appends a Markdown version of it to the GitHub job summary. summary.json is the input of build-reports-site.mjs.
//
// node scripts/summarize.mjs --results test-results/results.json --images images.json --out summary.json
//
// The run is described by environment variables: HD_VERSION, E2E_BROWSER and E2E_AIRFLOW_VERSION, and the GITHUB_* variables of Actions.
// A missing results file (the stack did not start, so no test ran) gives a summary with the status 'error'.

import fs from 'node:fs';
import path from 'node:path';
import {parseArgs} from 'node:util';

const {values: args} = parseArgs({
  options: {
    results: {type: 'string', default: 'test-results/results.json'},
    images: {type: 'string'},
    out: {type: 'string', default: 'summary.json'},
    // the specs are read to find the Jira tickets in the comments above a test or its describe block
    'test-dir': {type: 'string', default: 'tests'},
  },
});

const STATUS = {expected: 'passed', unexpected: 'failed', flaky: 'flaky', skipped: 'skipped'};
// the lines of the spec files, read once
const sources = new Map();
const env = process.env;
const readJson = file => (file && fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : undefined);
const report = readJson(args.results);

const summary = {
  version: env.HD_VERSION || 'unknown',
  browser: env.E2E_BROWSER || 'chromium',
  // the Airflow the suite ran against (E2E_AIRFLOW_VERSION): 2 or 3
  airflow: env.E2E_AIRFLOW_VERSION || '2',
  repository: env.GITHUB_REPOSITORY,
  serverUrl: env.GITHUB_SERVER_URL || 'https://github.com',
  runId: env.GITHUB_RUN_ID || 'local',
  runAttempt: env.GITHUB_RUN_ATTEMPT || '1',
  commit: env.GITHUB_SHA,
  ref: env.GITHUB_REF_NAME,
  startedAt: report?.stats?.startTime ?? new Date().toISOString(),
  durationMs: Math.round(report?.stats?.duration ?? 0),
  images: (readJson(args.images) ?? []).filter(image => image.digest),
  tests: report ? collectTests(report) : [],
};
summary.stats = countStatuses(summary.tests);
summary.features = Object.entries(Object.groupBy(summary.tests, test => test.feature))
  .map(([name, tests]) => ({name, ...countStatuses(tests)}));
// flaky tests passed on the retry, so they do not fail the run (as in Playwright itself)
summary.status = !report || report.errors?.length ? 'error' : summary.stats.failed ? 'failed' : 'passed';
summary.errors = (report?.errors ?? []).map(error => firstLines(error.message));
if (!report) summary.errors.push(`No test results (${args.results}): the tests did not run.`);

fs.writeFileSync(args.out, JSON.stringify(summary, null, 2));
if (env.GITHUB_STEP_SUMMARY) fs.appendFileSync(env.GITHUB_STEP_SUMMARY, markdown(summary));
console.log(`${summary.status}: ${JSON.stringify(summary.stats)} -> ${args.out}`);

// --- Playwright JSON report -> flat list of tests -------------------------------------------------------------------

// Suites are nested: file -> describe -> describe ...; specs are the test() calls, each with one test per project.
function collectTests(report) {
  const tests = [];
  const walk = (suite, groups) => {
    // the describe blocks a test is in, with their comments (a ticket named above one applies to all its tests)
    const suiteGroups = suite.line ? [...groups, {title: suite.title, ...commentAbove(suite.file, suite.line)}] : groups;
    for (const spec of suite.specs ?? []) {
      const comment = commentAbove(spec.file, spec.line);
      for (const test of spec.tests) {
        const results = test.results ?? [];
        const last = results.at(-1);
        tests.push({
          // the id of the test in the Playwright HTML report (report/#?testId=<id>)
          id: spec.id,
          feature: spec.file.split(/[\\/]/)[0].replace(/\.spec\.ts$/, ''),
          file: spec.file,
          line: spec.line,
          group: suiteGroups.map(group => group.title).join(' › '),
          groupDescription: suiteGroups.map(group => group.description).filter(Boolean).join('\n') || undefined,
          title: spec.title,
          description: comment.description,
          tags: spec.tags ?? [],
          jira: [...new Set([...suiteGroups.flatMap(group => group.jira), ...comment.jira])],
          // the test.step() calls of the last attempt
          steps: (last?.steps ?? []).map(step => ({title: step.title, failed: !!step.error})),
          status: STATUS[test.status] ?? test.status,
          durationMs: results.reduce((sum, result) => sum + result.duration, 0),
          retries: Math.max(results.length - 1, 0),
          error: test.status === 'unexpected' && last?.error ? firstLines(last.error.message) : undefined,
        });
      }
    }
    for (const child of suite.suites ?? []) walk({file: suite.file, ...child}, suiteGroups);
  };
  // the file suites have no line, their title is the file name
  for (const fileSuite of report.suites) walk({...fileSuite, line: undefined}, []);
  return tests;
}

function countStatuses(tests) {
  const counts = {total: tests.length, passed: 0, failed: 0, flaky: 0, skipped: 0};
  for (const test of tests) counts[test.status]++;
  return counts;
}

// The comment right above a test or describe block describes it, and names its Jira tickets, e.g.
// "// HELLODATA-2946". Both line comments and a /** ... */ block are read.
function commentAbove(file, line) {
  if (!sources.has(file)) {
    const source = path.join(args['test-dir'], file);
    sources.set(file, fs.existsSync(source) ? fs.readFileSync(source, 'utf8').split('\n') : []);
  }
  const lines = sources.get(file);
  const comment = [];
  let i = line - 2;
  if (/\*\/\s*$/.test(lines[i] ?? '')) {
    for (; i >= 0; i--) {
      comment.unshift(lines[i]);
      if (/^\s*\/\*/.test(lines[i])) break;
    }
  } else {
    for (; i >= 0 && /^\s*\/\//.test(lines[i]); i--) comment.unshift(lines[i]);
  }
  const text = comment
    .map(l => l.replace(/^\s*(\/\*\*?|\/\/|\*\/|\*)\s?/, '').replace(/\s*\*\/\s*$/, '').trim())
    .filter(Boolean);
  return {
    // a line that only names tickets is no description
    description: text.filter(l => !/^(HELLODATA-\d+[\s,]*)+$/.test(l)).join(' ') || undefined,
    jira: text.join(' ').match(/HELLODATA-\d+/g) ?? [],
  };
}

function firstLines(message = '') {
  // eslint-disable-next-line no-control-regex
  const plain = message.replace(/\u001b\[[0-9;]*m/g, '');
  return plain.split('\n').slice(0, 6).join('\n').slice(0, 800);
}

// --- GitHub job summary ---------------------------------------------------------------------------------------------

function markdown(summary) {
  const icon = {passed: '✅', failed: '❌', error: '⚠️'}[summary.status];
  const {stats} = summary;
  const lines = [
    `## ${icon} E2E ${summary.version} (${summary.browser}, Airflow ${summary.airflow}): ${summary.status}`,
    '',
    `${stats.total} tests: ${stats.passed} passed, ${stats.failed} failed, ${stats.flaky} flaky, ${stats.skipped} skipped`
    + ` in ${Math.round(summary.durationMs / 60_000)} min`,
    '',
    '| Feature | Passed | Failed | Flaky | Skipped |',
    '|---|---:|---:|---:|---:|',
    ...summary.features.map(f => `| ${f.name} | ${f.passed} | ${f.failed} | ${f.flaky} | ${f.skipped} |`),
    '',
  ];
  const failed = summary.tests.filter(test => test.status === 'failed');
  if (failed.length) {
    lines.push('### Failed tests', '');
    for (const test of failed) {
      lines.push(`- **${[test.group, test.title].filter(Boolean).join(' › ').replace(/</g, "&lt;")}** (\`${test.file}:${test.line}\`)`);
      if (test.error) lines.push('  ```', ...test.error.split('\n').map(line => `  ${line}`), '  ```');
    }
    lines.push('');
  }
  for (const error of summary.errors) lines.push(`> ${error.replace(/\n/g, '\n> ')}`, '');
  return lines.join('\n') + '\n';
}
