# HelloDATA BE Copilot Instructions

## Build, test, and lint commands

This repository is a multi-module Maven build rooted at `pom.xml`. Prefer the Maven wrapper from the repository root:

```bash
./mvnw clean install
./mvnw clean verify
./mvnw -pl <module-path> -am clean install
```

Useful module paths:

- `hello-data-portal/hello-data-portal-api`
- `hello-data-portal/hello-data-portal-ui`
- `hello-data-portal/hello-data-portal-ui-e2e`
- `hello-data-sidecars/<sidecar-module>`
- `hello-data-subsystems/<subsystem-module>`

Backend tests use Maven Surefire from the shared parent in `hello-data-commons/hello-data-spring-boot-starter-parent/pom.xml`, with `*Test.java`, `*Tests.java`, and `*Spec.java` included by default.

Run one backend test class or one test method:

```bash
./mvnw -pl hello-data-portal/hello-data-portal-api -am -Dtest=UserServiceTest test
./mvnw -pl hello-data-portal/hello-data-portal-api -am -Dtest=UserServiceTest#disableUserById test
```

Representative module test commands:

```bash
./mvnw -pl hello-data-commons/hello-data-common -am test
./mvnw -pl hello-data-sidecars/hello-data-sidecar-superset -am test
./mvnw -pl hello-data-subsystems/hello-data-monitoring-storage -am test
```

The Angular UI has direct Yarn scripts in `hello-data-portal/hello-data-portal-ui/package.json`:

```bash
cd hello-data-portal/hello-data-portal-ui
yarn install
yarn lint
yarn test
yarn test -- announcement-edit.component.spec.ts
yarn build
```

The Maven build for `hello-data-portal-ui` also installs Node/Yarn and runs `yarn lint` and `yarn build` through `frontend-maven-plugin`, but direct Yarn commands are faster when you are only working in the UI.

E2E tests live in `hello-data-portal-ui-e2e` and use Cypress:

```bash
cd hello-data-portal/hello-data-portal-ui-e2e
yarn install
yarn test:run
yarn test:open
```

The Maven E2E path is Docker-based and uses the `e2e-tests` profile:

```bash
./mvnw -pl hello-data-portal/hello-data-portal-ui-e2e -Pe2e-tests verify
```

CI uses the same conventions:

- `.github/workflows/maven.yml` runs `mvn -B package --file pom.xml`
- `.github/workflows/maven_module.yml` runs `mvn -B -pl <module> -am clean install`

Several backend tests use Testcontainers and/or Dockerized dependencies such as Keycloak and PostgreSQL, especially in `hello-data-portal-api`.

## High-level architecture

The root `pom.xml` defines five top-level modules:

- `hello-data-commons`: shared Java libraries, shared Spring Boot parent, common models, NATS integration, and portal persistence models.
- `hello-data-portal`: the main product surface, split into `hello-data-portal-api`, `hello-data-portal-ui`, and `hello-data-portal-ui-e2e`.
- `hello-data-sidecars`: integration services that react to platform events and synchronize external tools such as Superset, Airflow, CloudBeaver, dbt docs, JupyterHub, and SFTPGo.
- `hello-data-subsystems`: subsystem applications and gateways that expose or wrap external tooling.
- `hello-data-deployment`: Docker Compose and deployment packaging for local/demo environments.

The platform model described in `README.md` and `docs/docs/architecture/architecture.md` is:

- a **business domain** is the tenant-level container for shared services such as portal, orchestration, monitoring, and auth
- a **data domain** is the per-domain data space containing schemas, dbt assets, dashboards, documentation, and related tooling

At runtime, the portal is the control plane:

- `hello-data-portal-api` is the central Spring Boot backend
- `hello-data-portal-ui` is the Angular frontend
- Keycloak handles authentication and identity
- PostgreSQL stores portal and domain metadata
- NATS is the event bus between portal and sidecars

The important cross-module split is:

