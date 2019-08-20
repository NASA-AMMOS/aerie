# AMPSA Monorepo

> Advanced Mission Planning and Analysis services

## Usage

```bash
docker-compose up
```

## Services

AMPSA is comprised of the following Primary and Support services.

### Primary services

Primary services are developed in-house and should be focused on the core
offering of AMPSA. Primary services, with the exception of the `nest` service
should use the next available port, beginning with `27182`, which is a
truncated version of the mathematical constant E, to make it easy to remember.

| Service            | Port  |
| ------------------ | ----- |
| adaptation         | 27182 |
| adaptation-runtime | 27184 |
| nest               | 8080  |
| plan               | 27183 |
| simulation         | 27185 |
| tyk_gateway        | 8081  |
| sequence-files     | 27186 |

### Support services

Support services are ancillary to the operation of the Primary services. In
other words, the Primary services can function completely without them.

| Service         | Port  |
| --------------- | ----- |
| elasticsearch   | 9200  |
| elasticsearch   | 9300  |
| kibana          | 5601  |
| matomo          | 31423 |
| matomo_database | 3306  |
| rabbitmq        | 5672  |
| tyk_dashboard   | 3000  |
| tyk_dashboard   | 5000  |
| tyk_mongo       | 27017 |
| tyk_redis       | 6379  |

## Installation

To get Aerie running see [Installation](./docs/installation.md).
