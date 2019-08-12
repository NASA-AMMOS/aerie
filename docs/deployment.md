# deployment

This document describes how to deploy the Aerie services and Nest front-end via Docker. All of these instructions should be carried out on the machine you are installing to.

## Steps

1. Make sure [Docker](https://www.docker.com/) and [git](https://git-scm.com/) are installed, and that Docker is running. `docker-compose` should be automatically installed with the installation of Docker:

```bash
which docker
which docker-compose
which git
docker info
```

2. Clone the Aerie repository:

```bash
git clone git@github.jpl.nasa.gov:MPS/aerie.git
cd aerie
```

3. Log into the [Artifactory](https://cae-artifactory.jpl.nasa.gov) Docker repository:

```bash
docker login cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/ammos/mpsa/aerie
```

4. Use [Docker Compose](https://docs.docker.com/compose/reference/) to start the system:

```bash
docker-compose -f docker-compose.yml up --build
```

5. To stop and remove all the containers run:

```bash
docker-compose down
```

## Configuration

The `docker-compose` files are parameterized with the [.env](../.env) file in the root of the Aerie repository.

| Environment Variable | Description |
| -------------------- | ----------- |
| DOCKER_TAG | A [Docker Tag](https://docs.docker.com/engine/reference/commandline/tag/) of the Aerie version you are deploying. It has the form: `[BRANCH_NAME]+b[BUILD_NUMBER].r[SHORT_GIT_COMMIT_HASH].[yyyyMMdd]`. For example this is a real tag: `develop-b122.rcb8493e.20190529`. For a list of Docker image tags, first [log into Artifactory](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/login). The complete list of images for each Aerie service can be found [here](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-develop-local/gov/nasa/jpl/ammos/mpsa/aerie). Note each service has a tag of a single version. For example the `nest`, `adaptation`, and `plan` services all have a tag of `develop-b122.rcb8493e.20190529`. |
| DOCKER_URL | The URL of a Docker repository. Defaults to Artifactories [docker-develop-local](https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/docker-develop-local) repository. |
