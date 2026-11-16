#!/usr/bin/env node
// Adds the report of an e2e run to the static reports site (the e2e/ folder of the gh-pages branch, served by GitHub
// Pages next to the docs) and rebuilds its pages. The pages are always rebuilt from the summary.json files in the site,
// so a run can be added again (re-run of a workflow), runs can be removed by hand, and a new layout applies to all.
//
// node scripts/build-reports-site.mjs --site <dir> [--summary summary.json --report playwright-report]
//
// <site>/index.html                               all versions and their runs
// <site>/index.json                               the same as data (the summaries without their tests)
// <site>/<version>/index.html                     the runs of a version and what its latest run tested
// <site>/<version>/<setup>/<run>/index.html       what was tested in a run and its result
// <site>/<version>/<setup>/<run>/report/          the Playwright HTML report of the run
// The setup is the browser, with an -airflow3 suffix for runs against Airflow 3 only (see setupDir()).

import fs from 'node:fs';
import path from 'node:path';
import {parseArgs} from 'node:util';

const {values: args} = parseArgs({
  options: {
    site: {type: 'string'},
    summary: {type: 'string'},
    report: {type: 'string'},
    // runs kept per version (all browsers together), and versions kept in all; older ones are deleted from the site
    'keep-runs': {type: 'string', default: '5'},
    'keep-versions': {type: 'string', default: '30'},
  },
});
if (!args.site) throw new Error('--site is required');

const site = args.site;
const safe = name => String(name).replace(/[^A-Za-z0-9._-]/g, '_');

if (args.summary) addRun(JSON.parse(fs.readFileSync(args.summary, 'utf8')), args.report);
const runs = prune(loadRuns());
removeEmptyFolders();
for (const run of runs) fs.writeFileSync(path.join(site, run.path, 'index.html'), runPage(run));
for (const versionRuns of Object.values(Object.groupBy(runs, run => run.versionDir))) {
  fs.writeFileSync(path.join(site, versionRuns[0].versionDir, 'index.html'), versionPage(versionRuns));
}
fs.writeFileSync(path.join(site, 'index.html'), indexPage(runs));
fs.writeFileSync(path.join(site, 'index.json'), JSON.stringify(runs.map(({tests, ...run}) => run), null, 2));
console.log(`${runs.length} runs in ${site}`);

function addRun(summary, report) {
  const dir = path.join(site, safe(summary.version), safe(setupDir(summary)),
    safe(`${summary.runId}-${summary.runAttempt}`));
  fs.rmSync(dir, {recursive: true, force: true});
  fs.mkdirSync(dir, {recursive: true});
  fs.writeFileSync(path.join(dir, 'summary.json'), JSON.stringify(summary, null, 2));
  if (report && fs.existsSync(path.join(report, 'index.html'))) {
    fs.cpSync(report, path.join(dir, 'report'), {recursive: true});
  }
  console.log(`added ${path.relative(site, dir)}`);
}

function loadRuns() {
  const runs = [];
  for (const file of fs.globSync('*/*/*/summary.json', {cwd: site})) {
    const dir = path.dirname(file).split(path.sep);
    runs.push({
      ...JSON.parse(fs.readFileSync(path.join(site, file), 'utf8')),
      path: dir.join('/'),
      versionDir: dir[0],
      hasReport: fs.existsSync(path.join(site, ...dir, 'report', 'index.html')),
    });
  }
  return runs.sort((a, b) => b.startedAt.localeCompare(a.startedAt));
}

// runs are sorted newest first, so the first ones seen of a version are kept
function prune(runs) {
  const keepRuns = Number(args['keep-runs']);
  const keepVersions = Number(args['keep-versions']);
  const versions = [...new Set(runs.map(run => run.version))].slice(0, keepVersions);
  const seen = new Map();
  return runs.filter(run => {
    // runs are sorted newest first, so the first keepRuns runs of a version are its latest ones
    seen.set(run.version, (seen.get(run.version) ?? 0) + 1);
    if (versions.includes(run.version) && seen.get(run.version) <= keepRuns) return true;
    fs.rmSync(path.join(site, run.path), {recursive: true, force: true});
    console.log(`pruned ${run.path}`);
    return false;
  });
}

