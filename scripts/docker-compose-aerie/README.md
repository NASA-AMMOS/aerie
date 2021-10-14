# docker-compose-aerie

Run [Aerie](https://github.jpl.nasa.gov/Aerie/aerie) locally via Docker.
First make sure you have [Docker](https://docs.docker.com/get-docker/) installed.

## Configuration

- The Postgres database specifies a default `POSTGRES_USER` and `POSTGRES_PASSWORD`, these should be updated when deploying to production.

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
