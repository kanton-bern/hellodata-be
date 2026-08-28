#
# Copyright © 2024, Kanton Bern
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name of the <organization> nor the
#       names of its contributors may be used to endorse or promote products
#       derived from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

#
# This is an example "local" configuration file. In order to set/override config
# options that ONLY apply to your local environment, simply copy/rename this file
# to docker/pythonpath/superset_config_docker.py
# It ends up being imported by docker/superset_config.py which is loaded by
# superset/config.py
#

# Personalized configurations

# ---------------------------------------------------
# Babel config for translations
# ---------------------------------------------------
# Your application default translation path
BABEL_DEFAULT_FOLDER = "superset/translations"
# The allowed translation for you app
LANGUAGES = {
    "en": {"flag": "us", "name": "English"},
    "fr": {"flag": "fr", "name": "French"},
    "de": {"flag": "de", "name": "German"},
}

WTF_CSRF_ENABLED = False
FEATURE_FLAGS = {
    'DASHBOARD_RBAC': True,
    'ENABLE_JAVASCRIPT_CONTROLS': True,
    'EMBEDDED_SUPERSET': True,
    # HELLODATA-4067: enable chart screenshots (cache_screenshot REST API) for the portal PDF export.
    'THUMBNAILS': True,
    'THUMBNAILS_SQLA_LISTENERS': True,
}

import jwt
import logging
import os
import re
import requests
import string
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
from flask import flash, redirect, request, session, url_for
from flask_appbuilder._compat import as_unicode
from flask_appbuilder.security.manager import AUTH_OAUTH
from flask_appbuilder.security.views import AuthView
from flask_appbuilder.utils.base import get_safe_redirect
from flask_appbuilder.views import expose
from flask_login import login_user, logout_user
from random import SystemRandom
from superset.security import SupersetSecurityManager
from superset.tasks.types import ExecutorType
from typing import Optional
from werkzeug.wrappers import Response as WerkzeugResponse

# HELLODATA-4067: render chart screenshots as the requesting user so their row-level-security (RLS)
# filters apply; fall back to the selenium/thumbnail user for system renders.
THUMBNAIL_EXECUTE_AS = [ExecutorType.CURRENT_USER, ExecutorType.SELENIUM]

log = logging.getLogger(__name__)
logging.getLogger(__name__).setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security.manager').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security').setLevel(logging.DEBUG)

keycloak_metadata_url = os.getenv('KEYCLOAK_SERVER_METADATA_URL',
                                  'http://keycloak.localhost:38080/realms/hellodata/.well-known/openid-configuration')
api_base_url = os.getenv('KEYCLOAK_API_BASE_URL', 'http://keycloak.localhost:38080/realms/hellodata/protocol/')

log.info('keycloak_metadata_url')
log.info(keycloak_metadata_url)
log.info('api_base_url')
log.info(api_base_url)

# Custom security manager
# ---------------------------------------------------

OIDC_ISSUER = 'http://keycloak.localhost:38080/realms/hellodata'
req = requests.get(OIDC_ISSUER)
key_der_base64 = req.json()["public_key"]
key_der = b64decode(key_der_base64.encode())
public_key = serialization.load_der_public_key(key_der)

LETTERS_AND_DIGITS = string.ascii_letters + string.digits


def generate_random_string(length=30):
    rand = SystemRandom()
    return "".join(rand.choice(LETTERS_AND_DIGITS) for _ in range(length))


