/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import clone from 'lodash-es/clone';
import { Observable, Observer } from 'rxjs';
import { SequenceFile } from '../models';
import { FileServiceInterface } from './file-service-interface';

export const file0: SequenceFile = {
  childIds: [
    '717f2355-9d1e-44d2-9cae-78d913924668',
    'a1b802ae-cfd1-4ffa-bc91-aa52ff907ca6',
  ],
  content: '',
  expanded: false,
  id: '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
  name: 'Sequences',
  parentId: 'root',
  timeCreated: 1558112161536,
  timeLastUpdated: 1558114056664,
  type: 'folder',
};

export const file1: SequenceFile = {
  childIds: [],
  content: 'END 0',
  expanded: false,
  id: 'b255320a-a640-4cc9-8f4b-36d7590c98fd',
  name: 'Sequence 1',
  parentId: 'root',
  timeCreated: 1558114039168,
  timeLastUpdated: 1558114039168,
  type: 'file',
};

export const file2: SequenceFile = {
  childIds: [],
  content: 'START 0',
  expanded: false,
  id: '717f2355-9d1e-44d2-9cae-78d913924668',
  name: 'Sequence 2',
  parentId: '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
  timeCreated: 1559683148272,
  timeLastUpdated: 1559683148272,
  type: 'file',
};

export const file3: SequenceFile = {
  childIds: [],
  content: 'RETURN 0',
  expanded: false,
  id: 'a1b802ae-cfd1-4ffa-bc91-aa52ff907ca6',
  name: 'Sequence 3',
  parentId: '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
  timeCreated: 1559683661286,
  timeLastUpdated: 1559683661286,
  type: 'file',
};

export const file4: SequenceFile = {
  childIds: [],
  content: '',
  expanded: false,
  id: '8559ef8d-062f-4c21-8ebc-b80bc8271304',
  name: 'More Sequences',
  parentId: '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
  timeCreated: 1560986868095,
  timeLastUpdated: 1560986868095,
  type: 'folder',
};

export const file5: SequenceFile = {
  childIds: [],
  content: '',
  expanded: false,
  id: 'af3cf754-9ec2-47b8-bd3b-fda7b2bc096a',
  name: 'Sequence 4',
  parentId: '8559ef8d-062f-4c21-8ebc-b80bc8271304',
  timeCreated: 1560986906025,
  timeLastUpdated: 1560986906025,
  type: 'file',
};

export const files: SequenceFile[] = [
  { ...file0 },
  { ...file1 },
  { ...file2 },
  { ...file3 },
  { ...file4 },
  { ...file5 },
];

/**
 * Get a new set of cloned files.
 * MongoDB functions like insertMany mutates the input array so we use this to make
 * sure we have a fresh copy that we don't care about mutating.
 */
export function getFiles(): SequenceFile[] {
  return files.map(file => clone(file));
}

const mockFiles = getFiles();

export function getChildren(fileId: string): SequenceFile[] {
  const children = mockFiles.filter(file => file.parentId === fileId);
  return children;
}

@Injectable({
  providedIn: 'root',
})
export class FileMockService implements FileServiceInterface {
  fetchChildren(
    baseUrl: string = '',
    fileId: string,
  ): Observable<SequenceFile[]> {
    return new Observable((o: Observer<SequenceFile[]>) => {
      const children = getChildren(fileId);
      o.next(children);
      o.complete();
    });
  }
}
