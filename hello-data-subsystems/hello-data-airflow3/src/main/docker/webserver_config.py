#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# FAB auth manager configuration for Airflow 3 (apache-airflow-providers-fab).
# Keycloak does AUTHENTICATION only (public frontend-client, auth-code + PKCE,
# no secret); authorization stays in Airflow via FAB roles. Auto-redirects to
# Keycloak (no provider-selection page) so it embeds cleanly in the portal iframe.
#
import base64
import json
import os

from flask import g, redirect
from flask_appbuilder.security.manager import AUTH_OAUTH
from flask_appbuilder.security.views import AuthOAuthView
from flask_appbuilder.views import expose
from airflow.providers.fab.auth_manager.security_manager.override import (
    FabAirflowSecurityManagerOverride,
)

# --- Keycloak connection (env-driven, so the same image works per realm) ---
KEYCLOAK_BASE_URL = os.environ["KEYCLOAK_BASE_URL"].rstrip("/")   # e.g. https://sso.be.ch/auth
KEYCLOAK_REALM = os.environ.get("KEYCLOAK_REALM", "hellodata")
KEYCLOAK_CLIENT_ID = os.environ["KEYCLOAK_CLIENT_ID"]            # public client, e.g. frontend-client
_ISSUER = f"{KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}"

AUTH_TYPE = AUTH_OAUTH
AUTH_USER_REGISTRATION = True
AUTH_USER_REGISTRATION_ROLE = "Public"   # no access until a role maps
AUTH_ROLES_SYNC_AT_LOGIN = True

# Static Keycloak-role -> FAB-role mapping. The per-domain DD_<domain> roles are
# dynamic (created by the portal via the Keycloak admin API) and are mapped
# pass-through in HdSecurityManager._oauth_calculate_user_roles below.
AUTH_ROLES_MAPPING = {
    "airflow_admin": ["Admin"],
    "airflow_op": ["Op"],
    "airflow_user": ["User"],
    "airflow_viewer": ["Viewer"],
    "airflow_base": ["hd_base"],   # UI access without collective "all DAGs" read
}

OAUTH_PROVIDERS = [
    {
        "name": "keycloak",
        "icon": "fa-key",
        "token_key": "access_token",
        "remote_app": {
            "client_id": KEYCLOAK_CLIENT_ID,
            "client_secret": None,  # PUBLIC client -> PKCE, no secret
            "server_metadata_url": f"{_ISSUER}/.well-known/openid-configuration",
            "api_base_url": f"{_ISSUER}/protocol/openid-connect",
            "client_kwargs": {
                "scope": "openid email profile",
                "code_challenge_method": "S256",       # PKCE
                "token_endpoint_auth_method": "none",  # do not send client credentials
            },
        },
    }
]


def _claims(token):
    """Decode a JWT payload (no signature check; token came from Keycloak over TLS)."""
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))


class AutoRedirectAuthOAuthView(AuthOAuthView):
    """Skip FAB's 'Sign in with keycloak' page: with a single provider go straight to Keycloak."""

    @expose("/login/")
    @expose("/login/<provider>")
    def login(self, provider=None):
        if g.user is not None and g.user.is_authenticated:
            return redirect(self.appbuilder.get_url_for_index)
        return super().login(provider=provider or "keycloak")


class HdSecurityManager(FabAirflowSecurityManagerOverride):
    authoauthview = AutoRedirectAuthOAuthView

    def get_oauth_user_info(self, provider, response):
        if provider != "keycloak":
            return {}
        c = _claims(response["access_token"])
        realm_roles = c.get("realm_access", {}).get("roles", [])
        client_roles = c.get("resource_access", {}).get(KEYCLOAK_CLIENT_ID, {}).get("roles", [])
        return {
            "username": c.get("preferred_username"),
            "email": c.get("email"),
            "first_name": c.get("given_name", ""),
            "last_name": c.get("family_name", ""),
            "role_keys": realm_roles + client_roles,
        }

    def _oauth_calculate_user_roles(self, userinfo):
        """Static mapping for airflow_*, pass-through for dynamic DD_<domain> roles.

        The DD_<domain> FAB roles get their DAG permissions from the dag_policy
        (access_control) at parse time; here we just grant the same-named role
        when the Keycloak token carries it.
        """
        roles = set(super()._oauth_calculate_user_roles(userinfo))
        for key in userinfo.get("role_keys", []):
            if key.startswith("DD_"):
                role = self.find_role(key) or self.add_role(key)
                if role is not None:
                    roles.add(role)
        return list(roles)


SECURITY_MANAGER_CLASS = HdSecurityManager
