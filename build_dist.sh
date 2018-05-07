#! /bin/bash

# exit upon error
set -e

# echo commands
set -x

# clean
rm -rf node_modules
rm -rf dist

# install dependencies
time npm ci

# build
time npm run build-prod-mpsserver

# static code analysis
time npm run sonarqube

# tar up dist
cd dist
tar -czf raven2-$SEQBASETAG.tar.gz `ls -A`
cd ..

# run tests
set +e
time npm run test-for-build
# another command after running tests is required so the script returns 0
echo "build dist script finished"
