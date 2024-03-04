#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
  \echo 'Initializing aerie user...'
  CREATE USER "$AERIE_USERNAME" WITH PASSWORD '$AERIE_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing aerie database...'
  CREATE DATABASE aerie OWNER "$AERIE_USERNAME";
  \connect aerie
  ALTER SCHEMA public OWNER TO "$AERIE_USERNAME";
  \connect postgres
  \echo 'Done!'

  \echo 'Initializing aerie_hasura database...'
  CREATE DATABASE aerie_hasura;
  GRANT ALL PRIVILEGES ON DATABASE aerie_hasura TO "$AERIE_USERNAME";
  \echo 'Done!'
EOSQL

export PGPASSWORD="$AERIE_PASSWORD"

psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" <<-EOSQL
  \echo 'Initializing aerie database objects...'
  \ir /docker-entrypoint-initdb.d/sql/init.sql
  \echo 'Done!'
EOSQL
