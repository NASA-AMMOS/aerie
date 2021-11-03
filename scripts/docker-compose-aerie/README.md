# docker-compose-aerie

Run [Aerie](https://github.jpl.nasa.gov/Aerie/aerie) locally via Docker.
First make sure you have [Docker](https://docs.docker.com/get-docker/) installed.

## Configuration

- We provide default [Postgres initialization scripts](./postgres-init-db) to provision Postgres with the proper databases, users, and privileges for a standard Aerie deployment.
- Note that these initialization scripts will only run if the Postgres container data directory is empty. Any pre-existing database will be left untouched on container startup.
- We recommend you update the database user passwords to the non-defaults for your deployment.

## Start

```sh
docker login artifactory.jpl.nasa.gov:16003/gov/nasa/jpl/aerie
docker-compose up --detach
```

Goto [http://localhost](http://localhost)

## Stop

```sh
docker-compose down
```

## Environment Variables (.env)

| Variable | Description |
| - | - |
| AERIE_DOCKER_URL | URL of the Aerie Docker repository. Defaults to docker-release-local. |
| DOCKER_TAG | Version tag of the Aerie version you want to deploy. |

## Troubleshooting

- When logging into Docker Artifactory you need to specify your JPL username/password. If you don't have access please contact someone from the Aerie team via the [#mpsa-aerie-users](https://app.slack.com/client/T024LMMEZ/C0163E42UBF) Slack channel.