class HdAuthOAuthView(AuthView):
    login_template = "appbuilder/general/security/login_oauth.html"

    @expose("/login/")
    @expose("/login/<provider>")
    def login(self, provider: Optional[str] = None):
        token = request.cookies.get('auth.access_token')
        if token:
            log.info("got token")
            decoded_token = None
            try:
                decoded_token = jwt.decode(token, public_key, algorithms=['HS256', 'RS256'], audience='account')
            except jwt.exceptions.ExpiredSignatureError:
                log.error("Token has expired")
            if decoded_token is not None:
                ab_security_manager = self.appbuilder.sm
                userinfo = {
                    "username": decoded_token.get("email"),
                    "email": decoded_token.get("email"),
                    "first_name": decoded_token.get("given_name"),
                    "last_name": decoded_token.get("family_name"),
                }
                decoded_token = ab_security_manager.auth_user_oauth(userinfo)
                login_user(decoded_token, remember=True)

                lang = request.args.get('lang')
                if lang:
                    log.info("Setting Lang from request: " + lang)
                    session["locale"] = lang

                next = request.args.get('next')
                if next:
                    log.info("Redirecting to : " + next)
                    return redirect(get_safe_redirect(next))
                return redirect(self.appbuilder.get_url_for_index)
        if provider is None:
            return self.render_template(
                self.login_template,
                providers=self.appbuilder.sm.oauth_providers,
                title=self.title,
                appbuilder=self.appbuilder,
            )
        log.debug("Going to call authorize for: {0}".format(provider))
        random_state = generate_random_string()
        state = jwt.encode(
            request.args.to_dict(flat=False), random_state, algorithm="HS256"
        )
        session["oauth_state"] = random_state
        session.permanent = True
        try:
            return self.appbuilder.sm.oauth_remotes[provider].authorize_redirect(
                redirect_uri=url_for(
                    ".oauth_authorized", provider=provider, _external=True
                ),
                state=state.decode("ascii") if isinstance(state, bytes) else state,
            )
        except Exception as e:
            log.error("Error on OAuth authorize: {0}".format(e))
            flash(as_unicode(self.invalid_login_message), "warning")
            return redirect(self.appbuilder.get_url_for_index)

    @expose("/oauth-authorized/<provider>")
    def oauth_authorized(self, provider: str) -> WerkzeugResponse:
        log.debug("Authorized init")
        if provider not in self.appbuilder.sm.oauth_remotes:
            flash("Provider not supported.", "warning")
            log.warning("OAuth authorized got an unknown provider %s", provider)
            return redirect(self.appbuilder.get_url_for_login)
        try:
            resp = self.appbuilder.sm.oauth_remotes[provider].authorize_access_token()
        except Exception as e:
            log.error("Error authorizing OAuth access token: {0}".format(e))
            flash("The request to sign in was denied.", "error")
            return redirect(self.appbuilder.get_url_for_login)
        if resp is None:
            flash("You denied the request to sign in.", "warning")
            return redirect(self.appbuilder.get_url_for_login)
        log.debug("OAUTH Authorized resp: {0}".format(resp))
        # Retrieves specific user info from the provider
        try:
            self.appbuilder.sm.set_oauth_session(provider, resp)
            userinfo = self.appbuilder.sm.oauth_user_info(provider, resp)
            log.info("-------------------------> User info: {0}".format(userinfo))
            username = userinfo.get("username")
            email = userinfo.get("email")
            log.info("-------------------------> User name: {0}".format(username))
            log.info("-------------------------> User email: {0}".format(email))
            userinfo["username"] = email
            log.info("-------------------------> User info: {0}".format(userinfo))
        except Exception as e:
            log.error("Error returning OAuth user info: {0}".format(e))
            user = None
        else:
            log.debug("User info retrieved from {0}: {1}".format(provider, userinfo))
            # User email is not whitelisted
            if provider in self.appbuilder.sm.oauth_whitelists:
                whitelist = self.appbuilder.sm.oauth_whitelists[provider]
                allow = False
                for email in whitelist:
                    if "email" in userinfo and re.search(email, userinfo["email"]):
                        allow = True
                        break
                if not allow:
                    flash("You are not authorized.", "warning")
                    return redirect(self.appbuilder.get_url_for_login)
            else:
                log.debug("No whitelist for OAuth provider")
            user = self.appbuilder.sm.auth_user_oauth(userinfo)

        if user is None:
            flash(as_unicode(self.invalid_login_message), "warning")
            return redirect(self.appbuilder.get_url_for_login)
        else:
            try:
                state = jwt.decode(
                    request.args["state"], session["oauth_state"], algorithms=["HS256"]
                )
            except (jwt.InvalidTokenError, KeyError):
                flash(as_unicode("Invalid state signature"), "warning")
                return redirect(self.appbuilder.get_url_for_login)

            login_user(user)
            next_url = self.appbuilder.get_url_for_index
            # Check if there is a next url on state
            if "next" in state and len(state["next"]) > 0:
                next_url = get_safe_redirect(state["next"][0])
            return redirect(next_url)

    @expose("/logout/")
    def logout(self) -> WerkzeugResponse:
        logout_user()
        session.pop('username', None)
        redirect_param = request.args.get('redirect')
        if redirect_param:
            log.debug("LOGOUT CALL - redirect found: {0}".format(redirect_param))
            return redirect(f"{redirect_param}")
        else:
            return redirect("/")


