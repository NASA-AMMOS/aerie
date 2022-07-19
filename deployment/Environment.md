# Environment

This document provides detailed information about environment variables for each service in Aerie.

- [Aerie Commanding](#aerie-commanding)
- [Aerie Gateway](#aerie-gateway)
- [Aerie Merlin](#aerie-merlin)
- [Aerie Scheduler](#aerie-scheduler)
- [Aerie UI](#aerie-ui)
- [Hasura](#hasura)
- [Postgres](#postgres)

## Aerie Commanding

| Name                     | Description                                       | Type     | Default                            |
| ------------------------ | ------------------------------------------------- | -------- | ---------------------------------- |
| `LOG_FILE`               | Either an output filepath to log to, or 'console' | `string` | console                            |
| `LOG_LEVEL`              | Logging level for filtering logs                  | `string` | warn                               |
| `MERLIN_GRAPHQL_URL`     | URI of the Aerie GraphQL API                      | `string` | http://hasura:8080/v1/graphql      |
| `COMMANDING_SERVER_PORT` | Port the server listens on                        | `number` | 27184                              |
| `COMMANDING_DB`          | Name of Commanding Postgres database              | `string` | aerie_commanding                   |
| `COMMANDING_DB_SERVER`   | Hostname of Postgres instance                     | `string` | postgres                           |
| `COMMANDING_DB_PASSWORD` | Password of Postgres instance                     | `string` | aerie                              |
| `COMMANDING_DB_PORT`     | Port of Postgres instance                         | `number` | 5432                               |
| `COMMANDING_DB_USER`     | User of Postgres instance                         | `string` | aerie                              |
| `COMMANDING_LOCAL_STORE` | Local storage file storage in the container       | `string` | /usr/src/app/commanding_file_store |

## Aerie Gateway

| Name                          | Description                                              | Type     | Default                          |
| ----------------------------- | -------------------------------------------------------- | -------- | -------------------------------- |
| `GQL_API_URL`                 | URL of GraphQL API for the GraphQL Playground.           | `string` | http://localhost:8080/v1/graphql |
| `LOG_FILE`                    | Either an output filepath to log to, or 'console'.       | `string` | console                          |
| `LOG_LEVEL`                   | Logging level for filtering logs.                        | `string` | warn                             |
| `PORT`                        | Port the Gateway server listens on.                      | `number` | 9000                             |
| `POSTGRES_AERIE_MERLIN_DB`    | Name of Merlin Postgres database.                        | `string` | aerie_merlin                     |
| `POSTGRES_AERIE_SCHEDULER_DB` | Name of scheduler Postgres database.                     | `string` | aerie_scheduler                  |
| `POSTGRES_HOST`               | Hostname of Postgres instance.                           | `string` | localhost                        |
| `POSTGRES_PASSWORD`           | Password of Postgres instance.                           | `string` | aerie                            |
| `POSTGRES_PORT`               | Port of Postgres instance.                               | `number` | 5432                             |
| `POSTGRES_USER`               | User of Postgres instance.                               | `string` | aerie                            |
| `RATE_LIMITER_FILES_MAX`      | Max requests allowed every 15 minutes to file endpoints  | `number` | 1000                             |
| `RATE_LIMITER_LOGIN_MAX`      | Max requests allowed every 15 minutes to login endpoints | `number` | 1000                             |

## Aerie Merlin

| Name                 | Description                                               | Type     | Default                         |
| -------------------- | --------------------------------------------------------- | -------- | ------------------------------- |
| `JAVA_OPTS`          | Configuration for Merlin's logging level and output file  | `string` | log level: warn. output: stderr |
| `MERLIN_PORT`        | Port number for the Merlin server                         | `number` | 27183                           |
| `MERLIN_LOCAL_STORE` | Local storage for Merlin in the container                 | `string` | /usr/src/app/merlin_file_store  |
| `MERLIN_DB_SERVER`   | The DB instance that Merlin will connect with             | `string` | postgres                        |
| `MERLIN_DB_PORT`     | The DB instance port number that Merlin will connect with | `number` | 5432                            |
| `MERLIN_DB_USER`     | Username of the DB instance                               | `string` | aerie                           |
| `MERLIN_DB_PASSWORD` | Password of the DB instance                               | `string` | aerie                           |
| `MERLIN_DB`          | The DB for Merlin.                                        | `string` | aerie_merlin                    |

## Aerie Merlin Worker

| Name                        | Description                                               | Type     | Default                                      |
| --------------------------- | --------------------------------------------------------- | -------- | -------------------------------------------- |
| `JAVA_OPTS`                 | Configuration for Merlin's logging level and output file  | `string` | log level: warn. output: stderr              |
| `MERLIN_WORKER_LOCAL_STORE` | The local storage as for the Merlin container             | `string` | /usr/src/app/merlin_file_store               |
| `MERLIN_WORKER_DB_SERVER`   | The DB instance that Merlin will connect with             | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_PORT`     | The DB instance port number that Merlin will connect with | `number` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_USER`     | Username of the DB instance                               | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB_PASSWORD` | Password of the DB instance                               | `string` | (this must the same as the Merlin container) |
| `MERLIN_WORKER_DB`          | The DB for Merlin.                                        | `string` | (this must the same as the Merlin container) |

## Aerie Scheduler

| Name                    | Description                                                           | Type     | Default                                            |
| ----------------------- | --------------------------------------------------------------------- | -------- | -------------------------------------------------- |
| `JAVA_OPTS`             | Configuration for the scheduler's logging level and output file       | `string` | log level: warn. output: stderr                    |
| `MERLIN_GRAPHQL_URL`    | URI of the Merlin graphql interface to call                           | `string` | http://hasura:8080/v1/graphql                      |
| `MERLIN_LOCAL_STORE`    | Local storage for Merlin in the container (for backdoor jar access)   | `string` | /usr/src/app/merlin_file_store                     |
| `SCHEDULER_DB`          | The DB for scheduler                                                  | `string` | aerie_scheduler                                    |
| `SCHEDULER_DB_PASSWORD` | Password of the DB instance                                           | `string` | aerie                                              |
| `SCHEDULER_DB_PORT`     | The DB instance port number that scheduler will connect with          | `number` | 5432                                               |
| `SCHEDULER_DB_SERVER`   | The DB instance that scheduler will connect with                      | `string` | postgres                                           |
| `SCHEDULER_DB_USER`     | Username of the DB instance                                           | `string` | aerie                                              |
| `SCHEDULER_LOCAL_STORE` | Local storage for scheduler in the container                          | `string` | /usr/src/app/scheduler_file_store                  |
| `SCHEDULER_LOGGING`     | Whether or not you want Javalin to log server information             | `string` | true                                               |
| `SCHEDULER_OUTPUT_MODE` | how scheduler output is sent back to aerie                            | `string` | UpdateInputPlanWithNewActivities                   |
| `SCHEDULER_PORT`        | Port number for the scheduler server                                  | `number` | 27185                                              |
| `SCHEDULER_RULES_JAR`   | Jar file to load scheduling rules from (until user input to database) | `string` | /usr/src/app/merlin_file_store/scheduler_rules.jar |

## Aerie UI

| Name                 | Description                                                                                               | Type     | Default                          |
| -------------------- | --------------------------------------------------------------------------------------------------------- | -------- | -------------------------------- |
| `GATEWAY_CLIENT_URL` | Url of the Gateway as called from the client (i.e. web browser)                                           | `string` | http://localhost:9000            |
| `GATEWAY_SERVER_URL` | Url of the Gateway as called from the server (i.e. Node.js container)                                     | `string` | http://localhost:9000            |
| `HASURA_CLIENT_URL`  | Url of Hasura as called from the client (i.e. web browser)                                                | `string` | http://localhost:8080/v1/graphql |
| `HASURA_SERVER_URL`  | Url of Hasura as called from the server (i.e. Node.js container)                                          | `string` | http://localhost:8080/v1/graphql |
| `ORIGIN`             | Url of where the UI is served from. See the [Svelte Kit Adapter Node docs][svelte-kit-adapter-node-docs]. | `string` | http://localhost                 |

## Hasura

| Name                           | Description                             | Type     | Default                                              |
| ------------------------------ | --------------------------------------- | -------- | ---------------------------------------------------- |
| `AERIE_MERLIN_DATABASE_URL`    | Url of the Merlin Postgres database.    | `string` | postgres://aerie:aerie@postgres:5432/aerie_merlin    |
| `AERIE_SCHEDULER_DATABASE_URL` | Url of the scheduler Postgres database. | `string` | postgres://aerie:aerie@postgres:5432/aerie_scheduler |
| `AERIE_UI_DATABASE_URL`        | Url of the UI Postgres database         | `string` | postgres://aerie:aerie@postgres:5432/aerie_ui        |

Additionally, Hasura provides documentation on it's own environment variables you can use to fine-tune your deployment:

1. [graphql-engine](https://hasura.io/docs/latest/graphql/core/deployment/graphql-engine-flags/reference.html#server-flag-reference)
1. [metadata and migrations](https://hasura.io/docs/latest/graphql/core/migrations/advanced/auto-apply-migrations.html#applying-migrations)

## Postgres

The default Aerie deployment uses the default Postgres environment. See the [Docker Postgres documentation](https://hub.docker.com/_/postgres) for more complete information on those environment variables and how to use them.

[svelte-kit-adapter-node-docs]: https://github.com/sveltejs/kit/blob/master/packages/adapter-node/README.md
