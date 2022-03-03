#!/bin/bash

AUTH_URL="http://localhost:9000/auth/login"

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

USERNAME=$(whoami)
echo "Username: $USERNAME"
echo -n "Password: "
read -s PASSWORD
echo

curl -s -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}" -H 'Content-Type: application/json' $AUTH_URL > $SCRIPT_DIR/.ssotoken
cat $SCRIPT_DIR/.ssotoken