// the folders of pruned versions and browsers still hold their index.html
function removeEmptyFolders() {
  for (const dir of fs.globSync('*/', {cwd: site})) {
    if (!fs.globSync('*/*/summary.json', {cwd: path.join(site, dir)}).length) {
      fs.rmSync(path.join(site, dir), {recursive: true, force: true});
    }
  }
}

// The browser and the Airflows a run tested (e.g. "2,3"); runs before Airflow 3 was tested have no airflow and tested
// Airflow 2.
function setup(run) {
  return `${run.browser}, Airflow ${(run.airflow ?? '2').split(',').join(' + ')}`;
}

// the folder of a run's setup: the browser, plus -airflow3 for a run against Airflow 3 only
function setupDir(summary) {
  return (summary.airflow ?? '2').split(',').includes('2') ? summary.browser : `${summary.browser}-airflow3`;
}

// --- pages ----------------------------------------------------------------------------------------------------------

// one row per tested version with the result of its latest run in each browser; the version page has all its runs
// (flaky tests passed on the retry, so they count as passed)
function indexPage(runs) {
  const rows = Object.values(Object.groupBy(runs, run => run.versionDir)).map(versionRuns => {
    const [latest] = versionRuns;
    const latestPerBrowser = Object.values(Object.groupBy(versionRuns, setup)).map(([run]) => run);
    return `
      <tr>
        <td><a class="version" href="${esc(latest.versionDir)}/">${esc(latest.version)}</a></td>
        <td class="results">${latestPerBrowser.map(run => `
          <div><a href="${esc(run.path)}/">${badge(run.status)} ${esc(setup(run))}</a>
            <span class="muted small">${run.stats.passed + run.stats.flaky}/${run.stats.total} passed</span></div>`)
          .join('')}</td>
        <td>${date(latest.startedAt)}</td>
        <td>${commitLink(latest)}</td>
        <td class="num">${versionRuns.length}</td>
      </tr>`;
  }).join('');
  return page('HelloDATA E2E Reports', `
    <nav><a href="../">← HelloDATA docs</a></nav>
    <header>
      <h1>HelloDATA E2E Reports</h1>
      <p class="muted">The HelloDATA versions (docker image tags) the Playwright regression suite ran against, last tested
        first. Open a version for all its runs and what was tested.</p>
    </header>
    ${rows ? `
    <div class="table"><table>
      <thead><tr><th>Version</th><th>Latest result per browser and Airflow</th><th>Last tested</th><th>Commit</th>
        <th class="num">Runs</th></tr></thead>
      <tbody>${rows}</tbody>
    </table></div>` : '<p>No version tested yet.</p>'}`);
}

// the latest run of the version, with all its runs on top
function versionPage(versionRuns) {
  const [latest] = versionRuns;
  return page(`E2E ${latest.version}`, `
    <nav><a href="../">← All versions</a></nav>
    <header><h1>${esc(latest.version)}</h1></header>
    <h2>Runs</h2>
    ${runsTable(versionRuns, '../')}
    <h2>Latest run <span class="muted">${esc(setup(latest))}, ${date(latest.startedAt)}</span></h2>
    ${runDetails(latest, `${latest.path.slice(latest.versionDir.length + 1)}/`)}`);
}

function runPage(run) {
  return page(`E2E ${run.version} ${setup(run)}`, `
    <nav class="links"><a href="../../">← ${esc(run.version)}</a><a href="../../../">All versions</a></nav>
    <header><h1>${esc(run.version)} <span class="muted">${esc(setup(run))}</span> ${badge(run.status)}</h1></header>
    ${runDetails(run, '')}`);
}

// links of the table are relative to the site, after the prefix
function runsTable(runs, prefix) {
  return `
    <div class="table"><table>
      <thead><tr><th>Status</th><th>Setup</th><th>Started</th><th class="num">Passed</th><th class="num">Failed</th>
        <th class="num">Flaky</th><th class="num">Skipped</th><th>Commit</th><th>Links</th></tr></thead>
      <tbody>${runs.map(run => `
        <tr>
          <td>${badge(run.status)}</td>
          <td>${esc(setup(run))}</td>
          <td>${date(run.startedAt)}</td>
          <td class="num">${run.stats.passed}</td>
          <td class="num">${run.stats.failed}</td>
          <td class="num">${run.stats.flaky}</td>
          <td class="num">${run.stats.skipped}</td>
          <td>${commitLink(run)}</td>
          <td class="links"><a href="${esc(prefix + run.path)}/">Summary</a>
            ${run.hasReport ? `<a href="${esc(prefix + run.path)}/report/">Full report</a>` : ''}
            ${workflowLink(run)}</td>
        </tr>`).join('')}
      </tbody>
    </table></div>`;
}

