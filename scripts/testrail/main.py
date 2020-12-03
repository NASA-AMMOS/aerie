#!/usr/bin/env python3
# Uploads the results to Test Rail

from datetime import datetime
from enum import Enum
import functools
from joblib import Parallel, delayed
import os
import sys
from testrail import *
import xml.etree.ElementTree as ET

class StatusId(Enum):
    PASS = 1
    SKIP = 3
    FAIL = 5
    ERROR = 5

def main():
    if len(sys.argv) >= 3:
        path = sys.argv[1]
        suite_id = sys.argv[2]
    else:
        print("You must provide a path to a Junit XML file or,"
            "directory contain some XML files and a Test Rail suite id")
        exit(1)

    BASE_URL = os.environ['TESTRAIL_URL']
    USERNAME = os.environ['TESTRAIL_USERNAME']
    API_KEY = os.environ['TESTRAIL_API_KEY']

    print('Authenticated as {}'.format(USERNAME))

    client = APIClient(BASE_URL)
    client.user = USERNAME
    client.password = API_KEY

    user_id = client.send_get('get_user_by_email&email={}'.format(USERNAME))['id']

    allTests = get_all_tests(path)

    section_set = {test["section"] for test in allTests}
    project_id = get_project(client, suite_id)
    sections_dictionary = create_sections_dictionary(client, project_id, suite_id)
    for section in section_set:
        # ensure that there is no race case for creation of a section
        get_section_id(client, sections_dictionary, section, project_id, suite_id)

    cases_dictionary = create_cases_dictionary(client, project_id, suite_id)

    run_id = client.send_post(
        f'/add_run/{project_id}',
        {'name': f'{datetime.now()}', 'suite_id': suite_id}
    ).get('id')
    worker = functools.partial(
        upload_test,
        client,
        cases_dictionary,
        project_id,
        suite_id,
        run_id
    )

    # Splits the upload to n processes where n is the number of CPUs
    Parallel(n_jobs=-1, backend="threading", verbose=10)(delayed(worker)(test)
                                                             for test in allTests)
def get_all_tests(path):
    allTests = []
    if os.path.isdir(path):
        xml_files = [path+"/"+f for f in os.listdir(path) if f.endswith('.xml')]
        if not xml_files:
            print("Directory path doesn't contain any XML file")
            exit(1)
        for file in xml_files:
            with open(file) as xml_file:
                tests = ET.parse(xml_file)
                allTests = allTests + xml_to_tests(tests)
    elif os.path.isfile(path):
        with open(path) as xml_file:
            tests = ET.parse(xml_file)
            allTests = xml_to_tests(tests)
    else:
        print("Please provide a valid path for a single XML file or,"
            " a directory that contain XML files")
        exit(1)
    return allTests

def get_project(client, suite_id):
    return client.send_get(
        '/get_suite/{}'.format(suite_id)
    ).get('project_id')

def get_sections(client, project_id, suite_id):
    return client.send_get(
        f'/get_sections/{project_id}&suite_id={suite_id}'
    )

def get_cases(client, project_id, suite_id):
    return client.send_get(
        f'/get_cases/{project_id}&suite_id={suite_id}'
    )

def create_sections_dictionary(client, project_id, suite_id):
    sections = get_sections(client, project_id, suite_id)
    return {section.get('name'): section.get('id') for section in sections}

def create_cases_dictionary(client, project_id, suite_id):
    cases = get_cases(client, project_id, suite_id)
    return {case.get('title'): case.get('id') for case in cases}

def get_section_id(client, sections_dictionary, section_title, project_id, suite_id):
    if (section_title in sections_dictionary):
        return sections_dictionary.get(section_title)
    else:
        return client.send_post(
            f'/add_section/{project_id}',
            {'name': section_title, 'suite_id': suite_id}
        ).get('id')

def get_case_id(client, cases_dictionary, case_title, section_id):
    if (case_title in cases_dictionary):
        return cases_dictionary.get(case_title)
    else:
        return client.send_post(f'/add_case/{section_id}', {'title': case_title}).get('id')

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
    test_nodes = xml_file.findall('testcase')
    tests = []

    for test_node in test_nodes:
        status = get_test_status(test_node)
        # Test rail doesn't support skipped or errors status
        if status == StatusId.PASS or status == StatusId.FAIL:
            tests = tests + [
                {
                    'section': test_node.get('classname'),
                    'name': test_node.get('classname') + '.' + test_node.get("name"),
                    'status': status.value,
                    'time': test_node.get('time')
                }
            ]
    return tests

def get_test_status(node):
    if node.findall('failure'):
        return StatusId.FAIL
    if node.findall('errors'):
        return StatusId.ERROR
    if node.findall('skipped'):
        return StatusId.SKIP
    return StatusId.PASS

def test_to_payload(test):
    return {
        'status_id': test['status'],
        'elapsed': test['time'] + 's' if (float(test['time']) > 0) else '1s'
    }

def upload_test(client, cases_dictionary, project_id, suite_id, run_id, test):
    sections_dictionary = create_sections_dictionary(client, project_id, suite_id)
    section_id = get_section_id(
        client, sections_dictionary, test['section'], project_id, suite_id)
    case_id = get_case_id(client, cases_dictionary, test['name'], section_id)
    payload = test_to_payload(test)
    client.send_post(f'/add_result_for_case/{run_id}/{case_id}', payload)

if __name__ == "__main__":
    main()