- `hello-data-portal-api` owns user, role, dashboard, FAQ, announcement, orchestration, and metainfo APIs
- `hello-data-portal-common` in `hello-data-commons` owns JPA entities/repositories shared by portal features
- `hello-data-sidecar-common` and `hello-data-nats-spring` define shared event payloads, context models, and NATS/JetStream plumbing
- sidecars subscribe to events and translate HelloDATA state into tool-specific actions
- subsystem gateway modules expose Spring Cloud Gateway-style adapters in front of tools such as CloudBeaver and JupyterHub

That architecture shows up directly in code:

- portal REST endpoints live under feature packages like `portal/user/controller`, `portal/role/controller`, `portal/dashboard_comment/controller`
- portal business logic lives in matching `service` packages
- shared persistence lives in `hello-data-commons/hello-data-portal-common/.../{entity,repository}`
- sidecar consumers live in module-specific packages like `hello-data-sidecar-superset/.../service/user` and `hello-data-sidecar-portal/.../service/resource`

## Key conventions

Backend code is organized by feature first, not by technical layer alone. Within a feature, expect `controller`, `service`, `data`, and sometimes `event`, `util`, or `listener` packages. When adding a portal feature, follow the existing feature package layout instead of creating generic cross-feature folders.

Shared persistence for portal features belongs in `hello-data-portal-common`, not in `hello-data-portal-api`. For example, user, role, dashboard-access, query, and monitoring entities/repositories are defined in `hello-data-commons/hello-data-portal-common`.

Portal code commonly uses:

- constructor injection with Lombok `@RequiredArgsConstructor`
- `@Log4j2`
- transactional service methods
- `ResponseStatusException` for HTTP-facing failures
- `@PreAuthorize` on controller endpoints for permission checks

Messaging between the portal and external tools is event-driven. In portal services, publishing usually goes through `NatsSenderService.publishMessageToJetStream(...)`. In sidecars, consumers subscribe with `@JetStreamSubscribe`. If a change affects users, roles, dashboard access, or published resources, check whether an event contract in `hello-data-sidecar-common` also needs to change.

Role and context behavior is domain-aware. Many flows are keyed by `contextKey` and business/data domain distinctions from `HelloDataContextConfig`. Avoid implementing portal logic that assumes a single global domain.

The frontend is feature-organized too:

- page components live under `src/app/pages`
- reusable UI lives under `src/app/shared`
- state is managed with NgRx under `src/app/store/<feature>`

NgRx files follow a stable naming pattern per feature:

- `*.action.ts` or `*.actions.ts`
- `*.reducer.ts`
- `*.selector.ts`
- `*.effects.ts`
- `*.service.ts`
- `*.state.ts`

Routing is still centralized in `hello-data-portal-ui/src/app/app-routing.module.ts`, but most pages are lazy-loaded with `loadComponent(...)`. Permission checks in the UI are route-driven and usually use `PermissionsGuard` plus `requiredPermissions` route data.

Frontend tests usually instantiate standalone components directly in `TestBed` and use `src/app/test.module.ts` for shared store/interceptor/UI wiring. When updating a component spec, check whether it already relies on `Store`, `TranslocoTestingModule`, and PrimeNG services via that test module.

The repository mixes direct tool execution and Maven-managed frontend execution. If you change only Angular code, prefer direct `yarn` commands in `hello-data-portal-ui`; if you need CI-like behavior or cross-module integration, use Maven from the root.

The shared Spring Boot parent in `hello-data-commons/hello-data-spring-boot-starter-parent/pom.xml` is authoritative for Java versions, dependency management, Surefire defaults, JaCoCo, Spring Boot plugin setup, and other shared build behavior. Check that parent before adding plugin or dependency versions in child POMs.

## Verification checklist

After making changes, always verify the following before considering work complete:

1. **Compilation** — run `./mvnw -pl <changed-module> -am compile` (backend) or `yarn build` (frontend) and confirm zero errors.
2. **Tests** — run the relevant test suite for every changed module. For backend: `./mvnw -pl <changed-module> -am test`. For frontend: `yarn test`. Fix any failures before committing.
3. **Sonar / static analysis** — review the change for common SonarQube issues: unused imports, unclosed resources, missing `@Override`, raw types, empty catch blocks, and commented-out code. If JaCoCo coverage drops for the changed code, add or update tests to cover the new paths.
