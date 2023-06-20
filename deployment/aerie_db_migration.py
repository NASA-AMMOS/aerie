#!/usr/bin/env python3
"""Migrate an AERIE Database"""

import os
import argparse
import sys
import shutil
import subprocess
import psycopg

def clear_screen():
  os.system('cls' if os.name == 'nt' else 'clear')

# internal class
class DB_Migration:
  steps = []
  db_name = ''
  def __init__(self, db_name):
    self.db_name = db_name

  def add_migration_step(self, _migration_step):
    self.steps = sorted(_migration_step, key=lambda x:int(x.split('_')[0]))

def step_by_step_migration(database, apply):
  clear_screen()
  print('#' * len(database.db_name))
  print(database.db_name)
  print('#' * len(database.db_name))

  display_string = "\n\033[4mMIGRATION STEPS AVAILABLE:\033[0m\n"
  _output = subprocess.getoutput(f'hasura migrate status --database-name {database.db_name}').split("\n")
  del _output[0:3]
  display_string += _output[0] + "\n"

  # Filter out the steps that can't be applied given the current mode and currently applied steps
  available_steps = database.steps.copy()
  for i in range(1, len(_output)):
    split = list(filter(None, _output[i].split(" ")))

    if len(split) >= 5 and "Not Present" == (split[2]+" "+split[3]):
      print("\n\033[91mError\033[0m: Migration files exist on server that do not exist on this machine. "
            "Synchronize files and try again.\n")
      input("Press Enter to continue...")
      return

    if apply:
      if (len(split) == 4) or (not os.path.isfile(f'migrations/{database.db_name}/{split[0]}_{split[1]}/up.sql')):
        available_steps.remove(f'{split[0]}_{split[1]}')
      else:
        display_string += _output[i] + "\n"
    else:
      if (len(split) == 5 and "Not Present" == (split[3] + " " + split[4])) \
          or (not os.path.isfile(f'migrations/{database.db_name}/{split[0]}_{split[1]}/down.sql')):
        available_steps.remove(f'{split[0]}_{split[1]}')
      else:
        display_string += _output[i] + "\n"

  if available_steps:
    print(display_string)
  else:
    print("\nNO MIGRATION STEPS AVAILABLE\n")

  for step in available_steps:
    print("\033[4mCURRENT STEP:\033[0m\n")
    timestamp = step.split("_")[0]

    if apply:
      exit_code = os.system(f'hasura migrate apply --version {timestamp} --database-name {database.db_name} --dry-run --log-level WARN')
    else:
      exit_code = os.system(f'hasura migrate apply --version {timestamp} --type down --database-name {database.db_name} --dry-run --log-level WARN')

    if exit_code == 0:
      print()
      _value = ''
      while _value != "y" and _value != "n" and _value != "q":
        if apply:
          _value = input(f'Apply {step}? (y/n): ').lower()
        else:
          _value = input(f'Revert {step}? (y/n): ').lower()

      if _value == "q":
        sys.exit()
      if _value == "y":
        if apply:
          print('Applying...')
          os.system(f'hasura migrate apply --version {timestamp} --type up --database-name {database.db_name}')
        else:
          print('Reverting...')
          os.system(f'hasura migrate apply --version {timestamp} --type down --database-name {database.db_name}')
        os.system('hasura metadata reload')
        print()
      elif _value == "n":
        return
  input("Press Enter to continue...")

def bulk_migration(migration_db, apply):
  clear_screen()
  # Migrate each database
  for database in migration_db:
    print('#' * len(database.db_name))
    print(database.db_name)
    print('#' * len(database.db_name))

    if apply:
      exit_code = os.system(f'hasura migrate apply --database-name {database.db_name} --dry-run --log-level WARN')
      if exit_code != 0:
        continue
      os.system(f'hasura migrate apply --database-name {database.db_name}')
    else:
      exit_code = os.system(f'hasura migrate apply --goto 0 --database-name {database.db_name} --dry-run --log-level WARN')
      if exit_code != 0:
        continue
      os.system(f'hasura migrate apply --goto 0 --database-name {database.db_name}')

    os.system('hasura metadata reload')

  # Show the result after the migration
  print(f'\n'
        f'\n###############'
        f'\nDatabase Status'
        f'\n###############')
  for database in migration_db:
    os.system(f'hasura migrate status --database-name {database.db_name}')

def mark_current_version(dbs_to_apply, username, password, netloc):
  for db in dbs_to_apply:
    # Convert db.name to the actual format of the db name: aerie_dbSuffix
    name = "aerie_"+db.db_name.removeprefix("Aerie").lower()
    connectionString = "postgres://"+username+":"+password+"@"+netloc+":5432/"+name
    current_schema = 0

    # Connect to DB
    with psycopg.connect(connectionString) as connection:
      # Open a cursor to perform database operations
      with connection.cursor() as cursor:
        # Get the current schema version
        try:
          cursor.execute("SELECT migration_id FROM migrations.schema_migrations ORDER BY migration_id::int DESC LIMIT 1")
        except psycopg.errors.UndefinedTable:
          return
        current_schema = int(cursor.fetchone()[0])

    # Mark everything up to that as applied
    for i in range(0, current_schema+1):
      os.system('hasura migrate apply --skip-execution --version '+str(i)+' --database-name '+db.db_name+' >/dev/null 2>&1')

