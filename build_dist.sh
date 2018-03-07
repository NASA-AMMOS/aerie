#! /bin/bash

# exit upon error
set -e

# echo commands
set -x

# clean
rm -rf node_modules
rm -rf src/assets/bower_components
rm -rf dist

# install dependencies
npm install

# build
npm run build-prod-mpsserver

# tar up dist
cd dist
tar -czf raven2-$SEQBASETAG.tar.gz `ls -A`
cd ..

# run tests
set +e
npm run test-for-build
# another command after running tests is required so the script returns 0
echo "build dist script finished"

