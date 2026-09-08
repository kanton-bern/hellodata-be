#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# HelloDATA static Superset config baked into the image. These settings carry no
# per-environment values (no Helm templating) and do not depend on the chart base
# superset_config env() helper, so they ship verbatim. Environment-specific config
# (OAuth, cache prefixes, DB SSL, statsd, Celery/Redis, thumbnails) stays in the
# chart configOverrides. Loaded from superset_config.py via:
#   from hd_superset_config import *
# __all__ restricts the splat to real config keys, so module-internal helpers
# (logger, blueprint, view class) do not leak into / collide with superset_config.
#
import os

__all__ = [
    "SUPERSET_WEBSERVER_TIMEOUT",
    "SQLALCHEMY_ENGINE_OPTIONS",
    "SECRET_KEY",
    "SUPERSET_DASHBOARD_POSITION_DATA_LIMIT",
    "ENABLE_TIME_ROTATE",
    "LANGUAGES",
    "SMTP_HOST",
    "SMTP_STARTTLS",
    "SMTP_SSL",
    "SMTP_USER",
    "SMTP_PORT",
    "SMTP_PASSWORD",
    "THUMBNAIL_EXECUTE_AS",
    "FLASK_APP_MUTATOR",
    "WELCOME_PAGE_REDIRECT_ADMIN",
    "WELCOME_PAGE_REDIRECT_DEFAULT",
    "WELCOME_PAGE_REDIRECT_BY_ROLE",
    "FAB_INDEX_VIEW",
]

# --- webserver_timeout ---
SUPERSET_WEBSERVER_TIMEOUT = 600

# --- connection_pool ---
SQLALCHEMY_ENGINE_OPTIONS = {
  "pool_size": 5,
  "max_overflow": 2,
  "pool_recycle": 1800,
  "pool_pre_ping": True,
  "connect_args": {
    "keepalives": 1,
    "keepalives_idle": 60,
    "keepalives_interval": 10,
    "keepalives_count": 3,
  },
}

# --- secret_key ---
SECRET_KEY=os.environ["SUPERSET_SECRET_KEY"]

# --- base_app_config ---
# max dashboard size is 1GB
SUPERSET_DASHBOARD_POSITION_DATA_LIMIT = 1073741824

# --- enable_logging ---
ENABLE_TIME_ROTATE = True

# --- enable_languages ---
LANGUAGES = {
  "de": {"flag": "de", "name": "German"},
  "fr": {"flag": "fr", "name": "French"},
  "en": {"flag": "us", "name": "English"},
}

# --- smtp ---
import ast
SMTP_HOST = os.getenv("SMTP_HOST","localhost")
SMTP_STARTTLS = ast.literal_eval(os.getenv("SMTP_STARTTLS", "True"))
SMTP_SSL = ast.literal_eval(os.getenv("SMTP_SSL", "False"))
SMTP_USER = os.getenv("SMTP_USER","superset")
SMTP_PORT = os.getenv("SMTP_PORT",25)
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD","superset")

# --- pdf_export_rls ---
# HELLODATA-4067: render chart screenshots as the requesting user so their row-level-security
# (RLS) filters apply; fall back to the selenium/thumbnail user for system renders. Paired with
# an admin-only impersonation endpoint that mints a per-user Superset token for the portal PDF
# export (called by the superset sidecar).
from superset.tasks.types import ExecutorType
THUMBNAIL_EXECUTE_AS = [ExecutorType.CURRENT_USER, ExecutorType.FIXED_USER]

from flask import Blueprint, current_app, jsonify, request as _hd_request
from flask_jwt_extended import create_access_token, current_user, verify_jwt_in_request

hd_impersonation_bp = Blueprint("hd_impersonation", __name__)

@hd_impersonation_bp.route("/api/v1/hd_impersonation/token", methods=["POST"])
def hd_impersonation_token():
    # Requires a valid Superset JWT (the sidecar's admin/technical account).
    verify_jwt_in_request()
    caller = current_user
    if caller is None or not any(getattr(r, "name", None) == "Admin" for r in (getattr(caller, "roles", None) or [])):
        return jsonify(message="forbidden"), 403
    email = (_hd_request.get_json(silent=True) or {}).get("email")
    if not email:
        return jsonify(message="email is required"), 400
    target = current_app.appbuilder.sm.find_user(email=email)
    if target is None:
        return jsonify(message="user not found"), 404
    # Match FAB's /security/login: identity is the stringified user id (stored as the JWT "sub").
    access_token = create_access_token(identity=str(target.id), fresh=False)
    return jsonify(access_token=access_token), 200

def _hd_register_impersonation(app):
    app.register_blueprint(hd_impersonation_bp)
    try:
        app.extensions["csrf"].exempt(hd_impersonation_bp)
    except Exception:  # noqa: BLE001 - CSRF may be disabled; exemption is then unnecessary
        pass

FLASK_APP_MUTATOR = _hd_register_impersonation

# --- override_welcome_page ---
import logging
import pprint
from flask import Flask, redirect
from flask_appbuilder import expose, IndexView
from superset.superset_typing import FlaskResponse
from superset import security_manager
logger = logging.getLogger()
logger.warn("Loading config override for HELLODATA welcome page")

# WELCOME_PAGE_REDIRECT_ADMIN="/superset/dashboard/1/"
WELCOME_PAGE_REDIRECT_ADMIN="/dashboard/list/"
WELCOME_PAGE_REDIRECT_DEFAULT="/dashboard/list/"
WELCOME_PAGE_REDIRECT_BY_ROLE={
  'Gamma': '/dashboard/list/',
}

def is_user_admin() -> bool:
  user_roles = [role.name.lower() for role in list(security_manager.get_user_roles())]
  return "admin" in user_roles

# Change welcome page
# https://stackoverflow.com/a/69930056/1760643
class SupersetDashboardIndexView(IndexView):
  @expose("/")
  def index(self) -> FlaskResponse:
    from superset import security_manager

    user_roles = security_manager.get_user_roles()

    logger.warn('__DEBUG__ user roles: ' + pprint.pformat(user_roles))

    if is_user_admin():
        return redirect(WELCOME_PAGE_REDIRECT_ADMIN)
    else:
        for role in user_roles:
            role_name = role.name

            if role_name in WELCOME_PAGE_REDIRECT_BY_ROLE:
                return redirect(WELCOME_PAGE_REDIRECT_BY_ROLE[role_name])

        return redirect(WELCOME_PAGE_REDIRECT_DEFAULT)

FAB_INDEX_VIEW = f"{SupersetDashboardIndexView.__module__}.{SupersetDashboardIndexView.__name__}"
