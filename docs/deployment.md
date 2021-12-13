# Deployment

## Introduction

The Aerie project uses [Docker Compose](https://docs.docker.com/compose/) to configure and run the entire project.
Docker Compose utilizes YAML configuration files which include everything that is required to deploy and run Aerie with
the Docker Engine.

Unless otherwise configured, Docker Compose will fetch pre-built Docker images for each of the services which make up
the Aerie deployment. Once fetched, Docker Compose will orchestrate the containers and then start them from the images.

> **Important Note**: The Aerie product is an alpha phase development.
> Therefore, this deployment is **NOT** designed for production use or performance testing.

## Steps

1. Make sure [Docker](https://www.docker.com/) and [git](https://git-scm.com/) are installed, and that Docker is
   running. `docker-compose` should be automatically installed with the installation of Docker. To confirm that these
   three utilities are installed:

```sh
which docker
which docker-compose
which git
```

Use the following command to confirm the Docker server is running on your target machine. The command will return a
series of information describing your Docker server instance.

```sh
docker info
```

2. Download and extract
   the [aerie-docker-compose.tar.gz](https://artifactory.jpl.nasa.gov:16003/artifactory/webapp/#/artifacts/browse/tree/General/general/gov/nasa/jpl/aerie/aerie-docker-compose.tar.gz)
   archive into a directory called `aerie-docker-compose`. This directory contains the example
   deployment [docker-compose.yml](../scripts/docker-compose-aerie/docker-compose.yml)
   and [.env](../scripts/docker-compose-aerie/.env) files. You can change these files to taylor the deployment to your
   requirements as needed.

3. Log into the [Artifactory](https://artifactory.jpl.nasa.gov) Docker repository:

```sh
docker login artifactory.jpl.nasa.gov:16003/gov/nasa/jpl/aerie
```

5. Use Docker Compose to start Aerie.

```sh
cd aerie-docker-compose
docker-compose up --detach
```

6. To stop and remove all the containers run:

```sh
docker-compose down
```

## Advanced

### NFS Deployment

To test the use of NFS ([Network File System](https://en.wikipedia.org/wiki/Network_File_System)) to "seamlessly" share
these files between multiple hosts an additional docker-compose stack may be used.

#### NFS Server

A Dockerized NFS host may be created with a `docker-compose-nfs.yml` configuration:

```yaml
version: "3.7"

services:
  merlin_nfs:
    image: erichough/nfs-server
    container_name: merlin_nfs
    hostname: merlin_nfs
    restart: always
    volumes:
      - merlin_nfs_data:/data
    environment:
      NFS_PORT: 2049
      NFS_VERSION: 4.2
      NFS_EXPORT_0: /data *(rw,no_root_squash,no_subtree_check,fsid=0)
    cap_add:
      - SYS_ADMIN
    ports:
      - 2049:2049

volumes:
  merlin_nfs_data:
```

For more information regarding the `NFS_EXPORT_0` export please see the
offical `erichough/nfs-server` [documentation](https://github.com/ehough/docker-nfs-server#usage) and the
Linux `exports` [man page](https://linux.die.net/man/5/exports).

#### NFS Clients

Clients may mount the shared directory by making use of a Docker volume:

```yaml
volumes:
  postgres_data:
  merlin_file_store:
    driver: local
    driver_opts:
      type: nfs4
      o: addr=172.27.0.2,rw,noatime
      device: ":/"
```

In this case, the IP (`172.27.0.2`) was obtained by running:

```sh
$ docker-compose -f docker-compose-nfs.yml up --detach
Creating network "aerie_default" with the default driver
Creating merlin_nfs ... done
$ docker container ls | grep merlin_nfs | cut -d " " -f1
2724af1ec4d2
$ docker inspect 2724af1ec4d2 | grep IPAddress
            "SecondaryIPAddresses": null,
            "IPAddress": "",
                    "IPAddress": "172.27.0.2",
```

The volume may now be attached to an existing service's `volumes` list with:

```yaml
volumes:
  - merlin_file_store:/usr/src/app/data
```

`/usr/src/app/data` will now be the mount location of the shared directory.

#### Sharing Files

Continuing the previous example, it's possible to demonstrate file sharing between the NFS host and clients.

With the NFS container still running, log into the NFS server container and create a test file:

```sh
$ docker exec -it 2724af1ec4d2 /bin/bash
root@2724af1ec4d2:/# touch data/from_server.txt
```

Start the default Aerie services with `docker-compose up --detach` and get the container ID with:

```sh
$ docker container ls | grep merlin_1 | cut -d " " -f1
ba3ef517965e
```

To access existing files and create a new file on the shared FS run:

```sh
$ docker exec -it ba3ef517965e /bin/bash
root@ba3ef517965e:/usr/src/app# cd data
root@ba3ef517965e:/usr/src/app/data# touch from_client.txt
root@ba3ef517965e:/usr/src/app/data# ls
from_client.txt
from_server.txt
```
