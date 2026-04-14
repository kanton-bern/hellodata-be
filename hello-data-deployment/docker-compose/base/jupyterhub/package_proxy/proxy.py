"""
HelloDATA Package Allowlist Proxy

A PEP 503-compliant PyPI proxy that only serves packages listed in an allowlist.
Filters package listings and version links based on YAML configuration.
"""

import hashlib
import logging
import os
import re
import sys
import time
from html.parser import HTMLParser
from pathlib import Path

import yaml
from aiohttp import ClientSession, web
from packaging.specifiers import SpecifierSet, InvalidSpecifier
from packaging.version import Version, InvalidVersion

logging.basicConfig(
    level=os.environ.get("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s [%(levelname)s] %(message)s",
    stream=sys.stdout,
)
log = logging.getLogger("hd-package-proxy")

ALLOWLIST_PATH = Path(os.environ.get("ALLOWLIST_PATH", "/app/allowlist.yaml"))
UPSTREAM_INDEX = os.environ.get("UPSTREAM_INDEX", "https://pypi.org/simple/")

# Global state
_allowlist: dict[str, str] = {}
_allowlist_mtime: float = 0


def _normalize(name: str) -> str:
    """PEP 503 normalize: lowercase, replace [-_.]+  with single dash."""
    return re.sub(r"[-_.]+", "-", name).lower()


def load_allowlist() -> dict[str, str]:
    """Load allowlist from YAML, keyed by normalized package name."""
    global _allowlist, _allowlist_mtime
    try:
        mtime = ALLOWLIST_PATH.stat().st_mtime
    except FileNotFoundError:
        log.error("Allowlist file not found: %s", ALLOWLIST_PATH)
        return _allowlist

    if mtime == _allowlist_mtime and _allowlist:
        return _allowlist

    with open(ALLOWLIST_PATH) as f:
        data = yaml.safe_load(f) or {}

    packages = data.get("packages", {})
    _allowlist = {_normalize(k): str(v) for k, v in packages.items()}
    _allowlist_mtime = mtime
    log.info("Loaded allowlist: %d packages from %s", len(_allowlist), ALLOWLIST_PATH)
    for pkg, spec in sorted(_allowlist.items()):
        log.info("  %s: %s", pkg, spec)
    return _allowlist


def is_allowed(package: str) -> tuple[bool, str]:
    """Check if a package is in the allowlist. Returns (allowed, version_spec)."""
    al = load_allowlist()
    norm = _normalize(package)
    if norm not in al:
        return False, ""
    return True, al[norm]


def version_matches(version_str: str, spec_str: str) -> bool:
    """Check if a version string matches a specifier."""
    if spec_str == "*":
        return True
    try:
        ver = Version(version_str)
        spec = SpecifierSet(spec_str)
        return ver in spec
    except (InvalidVersion, InvalidSpecifier):
        return False


# ---------------------------------------------------------------------------
# HTML parser for PEP 503 simple index pages
# ---------------------------------------------------------------------------

class SimpleLinkParser(HTMLParser):
    """Parse <a> tags from a PEP 503 simple page."""

    def __init__(self):
        super().__init__()
        self.links: list[dict] = []  # [{href, text, attrs}]
        self._current: dict | None = None

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self._current = {"href": dict(attrs).get("href", ""), "attrs": attrs, "text": ""}

    def handle_data(self, data):
        if self._current is not None:
            self._current["text"] += data

    def handle_endtag(self, tag):
        if tag == "a" and self._current is not None:
            self.links.append(self._current)
            self._current = None


_VERSION_RE = re.compile(r"-(\d+[.\d]*(?:(?:a|b|rc|dev|post)\d*)*)")


def extract_version(filename: str) -> str | None:
    """Try to extract a version from a distribution filename."""
    # wheel: package-1.2.3-py3-none-any.whl
    if filename.endswith(".whl"):
        parts = filename.split("-")
        if len(parts) >= 3:
            return parts[1]
    # sdist: package-1.2.3.tar.gz / package-1.2.3.zip
    base = filename
    for ext in (".tar.gz", ".tar.bz2", ".zip", ".tar.xz"):
        if base.endswith(ext):
            base = base[: -len(ext)]
            break
    m = _VERSION_RE.search(base)
    return m.group(1) if m else None


def filter_links(links: list[dict], spec_str: str) -> list[dict]:
    """Filter links to only include versions matching the specifier."""
    if spec_str == "*":
        return links
    filtered = []
    for link in links:
        filename = link["href"].rsplit("/", 1)[-1].split("#")[0]
        ver = extract_version(filename)
        if ver and version_matches(ver, spec_str):
            filtered.append(link)
        elif not ver:
            # Can't determine version — include it (metadata, etc.)
            filtered.append(link)
    return filtered


def render_simple_page(title: str, links: list[dict]) -> str:
    """Render a PEP 503 simple HTML page."""
    parts = [
        "<!DOCTYPE html><html><head><title>{}</title></head><body><h1>{}</h1>".format(title, title)
    ]
    for link in links:
        attrs_str = " ".join(f'{k}="{v}"' for k, v in link["attrs"])
        parts.append(f'<a {attrs_str}>{link["text"]}</a><br/>')
    parts.append("</body></html>")
    return "\n".join(parts)


# ---------------------------------------------------------------------------
# HTTP handlers
# ---------------------------------------------------------------------------

async def handle_root(request: web.Request) -> web.Response:
    """GET /simple/ — list allowed packages only."""
    al = load_allowlist()
    lines = ['<!DOCTYPE html><html><head><title>HelloDATA Package Index</title></head><body><h1>HelloDATA Package Index</h1>']
    for pkg in sorted(al.keys()):
        lines.append(f'<a href="/simple/{pkg}/">{pkg}</a><br/>')
    lines.append("</body></html>")
    return web.Response(text="\n".join(lines), content_type="text/html")


async def handle_package(request: web.Request) -> web.Response:
    """GET /simple/<package>/ — proxy from upstream, filter versions."""
    package = request.match_info["package"]
    norm = _normalize(package)
    allowed, spec_str = is_allowed(norm)

    if not allowed:
        log.warning("DENIED: %s (not in allowlist)", package)
        return web.Response(
            status=403,
            text=f"Package '{package}' is not in the HelloDATA allowlist.\n"
                 f"Contact your platform administrator to request access.\n",
            content_type="text/plain",
        )

    # Proxy from upstream
    upstream_url = f"{UPSTREAM_INDEX.rstrip('/')}/{norm}/"
    try:
        async with ClientSession() as session:
            async with session.get(upstream_url) as resp:
                if resp.status != 200:
                    log.error("Upstream returned %d for %s", resp.status, norm)
                    return web.Response(status=resp.status, text=await resp.text())
                html = await resp.text()
    except Exception as e:
        log.error("Failed to fetch upstream for %s: %s", norm, e)
        return web.Response(status=502, text=f"Failed to reach upstream PyPI: {e}")

    # Parse and filter links
    parser = SimpleLinkParser()
    parser.feed(html)

    if spec_str != "*":
        before = len(parser.links)
        filtered = filter_links(parser.links, spec_str)
        log.info("ALLOWED: %s (spec=%s, %d/%d versions)", norm, spec_str, len(filtered), before)
    else:
        filtered = parser.links
        log.info("ALLOWED: %s (spec=*, all %d versions)", norm, len(filtered))

    result_html = render_simple_page(f"Links for {norm}", filtered)
    return web.Response(text=result_html, content_type="text/html")


async def handle_healthz(request: web.Request) -> web.Response:
    """Health check endpoint."""
    al = load_allowlist()
    return web.Response(
        text=f"OK ({len(al)} packages in allowlist)\n",
        content_type="text/plain",
    )


def create_app() -> web.Application:
    app = web.Application()
    app.router.add_get("/simple/", handle_root)
    app.router.add_get("/simple/{package}/", handle_package)
    app.router.add_get("/healthz", handle_healthz)
    # Redirect /simple to /simple/
    app.router.add_get("/simple", lambda r: web.HTTPFound("/simple/"))
    return app


if __name__ == "__main__":
    load_allowlist()
    port = int(os.environ.get("PORT", "8888"))
    log.info("Starting HelloDATA Package Proxy on port %d", port)
    log.info("Upstream index: %s", UPSTREAM_INDEX)
    web.run_app(create_app(), host="0.0.0.0", port=port)
