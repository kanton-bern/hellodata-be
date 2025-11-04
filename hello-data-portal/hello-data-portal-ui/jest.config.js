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
/**
 * Jest configuration for Angular 17+ with jest-preset-angular
 */
module.exports = {
  preset: 'jest-preset-angular',
  testEnvironment: 'jsdom', // 👈 required since Jest 28+
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['**/__tests__/**/*.spec.ts', '**/?(*.)+(spec|test).ts'],
  moduleFileExtensions: ['ts', 'html', 'js', 'json'],
  transform: {
    '^.+\\.(ts|mjs|js|html)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        stringifyContentPathRegex: '\\.html$',
      },
    ],
  },
  transformIgnorePatterns: [
    'node_modules/(?!.*\\.mjs$|(@angular|rxjs|primeng)/)',
  ],
  collectCoverageFrom: ['src/**/*.{ts,js}', '!src/main.ts', '!src/polyfills.ts'],
  coverageDirectory: 'target/coverage',
  coverageReporters: ['lcov', 'text-summary'],
  coveragePathIgnorePatterns: ['<rootDir>/src/app/core/'],
  modulePaths: ['<rootDir>'],
  moduleDirectories: ['node_modules', 'src'],
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: 'target',
        outputName: 'jest-junit.xml',
      },
    ],
    [
      'jest-sonar',
      {
        outputDirectory: 'target',
        outputName: 'jest-sonar.xml',
        reportedFilePath: 'relative',
        relativeRootDir: '..',
      },
    ],
  ],
};
