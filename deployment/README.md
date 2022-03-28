# aerie-deployment

This document describes how to deploy Aerie.

- [Prerequisite Software](#prerequisite-software)
- [Environment Variables](#environment-variables)
- [Starting the Services](#starting-the-services)
- [Postgres Considerations](#postgres-considerations)
- [Stopping the Services](#stopping-the-services)
- [Troubleshooting](#troubleshooting)

## Prerequisite Software

Before you can deploy Aerie, you must install and configure the following products on your deployment machine:

- [Docker](https://www.docker.com/) which is used to deploy containers for the Aerie services.

## Environment Variables

Each container has environment variables that can be used to fine-tune your deployment. See the [environment variable documentation](./Environment.md) for the complete set of variables. See the example [docker-compose.yml](./docker-compose.yml) file for examples on how to set the environment variables. **We highly recommend changing the default environment variable passwords before deploying Aerie**.

## Starting the Services

Download and unzip the latest [Deployment.zip](https://github.com/NASA-AMMOS/aerie/releases) file onto your local machine, then do:

```sh
cd deployment
docker-compose up --build --detach
```

## Postgres Considerations

When the Postgres container starts it will run [init-aerie.sh](./postgres-init-db/init-aerie.sh) to initialize the database with the Aerie [database objects](./postgres-init-db/sql).

**Note:** This script is only run if you start the container with a data directory that is empty; any pre-existing database will be left untouched on container startup. See the 'Initialization scripts' section of the [Docker Postgres documentation](https://hub.docker.com/_/postgres) for more detailed information.

## Stopping the Services

```sh
docker-compose down
```

## Troubleshooting

- Make sure you are starting all the Docker containers in a clean environment. This means before starting the services make sure all containers are removed, all images are removed, and all volumes are removed.
- If you are having trouble and cannot find the answer here, please contact someone from the Aerie team via the [#mpsa-aerie-users](https://app.slack.com/client/T024LMMEZ/C0163E42UBF) Slack channel.
