# Configuration file for JupyterHub
import jwt
import logging
import os
import requests
from base64 import b64decode
from cryptography.hazmat.primitives import serialization
from jupyterhub.app import JupyterHub
from jupyterhub.handlers import BaseHandler
from oauthenticator.generic import GenericOAuthenticator
from tornado import web
from traitlets import default
import sys

from jupyterhub.utils import url_path_join
from tornado.httpclient import AsyncHTTPClient
import logging
from tornado import web
from jupyterhub.handlers import BaseHandler
import jwt
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from tornado.ioloop import IOLoop
from functools import partial
import time
from sqlalchemy.exc import OperationalError



logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# load the config object (satisfies linters)
c = get_config()  # noqa: F821

# Make sure that modules placed in the same directory as the jupyterhub config are added to the pythonpath
configuration_directory = os.path.dirname(os.path.realpath(__file__))
sys.path.insert(0, configuration_directory)


# We rely on environment variables to configure JupyterHub so that we
# avoid having to rebuild the JupyterHub container every time we change a
# configuration parameter.

# Spawn single-user servers as Docker containers
c.JupyterHub.spawner_class = "dockerspawner.DockerSpawner"

# Spawn containers from this image
c.DockerSpawner.image = os.environ["DOCKER_NOTEBOOK_IMAGE"]

# Connect containers to this Docker network
network_name = os.environ["DOCKER_NETWORK_NAME"]
c.DockerSpawner.use_internal_ip = True
c.DockerSpawner.network_name = network_name

# Explicitly set notebook directory because we'll be mounting a volume to it.
# Most `jupyter/docker-stacks` *-notebook images run the Notebook server as
# user `jovyan`, and set the notebook directory to `/home/jovyan/work`.
# We follow the same convention.
notebook_dir = os.environ.get("DOCKER_NOTEBOOK_DIR", "/home/jovyan/work")
c.DockerSpawner.notebook_dir = notebook_dir

# Mount the real user's Docker volume on the host to the notebook user's
# notebook directory in the container
c.DockerSpawner.volumes = {'/var/run/docker.sock': '/var/run/docker.sock'}

# Remove containers once they are stopped
c.DockerSpawner.remove = True

# For debugging arguments passed to spawned containers
c.DockerSpawner.debug = True

# User containers will access hub by container name on the Docker network
# c.JupyterHub.hub_ip = "jupyterhub"
c.JupyterHub.hub_ip = os.environ.get("HUB_IP", "jupyterhub")
c.JupyterHub.hub_port = 8080

# Persist hub data on volume mounted inside container
c.JupyterHub.cookie_secret_file = "/srv/jupyterhub/shared_scripts/jupyterhub_cookie_secret"
c.JupyterHub.db_url = "sqlite:////data/jupyterhub.sqlite"

# Allow anyone to sign-up without approval
c.NativeAuthenticator.open_signup = True

# Allowed admins
admin = os.environ.get("JUPYTERHUB_ADMIN")
if admin:
    c.Authenticator.admin_users = [admin]

c.Authenticator.allow_all = True

# OAuth configuration
c.GenericOAuthenticator.client_id = os.environ['OAUTH2_CLIENT_ID']
c.GenericOAuthenticator.client_secret = os.environ['OAUTH2_CLIENT_SECRET']
c.GenericOAuthenticator.oauth_callback_url = os.environ['OAUTH2_CALLBACK_URL']
c.GenericOAuthenticator.authorize_url = os.environ['OAUTH2_AUTHORIZE_URL']
c.GenericOAuthenticator.token_url = os.environ['OAUTH2_TOKEN_URL']
c.GenericOAuthenticator.userdata_url = os.environ['OAUTH2_USERDATA_URL']
c.GenericOAuthenticator.scope = ['openid', 'profile', 'email']
c.GenericOAuthenticator.login_service = 'Keycloak'
c.GenericOAuthenticator.username_key = 'preferred_username'
c.GenericOAuthenticator.userdata_params = {'state': 'state'}
c.GenericOAuthenticator.allow_all = True
c.JupyterHub.admin_access = True

OIDC_ISSUER = 'http://keycloak:8080/realms/hellodata'
req = requests.get(OIDC_ISSUER)
key_der_base64 = req.json()["public_key"]
key_der = b64decode(key_der_base64.encode())
public_key = serialization.load_der_public_key(key_der)

# Database configuration
db_conn_url = 'postgresql://' + os.environ['DB_USERNAME'] + ':' + os.environ['DB_PASSWORD'] + '@' + os.environ['DB_URL']
engine = create_engine(db_conn_url)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

c.DockerSpawner.environment = {
    "API_URL": os.environ['SIDECAR_API_URL']
}

