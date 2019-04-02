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
import { StringTMap } from '../../shared/models';
import { MpsServerSource, RavenSource, SourceFilter } from '../models';

// NOTE: We use `as unknown as MpsServerSource` here because, despite these being
// legitimate, exact copies of the output from MPS Server, they do not match the
// expected TypeScript interface for `MpsServerSource`.

const filesystem: StringTMap<MpsServerSource> = {
  '/mongo/db1': ({
    __db_type: 'mongodb',
    __kind: 'fs_dir',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1',
    created: '2019-01-08 12:13:24-0800',
    label: 'db1',
    modified: '2019-01-08 12:13:24-0800',
    name: 'db1',
    permissions: 'rw-r--r-- null all.usjpl',
  } as unknown) as MpsServerSource,
  '/mongo/db1/abc': ({
    __db_type: 'mongodb',
    __kind: 'fs_file',
    __kind_sub: 'file_pef',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/list_pef-mongodb/mongo/db1/abc',
    created: '2018-08-06 11:05:44-0700',
    customMeta: [],
    file_data_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1/abc',
    label: 'abc',
    modified: '2018-08-06 11:05:44-0700',
    name: 'abc',
    permissions: 'rw-r--r-- null all.usjpl',
  } as unknown) as MpsServerSource,
  '/mongo/db1/xabcy': ({
    __db_type: 'mongodb',
    __kind: 'fs_file',
    __kind_sub: 'file_pef',
    contents_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/list_pef-mongodb/mongo/db1/xabcy',
    created: '2018-08-06 11:05:44-0700',
    customMeta: [],
    file_data_url:
      'https://leucadia.jpl.nasa.gov:9443/mpsserver/api/v2/fs-mongodb/mongo/db1/xabcy',
    label: 'xabcy',
    modified: '2018-08-06 11:05:44-0700',
    name: 'xabcy',
    permissions: 'rw-r--r-- null all.usjpl',
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
