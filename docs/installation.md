# Installation

The Aerie project uses Docker Compose to configure and run the entire
project. Docker Compose utilizes YAML configuration files which are
located in the root of the project. The YAML configs include everything
that is required to run Aerie with Docker and Docker Compose.

## Prerequisites

To start the services using Docker Compose you must have [Git](https://git-scm.com/),
[Docker](https://www.docker.com/), and [Docker Compose](https://docs.docker.com/compose/) installed.

## Basic Steps

The basic steps are:

1. Clone the repository using Git
2. Log in to Docker Hub
3. Create an Artifactory settings.xml file for local development
4. Configure services (if running the full configuration)
5. Start the services using the appropriate configuration file

## Authentication

Before the local and full configurations can be run, you will need to login to
Artifactory:

```
docker login -u $USERNAME cae-artifactory.jpl.nasa.gov:16001
```

Where `$USERNAME` is your LDAP username. You will be prompted to enter your
password. Once successful, you are logged in and can pull down MPSA images.

NOTE: Access is intended for MPSA Aerie Developers. If you have tried to login
and you get an access denied message, please contact seq.support@jpl.nasa.gov
to request access.

## Artifactory Settings.xml

You need an Artifactory settings.xml file with your JPL credentials so you can download and use Artifactory packages locally. Follow these steps to set this file up:

1. Go to https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/maven-libs-snapshot-local and log in with your JPL credentials
2. Click "Set Me Up"
3. Type in your JPL password
4. Use the settings shown below.
![](artifactory_maven_setup.png)
5. Click "Generate Maven Settings" and then "Generate Settings" which will download a `settings.xml` file
6. Move `settings.xml` into `~/.m2/`, replacing the old `settings.xml` if it exists.

## Configuration

The basic command that you will use to start services locally with Docker Compose is:

```
docker-compose -f docker-compose-local.yml up --build
```

Press `cmd+c` or `ctrl+c` to stop all services if docker-compose is running in
the foreground. Then to remove all the containers do:

```
docker-compose down
```

## Deployment 

See the deployment documentation [here](./deployment.md).
