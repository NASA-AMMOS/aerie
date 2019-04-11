#!/bin/bash
# Helper script to upload test adaptation to adaptation service

echo "Posting banananation to adaptations"

curl -X POST \
  'http://localhost:27182/api/adaptations?/=' \
  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \
  -F mission=test \
  -F name=testfile \
  -F owner=testueser \
  -F version=1.0.0 \
  -F file=@../banananation/target/banananation-1.0-SNAPSHOT.jar