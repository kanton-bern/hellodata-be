/*
 * Custom webpack config for the production browser build.
 *
 * Disables webpack module concatenation (scope hoisting) only.
 *
 * Why: PrimeNG's Editor lazy-loads Quill (`import('quill')`). Quill's
 * Editor.getDelta() calls `new Delta()`, where Delta comes from the CommonJS
 * `quill-delta` package (`module.exports = Delta`). With scope hoisting on,
 * webpack wraps the non-concatenatable CJS module in a deferred arrow wrapper
 * and mis-compiles `new Delta()` into `new (arrowWrapper)()`. Arrow functions
 * are not constructable, so the editor throws "X is not a constructor" at
 * runtime in production builds only (dev/`ng serve` uses the correct interop).
 * The symptom: announcements/FAQ/documentation editors render blank and save
 * empty. Disabling concatenateModules restores webpack's `__esModule` default
 * interop and Quill instantiates correctly. Minification stays enabled.
 */
module.exports = {
  optimization: {
    concatenateModules: false
  }
};
