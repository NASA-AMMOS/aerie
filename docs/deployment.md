# Introduction
The Aerie project uses Docker Compose to configure and run the entire project. Docker Compose utilizes YAML configuration files which are located in the root of the project. The YAML files include everything that is required to deploy Aerie using Docker Compose and to run Aerie with the Docker Engine. 

Unless otherwise configured, Docker Compose will fetch pre-built Docker containers for each of the services which make up the Aerie deployment.  Once fetched, Docker Compose will orchestrate the containers (network) and then start them.


# Developer Deployment

This document describes how a developer may deploy the Aerie services. All of these instructions should be carried out on the machine you are installing to.

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
2. Clone the Aerie repository:

```bash
git clone https://github.jpl.nasa.gov/MPS/aerie.git
cd aerie
```

3. Log into the [Artifactory](https://cae-artifactory.jpl.nasa.gov) Docker repository:

```bash
docker login cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/aerie
```

4. Use [Docker Compose](https://docs.docker.com/compose/reference/) to start the [services](./services.md):

```bash
docker-compose -f docker-compose.yml up --build
```

5. To stop and remove all the containers run:

```bash
docker-compose down
```
To configure and use the Aerie UI components one must setup both the Aerie UI and the Aerie Apollo API Gateway given the instructions supplied at,
- [Aerie UI Developer Instructions](https://github.jpl.nasa.gov/MPS/aerie-ui/blob/develop/docs/DEVELOPER.md) 
- [Aerie Apollo API Gateway](https://github.jpl.nasa.gov/MPS/aerie-apollo)

## Configuration

The `docker-compose` files are parameterized with the [.env](../.env) file in the root of the Aerie repository.

| Environment Variable | Description |
| -------------------- | ----------- |
| DOCKER_TAG| A [Docker Tag](https://docs.docker.com/engine/reference/commandline/tag/) of the Aerie version you are deploying. It has the form: `[BRANCH_NAME]`. For example: `develop`. For a list of Docker image tags, first [log into Artifactory](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/login). The complete list of images for each Aerie service can be found [here](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-develop-local/gov/nasa/jpl/aerie). Note each service has a tag of a single version. For example the `adaptation` and `plan` services both have a tag of `develop`. |
| DOCKER_URL | The URL of a Docker repository. Defaults to Artifactories [docker-develop-local](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-develop-local) repository. |


# Customer Deployment 

> **Important Note**: The Aerie product is an alpha phase development. Therefore, this deployment is **NOT** designed for production use or performance testing. It has not been optimized in any way.

All of these instructions should be carried out on the machine to which you are installing.

## Steps

1. Make sure [Docker](https://www.docker.com/) and [git](https://git-scm.com/) are installed, and that Docker is running. `docker-compose` should be automatically installed with the installation of Docker. To confirm that these three utilities are installed:

```bash
which docker
which docker-compose
which git
```
Use the following command to confirm the Docker server is running on your target machine. The command will return a series of information describing your Docker server instance.
```bash
cd [YOUR_DOCKER_TARGET_DIRECTORY]
docker info
```

2. Download the example [Aerie Docker Compose config files](https://cae-artifactory.jpl.nasa.gov:16003/artifactory/webapp/#/artifacts/browse/tree/General/general/gov/nasa/jpl/aerie) to the chosen docker target directory (one can also author a custom file).

3. Log into the [Artifactory](https://cae-artifactory.jpl.nasa.gov) Docker repository:
```bash
docker login cae-artifactory.jpl.nasa.gov:16003/gov/nasa/jpl/aerie
```

5. Use [Docker Compose](https://docs.docker.com/compose/reference/) to start the [services](./services.md):

```bash
docker-compose --file docker-compose.yml up --detach
```

6. To stop and remove all the containers run:

```bash
docker-compose down
```

## Configuration

The `docker-compose` file are parameterized with the [.env](../.env) file.

| Environment Variable | Description |
| -------------------- | ----------- |
| DOCKER_TAG | A [Docker Tag](https://docs.docker.com/engine/reference/commandline/tag/) of the Aerie version you are deploying. It has the form: `[BRANCH_NAME]`. For example this is a real tag: `release-0.3.0`. For a list of Docker image tags, first [log into Artifactory](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/login). The complete list of images for each Aerie service can be found [here](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-release-local/gov/nasa/jpl/aerie). Note each service has a tag of a single version. For example the `adaptation` and `plan` services all have a tag of `release-0.3.0`. |
| UI_DOCKER_TAG  | A [Docker Tag](https://docs.docker.com/engine/reference/commandline/tag/) of the Aerie UI version you are deploying. It has the form: `[BRANCH_NAME]`. For a list of Docker image tags, first [log into Artifactory](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/login). The available images can be found [here](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-release-local/gov/nasa/jpl/aerie/aerie-ui). |
| AERIE_DOCKER_URL ,<br/> AERIE_UI_DOCKER_URL ,<br/> AERIE_APOLLO_DOCKER_URL| A URL of a Docker repository. Defaults to CAE's Artifactory [docker-release-local](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-release-local/gov/nasa/jpl/aerie) repository. |








