#!/usr/bin/env python3
# Uploads the results produced by Protractor to Test Rail

from testrail import *
import pprint
import json
import re
from tqdm import tqdm
import configparser
import os
import sys
from joblib import Parallel, delayed

if len(sys.argv) == 2:
    run_id = sys.argv[1]
else:
    print('You must provide a Test Rail Run Id')
    print('Example: npm run e2e-report 1234')
    exit()

pp = pprint.PrettyPrinter(indent=4)

dirname = os.path.dirname(__file__)
creds_file = os.path.join(dirname, 'config/creds')

config = configparser.ConfigParser()
config.read(creds_file)

BASE_URL = 'https://cae-testrail.jpl.nasa.gov/testrail/'
USERNAME = config['TESTRAIL']['Email']
API_KEY = config['TESTRAIL']['APIKey']

print('Authenticated as {}'.format(USERNAME))

client = APIClient(BASE_URL)
client.user = USERNAME
client.password = API_KEY

id_regex = '(\[\w*\])'
user_id = client.send_get('get_user_by_email&email={}'.format(USERNAME))['id']

result_file = os.path.join(dirname, '../../e2e/results/result.json')


def post_payload(run_id, case_id, payload):
    client.send_post(
        '/add_result_for_case/{run_id}/{case_id}'.format(
            run_id=run_id,
            case_id=case_id
        ),
        payload
    )


def create_payload(result):
    result_status = result['assertions'][0]['passed']
    result_comment = ''

    # Maps test success True/False to Test Rail
    # http://docs.gurock.com/testrail-api2/reference-results#add_result
    if result_status:
        # test passed
        result_status = 1
    else:
        # test failed
        result_status = 5
        result_comment = result['assertions'][0]['errorMsg']

    description_tokens = re.compile(id_regex).split(result['description'])
    result_route = description_tokens[0]
    # remove prefix '[C' and suffix ']'
    case_id = description_tokens[1][2:-1]
    result_description = description_tokens[2].strip()

    result_version = ''
    result_elapsed = '{} s'.format(result['duration'] / 1000)

    if result['duration'] == 0.0:
        result_elapsed = 0

    result_defects = ''

    payload = {
        'status_id': result_status,
        'comment': result_comment,
        'version': result_version,
        'elapsed': result_elapsed,
        'defects': result_defects,
        'assignedto_id': user_id
    }

    return case_id, payload


def worker(result):
    case_id, payload = create_payload(result)
    post_payload(run_id, case_id, payload)


with open(result_file) as json_file:
    results = json.load(json_file)
    print('Uploading results to Test Rail Run: T{}'.format(run_id))

    # Splits the upload to n processes where n is the number of CPUs
    Parallel(n_jobs=-1, backend="threading", verbose=10)(delayed(worker)(result)
                                                         for result in results)
