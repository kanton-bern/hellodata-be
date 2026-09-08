#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# FAB security manager for Airflow 2 (HelloDATA). OAuth to Keycloak with portal
# cookie-SSO: a valid portal Keycloak token in the auth.access_token cookie logs
# the user straight in (no interactive screen); otherwise the standard OAuth
# authorization-code flow runs. All Keycloak connection details come from env
# (KEYCLOAK_BASE_URL = the full realm issuer, KEYCLOAK_CLIENT_ID), set by the
# chart / docker-compose, so this file ships verbatim in the image.
#
from airflow.www.fab_security.manager import AUTH_OAUTH, AUTH_REMOTE_USER
from airflow.www.security import AirflowSecurityManager
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
from flask import get_flashed_messages, g, request, redirect, flash, Response, url_for, request, session, app, Flask
from flask_appbuilder import expose
from flask_appbuilder._compat import as_unicode
from flask_appbuilder.security.views import AuthView
from flask_appbuilder.utils.base import get_safe_redirect
from flask_appbuilder.views import expose
from flask_login import login_user, logout_user
from random import SystemRandom
from typing import Optional
from werkzeug.wrappers import Response as WerkzeugResponse
from datetime import timedelta
import jwt
import logging
import os
import re
import requests
import string


basedir = os.path.abspath(os.path.dirname(__file__))
logging.getLogger('airflow.www.fab_security').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security').setLevel(logging.DEBUG)
logging.getLogger('flask_appbuilder.security.manager').setLevel(logging.DEBUG)

log = logging.getLogger(__name__)
log.setLevel(logging.DEBUG)

WTF_CSRF_ENABLED = False
AUTH_TYPE = AUTH_OAUTH
AUTH_ROLE_ADMIN = 'Admin'
AUTH_USER_REGISTRATION_ROLE = "Public"
AUTH_USER_REGISTRATION = True

# --- Keycloak connection (env-driven, so the same image works per environment) ---
# KEYCLOAK_BASE_URL is the full realm issuer, e.g. https://sso.be.ch/auth/realms/hellodata
OIDC_ISSUER = os.environ["KEYCLOAK_BASE_URL"].rstrip("/")
OIDC_BASE_URL = "{oidc_issuer}/protocol/openid-connect".format(oidc_issuer=OIDC_ISSUER)
OIDC_TOKEN_URL = "{oidc_base_url}/token".format(oidc_base_url=OIDC_BASE_URL)
OIDC_AUTH_URL = "{oidc_base_url}/auth".format(oidc_base_url=OIDC_BASE_URL)

OAUTH_PROVIDERS = [
  {
    "name": "keycloak",
    "icon": "fa-key",
    "token_key": "access_token",
    "remote_app": {
      "client_id": os.environ.get("KEYCLOAK_CLIENT_ID", "frontend-client"),
      "client_secret": "not required",
      "api_base_url":  OIDC_BASE_URL,
      "access_token_url":  OIDC_TOKEN_URL,
      "authorize_url":  OIDC_AUTH_URL,
      "authorize_params": {"kc_idp_hint": "adfs"},
      "client_kwargs": {"scope": "email profile openid"},
      "request_token_url": None
    }
  }
]

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
  def login(self, provider: Optional[str] = None) -> WerkzeugResponse:
    token = request.cookies.get('auth.access_token')
    if token:
      log.info("got token from the cookie")
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
      userinfo["username"] = userinfo["email"]
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
