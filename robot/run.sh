#!/bin/bash

docker run \
    -v ${PWD}/../robot/results:/opt/robotframework/reports/:Z \
    -v ${PWD}/../robot/tests:/opt/robotframework/tests:Z \
    -e BROWSER=chrome \
    --network=host \
    -it ppodgorsek/robot-framework \
    bash