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

logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

c = get_config()  # noqa: F821

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
c.DockerSpawner.volumes = {"jupyterhub-user-{username}": notebook_dir}

# Remove containers once they are stopped
c.DockerSpawner.remove = True

# For debugging arguments passed to spawned containers
c.DockerSpawner.debug = True

# User containers will access hub by container name on the Docker network
# c.JupyterHub.hub_ip = "jupyterhub"
c.JupyterHub.hub_ip = os.environ.get("HUB_IP", "jupyterhub")
c.JupyterHub.hub_port = 8080

# Persist hub data on volume mounted inside container
c.JupyterHub.cookie_secret_file = "/data/jupyterhub_cookie_secret"
c.JupyterHub.db_url = "sqlite:////data/jupyterhub.sqlite"

# Authenticate users with Native Authenticator
# c.JupyterHub.authenticator_class = "nativeauthenticator.NativeAuthenticator"

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


class BearerTokenAuthenticator(GenericOAuthenticator):

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
            raise web.HTTPError(401, "Token has expired")
        except jwt.exceptions.InvalidTokenError:
            raise web.HTTPError(403, "Invalid token")

        user_info = self.user_info_from_token_payload(decoded_token)
        if not user_info:
            raise web.HTTPError(403, "Failed to fetch user info from token payload")

        return user_info

    def user_info_from_token_payload(self, payload):
        # Extract the username and any other necessary information from the token payload
        # Customize this method according to your token structure
        username = payload.get("preferred_username")
        if not username:
            return None

        return {
            "name": username,
            "auth_state": {
                "access_token": payload,
                "user_info": payload,
            },
        }


c.JupyterHub.authenticator_class = BearerTokenAuthenticator


# autologin handler
class AutoLoginHandler(BaseHandler):
    async def get(self):
        user = await self.get_current_user()
        if user:
            self.redirect(self.get_next_url(user))
        else:
            token = self.get_cookie("auth.access_token")
            if token:
                user = await self.login_user(token)
                if user:
                    self.redirect(self.get_next_url(user))
                else:
                    raise web.HTTPError(401, "Unauthorized")
            authorization_header = self.request.headers.get("Authorization")
            if authorization_header and authorization_header.startswith("Bearer "):
                token = authorization_header.split(" ", 1)[1]
                user = await self.login_user(token)
                if user:
                    self.redirect(self.get_next_url(user))
                else:
                    raise web.HTTPError(401, "Unauthorized")
            else:
                raise web.HTTPError(401, "Authorization cookie missing")

    async def login_user(self, token):
        try:
            decoded_token = jwt.decode(token, public_key, algorithms=['HS256', 'RS256'], audience='account')
            user_info = self.authenticator.user_info_from_token_payload(decoded_token)
            user = await self.auth_to_user(user_info)
            self.set_login_cookie(user)
            return user
        except jwt.exceptions.ExpiredSignatureError:
            raise web.HTTPError(401, "Token has expired")
        except jwt.exceptions.InvalidTokenError:
            raise web.HTTPError(403, "Invalid token")


def patch_handlers(jupyterhub_app):
    web_app = jupyterhub_app.web_app
    handlers = web_app.handlers[0][1]
    for idx, handler in enumerate(handlers):
        if handler[0].endswith('/login'):
            handlers[idx] = (handler[0], AutoLoginHandler)
            break
    web_app.handlers[0] = (web_app.handlers[0][0], handlers)
    logging.info("Patched handlers to use AutoLoginHandler")


def on_jupyterhub_init(hub_app):
    setup_patches(hub_app)


c.JupyterHub.extra_handlers = [
    (r'/custom/login', AutoLoginHandler),
]


# Add an event hook to patch handlers after JupyterHub initializes
class CustomJupyterHub(JupyterHub):
    @default('config_file')
    def _default_config_file(self):
        return '/srv/jupyterhub/jupyterhub_config.py'

    def init_hub(self):
        super().init_hub()
        on_jupyterhub_init(self)


if __name__ == "__main__":
    CustomJupyterHub.launch_instance()

# CSP config
c.JupyterHub.tornado_settings = {
    'headers': {
        'Content-Security-Policy': "frame-ancestors localhost:8080",
    }
}

c.Spawner.args = ['--NotebookApp.allow_origin=*']
