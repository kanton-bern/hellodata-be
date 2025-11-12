#!/usr/bin/env node
const {spawn} = require("child_process");

let hasHigh = false;

const audit = spawn("yarn", ["audit", "--json"], {stdio: ["ignore", "pipe", "pipe"]});

audit.stdout.on("data", (data) => {
  const lines = data.toString().split("\n").map(l => l.trim()).filter(l => l.startsWith("{") && l.endsWith("}"));

  for (const line of lines) {
    try {
      const obj = JSON.parse(line);
      if (
        obj.type === "auditAdvisory" &&
        ["high", "critical"].includes(obj.data.advisory.severity)
      ) {
        console.error(
          `[VULNERABILITY] ${obj.data.advisory.severity.toUpperCase()}: ${obj.data.advisory.title}`
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
