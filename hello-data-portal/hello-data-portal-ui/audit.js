#!/usr/bin/env node
const {spawn} = require("child_process");

let hasHigh = false;

const audit = spawn("yarn", ["audit", "--json"], {stdio: ["ignore", "pipe", "pipe"]});

const ALLOWLIST = {
  ghsa: [
    'GHSA-8r9q-7v3j-jr4g', // MCP TypeScript SDK ReDoS (Angular CLI transitive)
  ],
  cve: [
    'CVE-2026-0621',
  ],
  packages: [
    '@modelcontextprotocol/sdk',
  ],
};

function isAllowed(advisory) {
  return (
    ALLOWLIST.ghsa.includes(advisory.github_advisory_id) ||
    advisory.cves?.some(cve => ALLOWLIST.cve.includes(cve)) ||
    ALLOWLIST.packages.includes(advisory.module_name)
  );
}

audit.stdout.on("data", (data) => {
  const lines = data.toString().split("\n").map(l => l.trim()).filter(l => l.startsWith("{") && l.endsWith("}"));

  for (const line of lines) {
    try {
      const obj = JSON.parse(line);
      if (
        obj.type === "auditAdvisory" &&
        ["high", "critical"].includes(obj.data.advisory.severity)
      ) {
        const advisory = obj.data.advisory;

        if (isAllowed(advisory)) {
          console.log(
            `[INFO] [ALLOWLISTED] ${advisory.severity.toUpperCase()}: ${advisory.title} (${advisory.github_advisory_id})`
          );
          continue;
        }

        console.error(
          `[VULNERABILITY] ${advisory.severity.toUpperCase()}: ${advisory.title}`
        );
        console.error("Details:");
        console.error(obj);
        hasHigh = true;
      }
    } catch (e) {
      // Ignore parsing errors for malformed lines
    }
  }
});

audit.stderr.on("data", (data) => {
  process.stderr.write(data);
});

audit.on("close", (code) => {
  if (hasHigh) {
    console.error("High or critical vulnerabilities found! Failing build.");
    process.exit(1);
  } else {
    process.exit(0);
  }
});