class HdSecurityManager(SupersetSecurityManager):
    authoauthview = HdAuthOAuthView

    def __init__(self, appbuilder):
        super(HdSecurityManager, self).__init__(appbuilder)


# This will make sure the redirect_uri is properly computed, even with SSL offloading
ENABLE_PROXY_FIX = True

AUTH_TYPE = AUTH_OAUTH

logging.getLogger('flask_appbuilder.security.manager').setLevel(logging.DEBUG)

OAUTH_PROVIDERS = [
    {
        "name": "keycloak",
        "icon": "fa-key",
        "token_key": "access_token",
        "remote_app": {
            "client_id": 'frontend-client',
            "client_secret": 'not required',
            "server_metadata_url": keycloak_metadata_url,
            "api_base_url": api_base_url,
            "authorize_params": {"kc_idp_hint": "adfs"},
            "client_kwargs": {"scope": "email profile openid"},
            "request_token_url": None
        }
    }
]
# Map Authlib roles to superset roles
AUTH_ROLE_ADMIN = 'Admin'
AUTH_ROLE_PUBLIC = 'Public'
# Will allow user self registration, allowing to create Flask users from Authorized User
AUTH_USER_REGISTRATION = True
# The default user self registration role
AUTH_USER_REGISTRATION_ROLE_JMESPATH = "contains(['admin@hellodata.ch'], email) && 'Admin' || 'BI_VIEWER'"
FAB_ADD_SECURITY_API = True
SESSION_REFRESH_EACH_REQUEST = True

# --Enable these to add custom Securitymanager---------------------
SECURITY_MANAGER_CLASS = HdSecurityManager
CUSTOM_SECURITY_MANAGER = HdSecurityManager
# --Enable these to add custom Securitymanager---------------------


TALISMAN_ENABLED = False
TALISMAN_CONFIG = {
    "content_security_policy": {
        "default-src": ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
        "img-src": ["'self'", "data:", "blob:"],
        "worker-src": ["'self'", "blob:"],
        "connect-src": ["'self'", "wss:", "https://api.mapbox.com", "https://events.mapbox.com"],
        "object-src": "'none'",
        "frame_options": "ALLOW_FROM",
        "frame_options_allow_from": "*"
    },
    "force_https": False,
    "force_https_permanent": False,
}


# --- HELLODATA-4067: per-user screenshot impersonation -----------------------------------------
# Admin-only endpoint that mints a short-lived Superset access token for a given user, so the
# portal PDF export can render chart screenshots as that user (their row-level-security filters
# apply). Paired with THUMBNAIL_EXECUTE_AS = [CURRENT_USER, SELENIUM] above.
from flask import Blueprint, current_app, jsonify
from flask_jwt_extended import create_access_token, current_user, verify_jwt_in_request

hd_impersonation_bp = Blueprint("hd_impersonation", __name__)


@hd_impersonation_bp.route("/api/v1/hd_impersonation/token", methods=["POST"])
def hd_impersonation_token():
    # Requires a valid Superset JWT (the sidecar's admin/technical account).
    verify_jwt_in_request()
    caller = current_user
    if caller is None or not any(getattr(r, "name", None) == "Admin" for r in (getattr(caller, "roles", None) or [])):
        return jsonify(message="forbidden"), 403
    email = (request.get_json(silent=True) or {}).get("email")
    if not email:
        return jsonify(message="email is required"), 400
    target = current_app.appbuilder.sm.find_user(email=email)
    if target is None:
        return jsonify(message="user not found"), 404
    # Pass the user object so Superset's JWT identity loader stores its id (same as /security/login).
    access_token = create_access_token(identity=target, fresh=False)
    return jsonify(access_token=access_token), 200


def _register_hd_impersonation(app):
    app.register_blueprint(hd_impersonation_bp)
    try:
        app.extensions["csrf"].exempt(hd_impersonation_bp)
    except Exception:  # noqa: BLE001 - CSRF may be disabled; exemption is then unnecessary
        pass


FLASK_APP_MUTATOR = _register_hd_impersonation
# -----------------------------------------------------------------------------------------------
