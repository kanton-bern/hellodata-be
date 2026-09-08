#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# FAB auth manager configuration for Airflow 3 (apache-airflow-providers-fab).
# Keycloak does AUTHENTICATION only (public frontend-client, auth-code + PKCE,
# no secret); authorization stays in Airflow via FAB roles. Portal iframe SSO:
# a valid portal Keycloak cookie logs the user straight in and re-syncs FAB roles
# (read live from Keycloak) on every open. All Keycloak connection details come
# from env (KEYCLOAK_* — set by the chart), so this file ships verbatim in the image.
#
import base64
import json
import logging
import os
from urllib.parse import quote

from flask import g, redirect, request
from flask_login import login_user, logout_user
from flask_appbuilder.security.manager import AUTH_OAUTH
from flask_appbuilder.security.views import AuthOAuthView
from flask_appbuilder.views import expose
from airflow.providers.fab.auth_manager.security_manager.override import (
    FabAirflowSecurityManagerOverride,
)

log = logging.getLogger("hd.webserver_config")

def _post_login_redirect():
    # The portal embeds the orchestration view, so land on the DAGs list. Airflow 3 strips
    # ?next at the /api/v2/auth/login -> /auth/login/ hop, so default to /dags; still honour
    # a safe relative next if a future version preserves it.
    nxt = request.args.get("next")
    if nxt and nxt.startswith("/") and not nxt.startswith("//"):
        return redirect(nxt)
    return redirect("/dags")

def _set_api_jwt_cookie(resp, user):
    # The Airflow 3 UI/API (/ui/config, /api/v2/*) authenticate with the "_token" JWT cookie
    # that the stock /api/v2/auth/login flow mints. Our cookie-SSO shortcut bypasses that
    # flow, so mint + set it here too — otherwise the SPA loops on /ui/config 401.
    from airflow.api_fastapi.app import get_auth_manager
    from airflow.api_fastapi.auth.managers.base_auth_manager import COOKIE_NAME_JWT_TOKEN
    from airflow.configuration import conf as _conf
    exp = _conf.getint("api_auth", "jwt_expiration_time", fallback=86400)
    jwt = get_auth_manager().generate_jwt(user=user, expiration_time_in_seconds=exp)
    resp.set_cookie(COOKIE_NAME_JWT_TOKEN, jwt, path="/", secure=True, httponly=True, samesite="Lax")

def _fetch_keycloak_realm_roles(email):
    # The user's CURRENT realm-role names, read straight from Keycloak via the admin
    # service account. The browser's auth.access_token cookie can carry stale roles right
    # after a portal role change (its refresh lags), so reading live from Keycloak makes the
    # change take effect on the very next open. Returns None on any failure -> caller falls
    # back to the token's own claims, so login still works if Keycloak is unreachable.
    url = os.environ.get("KEYCLOAK_ADMIN_URL", "").rstrip("/")
    realm = os.environ.get("KEYCLOAK_REALM", "")
    client_id = os.environ.get("KEYCLOAK_ADMIN_CLIENT_ID", "admin-cli")
    secret = os.environ.get("KEYCLOAK_ADMIN_CLIENT_SECRET", "")
    if not (url and realm and secret and email):
        return None
    try:
        import requests
        tok = requests.post(
            url + "/realms/" + realm + "/protocol/openid-connect/token",
            data={"grant_type": "client_credentials", "client_id": client_id, "client_secret": secret},
            timeout=5,
        ).json().get("access_token")
        if not tok:
            return None
        headers = {"Authorization": "Bearer " + tok}
        users = requests.get(url + "/admin/realms/" + realm + "/users",
                             params={"email": email, "exact": "true"}, headers=headers, timeout=5).json()
        if not users:
            return None
        roles = requests.get(url + "/admin/realms/" + realm + "/users/" + users[0]["id"] + "/role-mappings/realm",
                             headers=headers, timeout=5).json()
        return [r["name"] for r in roles]
    except Exception as e:  # noqa: BLE001
        log.warning("Keycloak role fetch failed for %s (%s); falling back to token claims", email, e)
        return None

