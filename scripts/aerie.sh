#!/bin/bash
# Helper script used to build Aerie (https://github.jpl.nasa.gov/MPS/aerie) locally
# Generated with http://linuxcommand.org/lc3_new_script.php

PROGNAME=${0##*/}
VERSION="0.1"

# Perform pre-exit housekeeping.
clean_up() {
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

# Handle trapped signals.
signal_exit() {
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
  echo -e "Usage: $PROGNAME [-b|--build] [-bc|--build-cli] [-bp|--build-plan] [-br|--build-run] [-bs|--build-schemas] [-c|--clean] [-cbr|--clean-build-run] [-h|--help] [-r|--run] [-w|--workdir workdir]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME ver. $VERSION
  Helper script used to build Aerie (https://github.jpl.nasa.gov/MPS/aerie) locally

  $(usage)

  Options:
  -b, --build             Build all the Aerie services that use Maven.
  -bc, --build-cli        Build the Merlin CLI only.
  -bp, --build-plan       Build the plan service only.
  -br, --build-run        Build and run all the Aerie services that use Maven.
  -bs, --build-schemas    Build the schemas only.
  -c, --clean             Clean up all Docker containers, images and Maven target folders.
  -cbr, --clean-build-run Clean, build, and run all the Aerie services that use Maven.
  -h, --help              Display this help message and exit.
  -r, --run               Run Docker containers for the Aerie services.
  -w, --workdir           Set the current working directory.

_EOF_
  return
}

# Trap signals.
trap "signal_exit TERM" TERM HUP
trap "signal_exit INT"  INT

build=false
build_cli=false
build_plan=false
build_run=false
build_schemas=false
clean=false
clean_build_run=false
run=false
workdir="."

# Parse command-line.
while [[ -n $1 ]]; do
  case $1 in
    -b | --build)
      shift; build=true ;;
    -bc | --build-cli)
      shift; build_cli=true ;;
    -bp | --build-plan)
      shift; build_plan=true ;;
    -br | --build-run)
      shift; build_run=true ;;
    -bs | --build-schemas)
      shift; build_schemas=true ;;
    -c | --clean)
      shift; clean=true ;;
    -cbr | --clean-build-run)
      shift; clean_build_run=true ;;
    -h | --help)
      help_message; graceful_exit ;;
    -r | --run)
      shift; run=true ;;
    -w | --workdir)
      shift; workdir="$1" ;;
    -* | --*)
      usage
      error_exit "Unknown option $1" ;;
    *)
      echo "Argument $1 to process..." ;;
  esac
  shift
done

build() {
  echo -e "Building all services..."
  cd_workdir
  mvn_build
}

build_cli() {
  echo -e "Building cli..."
  cd_workdir
  cd merlin-cli
  mvn_build
}

build_plan() {
  echo -e "Building plan..."
  cd_workdir
  cd plan
  mvn_build
}

build_run() {
  build
  run
}

build_schedule() {
  echo -e "Building schedule..."
  cd_workdir
  cd schedule
  mvn_build
}

build_schemas() {
  echo -e "Building schemas..."
  cd_workdir
  cd schemas
  npm run build
  mvn_build
}

cd_workdir() {
  eval cd "$workdir"
  echo "Working directory is $workdir"
}

clean() {
  echo -e "Running clean..."
  cd_workdir

  mvn_clean

  docker-compose down
  docker stop aerie_plan_1 aerie_tyk_gateway_1 aerie_adaptation_1 aerie_plan_mongo_1 aerie_tyk_redis_1 aerie_tyk_pump_1 aerie_tyk_dashboard_1 aerie_tyk_mongo_1 aerie_adaptation_runtime_1 aerie_simulation_1 aerie_rabbitmq_1
  docker rm aerie_plan_1 aerie_tyk_gateway_1 aerie_adaptation_1 aerie_adaptation_mongo_1 aerie_plan_mongo_1 aerie_tyk_redis_1 aerie_tyk_pump_1 aerie_tyk_dashboard_1 aerie_tyk_mongo_1 aerie_adaptation_runtime_1 aerie_simulation_1 aerie_rabbitmq_1
  docker rmi aerie_plan aerie_adaptation aerie_adaptation_runtime aerie_simulation
  echo -e "\nDocker Images:"
  docker images
  echo -e "\nDocker Containers:"
  docker ps -a
}

clean_build_run() {
  clean
  build
  run
}

mvn_build() {
  mvn_clean
  mvn_install
}

mvn_clean() {
  echo -e "Running mvn clean..."
  mvn clean -f pom.xml
  rm_m2
}

mvn_install() {
  echo -e "Running mvn install..."
  mvn install install -f pom.xml -DskipTests
}

rm_m2() {
  echo -e "Removing ~/.m2/repository/gov..."
  rm -rf ~/.m2/repository/gov
}

run() {
  echo -e "Running..."
  cd_workdir
  docker-compose -f docker-compose-local.yml up --build
}

main() {
  if $build; then
    rm_m2
    build
  fi

  if $build_cli; then
    build_cli
  fi

  if $build_plan; then
    build_plan
  fi

  if $build_run; then
    build_run
  fi

  if $build_schemas; then
    build_schemas
  fi

  if $clean; then
    clean
  fi

  if $clean_build_run; then
    clean_build_run
  fi

  if $run; then
    run
  fi
}

main
graceful_exit
