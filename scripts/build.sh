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
      shift; tag="$1" ;;
    -* | --*)
      usage
      error_exit "Unknown option $1" ;;
    *)
      echo "Argument $1 to process..." ;;
  esac
  shift
done

# File only change: 3997569
# Files and directories change: 7ea41f9
# Directories only change: 34fc057
# Nothing changed?: 9d28f3e
# Single directory changed: 5ee8315
changed=$(git diff-tree --no-commit-id --name-only $commit)
for d in $changed
do
  if [ -d $d ]
  then
    cd $d
    if [ -f Dockerfile ]
    then

      # PRE-BUILD STUFF
      if [ $d == "nest" ]; then
        # Create the special environment.ts file that the build script usually
        # would generate but is now, not.
        nest_version=$(grep -m 1 "version" ./package.json | sed -e "s/  \"version\"/packageJsonVersion/" | sed -e "s/\"/'/g")
        echo "export const version = {\n  ${nest_version}\n  version: '${tag}'\n};" > src/environments/version.ts
        cat src/environments/version.ts
      fi

      # 16001 is local, 2 is staging, 3 is release
      tag_name="cae-artifactory.jpl.nasa.gov:16001/gov/nasa/jpl/ammos/mpsa/aerie/$d:$tag"
      echo "building container $tag_name..."
      docker build -t "$tag_name" --rm .
      RETVAL=$?
      [ $RETVAL -ne 0 ] && error_exit "docker build failed for $tag_name"

      # POST BUILD STUFF
      if [ $d == "nest" ]; then
        # Copy the test-results and coverage to the host for later evaluation
        echo "copying nest build files from container to host..."

        run_name="$project_$d_$tag"
        docker run --name "$run_name" "$tag_name" date
        docker cp "$run_name":/var/log/build_log.tar.gz ./
        docker rm "$run_name"
        tar -xf build_log.tar.gz
        rm build_log.tar.gz

        # Tar the dist dir for legacy reasons
        tar -zcf "$project_$d_dist_$tag.tar.gz" dist/
        mv "$project_$d_dist_$tag.tar.gz" $root/
      fi
    fi
    cd $root
  fi
done

graceful_exit

