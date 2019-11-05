# Installation

## Prerequisites

You need the following software installed before starting:

1. [Git](https://git-scm.com/)
1. [Docker](https://www.docker.com/)
1. [Maven](https://maven.apache.org/)

## Steps for Running Services Locally

1. Create Artifactory settings.xml
1. Log in to JPL Docker Artifactory
1. Build and start the services

### Create Artifactory Settings.xml

You need an Artifactory settings.xml file with your JPL credentials so you can download and use Artifactory packages locally. Follow these steps to set this file up:

1. Go to https://cae-artifactory.jpl.nasa.gov/artifactory/webapp/#/artifacts/browse/tree/General/maven-libs-snapshot-local and log in with your JPL credentials
1. Click "Set Me Up"
1. Type in your JPL password
1. Use the settings shown below.
![](artifactory_maven_setup.png)
1. Click "Generate Maven Settings" and then "Generate Settings" which will download a `settings.xml` file
1. Move `settings.xml` into `~/.m2/`, replacing the old `settings.xml` if it exists.

### Log in to Docker Artifactory

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

### Build and Start the Services

```bash
git clone git@github.jpl.nasa.gov:MPS/aerie.git
cd aerie
mvn install -DskipTests
docker-compose -f docker-compose-local.yml up --build
```
For more information on the Aerie services, see the [services documentation](./services.md).

Press `cmd+c` or `ctrl+c` to stop all services if docker-compose is running in
the foreground. Then to remove all the containers and clean your Maven targets do:

```bash
docker-compose down
mvn clean
```
