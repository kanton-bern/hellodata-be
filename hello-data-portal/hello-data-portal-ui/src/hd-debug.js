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

/*
 * On-device diagnostics for environments where the browser console is not reachable
 * (e.g. Edge on MDM-managed phones). Open the portal with ?hd_debug=1 to enable it,
 * ?hd_debug=0 to disable it and clear the log.
 *
 * Plain JS loaded synchronously from index.html before Angular, so it also captures
 * failures during bootstrap and on /callback. The OIDC login leaves the page several
 * times (portal -> Keycloak -> ADFS -> /callback), so the enable flag and the log are
 * persisted in localStorage (flag also in a cookie) and survive those redirects.
 *
 * Captured: console output, JS errors, unhandled rejections, fetch/XHR (method, URL,
 * status, duration, and the first chars of error responses), history changes, and one
 * entry per page load with environment and storage health. Tokens, codes and state are
 * redacted from everything that is recorded. Request headers and bodies are never recorded.
 */
(function () {
  'use strict';

  var FLAG_KEY = 'hd_debug';
  var LOG_KEY = 'hd_debug_log';
  var FLAG_TTL_MS = 2 * 60 * 60 * 1000;
  var MAX_ENTRIES = 500;
  var MAX_MSG_LEN = 1000;
  var MAX_ERROR_BODY_LEN = 300;

  // ---- enable flag ---------------------------------------------------------------------

  function safeLocalStorage() {
    try {
      var k = '__hd_debug_probe__';
      window.localStorage.setItem(k, '1');
      window.localStorage.removeItem(k);
      return window.localStorage;
    } catch (e) {
      return null;
    }
  }

  var ls = safeLocalStorage();

  function readParam() {
    var m = /[?&#]hd_debug=([^&#]*)/.exec(window.location.search + window.location.hash);
    return m ? m[1] : null;
  }

  function setCookie(value, maxAgeSec) {
    try {
      document.cookie = FLAG_KEY + '=' + value + '; max-age=' + maxAgeSec + '; path=/; SameSite=Lax' +
        (window.location.protocol === 'https:' ? '; Secure' : '');
    } catch (e) { /* cookies disabled */ }
  }

  function enable() {
    var until = Date.now() + FLAG_TTL_MS;
    if (ls) {
      try { ls.setItem(FLAG_KEY, String(until)); } catch (e) { /* ignore */ }
    }
    setCookie(String(until), FLAG_TTL_MS / 1000);
  }

  function disable() {
    if (ls) {
      try {
        ls.removeItem(FLAG_KEY);
        ls.removeItem(LOG_KEY);
      } catch (e) { /* ignore */ }
    }
    setCookie('', 0);
  }

  function isEnabled() {
    var until = null;
    if (ls) {
      try { until = ls.getItem(FLAG_KEY); } catch (e) { /* ignore */ }
    }
    if (!until) {
      var m = new RegExp('(?:^|; )' + FLAG_KEY + '=([^;]*)').exec(document.cookie || '');
      until = m ? m[1] : null;
    }
    return !!until && Number(until) > Date.now();
  }

  var param = readParam();
  if (param === '1' || param === 'true') {
    enable();
  } else if (param === '0' || param === 'false') {
    disable();
  }
  if (!isEnabled()) {
    return;
  }

  // ---- log buffer ----------------------------------------------------------------------

  var SECRET_PARAMS = 'code|state|session_state|id_token|access_token|refresh_token|id_token_hint|' +
    'token|code_verifier|code_challenge|client_secret|password|nonce|SAMLResponse|SAMLRequest|RelayState';
  var URL_SECRET_RE = new RegExp('([?&#](?:' + SECRET_PARAMS + ')=)[^&#\\s"\']*', 'gi');
  var JSON_SECRET_RE = new RegExp('("(?:' + SECRET_PARAMS + ')"\\s*:\\s*")[^"]*', 'gi');
  var JWT_RE = /eyJ[\w-]+\.[\w-]+\.[\w-]*/g;
  var BEARER_RE = /(Bearer\s+)[\w.~+/=-]+/gi;

  function redact(s) {
    return String(s)
      .replace(URL_SECRET_RE, '$1[redacted]')
      .replace(JSON_SECRET_RE, '$1[redacted]')
      .replace(JWT_RE, '[jwt]')
      .replace(BEARER_RE, '$1[redacted]');
  }

  function stringify(v) {
    if (v instanceof Error) {
      return v.name + ': ' + v.message + (v.stack ? '\n' + v.stack : '');
    }
    if (typeof v === 'string') {
      return v;
    }
    try {
      return JSON.stringify(v);
    } catch (e) {
      return String(v);
    }
  }

  var entries = [];
  if (ls) {
    try { entries = JSON.parse(ls.getItem(LOG_KEY) || '[]') || []; } catch (e) { entries = []; }
  }

  var listeners = [];
  var persistTimer = null;
  var stopped = false;

  function persist() {
    if (persistTimer) {
      clearTimeout(persistTimer);
      persistTimer = null;
    }
    if (!ls || stopped) {
      return;
    }
    try {
      ls.setItem(LOG_KEY, JSON.stringify(entries));
    } catch (e) {
      // Quota exceeded - drop the oldest half and try once more.
      entries = entries.slice(Math.floor(entries.length / 2));
      try { ls.setItem(LOG_KEY, JSON.stringify(entries)); } catch (e2) { /* give up */ }
    }
  }

  function add(kind, msg) {
    if (stopped) {
      return;
    }
    var text = redact(msg);
    if (text.length > MAX_MSG_LEN) {
      text = text.substring(0, MAX_MSG_LEN) + '...[truncated]';
    }
    entries.push({t: Date.now(), k: kind, m: text});
    if (entries.length > MAX_ENTRIES) {
      entries.splice(0, entries.length - MAX_ENTRIES);
    }
    // Errors are written at once, the rest is batched; pagehide flushes before a redirect.
    if (kind === 'error') {
      persist();
    } else if (!persistTimer) {
      persistTimer = setTimeout(persist, 250);
    }
    for (var i = 0; i < listeners.length; i++) {
      try { listeners[i](); } catch (e) { /* ignore */ }
    }
  }

  // ---- page load snapshot ----------------------------------------------------------------

  function storageCheck(name) {
    try {
      var s = window[name];
      var k = '__hd_debug_rt__';
      s.setItem(k, '1');
      var ok = s.getItem(k) === '1';
      s.removeItem(k);
      var keys = [];
      for (var i = 0; i < s.length; i++) {
        keys.push(s.key(i));
      }
      return (ok ? 'ok' : 'roundtrip-failed') + ' keys=[' + keys.join(', ') + ']';
    } catch (e) {
      return 'unavailable (' + (e && e.name) + ')';
    }
  }

  function navInfo() {
    try {
      var n = performance.getEntriesByType('navigation')[0];
      if (n) {
        return 'type=' + n.type + ' redirectCount=' + n.redirectCount;
      }
    } catch (e) { /* ignore */ }
    return 'n/a';
  }

  add('load', 'PAGE LOAD ' + window.location.href +
    '\n  referrer: ' + (document.referrer || '-') +
    '\n  navigation: ' + navInfo() +
    '\n  userAgent: ' + navigator.userAgent +
    '\n  online: ' + navigator.onLine + ', cookiesEnabled: ' + navigator.cookieEnabled +
    ', visibility: ' + document.visibilityState +
    '\n  localStorage: ' + storageCheck('localStorage') +
    '\n  sessionStorage: ' + storageCheck('sessionStorage') +
    '\n  cookie names: [' + (document.cookie || '').split(/;\s*/).map(function (c) {
      return c.split('=')[0];
    }).filter(Boolean).join(', ') + ']');

  // ---- console, errors -------------------------------------------------------------------

  ['log', 'info', 'warn', 'error', 'debug'].forEach(function (level) {
    var orig = console[level];
    if (typeof orig !== 'function') {
      return;
    }
    console[level] = function () {
      try {
        add(level, Array.prototype.map.call(arguments, stringify).join(' '));
      } catch (e) { /* never break logging */ }
      return orig.apply(console, arguments);
    };
  });

  window.addEventListener('error', function (ev) {
    if (ev.target && ev.target !== window) {
      // Resource load failure (script, css, img)
      add('error', 'Resource failed to load: ' + (ev.target.src || ev.target.href || ev.target.tagName));
      return;
    }
    add('error', 'Uncaught ' + (ev.error ? stringify(ev.error) : ev.message) +
      ' @ ' + ev.filename + ':' + ev.lineno + ':' + ev.colno);
  }, true);

  window.addEventListener('unhandledrejection', function (ev) {
    add('error', 'Unhandled rejection: ' + stringify(ev.reason));
  });

  // ---- navigation ------------------------------------------------------------------------

  ['pushState', 'replaceState'].forEach(function (fn) {
    var orig = history[fn];
    history[fn] = function (state, title, url) {
      add('nav', fn + ' ' + (url !== undefined && url !== null ? url : '(same url)'));
      return orig.apply(history, arguments);
    };
  });
  window.addEventListener('popstate', function () {
    add('nav', 'popstate ' + window.location.href);
  });
  window.addEventListener('pagehide', function (ev) {
    add('nav', 'pagehide (leaving ' + window.location.href + ', persisted=' + ev.persisted + ')');
    persist();
  });
  window.addEventListener('pageshow', function (ev) {
    if (ev.persisted) {
      add('nav', 'pageshow from bfcache ' + window.location.href);
    }
  });
  document.addEventListener('visibilitychange', function () {
    add('nav', 'visibility ' + document.visibilityState);
    if (document.visibilityState === 'hidden') {
      persist();
    }
  });

  // ---- network ---------------------------------------------------------------------------

  function errorBody(text) {
    return text ? ' body: ' + String(text).replace(/\s+/g, ' ').substring(0, MAX_ERROR_BODY_LEN) : '';
  }

  if (typeof window.fetch === 'function') {
    var origFetch = window.fetch;
    window.fetch = function (input, init) {
      var start = Date.now();
      var method = (init && init.method) || (input && input.method) || 'GET';
      var url = typeof input === 'string' ? input : (input && input.url) || String(input);
      return origFetch.apply(this, arguments).then(function (res) {
        var line = 'fetch ' + method + ' ' + url + ' -> ' + res.status + ' (' + (Date.now() - start) + 'ms)';
        if (res.status >= 400) {
          res.clone().text().then(function (t) {
            add('http', line + errorBody(t));
          }, function () {
            add('http', line);
          });
        } else {
          add('http', line);
        }
        return res;
      }, function (err) {
        add('http', 'fetch ' + method + ' ' + url + ' -> FAILED (' + (Date.now() - start) + 'ms) ' + stringify(err));
        throw err;
      });
    };
  }

  var xhrProto = window.XMLHttpRequest && window.XMLHttpRequest.prototype;
  if (xhrProto) {
    var origOpen = xhrProto.open;
    var origSend = xhrProto.send;
    xhrProto.open = function (method, url) {
      this.__hdDebug = {method: method, url: String(url)};
      return origOpen.apply(this, arguments);
    };
    xhrProto.send = function () {
      var xhr = this;
      var info = xhr.__hdDebug || {method: '?', url: '?'};
      var start = Date.now();
      xhr.addEventListener('loadend', function () {
        var line = 'xhr ' + info.method + ' ' + info.url + ' -> ' +
          (xhr.status || 'FAILED (status 0 - network/CORS/aborted)') + ' (' + (Date.now() - start) + 'ms)';
        if (xhr.status >= 400) {
          try {
            if (xhr.responseType === '' || xhr.responseType === 'text') {
              line += errorBody(xhr.responseText);
            }
          } catch (e) { /* ignore */ }
        }
        add('http', line);
      });
      return origSend.apply(this, arguments);
    };
  }

  // ---- UI --------------------------------------------------------------------------------

  function pad(n, w) {
    var s = String(n);
    while (s.length < (w || 2)) {
      s = '0' + s;
    }
    return s;
  }

  function fmtTime(t) {
    var d = new Date(t);
    return pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()) + '.' + pad(d.getMilliseconds(), 3);
  }

  function asText() {
    var header = 'HelloDATA portal debug log\n' +
      'exported: ' + new Date().toISOString() + '\n' +
      'url: ' + redact(window.location.href) + '\n' +
      'userAgent: ' + navigator.userAgent + '\n' +
      'entries: ' + entries.length + (ls ? '' : ' (localStorage unavailable - log not persisted across redirects!)') +
      '\n\n';
    return header + entries.map(function (e) {
      return fmtTime(e.t) + ' [' + e.k + '] ' + e.m;
    }).join('\n');
  }

  function copyText(text, done) {
    function fallback() {
      var ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      var ok = false;
      try { ok = document.execCommand('copy'); } catch (e) { /* ignore */ }
      document.body.removeChild(ta);
      done(ok);
    }

    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(function () { done(true); }, fallback);
    } else {
      fallback();
    }
  }

  var CSS =
    ':host{all:initial}' +
    '.btn{font:600 13px/1 system-ui,sans-serif;border:0;border-radius:6px;padding:8px 10px;cursor:pointer;' +
    'background:#1f2937;color:#fff}' +
    '.fab{position:fixed;right:12px;bottom:12px;z-index:2147483647;background:#b91c1c;' +
    'box-shadow:0 2px 8px rgba(0,0,0,.3)}' +
    '.panel{position:fixed;inset:0;z-index:2147483647;background:#fff;color:#111;display:flex;' +
    'flex-direction:column;font:12px/1.4 ui-monospace,Menlo,Consolas,monospace}' +
    '.bar{display:flex;flex-wrap:wrap;gap:6px;padding:8px;border-bottom:1px solid #ddd;background:#f3f4f6}' +
    '.status{font:12px system-ui,sans-serif;align-self:center;color:#374151}' +
    'pre{flex:1;margin:0;padding:8px;overflow:auto;white-space:pre-wrap;word-break:break-all}';

  function mountUi() {
    var host = document.createElement('div');
    host.id = 'hd-debug-host';
    document.body.appendChild(host);
    var root = host.attachShadow ? host.attachShadow({mode: 'open'}) : host;

    var style = document.createElement('style');
    style.textContent = CSS;
    root.appendChild(style);

    var fab = document.createElement('button');
    fab.className = 'btn fab';
    root.appendChild(fab);

    var panel = document.createElement('div');
    panel.className = 'panel';
    panel.style.display = 'none';
    root.appendChild(panel);

    var bar = document.createElement('div');
    bar.className = 'bar';
    panel.appendChild(bar);

    var status = document.createElement('span');
    status.className = 'status';

    var pre = document.createElement('pre');
    panel.appendChild(pre);

    function button(label, onClick) {
      var b = document.createElement('button');
      b.className = 'btn';
      b.textContent = label;
      b.addEventListener('click', onClick);
      bar.appendChild(b);
    }

    function flash(msg) {
      status.textContent = msg;
      setTimeout(function () {
        status.textContent = '';
      }, 3000);
    }

    function render() {
      fab.textContent = 'Debug (' + entries.length + ')';
      if (panel.style.display !== 'none') {
        pre.textContent = asText();
      }
    }

    fab.addEventListener('click', function () {
      panel.style.display = 'flex';
      render();
      pre.scrollTop = pre.scrollHeight;
    });

    button('Close', function () {
      panel.style.display = 'none';
    });
    button('Copy', function () {
      copyText(asText(), function (ok) {
        flash(ok ? 'Copied' : 'Copy failed - select the text manually');
      });
    });
    if (navigator.share) {
      button('Share', function () {
        navigator.share({title: 'HelloDATA debug log', text: asText()}).catch(function (e) {
          flash('Share failed: ' + (e && e.name));
        });
      });
    }
    button('Download', function () {
      try {
        var a = document.createElement('a');
        a.href = URL.createObjectURL(new Blob([asText()], {type: 'text/plain'}));
        a.download = 'hellodata-debug-' + new Date().toISOString().replace(/[:.]/g, '-') + '.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      } catch (e) {
        flash('Download failed: ' + (e && e.name));
      }
    });
    button('Clear', function () {
      entries = [];
      persist();
      render();
    });
    button('Disable', function () {
      stopped = true;
      disable();
      host.parentNode.removeChild(host);
    });
    bar.appendChild(status);

    listeners.push(render);
    render();
  }

  if (document.body) {
    mountUi();
  } else {
    document.addEventListener('DOMContentLoaded', mountUi);
  }

  // Hook for app code (e.g. NgRx meta-reducer) to add own entries.
  window.hdDebug = {
    log: function (kind, msg) {
      add(kind, stringify(msg));
    }
  };
})();
