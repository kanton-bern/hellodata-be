import {exec} from './docker';
import {KEYCLOAK_REALM} from './instance';

// Changes users directly in Keycloak, the central authentication service, which the portal itself never changes
// (HELLODATA-4090). kcadm runs in the keycloak container (compose service or Kubernetes deployment `keycloak`) and logs in
// with the admin of the master realm from the container's own environment, so no password leaves the container.

const LOGIN = [
  'K=/opt/keycloak/bin/kcadm.sh; C=/tmp/kcadm-e2e.config',
  'U="${KC_BOOTSTRAP_ADMIN_USERNAME:-$KEYCLOAK_ADMIN}"; P="${KC_BOOTSTRAP_ADMIN_PASSWORD:-$KEYCLOAK_ADMIN_PASSWORD}"',
  '$K config credentials --config $C --server http://localhost:8080 --realm master --user "$U" --password "$P" >/dev/null 2>&1',
].join('; ');

function kcadm(script: string) {
  return exec('keycloak', ['sh', '-ec', `${LOGIN}; ${script}`]).trim();
}

function shellQuote(value: string) {
  return `'${value.replaceAll("'", `'\\''`)}'`;
}

/** The id of a user in the Keycloak realm, found by its e-mail. */
function keycloakUserId(email: string) {
  const id = kcadm(`$K get users -r ${shellQuote(KEYCLOAK_REALM)} --config $C -q exact=true -q email=${shellQuote(email)}`
    + ` --fields id --format csv --noquotes 2>/dev/null`).split('\n')[0];
  if (!id) {
    throw new Error(`user ${email} not found in the Keycloak realm ${KEYCLOAK_REALM}`);
  }
  return id;
}

/** Enables or disables a user in Keycloak only, the portal keeps its own enabled flag. */
export function setEnabledInKeycloak(email: string, enabled: boolean) {
  const id = keycloakUserId(email);
  kcadm(`$K update users/${id} -r ${shellQuote(KEYCLOAK_REALM)} --config $C -s enabled=${enabled} 2>/dev/null`);
}

/** The enabled flag of a user in Keycloak. */
export function enabledInKeycloak(email: string) {
  const id = keycloakUserId(email);
  return kcadm(`$K get users/${id} -r ${shellQuote(KEYCLOAK_REALM)} --config $C --fields enabled --format csv --noquotes 2>/dev/null`) === 'true';
}