def main():
  # Create a cli parser
  parser = argparse.ArgumentParser(description=__doc__)
  # Applying and Reverting are exclusive arguments
  exclusive_args = parser.add_mutually_exclusive_group(required='true')

  # Add arguments
  exclusive_args.add_argument(
    '-a', '--apply',
    help="apply migration steps to specified databases",
    action='store_true')

  exclusive_args.add_argument(
    '-r', '--revert',
    help="revert migration steps to specified databases",
    action='store_true')

  parser.add_argument(
    '--all',
    help="apply[revert] ALL unapplied[applied] migration steps to all databases if none are provided",
    action='store_true')

  parser.add_argument(
    '-db', '--db-names',
    help="list of databases to migrate. migrates all if unspecified",
    nargs='+',
    default=[])

  parser.add_argument(
    '-p', '--hasura-path',
    help="the path to the directory containing the config.yaml for Aerie. defaults to ./hasura")

  parser.add_argument(
    '-e', '--env-path',
    help="the path to the .env file used to deploy aerie. must define AERIE_USERNAME and AERIE_PASSWORD. defaults to .env",
    default='.env')

  parser.add_argument(
    '-n', '--network-location',
    help="the network location of the database. defaults to localhost",
    default='localhost')

  # Generate arguments
  args = parser.parse_args()

  HASURA_PATH = "./hasura"
  if args.hasura_path:
    HASURA_PATH = args.hasura_path
  MIGRATION_PATH = HASURA_PATH+"/migrations/"

  # find all migration folders for each Aerie database
  migration_db = []
  to_migrate_set = set(args.db_names)
  dbs_specified = True
  if not to_migrate_set:
    dbs_specified = False

  try:
    os.listdir(MIGRATION_PATH)
  except FileNotFoundError as fne:
    print("\033[91mError\033[0m:"+ str(fne).split("]")[1])
    sys.exit(1)
  for db in os.listdir(MIGRATION_PATH):
    # ignore hidden folders
    if db.startswith('.'):
      continue
    # Only process if the folder is on the list of databases or if we don't have a list of databases
    if not dbs_specified or db in to_migrate_set:
      migration = DB_Migration(db)
      for root,dirs,files in os.walk(MIGRATION_PATH+db):
        if dirs:
          migration.add_migration_step(dirs)
      if len(migration.steps) > 0:
        # If reverting, reverse the list
        if args.revert:
          migration.steps.reverse()
        migration_db.append(migration)
        to_migrate_set.discard(db)

  if to_migrate_set:
    print("\033[91mError\033[0m: The following Database(s) do not contain migrations:\n\t"
          +"\n\t".join(to_migrate_set))
    sys.exit(1)

  if not migration_db:
    print("\033[91mError\033[0m: No database migrations found.")
    sys.exit(1)

  # Check that hasura cli is installed
  if not shutil.which('hasura'):
    sys.exit(f'Hasura CLI is not installed. Exiting...')
  else:
    os.system('hasura version')

  # Get the Username/Password
  username = ""
  password = ""
  usernameFound = False
  passwordFound = False
  with open(args.env_path) as envFile:
    for line in envFile:
      if usernameFound and passwordFound:
        break
      line = line.strip()
      if line.startswith("AERIE_USERNAME"):
        username = line.removeprefix("AERIE_USERNAME=")
        usernameFound = True
        continue
      if line.startswith("AERIE_PASSWORD"):
        password = line.removeprefix("AERIE_PASSWORD=")
        passwordFound = True
        continue
  if not usernameFound:
    print("\033[91mError\033[0m: AERIE_USERNAME environment variable is not defined in "+args.env_path+".")
    sys.exit(1)
  if not passwordFound:
    print("\033[91mError\033[0m: AERIE_PASSWORD environment variable is not defined in "+args.env_path+".")
    sys.exit(1)

  # Navigate to the hasura directory
  os.chdir(HASURA_PATH)

  # Mark all migrations previously applied to the databases to be updated as such
  mark_current_version(migration_db, username, password, args.network_location)

  # Enter step-by-step mode if not otherwise specified
  if not args.all:
    while True:
      clear_screen()
      print(f'\n###############################'
            f'\nAERIE DATABASE MIGRATION HELPER'
            f'\n###############################')

      print(f'\n0) \033[4mQ\033[0muit the migration helper')
      for migration_number in range(0,len(migration_db)):
        print(f'\n{migration_number+1}) Database: {migration_db[migration_number].db_name}')
        output = subprocess.getoutput(f'hasura migrate status --database-name {migration_db[migration_number].db_name}').split("\n")
        del output[0:3]
        print("\n".join(output))

      value = -1
      while value < 0 or value > len(migration_db):
        _input = input(f"\nSelect a database to migrate (0-{len(migration_db)}): ").lower()
        if _input == 'q' or _input == '0':
          sys.exit()

        try:
          value = int(_input)
        except ValueError:
          value = -1

      # Go step-by-step through the migrations available for the selected database
      step_by_step_migration(migration_db[value-1], args.apply)
  else:
    bulk_migration(migration_db, args.apply)
  print()

if __name__ == "__main__":
  main()
