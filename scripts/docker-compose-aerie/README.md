# aerie-docker

Run [Aerie](https://github.jpl.nasa.gov/MPS/aerie) locally via Docker.  
First make sure you have [Docker](https://docs.docker.com/get-docker/) installed.

## Start

```sh
docker login cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/ammos/mpsa
docker-compose up --detach
```

Goto [http://localhost:8080/](http://localhost:8080/)

## Stop

```sh
docker-compose down
docker ps -a # List all containers
docker images # List all images
```

## Troubleshooting

- When logging into Docker Artifactory you need to specify your JPL username/password. If you don't have access please contact someone from the Aerie team.
