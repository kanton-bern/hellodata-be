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

import {ErrorHandler, Injectable} from '@angular/core';

/**
 * sessionStorage flag used to guard against a chunk-reload loop. It is set right before the
 * recovery reload and cleared again once the application bootstraps successfully (see main.ts),
 * so a genuinely broken build cannot reload the tab endlessly.
 */
export const CHUNK_RELOAD_FLAG_KEY = 'hd_chunk_reload';

/**
 * Global error handler that recovers from lazy-chunk load failures.
 *
 * A chunk load fails when the index.html currently running in the tab references bundle hashes
 * that no longer exist on the server - typically because a new release was deployed while the
 * tab was open, so a later route navigation requests an old (now removed) lazy chunk. Instead of
 * leaving the user on a broken/blank view, we force a single hard reload to pull the fresh
 * index.html and the current bundles.
 *
 * Note: this only covers the case where the app already booted. If the entry bundle itself is
 * missing (stale index.html served on a cold load), no application code runs at all - that case
 * is handled server-side by revalidating index.html (see nginx.conf cache policy).
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {

  handleError(error: unknown): void {
    if (GlobalErrorHandler.isChunkLoadError(error)) {
      this.recoverFromChunkLoadError(error);
      return;
    }
    // Default behaviour for every other error.
    console.error(error);
  }

  private static isChunkLoadError(error: unknown): boolean {
    const err = error as { name?: string; message?: string } | null;
    const name = err?.name ?? '';
    const message = err?.message ?? '';
    return (
      name === 'ChunkLoadError' ||
      /Loading chunk [^\s]+ failed/i.test(message) ||
      /Loading CSS chunk [^\s]+ failed/i.test(message) ||
      /Failed to fetch dynamically imported module/i.test(message) ||
      /error loading dynamically imported module/i.test(message)
    );
  }

  private recoverFromChunkLoadError(error: unknown): void {
    try {
      if (sessionStorage.getItem(CHUNK_RELOAD_FLAG_KEY)) {
        // We already reloaded once and the chunk still fails - stop, to avoid a reload loop.
        console.error('[GlobalErrorHandler] Chunk load still failing after reload, not retrying', error);
        sessionStorage.removeItem(CHUNK_RELOAD_FLAG_KEY);
        return;
      }
      sessionStorage.setItem(CHUNK_RELOAD_FLAG_KEY, '1');
    } catch {
      // sessionStorage can be unavailable (private mode / blocked storage) - reload once anyway.
    }
    console.warn('[GlobalErrorHandler] Chunk load failed (stale build?), reloading to fetch the new version', error);
    window.location.reload();
  }
}
