#
# Copyright © 2024, Kanton Bern
# SPDX-License-Identifier: BSD-3-Clause
#
# All HelloDATA Superset config, baked into the hello-data-superset image on the
# PYTHONPATH (/app/pythonpath-extra). Per-environment values come from env (set by
# the chart, see superset/templates/secret-env.yaml); no Helm templating here, so the
# module ships verbatim. superset_config.py pulls it in as the FIRST configOverride:
#   from hd_superset_config import *
# so any later chart / tenant configOverride can still override these defaults.
#
import os


def env(key, default=None):
    # Mirror of the chart base superset_config env() helper.
    return os.getenv(key, default)


# --- values provided by the chart via env (superset/templates/secret-env.yaml) ---
DATA_DOMAIN_KEY = os.environ["DATA_DOMAIN_KEY"]
DATA_DOMAIN_NAME = os.getenv("DATA_DOMAIN_NAME", "")
BUSINESS_DOMAIN_KEY = os.environ["BUSINESS_DOMAIN_KEY"]
SUPERSET_HOST = os.environ["SUPERSET_HOST"]
KEYCLOAK_BASE_URL = os.environ["KEYCLOAK_BASE_URL"]
KEYCLOAK_CLIENT_ID = os.environ["KEYCLOAK_CLIENT_ID"]
DB_SSLMODE = os.getenv("DB_SSLMODE", "")

# --- app_config ---
APP_NAME = f"HelloDATA {DATA_DOMAIN_NAME}"
# Specify the App icon
# APP_ICON = "/static/assets/images/superset-logo-horiz.png"
# Specify where clicking the logo would take the user
# e.g. setting it to '/' would take the user to '/superset/welcome/'
LOGO_TARGET_PATH = "#"
# Specify tooltip that should appear when hovering over the App Icon/Logo
LOGO_TOOLTIP = "HelloDATA BE"

WTF_CSRF_EXEMPT_LIST = [
  "superset.views.core.log",
  "superset.views.core.explore_json",
  "superset.charts.data.api.data",
  "superset.log",
]

# see: https://superset.apache.org/docs/security/
# force_https:-> set to "False" otherwise screenshoting does not work internally
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

#
# Flask session cookie options
#
# See https://flask.palletsprojects.com/en/1.1.x/security/#set-cookie-options
# for details
#
SESSION_COOKIE_HTTPONLY = False  # Prevent cookie from being read by frontend JS?
SESSION_COOKIE_SECURE = True  # Prevent cookie from being transmitted over non-tls?
SESSION_COOKIE_SAMESITE = "None"  # One of [None, 'None', 'Lax', 'Strict']

# PUBLIC_ROLE_LIKE = "BI_VIEWER"
ENABLE_PROXY_FIX = True
# HTTP_HEADERS = {'X-Frame-Options': '*'}
#      ENABLE_CORS = True
#      CORS_OPTIONS = {
#        'supports_credentials': True,
#        'allow_headers': ['*'],
#        'resources':['*'],
#        'origins': ['*']
#      }

# disable fab api page size
FAB_API_MAX_PAGE_SIZE = -1

HTML_SANITIZATION_SCHEMA_EXTENSIONS = {
  "attributes": {
    "*": ["style","className", "hidden", "width", "height", "controls", "src"],
  },
  "tagNames": ["style", "video", "source"],
}

D3_FORMAT = {
  "decimal": ".",
  "thousands": "'",
  "grouping": [3],
  "currency": ["CHF", ""]
}

import json
import logging
import os
import requests

logger = logging.getLogger("superset.custom_event_logger")
from superset.utils.log import AbstractEventLogger