class AutoRedirectAuthOAuthView(AuthOAuthView):
    # Portal iframe SSO. Two behaviours vs stock FAB:
    #  1. If the portal's Keycloak token is present as the auth.access_token cookie
    #     (portal sets it on the shared parent domain; sent same-site to this subdomain),
    #     log the user in directly from it — no interactive Keycloak screen.
    #  2. Otherwise, with a single provider, go straight to Keycloak (skip FAB's
    #     "Sign in with keycloak" button page).
    # _claims / _userinfo_from_claims are module-level (defined below); resolved at call time.
    @expose("/login/")
    @expose("/login/<provider>")
    def login(self, provider=None):
        # Check the cookie FIRST (before any existing-session short-circuit) so every hit
        # re-runs auth_user_oauth -> with AUTH_ROLES_SYNC_AT_LOGIN this re-syncs FAB roles
        # from the current token. Without this, a user keeps stale roles (e.g. a revoked
        # DD_<domain>) for the life of their Airflow session even after the portal removes
        # the role in Keycloak.
        token = request.cookies.get("auth.access_token")
        if token:
            try:
                # Signature NOT verified: the token comes from the portal over TLS on a
                # shared parent domain. TODO: verify against Keycloak JWKS for production.
                userinfo = _userinfo_from_claims(_claims(token))
                if userinfo.get("email"):
                    # Prefer the user's CURRENT realm roles from Keycloak (the browser token
                    # can lag a portal role change); fall back to the token's claims on failure.
                    live_roles = _fetch_keycloak_realm_roles(userinfo["email"])
                    if live_roles is not None:
                        userinfo["role_keys"] = live_roles
                    user = self.appbuilder.sm.auth_user_oauth(userinfo)
                    if user is not None:
                        login_user(user, remember=False)
                        resp = _post_login_redirect()
                        _set_api_jwt_cookie(resp, user)
                        log.info("Cookie SSO login for %s roles=%s", userinfo.get("email"), userinfo.get("role_keys"))
                        return resp
            except Exception as e:  # noqa: BLE001
                log.warning("Cookie SSO failed (%s); falling back to interactive", e)
        # No / invalid cookie: keep an existing session, else go straight to Keycloak.
        if g.user is not None and g.user.is_authenticated:
            return _post_login_redirect()
        return super().login(provider=provider or "keycloak")

    @expose("/logout/")
    def logout(self):
        # Portal "logout-first" open (mirrors the Superset embed). The portal points the
        # orchestration iframe here on every open; we tear down the FAB session AND the UI
        # _token JWT, then bounce to /auth/login/ which re-runs cookie SSO and re-syncs FAB
        # roles from the current portal token. Without this a still-valid _token keeps stale
        # roles (e.g. a revoked DD_<domain>) until it expires.
        from airflow.api_fastapi.auth.managers.base_auth_manager import COOKIE_NAME_JWT_TOKEN
        logout_user()
        nxt = request.args.get("next")
        target = nxt if (nxt and nxt.startswith("/") and not nxt.startswith("//")) else "/dags"
        resp = redirect("/auth/login/?next=" + quote(target, safe="/"))
        resp.delete_cookie(COOKIE_NAME_JWT_TOKEN, path="/")
        return resp

AUTH_TYPE = AUTH_OAUTH
AUTH_USER_REGISTRATION = True
AUTH_USER_REGISTRATION_ROLE = "Public"   # no access until a role maps
AUTH_ROLES_SYNC_AT_LOGIN = True

# Static Keycloak-role -> FAB-role mapping. The per-domain DD_<domain> roles are
# dynamic (created by the portal via Keycloak for each data domain) and mapped
# pass-through in KeycloakSecurityManager._oauth_calculate_user_roles below, so any
# domain works without editing this list.
AUTH_ROLES_MAPPING = {
    "airflow_admin": ["Admin"],
    "airflow_op": ["Op"],
    "airflow_user": ["User"],
    "airflow_viewer": ["Viewer"],
    # base role: UI access WITHOUT collective DAG read -> scoped users see
    # only their DD_<domain> DAGs (the AF_OPERATOR equivalent).
    "airflow_base": ["hd_base"],
}

# Keycloak base URL + realm come from env (KEYCLOAK_ADMIN_URL / KEYCLOAK_REALM), set by the chart
# from global.oauth. No build-time interpolation, so this file ships verbatim in the chart / image.
_KC = os.environ.get("KEYCLOAK_ADMIN_URL", "").rstrip("/")
_REALM = os.environ.get("KEYCLOAK_REALM", "")

OAUTH_PROVIDERS = [
    {
        "name": "keycloak",
        "icon": "fa-key",
        "token_key": "access_token",
        "remote_app": {
            "client_id": os.environ.get("KEYCLOAK_CLIENT_ID"),
            "client_secret": None,  # PUBLIC client: PKCE, no secret
            "server_metadata_url": f"{_KC}/realms/{_REALM}/.well-known/openid-configuration",
            "api_base_url": f"{_KC}/realms/{_REALM}/protocol/openid-connect",
            "client_kwargs": {
                "scope": "openid email profile",
                "code_challenge_method": "S256",       # PKCE
                "token_endpoint_auth_method": "none",  # don't send client creds
            },
        },
    }
]

def _claims(token):
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))

def _userinfo_from_claims(c):
    # Keycloak roles: realm roles + this client's roles -> role_keys, mapped to FAB
    # roles by AUTH_ROLES_MAPPING (airflow_* + DD_<domain>).
    cid = os.environ.get("KEYCLOAK_CLIENT_ID", "")
    roles = c.get("realm_access", {}).get("roles", []) + \
        c.get("resource_access", {}).get(cid, {}).get("roles", [])
    return {
        # HelloDATA keys users by email — use it as the FAB username too (not
        # preferred_username), so Airflow user identity matches the portal / other subsystems.
        "username": c.get("email") or c.get("preferred_username"),
        "email": c.get("email"),
        "first_name": c.get("given_name", ""),
        "last_name": c.get("family_name", ""),
        "role_keys": roles,
    }

class KeycloakSecurityManager(FabAirflowSecurityManagerOverride):
    authoauthview = AutoRedirectAuthOAuthView  # cookie SSO + auto-redirect

    def get_oauth_user_info(self, provider, response):
        if provider != "keycloak":
            return {}
        return _userinfo_from_claims(_claims(response["access_token"]))

    def _oauth_calculate_user_roles(self, userinfo):
        # Static airflow_* roles map via AUTH_ROLES_MAPPING; additionally pass through any
        # DD_<domain> role the token carries, creating the matching FAB role on first use.
        # Those FAB roles get their per-DAG permissions from the dag_policy access_control
        # at parse time, so a DATA_DOMAIN_ADMIN with realm role DD_<domain> sees + triggers
        # exactly that domain's DAGs.
        roles = set(super()._oauth_calculate_user_roles(userinfo))
        for key in userinfo.get("role_keys", []):
            if key.startswith("DD_"):
                role = self.find_role(key) or self.add_role(key)
                if role is not None:
                    roles.add(role)
        return list(roles)

SECURITY_MANAGER_CLASS = KeycloakSecurityManager
