# aerie-deployment

This document describes how to deploy Aerie.

- [Prerequisite Software](#prerequisite-software)
- [Environment Variables](#environment-variables)
- [Starting the Services](#starting-the-services)
- [Postgres Considerations](#postgres-considerations)
- [Applying Hasura Metadata](#applying-hasura-metadata)
- [Stopping the Services](#stopping-the-services)
- [Troubleshooting](#troubleshooting)

## Prerequisite Software

Before you can deploy Aerie, you must install and configure the following products on your deployment machine:

- [Docker](https://www.docker.com/) which is used to deploy containers for the Aerie services.
- [Hasura CLI](https://hasura.io/docs/latest/graphql/core/hasura-cli/install-hasura-cli.html#install-hasura-cli) which is used to upload Hasura metadata to the Hasura container. If you have [Node.js](https://nodejs.org/en/) installed you can install the CLI via:
  ```
  npm install hasura-cli -g
  ```

## Environment Variables

Each container has environment variables that can be used to fine-tune your deployment. See the main [docker-compose.yml](./docker-compose.yml) file for the complete set of variables.

## Starting the Services

```sh
cd deployment
docker login artifactory.jpl.nasa.gov:16003/gov/nasa/jpl/aerie
docker-compose up  --build --detach
```

## Postgres Considerations

When the Postgres container starts it will run [init-aerie.sh](./postgres-init-db/init-aerie.sh) to initialize the database with the Aerie [database objects](./postgres-init-db/sql).

**Note:** This script is only run if you start the container with a data directory that is empty; any pre-existing database will be left untouched on container startup.

## Applying Hasura Metadata

Apply metadata to Hasura from the [metadata directory](./hasura/metadata), by running the following command:

```
hasura metadata apply --endpoint http://localhost:8080
```

You can change the `endpoint` flag to point to a different instance of Hasura if needed.

## Stopping the Services

```sh
docker-compose down
```

## Troubleshooting

- When logging into Docker Artifactory you need to specify your JPL username/password. If you don't have access please contact someone from the Aerie team via the [#mpsa-aerie-users](https://app.slack.com/client/T024LMMEZ/C0163E42UBF) Slack channel.
