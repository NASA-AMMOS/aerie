/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as CodeMirror from 'codemirror';
import { MpsCommand } from '../../../../../../schemas';
import { StringTMap } from '../../../shared/models';

/**
 * Register a custom `mps` mode with Code Mirror.
 */
export function buildMpsMode(commandsByName: StringTMap<MpsCommand>): void {
  const mode = 'mps';
  const commandNames = Object.keys(commandsByName);
  const instructionRegex = `(${commandNames.join('|')})`;
  const instructionOnlyLine = new RegExp(`${instructionRegex}\\s*$`, 'i');
  const instructionWithArguments = new RegExp(`${instructionRegex}(\\s+)`, 'i');

  //@ts-ignore
  CodeMirror.defineSimpleMode(mode, {
    arguments: [
      {
        next: 'start',
        regex: /$/,
        token: null,
      },
      {
        next: 'start',
        token: null,
      },
    ],
    meta: {},
    start: [
      // Highlight an instruction without any arguments (for convenience).
      {
        regex: instructionOnlyLine,
        token: 'variable-2',
      },
      // Highlight an instruction followed by arguments.
      {
        next: 'arguments',
        regex: instructionWithArguments,
        token: ['variable-2', null],
      },
      // Strings.
      {
        regex: /"(?:[^\\]|\\.)*?(?:"|$)/,
        token: 'string',
      },
      // Variables.
      {
        regex: /[A-Z|a-z$][\w$]*/,
        token: 'variable',
      },
      // Numbers.
      {
        regex: /0x[a-f\d]+|[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
        token: 'number',
      },
    ],
  });

  //@ts-ignore
  CodeMirror.defineMIME(`text/x-${mode}`, mode);
}
