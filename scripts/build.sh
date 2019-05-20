#!/bin/bash

# ---------------------------------------------------------------------------
# Build projects which were changed.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License at <http://www.gnu.org/licenses/> for
# more details.
# ---------------------------------------------------------------------------

PROGNAME=${0##*/}
VERSION="0.1"

clean_up() { # Perform pre-exit housekeeping
  return
}

error_exit() {
  echo -e "${PROGNAME}: ${1:-"Unknown Error"}" >&2
  clean_up
  exit 1
}

graceful_exit() {
  clean_up
  exit
}

signal_exit() { # Handle trapped signals
  case $1 in
    INT)
      error_exit "Program interrupted by user" ;;
    TERM)
      echo -e "\n$PROGNAME: Program terminated" >&2
      graceful_exit ;;
    *)
      error_exit "$PROGNAME: Terminating on unknown signal" ;;
  esac
}

usage() {
  :%secho -e "Usage: $PROGNAME [-h|--help] [-b|--branch name] [-c|--commit hash] [-t|--tag name]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME ver. $VERSION
  Build projects which were changed 

  $(usage)

  Options:
  -h, --help  Display this help message and exit.
  -b, --branch name  Branch name
    Where 'name' is the name of the branch to use when cloning.
  -c, --commit hash Commit hash 
    Where 'hash' the commit hash to check.
  -t, --tag name Tag name 
    Where 'name' is a Docker tag.

_EOF_
  return
}

# Trap signals
trap "signal_exit TERM" TERM HUP
trap "signal_exit INT"  INT


project=aerie
root=$(pwd)
tag=dev

# Parse command-line
while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; graceful_exit ;;
    -b | --branch)
      shift; branch="$1" ;;
    -c | --commit)
      shift; commit="$1" ;;
    -t | --tag)
      shift; tag="$1" ; export SEQBASETAG=$tag ;;
    -* | --*)
      usage
      error_exit "Unknown option $1" ;;
    *)
      echo "Argument $1 to process..." ;;
  esac
  shift
done

# Create docker-compatible tag (remove + from tag)
tag_docker=`echo ${tag} | sed -e 's/+/-/g'`

if [ ! -z ${branch} ]
then
  echo "Branch passed, building select projects..."
  changed=$(git diff --name-only $branch... | cut -d "/" -f1 | uniq)
else
  echo "No branch detected, building everything..."
  changed=$(ls -1)
fi

printf "\nTop level changes:\n==================\n$changed\n\n"

# Build Java modules
# Always build all the maven modules from the root rather than being selective
# This is because maven figures out the correct build order and handles dependencies
# between modules more intelligently than the above git command.
echo "Building all maven modules from the root..."
mvn -B -f pom.xml -s settings.xml install
[ $? -ne 0 ] && error_exit "mvn install failed"

# Build sequencing

if echo "$changed" | grep --quiet "\(sequencing\|schemas\)"; then
  printf "\nBuilding sequencing...\n\n"
  cd sequencing

  npm ci
  [ $? -ne 0 ] && error_exit "npm ci failed"

  npm run test
  [ $? -ne 0 ] && error_exit "npm run test failed"

  npm run build
  [ $? -ne 0 ] && error_exit "npm run build failed"

  cd $root
fi

# Build nest

if echo "$changed" | grep --quiet "\(nest\|schemas\)"; then
  printf "\nBuilding nest...\n\n"
  cd nest

  npm ci
  [ $? -ne 0 ] && error_exit "npm ci failed"
  
  npm run license:check
  [ $? -ne 0 ] && error_exit "npm run license:check failed"

  npm run build-prod
  [ $? -ne 0 ] && error_exit "npm run build-prod failed"

  # Build MPS Server, this will eventually go away
  npm run build-prod-mpsserver
  [ $? -ne 0 ] && error_exit "npm run build-prod-mpsserver failed"

  npm run test-for-build
  [ $? -ne 0 ] && error_exit "npm run test-for-build failed"

  cd $root
fi

# Build Docker containers
for d in $changed
do
  if [ -d "$d" ]; then
    tag_name="cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/ammos/mpsa/aerie/$d:$tag_docker"
    cd $d
    if [ -f Dockerfile ]; then
        printf "\nBuilding $d Docker container: $tag_name...\n\n"
        docker build -t "$tag_name" --rm .
        [ $? -ne 0 ] && error_exit "Docker build failed for $tag_name"
    fi
    cd $root
  fi
done

graceful_exit

