# Deployment

## Introduction

The Aerie project uses [Docker Compose](https://docs.docker.com/compose/) to configure and run the entire project. Docker Compose utilizes YAML configuration files which include everything that is required to deploy and run Aerie with the Docker Engine.

Unless otherwise configured, Docker Compose will fetch pre-built Docker images for each of the services which make up the Aerie deployment. Once fetched, Docker Compose will orchestrate the containers and then start them from the images.

> **Important Note**: The Aerie product is an alpha phase development. Therefore, this deployment is **NOT** designed for production use or performance testing. It has not been optimized in any way.

## Steps

1. Make sure [Docker](https://www.docker.com/) and [git](https://git-scm.com/) are installed, and that Docker is running. `docker-compose` should be automatically installed with the installation of Docker. To confirm that these three utilities are installed:

```bash
which docker
which docker-compose
which git
```

Use the following command to confirm the Docker server is running on your target machine. The command will return a series of information describing your Docker server instance.

```bash
docker info
```

2. Download and extract the [aerie-docker-compose.tar.gz](https://cae-artifactory.jpl.nasa.gov:16003/artifactory/webapp/#/artifacts/browse/tree/General/general/gov/nasa/jpl/aerie/aerie-docker-compose.tar.gz) archive into a directory called `aerie-docker-compose`. This directory contains the example deployment [docker-compose.yml](../scripts/docker-compose-aerie/docker-compose.yml) and [.env](../scripts/docker-compose-aerie/.env) files. You can change these files to taylor the deployment to your requirements as needed.

3. Log into the [Artifactory](https://cae-artifactory.jpl.nasa.gov) Docker repository:

```bash
docker login cae-artifactory.jpl.nasa.gov:16003/gov/nasa/jpl/aerie
```

5. Use Docker Compose to start Aerie.

```bash
cd aerie-docker-compose
docker-compose up --detach
```

6. To stop and remove all the containers run:

```bash
docker-compose down
```
