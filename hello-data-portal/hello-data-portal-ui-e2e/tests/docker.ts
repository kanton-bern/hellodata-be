import {execFileSync} from 'node:child_process';
import {test} from '@playwright/test';

// Some tests prepare data or check results inside the containers of the installation under test. By default that is the
// docker compose stack, so they need the docker CLI and the stack on the machine running Playwright. With
// E2E_KUBE_NAMESPACE set they run in the pods of a Kubernetes installation instead (kubectl exec into the deployment
// that plays the part of the compose service), optionally in the kube context E2E_KUBE_CONTEXT. With E2E_NO_EXEC set
// (e.g. a tester without kubectl access) these tests are skipped, see requireExec().

const COMPOSE_PROJECT = process.env.E2E_COMPOSE_PROJECT ?? process.env.COMPOSE_PROJECT_NAME ?? 'hellodata-e2e';
const KUBE_NAMESPACE = process.env.E2E_KUBE_NAMESPACE;
const KUBE_CONTEXT = process.env.E2E_KUBE_CONTEXT;
const NO_EXEC = !!process.env.E2E_NO_EXEC;
const NO_EXEC_REASON = 'needs docker or kubectl access to the installation (E2E_NO_EXEC is set)';

/**
 * Skips the tests of the enclosing describe, and its hooks, when E2E_NO_EXEC is set. Call it first in every describe
 * whose tests or hooks use exec() or sql(), directly or through the helpers of batch-import.ts and data-eng.ts.
 */
export function requireExec() {
  test.skip(NO_EXEC, NO_EXEC_REASON);
}

// the Helm deployments of the compose services the tests exec into (a service missing here keeps its name)
const KUBE_DEPLOYMENTS: Record<string, string> = {
  'hello-data-portal-api': 'portal-backend',
  'postgres': 'postgres',
};

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

/** kubectl exec into the first container of the deployment of a compose service. */
function kubeExec(service: string, args: string[], input?: string) {
  const deployment = KUBE_DEPLOYMENTS[service] ?? service;
  const kubectl = [...(KUBE_CONTEXT ? ['--context', KUBE_CONTEXT] : []), '-n', KUBE_NAMESPACE!];
  try {
    return execFileSync('kubectl', [...kubectl, 'exec', '-i', `deploy/${deployment}`, '--', ...args],
      {encoding: 'utf8', input, stdio: ['pipe', 'pipe', 'pipe']});
  } catch (error: any) {
    // keep kubectl's own message (e.g. no access to the cluster) next to the failing command
    throw new Error(`kubectl exec into deploy/${deployment} in ${KUBE_NAMESPACE} failed: ${error.stderr || error.message}`);
  }
}

/**
 * Runs a command in the container of a compose service, optionally as another user (e.g. '0' for root). kubectl exec
 * cannot switch the user, so on Kubernetes the command runs as the user of the container (the shared storage is
 * writable for it there).
 */
export function exec(service: string, args: string[], {input, user}: {input?: string, user?: string} = {}) {
  if (NO_EXEC) {
    // a describe that uses exec() is missing requireExec()
    throw new Error(`exec into ${service}: ${NO_EXEC_REASON}, add requireExec() to the describe`);
  }
  if (KUBE_NAMESPACE) {
    return kubeExec(service, args, input);
  }
  const options = user ? ['-u', user] : [];
  return execFileSync('docker', ['exec', '-i', ...options, container(service), ...args], {encoding: 'utf8', input});
}

/** Runs a query in the postgres container and returns its rows, each as a list of column values. */
export function sql(database: string, query: string): string[][] {
  const out = exec('postgres', ['psql', '-U', 'postgres', '-d', database, '-v', 'ON_ERROR_STOP=1', '-tA', '-F', '|', '-c', query]);
  return out.split('\n').filter(line => line.length > 0).map(line => line.split('|'));
}
