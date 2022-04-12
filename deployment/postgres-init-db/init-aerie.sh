#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  \echo 'Initializing aerie user...'
  CREATE USER aerie WITH PASSWORD 'aerie';
  \echo 'Done!'

  \echo 'Initializing aerie_hasura database...'
  CREATE DATABASE aerie_hasura;
  GRANT ALL PRIVILEGES ON DATABASE aerie_hasura TO aerie;
  \echo 'Done!'

  \echo 'Initializing aerie_merlin database...'
  CREATE DATABASE aerie_merlin;
  GRANT ALL PRIVILEGES ON DATABASE aerie_merlin TO aerie;
  \echo 'Done!'

  \echo 'Initializing aerie_scheduler database...'
  CREATE DATABASE aerie_scheduler;
  GRANT ALL PRIVILEGES ON DATABASE aerie_scheduler TO aerie;
  \echo 'Done!'

  \echo 'Initializing aerie_ui database...'
  CREATE DATABASE aerie_ui;
  GRANT ALL PRIVILEGES ON DATABASE aerie_ui TO aerie;
  \echo 'Done!'

  \echo 'Initializing aerie_commanding database...'
  CREATE DATABASE aerie_commanding;
  GRANT ALL PRIVILEGES ON DATABASE aerie_commanding TO aerie;
  \echo 'Done!'

EOSQL

export PGPASSWORD=aerie

psql -v ON_ERROR_STOP=1 --username "aerie" --dbname "aerie_merlin" <<-EOSQL
  \echo 'Initializing aerie_merlin database objects...'
  \ir /docker-entrypoint-initdb.d/sql/merlin/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "aerie" --dbname "aerie_scheduler" <<-EOSQL
  \echo 'Initializing aerie_scheduler database objects...'
  \ir /docker-entrypoint-initdb.d/sql/scheduler/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "aerie" --dbname "aerie_ui" <<-EOSQL
  \echo 'Initializing aerie_ui database objects...'
  \ir /docker-entrypoint-initdb.d/sql/ui/init.sql
  \echo 'Done!'
EOSQL

psql -v ON_ERROR_STOP=1 --username "aerie" --dbname "aerie_commanding" <<-EOSQL
  \echo 'Initializing aerie_commanding database objects...'
  \ir /docker-entrypoint-initdb.d/sql/commanding/init.sql
  \echo 'Done!'
EOSQL