class MatomoEventLogger(AbstractEventLogger):
  MATOMO_URL = os.getenv("MATOMO_URL", None)
  MATOMO_DEBUG = os.getenv("MATOMO_DEBUG", "false").lower() in ("1", "true", "yes")
  SITE_ID = os.getenv("MATOMO_SITE_ID", '0')
  TRACKING_TOKEN = os.getenv("MATOMO_TRACKING_TOKEN","")
  SUPERSET_URL = f"https://{SUPERSET_HOST}",

  def log(self, user_id, action, *args, **kwargs):
    if self.MATOMO_URL:
      dashboard_id = kwargs.get("dashboard_id")
      duration_ms = kwargs.get("duration_ms")
      referrer = kwargs.get("referrer")
      records = kwargs.get("records", [])

      event_data = {
        'idsite': self.SITE_ID,
        'rec': 1,
        'url': referrer or self.SUPERSET_URL,
        'uid': str(user_id),
        'e_c': f'Superset {DATA_DOMAIN_NAME}',
        'e_a': action,
        # 'e_n': action,
        'e_v': duration_ms or 0,
        'apiv': 1,
        'token_auth': self.TRACKING_TOKEN
      }

      if self.MATOMO_DEBUG:
        logger.info(f"[Matomo Debug Logger] --- user_id: {user_id}")
        logger.info(f"[Matomo Debug Logger] --- action: {action}")
        logger.info(f"[Matomo Debug Logger] --- args: {args}")
        logger.info(f"[Matomo Debug Logger] --- kwargs: {kwargs}")
        logger.info(f"[Matomo Debug Logger] --- records: {records}")
        logger.info(f"[Matomo Debug Logger] --- Event data: {event_data}")

      try:
        response = requests.get(self.MATOMO_URL, params=event_data)
        response.raise_for_status()
        if self.MATOMO_DEBUG:
          logger.info(f"[Matomo Debug Logger] Request URL: {response.url}")
          logger.info(f"[Matomo Debug Logger] Response Status Code: {response.status_code}")
          logger.info(f"[Matomo Debug Logger] Response Content: {response.text}")
          logger.info(f"[Matomo Debug Logger] Response Headers: {response.headers}")
      except requests.exceptions.HTTPError as http_err:
        logger.info(f"[Matomo Debug Logger] HTTP error occurred: {http_err}")
        logger.info(f"[Matomo Debug Logger] Response content: {response.text}")
      except Exception as e:
        logger.info(f"[Matomo Debug Logger] Failed to send event: {e}")

# EVENT_LOGGER = MatomoEventLogger()

# --- base_app_config ---
# max dashboard size is 1GB
SUPERSET_DASHBOARD_POSITION_DATA_LIMIT = 1073741824

# --- celery_config ---
from celery.schedules import crontab

class CeleryConfig(object):
  broker_url = f"redis://{env('REDIS_HOST')}:{env('REDIS_PORT')}/{env('REDIS_CELERY_DB', 0)}"
  imports = ('superset.sql_lab', 'superset.tasks', 'superset.tasks.thumbnails', )
  result_backend = f"redis://{env('REDIS_HOST')}:{env('REDIS_PORT')}/{env('REDIS_RESULTS_DB', 0)}"
  worker_log_level = "DEBUG"
  task_annotations = {
  'sql_lab.get_sql_results': {
    'rate_limit': '100/s',
  },
  'email_reports.send': {
    'rate_limit': '1/s',
    'time_limit': 600,
    'soft_time_limit': 600,
    'ignore_result': True,
  },
  }
  beat_schedule = {
  'reports.scheduler': {
    'task': 'reports.scheduler',
    'schedule': crontab(minute='*', hour='*'),
  },
  'reports.prune_log': {
    'task': 'reports.prune_log',
    'schedule': crontab(minute=0, hour=0),
  },
  'cache-warmup-hourly': {
    'task': 'cache-warmup',
    'schedule': crontab(minute='*/30', hour='*'),
    'kwargs': {
      'strategy_name': 'top_n_dashboards',
      'top_n': 10,
      'since': '7 days ago',
    },
  }
}

CELERY_CONFIG = CeleryConfig

# SCREENSHOT_LOCATE_WAIT = 100
# SCREENSHOT_LOAD_WAIT = 600

ESTIMATE_QUERY_COST = True

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

# --- custom_security_manager ---
# hd_security_manager.py is baked into the image on the PYTHONPATH (/app/pythonpath-extra);
# it reads KEYCLOAK_BASE_URL from env (see secret-env.yaml). No chart Secret/mount.
from hd_security_manager import HdSecurityManager
SECURITY_MANAGER_CLASS = HdSecurityManager
CUSTOM_SECURITY_MANAGER = HdSecurityManager

    ## must override SQLALCHEMY_DATABASE_URI if DB requires specific sslmode

# --- domain_sharding ---
SUPERSET_WEBSERVER_DOMAINS={SUPERSET_HOST}
SESSION_COOKIE_DOMAIN = SUPERSET_HOST
ENABLE_CORS = True
CORS_OPTIONS = {
  'supports_credentials': True,
  'allow_headers': ['*'],
  'resources':['*'],
  'origins': ['*']
}

