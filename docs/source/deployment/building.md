# Building Aerie

## Prerequisites

### Building

| Name    | Version          | Notes        | Download             |
| ------- | ---------------- | ------------ | -------------------- |
| OpenJDK | Temurin 19 (LTS) | HotSpot JVM  | https://adoptium.net |
| Gradle  | 7.6              | Build system | https://gradle.org   |

### Testing

| Name       | Version | Notes                      | Download                   |
| ---------- | ------- | -------------------------- | -------------------------- |
| PostgreSQL | 14.1    | Database management system | https://www.postgresql.org |

## Compile

### Building

Run `gradle classes`.

### Testing

Run `gradle test`.

### Updating Dependencies

Run `gradle dependencyUpdates` to view a report of the project dependencies that are have updates available or are
up-to-date.

## Docker

The docker-compose.yml in the Aerie root directory deploys Aerie locally. All but one of the docker images are pulled
from the SNAPSHOT docker image repository containing the latest development from the Aerie project repositories.
The `merlin` container is sourced by building the container anew using the build products output from the Gradle build
process. This is evidenced by the `build:` directive in the `merlin`
section in docker-compose.yml.

Prior to building docker-compose.yml, it is necessary to create a .env file using the template found in .env.template in the Aerie root directory.

### Docker Compose Build

`docker compose build`

### Docker Compose Up/Down

To deploy Aerie run:

`docker compose up`

When working with Aerie it is helpful to be able to inspect the live console output from the running containers.
However, to deploy Aerie and hide logging output one can add the `--detach` argument and run:

`docker compose up --detach`

To stop Aerie run:

`docker compose down`

Note, to be able to make changes to your docker images and volumes you must first run bring down your running
containers.

### Remove Docker Images

Removing a docker image from your local cache forces docker to either rebuild it or fetch it from a repository (e.g.
DockerHub, Artifactory).

`docker image rm <image name of image id>`

Sometimes it's helpful just to clear all images and start from scratch.

`docker rmi $(docker images -a -q)`

### Prune Docker Volumes

`docker volume prune`

Sometimes it's necessary to clear the contents of file system volumes mounted by docker. For Aerie this could be needing
to start with a clean install and wanting to delete the database contents, mission model jars, and mission simulation
data files.

Ensure all containers are down by running `docker compose down`. Only once a container is down may one run the volume
pruning operation.

### Remove Everything from Docker (clean slate)

This will wipe away everything (containers, volumes, etc.) including other projects' docker containers. Powerful but
dangerous!

`docker system prune --all --volumes `

### Entering a Docker Container

At times it is helpful to enter a docker container and inspect the filesystem or run cli utilities such as
`psql` or `hasura-cli`. To enter a docker container you can run `docker exec -it` followed by the container name and the
application to run inside the container. For example, a shell can be initialized in the postgres container with:

`docker exec -it aerie-postgres-1 /bin/sh`

### Debugging an Application Within a Docker Container

Using IntelliJ one can attach a debugger to a docker container and trigger break points in the associated Java code. To
attach a debugger to the `merlin` container one should edit their docker-compose.yml file exposing port 5005 and adding
the `JAVA_OPTS` environment variable as shown below.

```yaml
merlin:
  build:
    context: .
    dockerfile: merlin-server/Dockerfile
  depends_on: ["postgres"]
  ports: ["27183:27183", "5005:5005"]
  restart: always
  volumes:
    - aerie_file_store:/usr/src/app/merlin_file_store
  environment:
    JAVA_OPTS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
```

The above environment variable will suspend the launching of the merlin-server until a dubugger connects on the assigned
port 5005.

In IntelliJ create a new "Remote JVM Debug" configuration via Run > Edit Configurations. The default configuration
should already be setup with

- Port:5005,
- Host:localhost,
- Command Line arguments for remote JVM: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=\*:5005.

Once configured having the debugger attach while using Aerie is as simple as running `docker compose up` and running the
debugger (nominally within IntelliJ).
