#!/usr/bin/env node
// Adds the report of an e2e run to the static reports site (a checkout of the reports repository, served by GitHub
// Pages) and rebuilds its pages. Run pages and the index are always rebuilt from the summary.json files in the site, so
// a run can be added again (re-run of a workflow), runs can be removed by hand, and a new page layout applies to all.
//
// node scripts/build-reports-site.mjs --site <checkout> [--summary summary.json --report playwright-report]
//
// <site>/index.html                                      all versions and their runs
// <site>/index.json                                      the same as data (the summaries without their tests)
// <site>/reports/<version>/<browser>/<run>/index.html    what was tested in the run and its result
// <site>/reports/<version>/<browser>/<run>/report/       the Playwright HTML report of the run

import fs from 'node:fs';
import path from 'node:path';
import {parseArgs} from 'node:util';

const {values: args} = parseArgs({
  options: {
    site: {type: 'string'},
    summary: {type: 'string'},
    report: {type: 'string'},
    // runs kept per version and browser, and versions kept in all; older ones are deleted from the site
    'keep-runs': {type: 'string', default: '5'},
    'keep-versions': {type: 'string', default: '50'},
  },
});
if (!args.site) throw new Error('--site is required');

const reportsDir = path.join(args.site, 'reports');
const safe = name => String(name).replace(/[^A-Za-z0-9._-]/g, '_');

if (args.summary) addRun(JSON.parse(fs.readFileSync(args.summary, 'utf8')), args.report);
const runs = prune(loadRuns());
for (const run of runs) fs.writeFileSync(path.join(reportsDir, run.path, 'index.html'), runPage(run));
fs.writeFileSync(path.join(args.site, 'index.html'), indexPage(runs));
fs.writeFileSync(path.join(args.site, 'index.json'),
  JSON.stringify(runs.map(({tests, ...run}) => run), null, 2));
// without it GitHub Pages runs Jekyll, which drops the files of the Playwright report that start with an underscore
fs.writeFileSync(path.join(args.site, '.nojekyll'), '');
console.log(`${runs.length} runs in ${args.site}`);

function addRun(summary, report) {
  const dir = path.join(reportsDir, safe(summary.version), safe(summary.browser),
    safe(`${summary.runId}-${summary.runAttempt}`));
  fs.rmSync(dir, {recursive: true, force: true});
  fs.mkdirSync(dir, {recursive: true});
  fs.writeFileSync(path.join(dir, 'summary.json'), JSON.stringify(summary, null, 2));
  if (report && fs.existsSync(path.join(report, 'index.html'))) {
    fs.cpSync(report, path.join(dir, 'report'), {recursive: true});
  }
  console.log(`added ${path.relative(args.site, dir)}`);
}

