import {execFileSync} from 'node:child_process';

// Some tests prepare data or check results inside the containers of the docker compose stack, so they need the docker
// CLI and the stack on the machine running Playwright.

const COMPOSE_PROJECT = process.env.E2E_COMPOSE_PROJECT ?? process.env.COMPOSE_PROJECT_NAME ?? 'hellodata-e2e';

const containers = new Map<string, string>();

/** The id of the running container of a compose service. */
function container(service: string) {
  if (!containers.has(service)) {
    const id = execFileSync('docker', ['ps', '-q',
      '--filter', `label=com.docker.compose.project=${COMPOSE_PROJECT}`,
      '--filter', `label=com.docker.compose.service=${service}`], {encoding: 'utf8'}).trim().split('\n')[0];
    if (!id) {
      throw new Error(`service ${service} of compose project ${COMPOSE_PROJECT} is not running (set E2E_COMPOSE_PROJECT)`);
    }
    containers.set(service, id);
  }
  return containers.get(service)!;
}

/** Runs a command in the container of a compose service, optionally as another user (e.g. '0' for root). */
export function exec(service: string, args: string[], {input, user}: {input?: string, user?: string} = {}) {
  const options = user ? ['-u', user] : [];
  return execFileSync('docker', ['exec', '-i', ...options, container(service), ...args], {encoding: 'utf8', input});
}

/** Runs a query in the postgres container and returns its rows, each as a list of column values. */
export function sql(database: string, query: string): string[][] {
  const out = exec('postgres', ['psql', '-U', 'postgres', '-d', database, '-v', 'ON_ERROR_STOP=1', '-tA', '-F', '|', '-c', query]);
  return out.split('\n').filter(line => line.length > 0).map(line => line.split('|'));
}