// what the run tested; the links to its files are relative to the run folder, after the prefix
function runDetails(run, prefix) {
  const groups = Object.groupBy(run.tests, test => test.feature);
  const features = run.features.map(f => `
    <tr><td><a href="#${esc(f.name)}">${esc(f.name)}</a></td><td class="num">${f.passed}</td>
      <td class="num">${f.failed}</td><td class="num">${f.flaky}</td><td class="num">${f.skipped}</td></tr>`).join('');
  const tests = Object.entries(groups).map(([feature, featureTests]) => `
    <section id="${esc(feature)}">
      <h3>${esc(feature)}</h3>
      <div class="table"><table>
        <thead><tr><th>Status</th><th>Test</th><th>Jira</th><th class="num">Duration</th></tr></thead>
        <tbody>${Object.values(Object.groupBy(featureTests, test => `${test.file} ${test.group}`)).map(groupTests => `
          ${groupTests[0].group ? `<tr class="group"><td colspan="4"><strong>${esc(groupTests[0].group)}</strong>
            ${groupTests[0].groupDescription ? `<div class="muted">${esc(groupTests[0].groupDescription)}</div>` : ''}
          </td></tr>` : ''}
          ${groupTests.map(test => testRow(test, run, prefix)).join('')}`).join('')}
        </tbody>
      </table></div>
    </section>`).join('');
  const images = run.images.map(image => `
    <tr><td>${esc(image.image)}</td><td><code>${esc(image.digest.split('@')[1] ?? image.digest)}</code></td></tr>`)
    .join('');
  const errors = run.errors.map(error => `<pre class="error">${esc(error)}</pre>`).join('');
  return `
    <dl>
      <dt>Status</dt><dd>${badge(run.status)}</dd>
      <dt>Started</dt><dd>${date(run.startedAt)} (${Math.round(run.durationMs / 60_000)} min)</dd>
      <dt>Tests</dt><dd>${run.stats.total}: ${run.stats.passed} passed, ${run.stats.failed} failed,
        ${run.stats.flaky} flaky, ${run.stats.skipped} skipped</dd>
      <dt>Tests from</dt><dd>${esc(run.ref ?? '')} ${commitLink(run)}</dd>
      <dt>Links</dt><dd class="links">${run.hasReport ? `<a href="${esc(prefix)}report/">Full Playwright report</a>` : ''}
        ${prefix ? `<a href="${esc(prefix)}">Permalink of this run</a>` : ''}
        ${workflowLink(run, 'Workflow run (logs, traces)')}</dd>
    </dl>
    ${errors}
    <h2>Features</h2>
    <div class="table"><table>
      <thead><tr><th>Feature</th><th class="num">Passed</th><th class="num">Failed</th><th class="num">Flaky</th>
        <th class="num">Skipped</th></tr></thead>
      <tbody>${features}</tbody>
    </table></div>
    <h2>Tests</h2>
    ${tests || '<p>No test ran.</p>'}
    <h2>Tested images</h2>
    <div class="table"><table>
      <thead><tr><th>Image</th><th>Digest</th></tr></thead>
      <tbody>${images || '<tr><td colspan="2">Not recorded</td></tr>'}</tbody>
    </table></div>`;
}

function testRow(test, run, prefix) {
  // the HTML report shows every action of the test (clicks, navigations, assertions) and its trace
  const details = run.hasReport && test.id ? `<a href="${esc(`${prefix}report/#?testId=${test.id}`)}">all actions in the report</a>` : '';
  const steps = test.steps?.length ? `<ol class="steps small">${test.steps.map(step =>
    `<li${step.failed ? ' class="failed"' : ''}>${esc(step.title)}</li>`).join('')}</ol>` : '';
  return `
    <tr>
      <td>${badge(test.status)}${test.retries ? ` <span class="muted">${test.retries}× retried</span>` : ''}</td>
      <td><div class="title">${esc(test.title)}</div>
        ${test.description ? `<div class="description">${esc(test.description)}</div>` : ''}
        ${steps}
        <div class="muted small links">${esc(test.file)}:${test.line} ${details}</div>
        ${test.error ? `<pre>${esc(test.error)}</pre>` : ''}</td>
      <td class="nowrap">${test.jira.map(esc).join('<br>')}</td>
      <td class="num">${(test.durationMs / 1000).toFixed(1)} s</td>
    </tr>`;
}

