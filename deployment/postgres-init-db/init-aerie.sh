#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
  \echo 'Initializing aerie user...'
  CREATE USER "$AERIE_USERNAME" WITH PASSWORD '$AERIE_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing aerie_hasura database...'
  CREATE DATABASE aerie_hasura;
  GRANT ALL PRIVILEGES ON DATABASE aerie_hasura TO "$AERIE_USERNAME";
  \echo 'Done!'

  \echo 'Initializing aerie_merlin database...'
  CREATE DATABASE aerie_merlin;
  GRANT ALL PRIVILEGES ON DATABASE aerie_merlin TO "$AERIE_USERNAME";
  \echo 'Done!'

  \echo 'Initializing aerie_scheduler database...'
  CREATE DATABASE aerie_scheduler;
  GRANT ALL PRIVILEGES ON DATABASE aerie_scheduler TO "$AERIE_USERNAME";
  \echo 'Done!'

  \echo 'Initializing aerie_ui database...'
  CREATE DATABASE aerie_ui;
  GRANT ALL PRIVILEGES ON DATABASE aerie_ui TO "$AERIE_USERNAME";
  \echo 'Done!'

  \echo 'Initializing aerie_commanding database...'
  CREATE DATABASE aerie_commanding;
  GRANT ALL PRIVILEGES ON DATABASE aerie_commanding TO "$AERIE_USERNAME";
  \echo 'Done!'

EOSQL

export PGPASSWORD="$AERIE_PASSWORD"

psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie_merlin" <<-EOSQL
  \echo 'Initializing aerie_merlin database objects...'
  \ir /docker-entrypoint-initdb.d/sql/merlin/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie_scheduler" <<-EOSQL
  \echo 'Initializing aerie_scheduler database objects...'
  \ir /docker-entrypoint-initdb.d/sql/scheduler/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie_ui" <<-EOSQL
  \echo 'Initializing aerie_ui database objects...'
  \ir /docker-entrypoint-initdb.d/sql/ui/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie_commanding" <<-EOSQL
  \echo 'Initializing aerie_commanding database objects...'
  \ir /docker-entrypoint-initdb.d/sql/commanding/init.sql
  \echo 'Done!'
EOSQL
