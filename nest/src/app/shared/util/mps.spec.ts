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
    __kind: 'fs_dir',
    __db_type: 'mongodb',
    name: 'layouts',
    label: 'layouts',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/europa-mps/layouts',
    permissions: 'rw-rw-rw- null null',
  } as unknown) as MpsServerSource,
];

const fileEntries: MpsServerSource[] = [
  ({
    customMeta: [],
    __kind: 'fs_file',
    __db_type: 'mongodb',
    name: 'states_layouts_and_links_layout',
    label: 'states_layouts_and_links_layout',
    __kind_sub: 'file_generic',
    contents_url:
      'https://localhost:8443/mpsserver/api/v2/list_generic-mongodb/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
    file_data_url:
      'https://localhost:8443/mpsserver/api/v2/fs-mongodb/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
    permissions: 'rw-r--r-- null all.usjpl',
    created: '2018-02-14 02:07:58-0800',
    modified: '2018-02-14 02:07:58-0800',
  } as unknown) as MpsServerSource,
  ({
    customMeta: [],
    __kind: 'fs_file',
    __db_type: 'mongodb',
    name: 'states_layouts_and_links_state',
    label: 'states_layouts_and_links_state',
    __kind_sub: 'file_generic',
    contents_url:
      'https://localhost:8443/mpsserver/api/v2/list_generic-mongodb/TEST_ATS/STATES/states_layouts_and_links_state',
    file_data_url:
      'https://localhost:8443/mpsserver/api/v2/fs-mongodb/TEST_ATS/STATES/states_layouts_and_links_state',
    permissions: 'rw-r--r-- null all.usjpl',
    created: '2018-02-14 02:07:28-0800',
    modified: '2018-02-14 02:07:28-0800',
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
        '/TEST_ATS/LAYOUTS/states_layouts_and_links_layout',
        '/TEST_ATS/STATES/states_layouts_and_links_state',
      ]);
    });
  });
});
