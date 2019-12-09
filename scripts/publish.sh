#!/bin/bash

# ---------------------------------------------------------------------------
# Publish projects which were changed.
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
      shift; tag="$1" ;;
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

# TODO: Set PORT based on environment (16001 for dev, 16002 for staging, 16003 for release)
echo "Logging into Docker with user ${DOCKER_LOGIN_USERNAME}"
echo "${DOCKER_LOGIN_PASSWORD}" | docker login -u "${DOCKER_LOGIN_USERNAME}" cae-artifactory.jpl.nasa.gov:16001 --password-stdin

if [ ! -z ${branch} ]
then
  echo "Branch passed, building select projects..."
  changed=$(git diff --name-only $branch... | cut -d "/" -f1 | uniq)
else
  echo "No branch detected, building everything..."
  changed=$(ls -1)
fi

printf "\nPublishing schemas...\n\n"
cd schemas
mvn -B -s settings.xml deploy -DskipTests
[ $? -ne 0 ] && error_exit "mvn deploy failed"
cd $root

printf "\nPublishing merlin-sdk...\n\n"
cd merlin-sdk
mvn -B -s settings.xml deploy -DskipTests
[ $? -ne 0 ] && error_exit "mvn deploy failed"
cd $root

# Publish Docker images for the Aerie services.
# We don't check $changed here because these images bundle their dependencies,
# and we don't have a way from this script to check if any of the dependencies
# changed even if the codebase of the services didn't.
(
  export tag_name_base="cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/ammos/mpsa/aerie"

  (
    cd services/

    d=adaptation
    tag_name="$tag_name_base/$d:$tag_docker"

    echo "Publishing $tag_name to Artifactory"
    docker push "$tag_name"
    [ $? -ne 0 ] && error_exit "docker push failed for $tag_name"
  )

  (
    cd services/

    d=plan
    tag_name="$tag_name_base/$d:$tag_docker"

    echo "Publishing $tag_name to Artifactory"
    docker push "$tag_name"
    [ $? -ne 0 ] && error_exit "docker push failed for $tag_name"
  )
)

docker logout cae-artifactory.jpl.nasa.gov:16001

graceful_exit
