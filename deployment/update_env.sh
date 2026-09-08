#!/bin/bash
set -euo pipefail

# Fix Gateway Container Permissions
docker compose exec -u root aerie_gateway chown -R node:node /app/files

# preserve the existing file store and databases
VOLUME_NAME=$(docker inspect -f '{{ .Mounts }}' aerie_gateway | grep aerie_file_store | awk '{print $2}')
echo "PLANDEV_FILE_STORE_NAME=$VOLUME_NAME" >> .env
echo "PLANDEV_DATABASE_NAME=aerie" >> .env
echo "PLANDEV_METADATA_DATABASE_NAME=aerie_hasura" >> .env

# copy renamed configuration
grep '^AERIE_USERNAME=' .env | sed 's/^AERIE_USERNAME=/PLANDEV_USERNAME=/' >> .env
grep '^AERIE_PASSWORD=' .env | sed 's/^AERIE_PASSWORD=/PLANDEV_PASSWORD=/' >> .env
grep '^AERIE_HOST=' .env | sed 's/^AERIE_HOST=/PLANDEV_HOST=/' >> .env || true
