# HelloDATA Portal UI E2E Module

This module contains Cypress E2E Infrastructure and Tests for the application.

# Running with Maven

The Cypress Tests are integrated directly into the maven build with the `maven-failsafe-plugin`
and `docker-maven-plugin`s. They ***require a local docker installation*** and are executed in
the `docker` maven profile.

```bash
./mvnw clean verify -Pdocker
```

Test Results, including screenshots and video, are written to the `target/e2e/results` folder.

# Running Manually

Running the cypress tests manually is useful during development and when analysing failing tests.

## Start Keycloak

Start the keycloak docker container. See [Docker Compose README](../docker-compose/README.md)

## Start Backend

Start the backend. See [Backend README](../hellodata-portal-api/README.md)

## Start Cypress

Before you run cypress locally for the first time, you need to run `yarn install` in the `e2e`
directory.

### Single Run

```bash
yarn test:run 
```

### Interactive Cypress Test Runner

```bash
yarn test:open 
```

# Why are there custom Cypress Images?

Normally we could parameterize the official images directly, mounting any necessary Configuration
during the build. Unfortunately, this does not work on our Bamboo build system. Here's why:

Each Bamboo Agent runs in a Docker Container that in turn can start other docker containers during
the build. These Containers run on a Docker Daemon outside the Docker Agent Container where they
were started. For this reason, Volume Mounts do not function as one would expect, since the Docker
Host looks for them locally, and not on the Agent where they are actually located. The workaround is
to bake any required external files into a custom image, so we don't need to mount anything.