# --- enable_languages ---
LANGUAGES = {
  "de": {"flag": "de", "name": "German"},
  "fr": {"flag": "fr", "name": "French"},
  "en": {"flag": "us", "name": "English"},
}

    # --> follow installation: https://superset.apache.org/docs/installation/alerts-reports/

# --- enable_logging ---
ENABLE_TIME_ROTATE = True

# --- enable_oauth ---
# This will make sure the redirect_uri is properly computed, even with SSL offloading
ENABLE_PROXY_FIX = True
from flask_appbuilder.security.manager import AUTH_OAUTH
AUTH_TYPE = AUTH_OAUTH
import logging
logging.getLogger('flask_appbuilder.security.manager').setLevel(logging.DEBUG)
OAUTH_PROVIDERS = [
  {
    "name": "keycloak",
    "icon": "fa-key",
    "token_key": "access_token",
    "remote_app": {
      "client_id": KEYCLOAK_CLIENT_ID,
      "client_secret": "not required",
      "server_metadata_url": f"{KEYCLOAK_BASE_URL}/.well-known/openid-configuration",
      "api_base_url":  f"{KEYCLOAK_BASE_URL}/protocol/",
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
AUTH_USER_REGISTRATION_ROLE_JMESPATH = "contains(['techadmin@hellodatabedag.ch', 'admin@hellodatabedag.ch'], email) && 'Admin' || 'BI_VIEWER'"
FAB_ADD_SECURITY_API=True

# --- enable_statsd_logging ---
from superset.stats_logger import StatsdStatsLogger
STATS_LOGGER = StatsdStatsLogger(host=os.getenv("STATSD_HOST", "localhost"), port=int(os.getenv("STATSD_PORT", 9125)), prefix=f"superset-{DATA_DOMAIN_KEY}")

# --- feature_flags ---
FEATURE_FLAGS = {
  'ENABLE_JAVASCRIPT_CONTROLS': True,     # Allow for javascript controls components
  'ENABLE_TEMPLATE_PROCESSING': True,
  'ENABLE_TEMPLATE_REMOVE_FILTERS': True,
  'DASHBOARD_RBAC': True,
  'ALERT_REPORTS': True,
  # HELLODATA-4067: enable the chart screenshot REST API (cache_screenshot) used by the
  # portal PDF export. Requires the Celery worker (supersetWorker) to render screenshots.
  'THUMBNAILS': True,
  'THUMBNAILS_SQLA_LISTENERS': True,
  'GLOBAL_ASYNC_QUERIES': False,
  'HORIZONTAL_FILTER_BAR': True,
  'GENERIC_CHART_AXES': True,             # Allow for a non-time x-axis in timeseries charts
}
GLOBAL_ASYNC_QUERIES_JWT_SECRET = env("REDIS_JWT_SECRET")
GLOBAL_ASYNC_QUERIES_REDIS_CONFIG = {
    "port": env("REDIS_PORT"),
    "host": env("REDIS_HOST"),
    "password": "",
    "db": 0,
    "ssl": False,
}

# --- filter_cache_config ---
REDIS_HOST = env("REDIS_HOST")
REDIS_PORT = env("REDIS_PORT")
REDIS_CELERY_DB = env("REDIS_CELERY_DB", 0)
REDIS_RESULTS_DB = env("REDIS_RESULTS_DB", 0)

# enable results caching
from cachelib.redis import RedisCache
RESULTS_BACKEND = RedisCache(host=env('REDIS_HOST'), port=env('REDIS_PORT'), key_prefix=f'superset_results_{DATA_DOMAIN_KEY}_')

# Dashboard filter state (required)
FILTER_STATE_CACHE_CONFIG = {
  'CACHE_TYPE': 'redis',
  'CACHE_DEFAULT_TIMEOUT': 86400,
  'CACHE_KEY_PREFIX': f'superset_filter_{DATA_DOMAIN_KEY}_',
  'CACHE_REDIS_HOST': env('REDIS_HOST'),
  'CACHE_REDIS_PORT': env('REDIS_PORT'),
  'CACHE_REDIS_PASSWORD': env('REDIS_PASSWORD'),
  'CACHE_REDIS_DB': env('REDIS_DB', 1),
  # should the timeout be reset when retrieving a cached value
  'REFRESH_TIMEOUT_ON_RETRIEVAL': True,
}
# Explore chart form data (required)
EXPLORE_FORM_DATA_CACHE_CONFIG = {
  'CACHE_TYPE': 'redis',
  'CACHE_DEFAULT_TIMEOUT': 86400,
  'CACHE_KEY_PREFIX': f'superset_form_data_{DATA_DOMAIN_KEY}_',
  'CACHE_REDIS_HOST': env('REDIS_HOST'),
  'CACHE_REDIS_PORT': env('REDIS_PORT'),
  'CACHE_REDIS_PASSWORD': env('REDIS_PASSWORD'),
  'CACHE_REDIS_DB': env('REDIS_DB', 1),
  # should the timeout be reset when retrieving a cached value
  'REFRESH_TIMEOUT_ON_RETRIEVAL': True,
}
#  Charting data queried from datasets (optional)
DATA_CACHE_CONFIG = {
  "CACHE_TYPE": "redis",
  "CACHE_DEFAULT_TIMEOUT": 86400,
  "CACHE_KEY_PREFIX": f"superset_{DATA_DOMAIN_KEY}_",
  'CACHE_REDIS_HOST': env('REDIS_HOST'),
  'CACHE_REDIS_PORT': env('REDIS_PORT'),
  'CACHE_REDIS_PASSWORD': env('REDIS_PASSWORD'),
  "CACHE_REDIS_DB": env("REDIS_RESULTS_DB", 0),
}
CACHE_CONFIG = {
  "CACHE_TYPE": "redis",
  "CACHE_DEFAULT_TIMEOUT": 86400,
  "CACHE_KEY_PREFIX": f"superset_{DATA_DOMAIN_KEY}_",
  'CACHE_REDIS_HOST': env('REDIS_HOST'),
  'CACHE_REDIS_PORT': env('REDIS_PORT'),
  'CACHE_REDIS_PASSWORD': env('REDIS_PASSWORD'),
  "CACHE_REDIS_DB": env("REDIS_RESULTS_DB", 0),
}

# --- instance_config ---
# This is for internal use, you can keep http
WEBDRIVER_BASEURL = os.environ["SUPERSET_WEBDRIVER_BASEURL"]
# This is the link sent to the recipient. Change to your domain, e.g. https://superset.mydomain.com
WEBDRIVER_BASEURL_USER_FRIENDLY = f"https://{SUPERSET_HOST}"

# --- lookup_password ---
def lookup_password(url):
  import os
  return os.getenv(f"{BUSINESS_DOMAIN_KEY}_{DATA_DOMAIN_KEY}_DWH_U_BIEDITOR_PASSWORD".upper())

SQLALCHEMY_CUSTOM_PASSWORD_STORE = lookup_password

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

# --- secret_key ---
SECRET_KEY=os.environ["SUPERSET_SECRET_KEY"]

# --- smtp ---
import ast
SMTP_HOST = os.getenv("SMTP_HOST","localhost")
SMTP_STARTTLS = ast.literal_eval(os.getenv("SMTP_STARTTLS", "True"))
SMTP_SSL = ast.literal_eval(os.getenv("SMTP_SSL", "False"))
SMTP_USER = os.getenv("SMTP_USER","superset")
SMTP_PORT = os.getenv("SMTP_PORT",25)
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD","superset")

# --- sqlalchemy_ssl_config ---
SQLALCHEMY_DATABASE_URI = f"postgresql+psycopg2://{env('DB_USER')}:{env('DB_PASS')}@{env('DB_HOST')}:{env('DB_PORT')}/{env('DB_NAME')}?sslmode={DB_SSLMODE}"

# --- thumbnail_config ---
from superset.superset_typing import CacheConfig
THUMBNAIL_CACHE_CONFIG: CacheConfig = {
    'CACHE_TYPE': 'redis',
    'CACHE_DEFAULT_TIMEOUT': 24*60*60*7,
    'CACHE_KEY_PREFIX': f'thumbnail_{DATA_DOMAIN_KEY}_',
    'CACHE_REDIS_URL': f"redis://{env('REDIS_HOST')}:{env('REDIS_PORT')}/1"
}
# Async selenium thumbnail task will use the following user
THUMBNAIL_SELENIUM_USER = "techadmin"
# HELLODATA-4067: Superset caches a FAILED screenshot render as status=Error and, by default,
# refuses to re-render it for THUMBNAIL_ERROR_CACHE_TTL (Superset default: 1 day). A single
# transient render failure would therefore poison a chart's PDF-export screenshot for a full
# day (a stuck 404 -> the export times out). Shorten it so a failed render self-heals within
# seconds and the next export attempt re-renders instead of returning the cached error.
THUMBNAIL_ERROR_CACHE_TTL = 10

# --- webserver_timeout ---
SUPERSET_WEBSERVER_TIMEOUT = 600
