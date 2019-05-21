/**
 * Copyright 2019, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { clone } from 'lodash';
import { SequenceFile } from '../src/models';

export const root: SequenceFile = {
  childIds: [
    '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
    'b255320a-a640-4cc9-8f4b-36d7590c98fd',
  ],
  content: '',
  id: 'root',
  name: 'root',
  timeCreated: 0,
  timeLastUpdated: 0,
  type: 'directory',
};

export const file0: SequenceFile = {
  childIds: [],
  content: 'START 0',
  id: '13c2c7ac-9e8c-43d5-8b4d-095e3747feb2',
  name: 'Sequence 0',
  timeCreated: 1558112161536,
  timeLastUpdated: 1558114056664,
  type: 'file',
};

export const file1: SequenceFile = {
  childIds: [],
  content: 'END 0',
  id: 'b255320a-a640-4cc9-8f4b-36d7590c98fd',
  name: 'Sequence 1',
  timeCreated: 1558114039168,
  timeLastUpdated: 1558114039168,
  type: 'file',
};

export const files: SequenceFile[] = [{ ...root }, { ...file0 }, { ...file1 }];

/**
 * Get a new set of cloned files.
 * MongoDB functions like insertMany mutates the input array so we use this to make
 * sure we have a fresh copy that we don't care about mutating.
 */
export function getFiles(): SequenceFile[] {
  return files.map(file => clone(file));
}
