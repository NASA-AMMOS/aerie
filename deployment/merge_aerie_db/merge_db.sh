#!/bin/bash

########
# Help #
########
Help()
{
   # Display Help
   echo "Migrate from a pre-v2.8.0 Aerie Database to v2.8.0"
   echo
   echo "usage: merge_db.sh [-h] [-d] [-e ENV_PATH] [-p HASURA_PATH] [-n NETWORK_LOCATION]"
   echo "options:"
   echo "-h    print this message and exit"
   echo "-d    drop the old databases after the merge"
   echo "-e    path to the .env file used to deploy v2.8.0 Aerie. defaults to ../.env"
   echo "-p    path to the directory containing the config.yaml for the Aerie deployment. defaults to ../hasura"
   echo "-n    network location of the database. defaults to localhost"
   echo
}

#################
# Set variables #
#################

EnvFile="../.env"
HasuraPath="../hasura"
NetLoc="localhost"
DropOld=-1

#########################
# Process input options #
#########################

# Get the options
while getopts "he:p:n:d" option; do
   case $option in
      e) EnvFile=$OPTARG;;
      p) HasuraPath=$OPTARG;;
      n) NetLoc=$OPTARG;;
      d) DropOld=1;;
      h | *)
         Help
         exit;;
   esac
done

#################
# Main program  #
#################
source $EnvFile

# Migrate the existing DB to the latest
echo 'Migrate existing DBs to latest...'
python3 aerie_db_migration_preMerge.py -a --all -p $HasuraPath -e $EnvFile -n $NetLoc
return_code=$?
if [ $return_code -ne 0 ]; then
  echo 'Migrating to latest failed, aborting merge...'
  exit $return_code
fi

echo 'Done!'

cd merge_db

# Start the new DB on the server
echo 'Creating merged database...'
set -e
export PGPASSWORD="$POSTGRES_PASSWORD"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres --host "$NetLoc" <<-EOSQL
  \echo 'Initializing aerie user...'
  DO \$\$ BEGIN
      CREATE USER "$AERIE_USERNAME" WITH PASSWORD '$AERIE_PASSWORD';
  EXCEPTION
    WHEN duplicate_object THEN NULL;
  END \$\$;
  \echo 'Done!'

  \echo 'Initializing gateway user...'
  CREATE USER "$GATEWAY_USERNAME" WITH PASSWORD '$GATEWAY_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing merlin user...'
  CREATE USER "$MERLIN_USERNAME" WITH PASSWORD '$MERLIN_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing scheduler user...'
  CREATE USER "$SCHEDULER_USERNAME" WITH PASSWORD '$SCHEDULER_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing sequencing user...'
  CREATE USER "$SEQUENCING_USERNAME" WITH PASSWORD '$SEQUENCING_PASSWORD';
  \echo 'Done!'

  \echo 'Initializing aerie database...'
  CREATE DATABASE aerie OWNER "$AERIE_USERNAME";
  \connect aerie
  ALTER SCHEMA public OWNER TO "$AERIE_USERNAME";
  \echo 'Done!'
EOSQL

echo 'Migrating aerie_merlin Database...'
# Move Merlin
export PGPASSWORD="$AERIE_PASSWORD"
pg_dump -U $AERIE_USERNAME -h $NetLoc aerie_merlin | psql -U $AERIE_USERNAME -h $NetLoc -d aerie
# Migrate Merlin
psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" -h $NetLoc < migrate_merlin.sql
echo 'Done!'

echo 'Migrating aerie_scheduler Database...'
# Move Scheduler
pg_dump -U $AERIE_USERNAME -h $NetLoc --exclude-schema=migrations aerie_scheduler | psql -U $AERIE_USERNAME -h $NetLoc -d aerie
# Migrate Scheduler
psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" -h $NetLoc < migrate_scheduler.sql
echo 'Done!'

echo 'Migrating aerie_sequencing Database...'
# Move Sequencing
pg_dump -U $AERIE_USERNAME -h $NetLoc --exclude-schema=migrations aerie_sequencing | psql -U $AERIE_USERNAME -h $NetLoc -d aerie
# Migrate Sequencing
psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" -h $NetLoc < migrate_sequencing.sql
echo 'Done!'

echo 'Migrating aerie_ui Database...'
# Move UI
pg_dump -U $AERIE_USERNAME -h $NetLoc --exclude-schema=migrations aerie_ui | psql -U $AERIE_USERNAME -h $NetLoc -d aerie
# Migrate UI
psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" -h $NetLoc < migrate_ui.sql
echo 'Done!'

echo 'Setting up Database Permissions...'
psql -v ON_ERROR_STOP=1 --username "$AERIE_USERNAME" --dbname "aerie" --host "$NetLoc" <<-EOSQL
  \set aerie_user $AERIE_USERNAME
  \set postgres_user $POSTGRES_USER
  \set gateway_user $GATEWAY_USERNAME
  \set merlin_user $MERLIN_USERNAME
  \set scheduler_user $SCHEDULER_USERNAME
  \set sequencing_user $SEQUENCING_USERNAME
  \ir database_permissions.sql
EOSQL
echo 'Done!'


# Drop the old DBs
if [ $DropOld -eq 1 ]; then
  echo 'Dropping unmerged databases...'
  PGPASSWORD="$POSTGRES_PASSWORD" \
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" --host "$NetLoc" <<-EOSQL
    DROP DATABASE aerie_merlin;
    DROP DATABASE aerie_scheduler;
    DROP DATABASE aerie_sequencing;
    DROP DATABASE aerie_ui;
    \echo Done!
EOSQL
fi
exit 0
