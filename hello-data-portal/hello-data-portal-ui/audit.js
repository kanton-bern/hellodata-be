#!/usr/bin/env node

const {spawn} = require("child_process");

let hasHigh = false;

const audit = spawn("yarn", ["npm", "audit", "--json"], {stdio: ["ignore", "pipe", "pipe"]});

audit.stdout.on("data", (data) => {
  console.log(data.toString());
  const lines = data.toString().trim().split("\n");
  for (const line of lines) {
    try {
      const obj = JSON.parse(line);
      console.log(obj);
      if (
        ["high", "critical", "moderate"].includes(obj.children.Severity)
      ) {
        hasHigh = true;
      }
    } catch (e) {
      console.log(e);
      process.exit(1);
    }
  }
});

audit.on("close", (code) => {
  if (hasHigh) {
    console.error("High or critical vulnerabilities found! Failing build.");
    process.exit(1);
  } else {
    console.error("Vulnerability audit finished ok")
    process.exit(0);
  }
});
