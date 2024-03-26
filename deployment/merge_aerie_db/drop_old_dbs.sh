#!/bin/bash

########
# Help #
########
Help()
{
   # Display Help
   echo "Drop the pre-v2.8.0 unmerged Aerie Databases"
   echo
   echo "usage: drop_old_dbs.sh [-h] [-e ENV_PATH] [-n NETWORK_LOCATION]"
   echo "options:"
   echo "-h    print this message and exit"
   echo "-e    path to the .env file used to deploy v2.8.0 Aerie. defaults to ../.env"
   echo "-n    network location of the database. defaults to localhost"
   echo
}

#################
# Set variables #
#################

EnvFile="../.env"
NetLoc="localhost"

#########################
# Process input options #
#########################

# Get the options
while getopts "he:n" option; do
   case $option in
      e) EnvFile=$OPTARG;;
      n) NetLoc=$OPTARG;;
      h | *)
         Help
         exit;;
   esac
done

#################
# Main program  #
#################
source $EnvFile

echo 'Dropping unmerged databases...'
PGPASSWORD="$POSTGRES_PASSWORD" \
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" --host "$NetLoc" <<-EOSQL
  DROP DATABASE aerie_merlin;
  DROP DATABASE aerie_scheduler;
  DROP DATABASE aerie_sequencing;
  DROP DATABASE aerie_ui;
  \echo Done!
EOSQL
