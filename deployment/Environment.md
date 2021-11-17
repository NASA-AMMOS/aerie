# Environment

This document provides detailed information about environment variables for each service in Aerie.

## Aerie Gateway

| Name                       | Description                                                            | Type     | Default                                        |
| -------------------------- | ---------------------------------------------------------------------- | -------- | ---------------------------------------------- |
| `AUTH_TYPE`                | Mode of authentication. Set to `none` to fully disable authentication. | `string` | cam                                            |
| `AUTH_URL`                 | URL of authentication API for the given `AUTH_TYPE`.                   | `string` | https://atb-ocio-12b.jpl.nasa.gov:8443/cam-api |
| `GQL_API_URL`              | URL of GraphQL API for the GraphQL Playground.                         | `string` | http://localhost:8080/v1/graphql               |
| `PORT`                     | Port the Gateway server listens on.                                    | `number` | 9000                                           |
| `POSTGRES_AERIE_MERLIN_DB` | Name of Merlin Postgres database.                                      | `string` | aerie_merlin                                   |
| `POSTGRES_AERIE_UI_DB`     | Name of UI Postgres database.                                          | `string` | aerie_ui                                       |
| `POSTGRES_HOST`            | Hostname of Postgres instance.                                         | `string` | localhost                                      |
| `POSTGRES_PASSWORD`        | Password of Postgres instance.                                         | `string` | aerie                                          |
| `POSTGRES_PORT`            | Port of Postgres instance.                                             | `number` | 5432                                           |
| `POSTGRES_USER`            | User of Postgres instance.                                             | `string` | aerie                                          |
| `VERSION`                  | Current release version of Aerie.                                      | `string` | 0.9.1                                          |

## Aerie UI

| Name                 | Description                                                            | Type     | Default                          |
| -------------------- | ---------------------------------------------------------------------- | -------- | -------------------------------- |
| `AUTH_TYPE`          | Mode of authentication. Set to `none` to fully disable authentication. | `string` | cam                              |
| `GATEWAY_CLIENT_URL` | Url of the Gateway as called from the client (i.e. web browser)        | `string` | http://localhost:9000            |
| `GATEWAY_SERVER_URL` | Url of the Gateway as called from the server (i.e. Node.js container)  | `string` | http://localhost:9000            |
| `HASURA_CLIENT_URL`  | Url of Hasura as called from the client (i.e. web browser)             | `string` | http://localhost:8080/v1/graphql |
| `HASURA_SERVER_URL`  | Url of Hasura as called from the server (i.e. Node.js container)       | `string` | http://localhost:8080/v1/graphql |

## Hasura

| Name                        | Description                          | Type     | Default                                           |
| --------------------------- | ------------------------------------ | -------- | ------------------------------------------------- |
| `AERIE_MERLIN_DATABASE_URL` | Url of the Merlin Postgres database. | `string` | postgres://aerie:aerie@postgres:5432/aerie_merlin |
| `AERIE_UI_DATABASE_URL`     | Url of the UI Postgres database      | `string` | postgres://aerie:aerie@postgres:5432/aerie_ui     |

Additionally, Hasura provides documentation on it's own environment variables you can use to fine-tune your deployment:

1. [graphql-engine](https://hasura.io/docs/latest/graphql/core/deployment/graphql-engine-flags/reference.html#server-flag-reference)
1. [metadata and migrations](https://hasura.io/docs/latest/graphql/core/migrations/advanced/auto-apply-migrations.html#applying-migrations)

## Merlin

Coming Soon

## Postgres

The default Aerie deployment uses the default Postgres environment. See the [Docker Postgres documentation](https://hub.docker.com/_/postgres) for more complete information on those environment variables and how to use them.
