#!/usr/bin/env bash

cd scripts/testrail
python3 -m venv env
source env/bin/activate
pip3 install -r requirements.txt

python3 main.py $1 $2

cd -
