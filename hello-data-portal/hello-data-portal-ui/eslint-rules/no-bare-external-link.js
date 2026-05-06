// @ts-check
'use strict';

/**
 * Custom ESLint rule: no-bare-external-link
 *
 * Enforces that all external <a> tags in Angular templates use the
 * `appExternalLink` directive instead of bare `target="_blank"` or
 * unsecured external hrefs.
 *
 * Rationale: the `appExternalLink` directive is the single authoritative
 * place that sets target="_blank", rel="noopener noreferrer", and appends
 * the visual external-link icon. Bypassing it creates an inconsistent UX
 * and potential security issues.
 *
 * Flagged cases:
 *   - <a href="https://..."> or <a href="http://..."> or <a href="//...">
 *     without appExternalLink
 *   - <a [href]="..."> or <a href="{{...}}"> without appExternalLink
 *     (dynamic hrefs are always external tool URLs in this app)
 *   - <a target="_blank"> without appExternalLink
 *
 * Safe cases (not flagged):
 *   - <a [routerLink]="..."> — internal Angular navigation
 *   - <a href="/internal-path"> — relative/absolute internal path
 *   - <a href="#anchor"> — fragment links
 *   - <a href="mailto:..."> or <a href="tel:..."> — protocol links
 *   - Any <a appExternalLink> — already correct
 */

/** @type {import('eslint').Rule.RuleModule} */
module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description:
        'Require the appExternalLink directive on external <a> tags. ' +
        'See CONTRIBUTING.md § "Frontend Conventions > Link Behavior".',
      recommended: true,
    },
    schema: [],
    messages: {
      missingDirective:
        'External links must use the appExternalLink directive ' +
        '(<a href="..." appExternalLink>). ' +
        'It sets target="_blank", rel="noopener noreferrer", and adds a visual indicator.',
      bareTargetBlank:
        'Do not set target="_blank" directly. ' +
        'Use the appExternalLink directive instead.',
    },
  },

  create(context) {
    return {
      'Element[name="a"]'(node) {
        const attrs = node.attributes ?? [];
        const inputs = node.inputs ?? [];

        const hasDirective =
          attrs.some((a) => a.name === 'appExternalLink') ||
          inputs.some((i) => i.name === 'appExternalLink');

        if (hasDirective) return;

        // Flag bare target="_blank" without the directive
        const hasTargetBlank = attrs.some(
          (a) => a.name === 'target' && a.value === '_blank'
        );
        if (hasTargetBlank) {
          context.report({ node, messageId: 'bareTargetBlank' });
          return;
        }

        // Check static href value
        const staticHref = attrs.find((a) => a.name === 'href');
        if (staticHref) {
          const val = staticHref.value ?? '';
          const isExternal =
            val.startsWith('http://') ||
            val.startsWith('https://') ||
            val.startsWith('//');
          if (isExternal) {
            context.report({ node, messageId: 'missingDirective' });
          }
          // Non-external static hrefs (/path, #anchor, mailto:, tel:) are fine
          return;
        }

        // Check dynamic / interpolated href (BoundAttribute)
        const boundHref = inputs.find((i) => i.name === 'href');
        if (boundHref) {
          context.report({ node, messageId: 'missingDirective' });
        }
      },
    };
  },
};
