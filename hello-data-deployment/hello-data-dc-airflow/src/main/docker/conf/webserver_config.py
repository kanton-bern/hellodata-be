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

import jwt
import logging
import os
import requests
import string
from airflow.www.fab_security.manager import AUTH_OAUTH
from airflow.www.security import AirflowSecurityManager
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
from flask import g, request, redirect, flash, url_for, session, Flask
from flask_appbuilder import expose
from flask_appbuilder._compat import as_unicode
from flask_appbuilder.security.views import AuthView
from flask_appbuilder.utils.base import get_safe_redirect
from flask_login import login_user, logout_user
from random import SystemRandom
from typing import Optional
from werkzeug.wrappers import Response as WerkzeugResponse

basedir = os.path.abspath(os.path.dirname(__file__))
logging.getLogger('airflow.www.fab_security').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security').setLevel(logging.DEBUG)

log = logging.getLogger(__name__)

WTF_CSRF_ENABLED = False
AUTH_TYPE = AUTH_OAUTH
# AUTH_TYPE = AUTH_REMOTE_USER
AUTH_ROLE_ADMIN = 'Admin'
AUTH_USER_REGISTRATION_ROLE = "Viewer"
# AUTH_USER_REGISTRATION_ROLE = "Admin"
AUTH_USER_REGISTRATION = True
AUTH_ROLES_MAPPING = {
    "airflow_admin": ["Admin"],
    "airflow_op": ["Op"],
    "airflow_user": ["User"],
    "airflow_viewer": ["Viewer"],
    "airflow_public": ["Public"],
}

keycloak_metadata_url = os.getenv('KEYCLOAK_METADATA_URL',
                                  'http://keycloak:8080/realms/hellodata/.well-known/openid-configuration')
api_base_url = os.getenv('KEYCLOAK_API_BASE_URL', 'http://keycloak:8080/realms/hellodata/protocol/')

log.info('keycloak_metadata_url')
log.info(keycloak_metadata_url)
log.info('api_base_url')
log.info(api_base_url)

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

OIDC_ISSUER = 'http://keycloak:8080/realms/hellodata'
log.info("-=-=-=-=Going to call authorize for: ".format(OIDC_ISSUER))
req = requests.get(OIDC_ISSUER)
log.info("-=-=-=-=Authorize info: {0}".format(req.json()))
response_data = req.json()
key_der_base64 = response_data.get("public_key")
if key_der_base64 is None:
    log.error("Key 'public_key' not found in response: {0}".format(response_data))
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
    @expose("/login/<provider>/<register>")
    def login(self, provider: Optional[str] = None) -> WerkzeugResponse:
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
                next = request.args.get('next')
                if next:
                    return redirect(get_safe_redirect(next))
                return redirect(self.appbuilder.get_url_for_index)
        log.debug("Provider: {0}".format(provider))
        if g.user is not None and g.user.is_authenticated:
            log.debug("Already authenticated {0}".format(g.user))
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


class HdSecurityManager(AirflowSecurityManager):
    authoauthview = HdAuthOAuthView

    def __init__(self, appbuilder):
        super(HdSecurityManager, self).__init__(appbuilder)


SECURITY_MANAGER_CLASS = HdSecurityManager
CUSTOM_SECURITY_MANAGER = HdSecurityManager
SESSION_REFRESH_EACH_REQUEST = True
ENABLE_PROXY_FIX = True
AIRFLOW__API__AUTH_BACKENDS: 'airflow.api.auth.backend.basic_auth'