function loadRuns() {
  const runs = [];
  for (const file of fs.globSync('*/*/*/summary.json', {cwd: reportsDir})) {
    const dir = path.dirname(file);
    runs.push({
      ...JSON.parse(fs.readFileSync(path.join(reportsDir, file), 'utf8')),
      path: dir.split(path.sep).join('/'),
      hasReport: fs.existsSync(path.join(reportsDir, dir, 'report', 'index.html')),
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
    const key = `${run.version}/${run.browser}`;
    seen.set(key, (seen.get(key) ?? 0) + 1);
    if (versions.includes(run.version) && seen.get(key) <= keepRuns) return true;
    fs.rmSync(path.join(reportsDir, run.path), {recursive: true, force: true});
    console.log(`pruned ${run.path}`);
    return false;
  });
}

// --- pages ----------------------------------------------------------------------------------------------------------

function indexPage(runs) {
  const byVersion = Object.groupBy(runs, run => run.version);
  const sections = Object.entries(byVersion).map(([version, versionRuns]) => `
    <section>
      <h2>${esc(version)}</h2>
      <div class="table"><table>
        <thead><tr><th>Status</th><th>Browser</th><th>Started</th><th class="num">Passed</th><th class="num">Failed</th>
          <th class="num">Flaky</th><th class="num">Skipped</th><th>Commit</th><th>Links</th></tr></thead>
        <tbody>${versionRuns.map(run => `
          <tr>
            <td>${badge(run.status)}</td>
            <td>${esc(run.browser)}</td>
            <td>${date(run.startedAt)}</td>
            <td class="num">${run.stats.passed}</td>
            <td class="num">${run.stats.failed}</td>
            <td class="num">${run.stats.flaky}</td>
            <td class="num">${run.stats.skipped}</td>
            <td>${commitLink(run)}</td>
            <td class="links"><a href="reports/${run.path}/">Summary</a>
              ${run.hasReport ? `<a href="reports/${run.path}/report/">Full report</a>` : ''}
              ${workflowLink(run)}</td>
          </tr>`).join('')}
        </tbody>
      </table></div>
    </section>`).join('');
  return page('HelloDATA E2E Reports', `
    <header>
      <h1>HelloDATA E2E Reports</h1>
      <p class="muted">Playwright regression runs of the portal against the docker compose installation, grouped by the
        tested image tag, newest first.</p>
    </header>
    ${sections || '<p>No runs yet.</p>'}`);
}

function runPage(run) {
  const groups = Object.groupBy(run.tests, test => test.feature);
  const features = run.features.map(f => `
    <tr><td><a href="#${esc(f.name)}">${esc(f.name)}</a></td><td class="num">${f.passed}</td>
      <td class="num">${f.failed}</td><td class="num">${f.flaky}</td><td class="num">${f.skipped}</td></tr>`).join('');
  const tests = Object.entries(groups).map(([feature, featureTests]) => `
    <section id="${esc(feature)}">
      <h3>${esc(feature)}</h3>
      <div class="table"><table>
        <thead><tr><th>Status</th><th>Test</th><th>Jira</th><th class="num">Duration</th></tr></thead>
        <tbody>${featureTests.map(test => `
          <tr>
            <td>${badge(test.status)}${test.retries ? ` <span class="muted">${test.retries}× retried</span>` : ''}</td>
            <td>${esc(test.title)}<div class="muted small">${esc(test.file)}:${test.line}</div>
              ${test.error ? `<pre>${esc(test.error)}</pre>` : ''}</td>
            <td class="nowrap">${test.jira.map(esc).join('<br>')}</td>
            <td class="num">${(test.durationMs / 1000).toFixed(1)} s</td>
          </tr>`).join('')}
        </tbody>
      </table></div>
    </section>`).join('');
  const images = run.images.map(image => `
    <tr><td>${esc(image.image)}</td><td><code>${esc(image.digest.split('@')[1] ?? image.digest)}</code></td></tr>`)
    .join('');
  const errors = run.errors.map(error => `<pre class="error">${esc(error)}</pre>`).join('');
  return page(`E2E ${run.version} ${run.browser}`, `
    <nav><a href="../../../../">← All reports</a></nav>
    <header>
      <h1>${esc(run.version)} <span class="muted">${esc(run.browser)}</span> ${badge(run.status)}</h1>
      <dl>
        <dt>Started</dt><dd>${date(run.startedAt)} (${Math.round(run.durationMs / 60_000)} min)</dd>
        <dt>Tests</dt><dd>${run.stats.total}: ${run.stats.passed} passed, ${run.stats.failed} failed,
          ${run.stats.flaky} flaky, ${run.stats.skipped} skipped</dd>
        <dt>Tests from</dt><dd>${esc(run.ref ?? '')} ${commitLink(run)}</dd>
        <dt>Links</dt><dd class="links">${run.hasReport ? '<a href="report/">Full Playwright report</a>' : ''}
          ${workflowLink(run, 'Workflow run (logs, traces)')}</dd>
      </dl>
    </header>
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
    </table></div>`);
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
