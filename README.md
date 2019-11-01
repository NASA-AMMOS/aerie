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

| Service    | Port  |
| ---------- | ----- |
| adaptation | 27182 |
| nest       | 8080  |
| plan       | 27183 |
| simulation | 27185 |

## Installation

To get Aerie running see [Installation](./docs/installation.md).
