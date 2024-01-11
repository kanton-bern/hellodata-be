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

"""Default configuration for the Airflow webserver"""
import jwt
import logging
import os
import re
import requests
import string
from airflow.www.fab_security.manager import AUTH_OAUTH
from airflow.www.security import AirflowSecurityManager
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
from datetime import timedelta
from flask import g, redirect, flash, url_for, request, session, app
from flask_appbuilder._compat import as_unicode
from flask_appbuilder.security.views import AuthView
from flask_appbuilder.utils.base import get_safe_redirect
from flask_appbuilder.views import expose
from flask_login import login_user, logout_user
from random import SystemRandom
from typing import Optional
from werkzeug.wrappers import Response as WerkzeugResponse

basedir = os.path.abspath(os.path.dirname(__file__))
log = logging.getLogger(__name__)
logging.getLogger(__name__).setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security.manager').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security').setLevel(logging.DEBUG)

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

# REQUIRED ENV VARS:
# KEYCLOAK_CLIENT_ID
# KEYCLOAK_BASE_URL e.g.: http://keycloak:8080/realms/hello-data-local

# "client_secret":,
# "server_metadata_url": os.environ['KEYCLOAK_SERVER_METADATA_URL'],
# "api_base_url": os.environ['KEYCLOAK_API_BASE_URL'],

# CLIENT_SECRET = 'Sb3FraabOnGVwsCl7IdXUzuswqt6LWwN'
OIDC_ISSUER = os.environ['KEYCLOAK_BASE_URL']
# When using OAuth Auth, uncomment to set up provider(s) info
PROVIDER_NAME = 'keycloak'

OIDC_BASE_URL = "{oidc_issuer}/protocol/openid-connect".format(oidc_issuer=OIDC_ISSUER)
OIDC_TOKEN_URL = "{oidc_base_url}/token".format(oidc_base_url=OIDC_BASE_URL)
OIDC_AUTH_URL = "{oidc_base_url}/auth".format(oidc_base_url=OIDC_BASE_URL)
OAUTH_PROVIDERS = [{
    'name': PROVIDER_NAME,
    'token_key': 'access_token',
    'icon': 'fa-circle-o',
    'remote_app': {
        'api_base_url': OIDC_BASE_URL,
        'access_token_url': OIDC_TOKEN_URL,
        'authorize_url': OIDC_AUTH_URL,
        'request_token_url': None,
        'client_id': os.environ['KEYCLOAK_CLIENT_ID'],
        # 'client_secret': os.environ['KEYCLOAK_CLIENT_SECRET'],
        'client_kwargs': {
            'scope': 'email profile'
        },
    }
}]

req = requests.get(OIDC_ISSUER)
key_der_base64 = req.json()["public_key"]
key_der = b64decode(key_der_base64.encode())
public_key = serialization.load_der_public_key(key_der)

basedir = os.path.abspath(os.path.dirname(__file__))
logging.getLogger('airflow.www.fab_security').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security').setLevel(logging.DEBUG)

LETTERS_AND_DIGITS = string.ascii_letters + string.digits


def generate_random_string(length=30):
    rand = SystemRandom()
    return "".join(rand.choice(LETTERS_AND_DIGITS) for _ in range(length))


with app.app_context():  # runs before FIRST request (only once)
    session.permanent = True
    app.permanent_session_lifetime = timedelta(hours=9)


class HdAuthOAuthView(AuthView):
    login_template = "appbuilder/general/security/login_oauth.html"

    @expose("/login/")
    @expose("/login/<provider>")
    @expose("/login/<provider>/<register>")
    def login(self, provider: Optional[str] = None) -> WerkzeugResponse:
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
        try:
            if provider == "twitter":
                return self.appbuilder.sm.oauth_remotes[provider].authorize_redirect(
                    redirect_uri=url_for(
                        ".oauth_authorized",
                        provider=provider,
                        _external=True,
                        state=state,
                    )
                )
            else:
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
            session.permanent = True
            app.permanent_session_lifetime = timedelta(hours=9)
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

# ----------------------------------------------------
# Theme CONFIG
# ----------------------------------------------------
# Flask App Builder comes up with a number of predefined themes
# that you can use for Apache Airflow.
# http://flask-appbuilder.readthedocs.io/en/latest/customizing.html#changing-themes
# Please make sure to remove "navbar_color" configuration from airflow.cfg
# in order to fully utilize the theme. (or use that property in conjunction with theme)
# APP_THEME = "bootstrap-theme.css"  # default bootstrap
# APP_THEME = "amelia.css"
# APP_THEME = "cerulean.css"
# APP_THEME = "cosmo.css"
# APP_THEME = "cyborg.css"
# APP_THEME = "darkly.css"
# APP_THEME = "flatly.css"
# APP_THEME = "journal.css"
# APP_THEME = "lumen.css"
# APP_THEME = "paper.css"
# APP_THEME = "readable.css"
# APP_THEME = "sandstone.css"
# APP_THEME = "simplex.css"
# APP_THEME = "slate.css"
# APP_THEME = "solar.css"
# APP_THEME = "spacelab.css"
# APP_THEME = "superhero.css"
# APP_THEME = "united.css"
# APP_THEME = "yeti.css"
