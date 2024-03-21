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

def step_by_step_migration(db_migration, apply):
  display_string = "\n\033[4mMIGRATION STEPS AVAILABLE:\033[0m\n"
  _output = subprocess.getoutput(f'hasura migrate status --database-name {db_migration.db_name}').split("\n")
  del _output[0:3]
  display_string += _output[0] + "\n"

  # Filter out the steps that can't be applied given the current mode and currently applied steps
  available_steps = db_migration.steps.copy()
  for i in range(1, len(_output)):
    split = list(filter(None, _output[i].split(" ")))

    if len(split) >= 5 and "Not Present" == (split[2]+" "+split[3]):
      print("\n\033[91mError\033[0m: Migration files exist on server that do not exist on this machine. "
            "Synchronize files and try again.\n")
      input("Press Enter to continue...")
      return

    if apply:
      if (len(split) == 4) or (not os.path.isfile(f'migrations/{db_migration.db_name}/{split[0]}_{split[1]}/up.sql')):
        available_steps.remove(f'{split[0]}_{split[1]}')
      else:
        display_string += _output[i] + "\n"
    else:
      if (len(split) == 5 and "Not Present" == (split[3] + " " + split[4])) \
          or (not os.path.isfile(f'migrations/{db_migration.db_name}/{split[0]}_{split[1]}/down.sql')):
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
      os.system(f'hasura migrate apply --version {timestamp} --database-name {db_migration.db_name} --dry-run --log-level WARN')
    else:
      os.system(f'hasura migrate apply --version {timestamp} --type down --database-name {db_migration.db_name} --dry-run --log-level WARN')

    print()
    _value = ''
    while _value != "y" and _value != "n" and _value != "q" and _value != "quit":
      if apply:
        _value = input(f'Apply {step}? (y/n/\033[4mq\033[0muit): ').lower()
      else:
        _value = input(f'Revert {step}? (y/n/\033[4mq\033[0muit): ').lower()

    if _value == "q" or _value == "quit":
      sys.exit()
    if _value == "y":
      if apply:
        print('Applying...')
        exit_code = os.system(f'hasura migrate apply --version {timestamp} --type up --database-name {db_migration.db_name}')
      else:
        print('Reverting...')
        exit_code = os.system(f'hasura migrate apply --version {timestamp} --type down --database-name {db_migration.db_name}')
      os.system('hasura metadata reload')
      print()
      if exit_code != 0:
        return
    elif _value == "n":
      return
  input("Press Enter to continue...")

def bulk_migration(db_migration, apply):
  # Migrate each database
  exit_with = 0
  if apply:
    os.system(f'hasura migrate apply --database-name {db_migration.db_name} --dry-run --log-level WARN')
    exit_code = os.system(f'hasura migrate apply --database-name {db_migration.db_name}')
    if exit_code != 0:
      exit_with = 1
  else:
    os.system(f'hasura migrate apply --goto 1 --database-name {db_migration.db_name} --dry-run --log-level WARN &&'
              f'hasura migrate apply --down 1 --database-name {db_migration.db_name} --dry-run --log-level WARN')
    exit_code = os.system(f'hasura migrate apply --goto 1 --database-name {db_migration.db_name} &&'
                          f'hasura migrate apply --down 1 --database-name {db_migration.db_name}')
    if exit_code != 0:
      exit_with = 1

  os.system('hasura metadata reload')

  # Show the result after the migration
  print(f'\n###############'
        f'\nDatabase Status'
        f'\n###############')
  _output = subprocess.getoutput(f'hasura migrate status --database-name {db_migration.db_name}').split("\n")
  del _output[0:3]
  print("\n".join(_output))
  exit(exit_with)

def mark_current_version(username, password, netloc):
  # Convert db.name to the actual format of the db name: aerie_dbSuffix
  connectionString = "postgres://"+username+":"+password+"@"+netloc+":5432/aerie"

  # Connect to DB
  with psycopg.connect(connectionString) as connection:
    # Open a cursor to perform database operations
    with connection.cursor() as cursor:
      # Get the current schema version
      cursor.execute("SELECT migration_id FROM migrations.schema_migrations ORDER BY migration_id::int DESC LIMIT 1")
      current_schema = int(cursor.fetchone()[0])

  # Mark everything up to that as applied
  for i in range(0, current_schema+1):
    os.system('hasura migrate apply --skip-execution --version '+str(i)+' --database-name aerie >/dev/null 2>&1')

def main():
  # Create a cli parser
  parser = argparse.ArgumentParser(description=__doc__)
  # Applying and Reverting are exclusive arguments
  exclusive_args = parser.add_mutually_exclusive_group(required='true')

  # Add arguments
  exclusive_args.add_argument(
    '-a', '--apply',
    help="apply migration steps to the database",
    action='store_true')

  exclusive_args.add_argument(
    '-r', '--revert',
    help="revert migration steps to the databases",
    action='store_true')

  parser.add_argument(
    '--all',
    help="apply[revert] ALL unapplied[applied] migration steps to the database",
    action='store_true')

  parser.add_argument(
    '-p', '--hasura-path',
    help="the path to the directory containing the config.yaml for Aerie. defaults to ./hasura")

  parser.add_argument(
    '-e', '--env-path',
    help="the path to the .env file used to deploy aerie. must define AERIE_USERNAME and AERIE_PASSWORD")

  parser.add_argument(
    '-n', '--network-location',
    help="the network location of the database. defaults to localhost",
    default='localhost')

  # Generate arguments
  args = parser.parse_args()

  HASURA_PATH = "./hasura"
  if args.hasura_path:
    HASURA_PATH = args.hasura_path
  MIGRATION_PATH = HASURA_PATH+"/migrations/Aerie"

  # find all migration folders for the database
  migration = DB_Migration("Aerie")
  try:
    for root,dirs,files in os.walk(MIGRATION_PATH):
      if dirs:
        migration.add_migration_step(dirs)
  except FileNotFoundError as fne:
    print("\033[91mError\033[0m:"+ str(fne).split("]")[1])
    sys.exit(1)
  if len(migration.steps) <= 0:
    print("\033[91mError\033[0m: No database migrations found.")
    sys.exit(1)

  # If reverting, reverse the list
  if args.revert:
    migration.steps.reverse()

  # Check that hasura cli is installed
  if not shutil.which('hasura'):
    sys.exit(f'Hasura CLI is not installed. Exiting...')
  else:
    os.system('hasura version')

  # Get the Username/Password
  username = os.environ.get('AERIE_USERNAME', "")
  password = os.environ.get('AERIE_PASSWORD', "")

  if args.env_path:
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
  mark_current_version(username, password, args.network_location)

  clear_screen()
  print(f'\n###############################'
        f'\nAERIE DATABASE MIGRATION HELPER'
        f'\n###############################')
  # Enter step-by-step mode if not otherwise specified
  if not args.all:
    # Go step-by-step through the migrations available for the selected database
    step_by_step_migration(migration, args.apply)
  else:
    bulk_migration(migration, args.apply)

if __name__ == "__main__":
  main()
