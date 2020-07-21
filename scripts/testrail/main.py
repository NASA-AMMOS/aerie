#!/usr/bin/env python3
# Uploads the results to Test Rail

from datetime import datetime
from enum import Enum
from joblib import Parallel, delayed
from testrail import *
import functools
import ipdb
import os
import sys
import xml.etree.ElementTree as ET


class StatusId(Enum):
    PASS = 1
    FAIL = 5


if len(sys.argv) == 3:
    result_file_relative = sys.argv[1]
    suite_id = sys.argv[2]
else:
    print('You must provide a path to a testOutput.xml file, and a Test Rail suite id')
    exit()

dirname = os.path.dirname(__file__)

BASE_URL = 'https://cae-testrail.jpl.nasa.gov/testrail/'
USERNAME = os.environ['TESTRAIL_USERNAME']
API_KEY = os.environ['TESTRAIL_API_KEY']

print('Authenticated as {}'.format(USERNAME))

client = APIClient(BASE_URL)
client.user = USERNAME
client.password = API_KEY

id_regex = '(\[\w*\])'
user_id = client.send_get('get_user_by_email&email={}'.format(USERNAME))['id']

result_file = os.path.join(dirname, result_file_relative)


def get_project(suite_id):
    return client.send_get(
        '/get_suite/{}'.format(suite_id)
    ).get('project_id')


def get_sections(project_id, suite_id):
    return client.send_get(
        f'/get_sections/{project_id}&suite_id={suite_id}'
    )


def get_cases(project_id, suite_id):
    return client.send_get(
        f'/get_cases/{project_id}&suite_id={suite_id}'
    )


def create_sections_dictionary(project_id, suite_id):
    sections = get_sections(project_id, suite_id)
    return {section.get('name'): section.get('id') for section in sections}


def create_cases_dictionary(project_id, suite_id):
    cases = get_cases(project_id, suite_id)
    return {case.get('title'): case.get('id') for case in cases}


def get_section_id(sections_dictionary, section_title, project_id, suite_id):
    if (section_title in sections_dictionary):
        return sections_dictionary.get(section_title)
    else:
        return client.send_post(
            f'/add_section/{project_id}',
            {'name': section_title, 'suite_id': suite_id}
        ).get('id')


def get_case_id(cases_dictionary, case_title, section_id):
    if (case_title in cases_dictionary):
        return cases_dictionary.get(case_title)
    else:
        return client.send_post(f'/add_case/{section_id}', {'title': case_title}).get('id')


def post_payload(run_id, case_id, payload):
    client.send_post(
        '/add_result_for_case/{run_id}/{case_id}'.format(
            run_id=run_id,
            case_id=case_id
        ),
        payload
    )

    client.send_get('/get_cases/&suite_id=4115')


def xml_node_contains_member_once_with_text(node, member):
    return len(node.findall(member)) == 1 and node.findall(member)[0].text != None


def xml_node_to_description(xml_node):
    if (xml_node_contains_member_once_with_text(xml_node, 'message')):
        if (xml_node_contains_member_once_with_text(xml_node, 'stacktrace')):
            return 'BEGIN MESSAGE\n' + xml_node.findall('message')[0].text + '\nEND MESSAGE\nBEGIN STACKTRACE\n' + xml_node.findall('stacktrace')[0].text + 'END STACKTRACE'
        else:
            return 'Message: ' + xml_node.findall('message')[0].text
    else:
        return 'No message'


def xml_to_tests(xml_file):
    test_nodes = xml_file.findall('test')
    tests = []

    for test_node in test_nodes:
        testresult_nodes = test_node.findall('testresult')
        if (len(testresult_nodes) != 0):
            tests = tests + [
                {
                    'section': test_node.get('name'),
                    'comment': xml_node_to_description(testresult_node),
                    'name': test_node.get('name') + '.' + testresult_node.get("name"),
                    'status': testresult_node.get('status'),
                    'time': testresult_node.get('time')
                } for testresult_node in testresult_nodes
            ] + [
                {
                    'section': test_node.get('name'),
                    'comment': xml_node_to_description(test_node),
                    'name': test_node.get('name'),
                    'status': test_node.get('status'),
                    'time': test_node.get('time')
                }
            ]
    return tests


def test_to_payload(test):
    return {
        'comment': test['comment'],
        'status_id': StatusId[test['status']].value,
        'elapsed': test['time'] + 's' if (int(test['time']) > 0) else '1s'
    }


def upload_test(cases_dictionary, project_id, suite_id, run_id, test):
    sections_dictionary = create_sections_dictionary(project_id, suite_id)
    section_id = get_section_id(
        sections_dictionary, test['section'], project_id, suite_id)
    case_id = get_case_id(cases_dictionary, test['name'], section_id)
    payload = test_to_payload(test)
    client.send_post(f'/add_result_for_case/{run_id}/{case_id}', payload)


with open(result_file) as xml_file:
    results = ET.parse(xml_file)
    project_id = get_project(suite_id)

    tests = xml_to_tests(results)
    section_set = {test["section"] for test in tests}
    sections_dictionary = create_sections_dictionary(project_id, suite_id)
    for section in section_set:
        # ensure that there is no race case for creation of a section
        get_section_id(sections_dictionary, section, project_id, suite_id)

    cases_dictionary = create_cases_dictionary(project_id, suite_id)

    run_id = client.send_post(
        f'/add_run/{project_id}',
        {'name': f'{datetime.now()}', 'suite_id': suite_id}
    ).get('id')

    worker = functools.partial(
        upload_test,
        cases_dictionary,
        project_id,
        suite_id,
        run_id
    )

    # Splits the upload to n processes where n is the number of CPUs
    Parallel(n_jobs=-1, backend="threading", verbose=10)(delayed(worker)(test)
                                                         for test in tests)
