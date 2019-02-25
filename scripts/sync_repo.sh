#!/bin/bash

# ---------------------------------------------------------------------------
# Perform a one-way syncronization from a Git repository deleting all files
# in the destination.
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
  echo -e "Usage: $PROGNAME [-h|--help] [-b|--branch name] [-d|--dest path] [-r|--repo url]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME ver. $VERSION
  Perform a one-way syncronization from a Git repository deleting all files in the destination.

  $(usage)

  Options:
  -h, --help  Display this help message and exit.
  -b, --branch name  Branch name
    Where 'name' is the name of the branch to use when cloning.
  -d, --dest path  Destination path
    Where 'path' is the path where the repository will be extracted.
  -r, --repo url  External repository to syncronize
    Where 'url' is the HTTPS or SSH Git URL which will be copied.

_EOF_
  return
}

# Trap signals
trap "signal_exit TERM" TERM HUP
trap "signal_exit INT"  INT



# Parse command-line
while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; graceful_exit ;;
    -b | --branch)
      shift; branch="$1" ;;
    -d | --dest)
      shift; dest="$1" ;;
    -r | --repo)
      shift; url="$1" ;;
    -* | --*)
      usage
      error_exit "Unknown option $1" ;;
    *)
      echo "Argument $1 to process..." ;;
  esac
  shift
done

read -p "This operation will delete all files at dest. Continue? (y/N)? " choice
case "$choice" in 
  y|Y ) rm -rf $dest ;;
  n|N ) graceful_exit ;;
  * ) graceful_exit ;;
esac

git clone --depth=1 -b "$branch" "$url" "$dest"

rm -rf $dest/.git

echo "This was cloned from $url@$branch." > $dest/DO_NOT_EDIT

graceful_exit

