#!/bin/sh

version="v0.5.3"
os=$(uname | tr '[A-Z]' '[a-z]')

cp ../examples/banananation/build/libs/banananation.jar ./assets

npm install && npm run build

curl -L "https://github.com/grafana/xk6-dashboard/releases/download/${version}/xk6-dashboard_${version}_${os}_amd64.tar.gz" | tar xvz k6

./k6 run \
    --out "json=load-report.json" \
    --out "dashboard=period=1s&report=load-report.html" \
    ./dist/load-test.js
