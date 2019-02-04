# AMPSA Monorepo

> Advanced Mission Planning and Analysis services 

## Usage

```bash
cd docker && docker-compose up 
```
## Services

AMPSA is comprised of the following Primary and Support services.

### Primary services

Primary services are developed in-house and should be focused on the core
offering of AMPSA. Primary services, with the exception of the `nest` service
should use the next available port, beginning with `27182`, which is a
truncated version of the mathematical constant E, to make it easy to remember.

| Service       | Port   |
| ---           | ---    |
| adaptation    | 27182  |
| plan          | 27183  |
| nest          | 8080   |

### Support services

Support services are ancillary to the operation of the Primary services. In
other words, the Primary services can function completely without them.

| Service         | Port  |
| ---             | ---   |
| elasticsearch   | 9200  |
| elasticsearch   | 9300  |
| kibana          | 5601  |
| matomo          | 31423 |
| matomo_database | 3306  |
| tyk_dashboard   | 3000  |
| tyk_dashboard   | 5000  |
| tyk_gateway     | 8081  |
| tyk_mongo       | 27017 |
| tyk_redis       | 6379  | 

## Getting Started

These instructions will get a copy of the project up and running on your local
machine for development and testing purposes.

### Prerequisites

To start the services **locally** using Docker Compose you must have Git,
[Docker][docker], and [Docker Compose][compose] installed.

### Installation

The basic steps are:

1. Clone the repository
2. Build the adaptation service according to its [README][adaptation]
3. Fetch and build the NEST service according to its [README][nest]
4. Start the stack according to the Docker [README][docker_readme]
5. Run the setup.sh files for each service that you started

For now primary services are pre-built, but in the future they should be built
with multi-stage Docker builds.


[adaptation]: ./adaptation/README.md
[compose]: https://docs.docker.com/compose/
[docker]: https://www.docker.com/get-docker
[docker_readme]: ./docker/README.md
[nest]: ./nest/README.md
