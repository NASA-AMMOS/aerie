/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HbCommand } from '../models/hb-command';
import { HbCommandDictionary } from '../models/hb-command-dictionary';

/**
 * Mock data for command dictionary list testing
 */
export const commandDictionaryList: HbCommandDictionary[] = [
  {
    id: 'TEST_1',
    name: 'Test 1',
    selected: false,
    version: '1.0.0',
  },
  {
    id: 'TEST_2',
    name: 'Test 2',
    selected: false,
    version: '2.0.0',
  },
];

export function getCommandList(
  seed: number = 1,
  prefix: string = 'TEST',
): HbCommand[] {
  const list = [];

  // MSL has a list of almost 4000 commands.
  for (let i = 0, l = 4000; i < l; ++i) {
    list.push({
      definitionMaturity: 'UNLOCKED',
      description: 'A test command',
      implementationMaturity: 'FUNCTIONAL',
      name: `${prefix}_${seed}${i}`,
      opcode: `${seed}${i}`,
      operationalCategory: 'TEST',
      parameterDefs: [
        {
          default_: '"TRUE_IS_TRUE"',
          description: 'True is true',
          mode: null,
          name: 'condition',
          range: '"TEST_IN_USE","TEST_ERROR","TEST_SUCCESS"',
          type: {
            simple: {
              arraySize: null,
              baseType: 'STRING',
            },
            varArray: null,
          },
          units: null,
        },
      ],
      processorString: null,
      restrictedPhases: ['PRE-LAUNCH', 'LAUNCH'],
    });
  }
  return list;
}
