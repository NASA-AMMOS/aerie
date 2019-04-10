#!/bin/bash

# ---------------------------------------------------------------------------
# Analyze projects which were changed.
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

CLOC_REL="1.80"
CLOC_URL="https://github.com/AlDanial/cloc/releases/download/${CLOC_REL}/cloc-${CLOC_REL}.tar.gz"
SONAR_REL="3.3.0.1492"
SONAR_URL="https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_REL}-linux.zip"

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

# Run cloc analysis, use --exclude-dir because it is processed before files are
# counted and thus has better perf. See https://stackoverflow.com/a/26679008
# NOTE: path separators are not allowed, see documentation for --exclude-dir
echo "Running cloc analysis..."
clocignore=$(tr '\n' ',' < .clocignore)
echo "$clocignore"
cloc --report-file=aerie-cloc-${tag}.txt --exclude-dir="$clocignore" ./

# Run sonar-scanner on changed projects that have sonar-project.properties files
if [ ! -z ${branch} ]
then
  echo "Branch passed, building select projects..."
  changed=$(git diff --name-only $branch... | cut -d "/" -f1 | uniq)
else
  echo "No branch detected, building everything..."
  changed=$(ls -1)
fi

for d in $changed
do
  if [ -d $d ]
  then
    cd $d
    if [ -d == "nest" ]
    then
      npx sonar-scanner
    fi
    cd $root
  fi
done

graceful_exit