class HelloDataBearerTokenAuthenticator(GenericOAuthenticator):

    async def authenticate(self, handler, data=None):
        logging.info('===> Authorization process started')
        authorization_header = handler.request.headers.get("Authorization")
        if not authorization_header:
            raise web.HTTPError(401, "===> Authorization header missing")

        if not authorization_header.startswith("Bearer "):
            raise web.HTTPError(403, "===> Invalid token type")

        token = authorization_header.split(" ", 1)[1]

        try:
            # Decode the token
            decoded_token = jwt.decode(token, public_key, algorithms=['HS256', 'RS256'], audience='account')
        except jwt.exceptions.ExpiredSignatureError:
            logging.error('!!!> Token has expired')
            raise web.HTTPError(401, "Token has expired")
        except jwt.exceptions.InvalidTokenError:
            logging.error('!!!> Invalid token')
            raise web.HTTPError(403, "Invalid token")

        user_info = self.user_info_from_token_payload(decoded_token)
        if not user_info:
            logging.error('!!!> Failed to fetch user info from token payload')
            raise web.HTTPError(403, "Failed to fetch user info from token payload")

        return user_info

    def user_info_from_token_payload(self, payload):
        # Extract the username and any other necessary information from the token payload
        # Customize this method according to your token structure
        username = payload.get("preferred_username")
        logging.debug(f'!!!> Username: {username}')
        if not username:
            return None

        return {
            "name": username,
            "auth_state": {
                "access_token": payload,
                "user_info": payload,
            },
        }

    async def check_user_authorization(self, email):
        if not email:
            return False
        # Run database query in a separate thread to avoid blocking
        loop = IOLoop.current()
        authorized = await loop.run_in_executor(None, partial(self._db_check_authorization, email))
        return authorized

    def _db_check_authorization(self, email, retries=3):
        for attempt in range(retries):
            try:
                logging.debug(f'!!!> Attempt {attempt + 1} to check user authorization for email: {email}')
                with SessionLocal() as session:
                    query = text("""
                        SELECT pr.permissions
                        FROM user_ u
                        JOIN user_portal_role upr ON u.id = upr.user_id
                        JOIN portal_role pr ON pr.id = upr.portal_role_id
                        WHERE u.email = :email
                        AND upr.context_key = :context_key
                    """)
                    result = session.execute(query, {"email": email, "context_key": os.environ['CONTEXT_KEY']})

                    # Convert the entire result to a string and check for 'DATA_JUPYTER'
                    result_str = str(result.fetchall())
                    logging.debug(f'!!!> Result: {result_str}')
                    return 'DATA_JUPYTER' in result_str
            except OperationalError as e:
                if attempt < retries - 1:
                    time.sleep(2)  # Wait before retrying
                    continue
                else:
                    raise e  # Reraise after max retries


c.JupyterHub.authenticator_class = HelloDataBearerTokenAuthenticator


# autologin handler
class AutoLoginHandler(BaseHandler):
    async def get(self):
        logging.info('===> AutoLoginHandler called')
        user = await self.get_current_user()
        if user:
            logging.debug(f'!!!> User {user.name} is already logged in')
            self.redirect(self.get_next_url(user))
        else:
            logging.debug('!!!> User not logged in, checking for token')
            token = self.get_cookie("auth.access_token")
            if token:
                logging.debug('!!!> Token found in cookie')
                user = await self.login_user(token)
                if user:
                    logging.debug(f'!!!> User {user.name} logged in successfully')
                    self.redirect(self.get_next_url(user))
                else:
                    logging.error('!!!> Unauthorized')
                    raise web.HTTPError(401, "Unauthorized")
            authorization_header = self.request.headers.get("Authorization")
            logging.debug(f'!!!> Authorization header: {authorization_header}')
            if authorization_header and authorization_header.startswith("Bearer "):
                token = authorization_header.split(" ", 1)[1]
                user = await self.login_user(token)
                if user:
                    logging.info(f'!!!> User {user.name} logged in successfully')
                    self.redirect(self.get_next_url(user))
                else:
                    logging.error('!!!> Unauthorized')
                    raise web.HTTPError(401, "Unauthorized")
            else:
                logging.error('!!!> Authorization cookie missing')
                raise web.HTTPError(401, "Authorization cookie missing")

    async def login_user(self, token):
        try:
            decoded_token = jwt.decode(token, public_key, algorithms=['HS256', 'RS256'], audience='account')
            user_info = self.authenticator.user_info_from_token_payload(decoded_token)
            # Check user authorization in the database for DATA_JUPYTER permission
            logging.debug(f'!!! user_info: {user_info}')
            is_authorized = await self.authenticator.check_user_authorization(
                user_info['auth_state']['user_info']['email'])
            logging.debug(f'!!!> User {user_info["name"]} authorization status: {is_authorized}')
            if not is_authorized:
                logging.error(f'!!!> User {user_info["name"]} is not authorized')
                raise web.HTTPError(403, "User is not authorized")
            user = await self.auth_to_user(user_info)
            self.set_login_cookie(user)
            return user
        except jwt.exceptions.ExpiredSignatureError:
            logging.error('!!!> Token has expired')
            raise web.HTTPError(401, "Token has expired")
        except jwt.exceptions.InvalidTokenError:
            logging.error('!!!> Invalid token')
            raise web.HTTPError(403, "Invalid token")


c.JupyterHub.extra_handlers = [
    (r'/custom/login', AutoLoginHandler),
]

# CSP config
c.JupyterHub.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors localhost:8080",
    }
}

c.Spawner.args = ['--NotebookApp.allow_origin=*']
