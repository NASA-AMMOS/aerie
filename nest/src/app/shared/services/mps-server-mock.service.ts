/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import {
  MpsServerSource,
  RavenSource,
  SourceFilter,
  StringTMap,
} from '../models';

// NOTE: We use `as unknown as MpsServerSource` here because, despite these being
// legitimate, exact copies of the output from MPS Server, they do not match the
// expected TypeScript interface for `MpsServerSource`.

const filesystem: StringTMap<MpsServerSource> = {
  '/mongo/db1': ({
    __kind: 'fs_dir',
    __db_type: 'mongodb',
    name: 'db1',
    label: 'db1',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1',
    permissions: 'rw-r--r-- null all.usjpl',
    created: '2019-01-08 12:13:24-0800',
    modified: '2019-01-08 12:13:24-0800',
  } as unknown) as MpsServerSource,
  '/mongo/db1/abc': ({
    customMeta: [],
    __kind: 'fs_file',
    __db_type: 'mongodb',
    name: 'abc',
    label: 'abc',
    __kind_sub: 'file_pef',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/list_pef-mongodb/mongo/db1/abc',
    file_data_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1/abc',
    permissions: 'rw-r--r-- null all.usjpl',
    created: '2018-08-06 11:05:44-0700',
    modified: '2018-08-06 11:05:44-0700',
  } as unknown) as MpsServerSource,
  '/mongo/db1/xabcy': ({
    customMeta: [],
    __kind: 'fs_file',
    __db_type: 'mongodb',
    name: 'xabcy',
    label: 'xabcy',
    __kind_sub: 'file_pef',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/list_pef-mongodb/mongo/db1/xabcy',
    file_data_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1/xabcy',
    permissions: 'rw-r--r-- null all.usjpl',
    created: '2018-08-06 11:05:44-0700',
    modified: '2018-08-06 11:05:44-0700',
  } as unknown) as MpsServerSource,
};

export class NotImplementedError extends Error {}

export const MockedFilters = {
  'name matches abc': { name: { matches: 'abc' } },
};

@Injectable({
  providedIn: 'root',
})
export class MpsServerMockService {
  fetchNewSources(
    url: string,
    parentId: string,
    isServer: boolean,
    tree: StringTMap<RavenSource> | null,
  ) {
    throw new NotImplementedError('fetchNewSources is not mocked.');
  }

  fetchState(url: string) {
    throw new NotImplementedError('fetchState is not mocked.');
  }

  importMappingFile(sourceUrl: string, name: string, mapping: string) {
    throw new NotImplementedError('importMappingFile is not mocked.');
  }

  removeSource(sourceUrl: string, sourceId: string) {
    throw new NotImplementedError('removeSource is not mocked.');
  }

  saveState(sourceUrl: string, name: string, state: any) {
    throw new NotImplementedError('saveState is not mocked.');
  }

  updateState(stateUrl: string, state: any) {
    throw new NotImplementedError('updateState is not mocked.');
  }

  getSourcesMatchingFilter(
    fsUrl: string,
    filter: SourceFilter,
  ): Observable<MpsServerSource[]> {
    if (filter === MockedFilters['name matches abc']) {
      return of([filesystem['/mongo/db1/abc'], filesystem['/mongo/db1/xabcy']]);
    } else {
      throw new NotImplementedError('The provided filter is not mocked.');
    }
  }
}
