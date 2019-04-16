/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MpsServerSource } from '../models';
import { getMpsPathForSource } from './mps';

// NOTE: We use `as unknown as MpsServerSource` here because, despite these being
// legitimate, exact copies of the output from MPS Server, they do not match the
// expected TypeScript interface for `MpsServerSource`.

const dirEntries: MpsServerSource[] = [
  ({
    __db_type: 'mongodb',
    __kind: 'fs_dir',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/europa-mps/layouts',
    label: 'layouts',
    name: 'layouts',
    permissions: 'rw-rw-rw- null null',
  } as unknown) as MpsServerSource,
];

const fileEntries: MpsServerSource[] = [
  ({
    __db_type: 'mongodb',
    __kind: 'fs_file',
    __kind_sub: 'file_pef',
    contents_url:
      'http://localhost:8080/mpsserver/api/v2/list_pef-mongodb/TEST_ATS/Big_Combo_Test/pef/prelim/test.json',
    created: '2019-01-08 10:49:46-0800',
    customMeta: [],
    file_data_url:
      'http://localhost:8080/mpsserver/api/v2/fs-mongodb/TEST_ATS/Big_Combo_Test/pef/prelim/test.json',
    label: 'test.json',
    modified: '2019-01-08 10:49:46-0800',
    name: 'test.json',
    permissions: 'rw-r--r-- null all.usjpl',
  } as unknown) as MpsServerSource,
  ({
    __db_type: 'mongodb',
    __kind: 'fs_file',
    __kind_sub: 'file_generic',
    contents_url:
      'https://localhost:8443/mpsserver/api/v2/list_generic-mongodb/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
    created: '2018-02-14 02:07:58-0800',
    customMeta: [],
    file_data_url:
      'https://localhost:8443/mpsserver/api/v2/fs-mongodb/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
    label: 'states_layouts_and_links_layout',
    modified: '2018-02-14 02:07:58-0800',
    name: 'states_layouts_and_links_layout',
    permissions: 'rw-r--r-- null all.usjpl',
  } as unknown) as MpsServerSource,
  ({
    __db_type: 'mongodb',
    __kind: 'fs_file',
    __kind_sub: 'file_generic',
    contents_url:
      'https://localhost:8443/mpsserver/api/v2/list_generic-mongodb/TEST_ATS/STATES/states_layouts_and_links_state',
    created: '2018-02-14 02:07:28-0800',
    customMeta: [],
    file_data_url:
      'https://localhost:8443/mpsserver/api/v2/fs-mongodb/TEST_ATS/STATES/states_layouts_and_links_state',
    label: 'states_layouts_and_links_state',
    modified: '2018-02-14 02:07:28-0800',
    name: 'states_layouts_and_links_state',
    permissions: 'rw-r--r-- null all.usjpl',
  } as unknown) as MpsServerSource,
];

describe('util.ts', () => {
  describe('getMpsPathForSource', () => {
    it('should get the path for directories', () => {
      expect(dirEntries.map(getMpsPathForSource)).toEqual([
        '/europa-mps/layouts',
      ]);
    });

    it('should get the path for files', () => {
      expect(fileEntries.map(getMpsPathForSource)).toEqual([
        '/TEST_ATS/Big_Combo_Test/pef/prelim/test.json',
        '/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
        '/TEST_ATS/STATES/states_layouts_and_links_state',
      ]);
    });
  });
});
