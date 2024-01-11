/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

/**
 * @type {Cypress.PluginConfig}
 */
module.exports = (on, config) => {
  require('cypress-log-to-output').install(on);
  // `on` is used to hook into various events Cypress emits
  // `config` is the resolved Cypress config
  on('after:run', (results) => {
    const fs = require('fs');
    if (config.reporterOptions?.failsafeSummary) {
      // write a failsafe-summary.xml so we can fail the build using failsafe:verify
      // this is only needed when running the tests as part of a maven build
      let summary = '<?xml version="1.0" encoding="UTF-8"?>\n';
      summary += `<failsafe-summary result="${results.totalTests}" timeout="false">\n`;
      summary += `  <completed>${results.totalPassed}</completed>\n`;
      summary += `  <errors>${results.totalFailed}</errors>\n`;
      summary += `  <skipped>${results.totalSkipped}</skipped>\n`;
      summary += '  <failures>0</failures>\n';
      summary += '  <failureMessage/>\n';
      summary += '</failsafe-summary>\n';
      fs.writeFile(config.reporterOptions.failsafeSummary, summary,
        (err) => {
          if (err) throw err;
          console.log(
            `Test results saved to ${config.reporterOptions.failsafeSummary}`);
        });
    }
  });
}