function page(title, body) {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
<style>
  :root { --bg: #ffffff; --fg: #1f2328; --muted: #656d76; --border: #d0d7de; --row: #f6f8fa; --link: #0969da;
    --passed: #1a7f37; --failed: #cf222e; --flaky: #9a6700; --skipped: #656d76; --error: #8250df; }
  @media (prefers-color-scheme: dark) { :root { --bg: #0d1117; --fg: #e6edf3; --muted: #8d96a0; --border: #30363d;
    --row: #161b22; --link: #4493f8; --passed: #3fb950; --failed: #f85149; --flaky: #d29922; --skipped: #8d96a0;
    --error: #a371f7; } }
  * { box-sizing: border-box; }
  body { margin: 0 auto; max-width: 1200px; padding: 24px 16px; background: var(--bg); color: var(--fg);
    font: 14px/1.5 system-ui, -apple-system, "Segoe UI", sans-serif; }
  a { color: var(--link); text-decoration: none; } a:hover { text-decoration: underline; }
  h1 { font-size: 24px; margin: 0 0 8px; } h2 { font-size: 18px; margin: 32px 0 8px; } h3 { font-size: 15px; margin: 20px 0 8px; }
  .muted { color: var(--muted); font-weight: normal; } .small { font-size: 12px; }
  .table { overflow-x: auto; }
  table { width: 100%; border-collapse: collapse; }
  th, td { text-align: left; vertical-align: top; padding: 6px 10px; border-bottom: 1px solid var(--border); }
  th { font-size: 12px; color: var(--muted); font-weight: 600; } tbody tr:hover { background: var(--row); }
  .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
  .links a { margin-right: 12px; white-space: nowrap; }
  .links.small a { margin: 0 0 0 8px; }
  .version { font-weight: 600; white-space: nowrap; }
  .results div { white-space: nowrap; } .results div + div { margin-top: 4px; } .results a { color: var(--fg); }
  tr.group td { background: var(--row); border-bottom: 1px solid var(--border); padding-top: 10px; }
  .title { font-weight: 600; } .description { margin: 2px 0; max-width: 75ch; }
  .steps { margin: 4px 0; padding-left: 20px; color: var(--muted); } .steps .failed { color: var(--failed); }
  .badge { display: inline-block; padding: 0 8px; border-radius: 10px; font-size: 12px; font-weight: 600;
    color: var(--bg); background: var(--status); }
  pre { white-space: pre-wrap; word-break: break-word; font-size: 12px; background: var(--row); padding: 8px;
    border-radius: 6px; margin: 6px 0 0; }
  pre.error { border-left: 3px solid var(--error); }
  dl { display: grid; grid-template-columns: max-content 1fr; gap: 4px 16px; margin: 16px 0 0; }
  dt { color: var(--muted); } dd { margin: 0; }
  code { font-size: 12px; }
  td > code { word-break: break-all; } .nowrap { white-space: nowrap; }
</style>
</head>
<body>
${body}
</body>
</html>
`;
}

function badge(status) {
  return `<span class="badge" style="--status: var(--${esc(status)})">${esc(status)}</span>`;
}

function commitLink(run) {
  if (!run.commit || !run.repository) return '';
  return `<a class="nowrap" href="${esc(`${run.serverUrl}/${run.repository}/commit/${run.commit}`)}"><code>${esc(run.commit.slice(0, 8))}</code></a>`;
}

function workflowLink(run, label = 'Workflow run') {
  if (!run.repository || run.runId === 'local') return '';
  return `<a href="${esc(`${run.serverUrl}/${run.repository}/actions/runs/${run.runId}/attempts/${run.runAttempt}`)}">${label}</a>`;
}

function date(iso) {
  return `<span class="nowrap">${esc(iso.replace('T', ' ').slice(0, 16))} UTC</span>`;
}

function esc(value) {
  return String(value).replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'})[c]);
}
