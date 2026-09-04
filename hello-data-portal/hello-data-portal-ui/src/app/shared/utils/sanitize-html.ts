///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import DOMPurify from 'dompurify';

/**
 * Sanitizes rich-text HTML (e.g. Quill/p-editor output for FAQ and announcement
 * messages) before it is rendered as trusted markup — either via
 * DomSanitizer.bypassSecurityTrustHtml / [innerHTML], or fed back into a
 * read-only Quill editor.
 *
 * This neutralizes stored XSS payloads (scripts, event-handler attributes,
 * javascript: URLs, ...) while keeping the safe formatting tags the editor
 * produces. It is the app-side remediation for GHSA-v3m3-f69x-jf25 (Quill XSS
 * via HTML export), which has no upstream patched release.
 */
export function sanitizeRichText(html: string | null | undefined): string {
  return DOMPurify.sanitize(html ?? '', { USE_PROFILES: { html: true } });
}

/**
 * Returns true when rich-text HTML (Quill/p-editor output) carries no visible
 * content. Quill never stores a truly empty string: an emptied editor is saved
 * as `<p></p>` / `<p><br></p>`, and "blank" content may be whitespace or
 * &nbsp;-only or empty tags (e.g. `<h1></h1>`). A plain `!value` or
 * `value.trim() === ''` check misses all of these, so the "fallback to default
 * language" logic never triggers for emptied translations. This strips markup
 * and checks for any remaining text (images are disabled in these editors).
 */
export function isRichTextEmpty(html: string | null | undefined): boolean {
  if (!html) {
    return true;
  }
  const textContent = new DOMParser().parseFromString(html, 'text/html').body.textContent ?? '';
  return textContent.trim().length === 0;
}
