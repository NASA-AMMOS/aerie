/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as CodeMirror from 'codemirror';
import { MpsCommand } from '../../../../../../schemas/types/ts';
import { StringTMap } from '../../../shared/models';
import { CodeMirrorLintError } from '../../models';

/**
 * Register a custom `mps` linter with Code Mirror.
 */
export function buildMpsLint(commandsByName: StringTMap<MpsCommand>) {
  CodeMirror.registerHelper('lint', 'mps', (text: string) => {
    const found: CodeMirror.Annotation[] = [];

    const parseError = (err: CodeMirrorLintError) => {
      const loc = err.lineNumber;

      found.push({
        from: CodeMirror.Pos(loc - 1, 0),
        message: err.message,
        severity: err.level,
        to: CodeMirror.Pos(loc, 0),
      });
    };

    try {
      const errors = verify(commandsByName, text);

      for (let i = 0, l = errors.length; i < l; ++i) {
        parseError(errors[i]);
      }
    } catch (e) {
      found.push({
        from: CodeMirror.Pos(e.location.first_line, 0),
        message: e.message,
        severity: 'error',
        to: CodeMirror.Pos(e.location.last_line, e.location.last_column),
      });
    }

    return found;
  });
}

/**
 * Get a param regex for a range of string values.
 */
export function regexFromRangeString(range: string[]): RegExp {
  let regex = '';

  range.forEach((str: string, i: number) => {
    if (i !== range.length - 1) {
      regex += `(^${str}$)|`;
    } else {
      regex += `(^${str}$)`;
    }
  });

  return new RegExp(regex);
}

/**
 * Verify a blob of text line-by-line based on a command dictionary.
 */
export function verify(
  commandsByName: StringTMap<MpsCommand>,
  text: string,
): CodeMirrorLintError[] {
  let errors: CodeMirrorLintError[] = [];

  text.split(/\r?\n/).forEach((line, i) => {
    if (line !== '') {
      const lineErrors = verifyLine(commandsByName, line, i + 1);
      errors = errors.concat(lineErrors);
    }
  });

  return errors;
}

/**
 * Verify an individual line based on a command dictionary.
 */
export function verifyLine(
  commandsByName: StringTMap<MpsCommand>,
  line: string,
  lineNumber: number,
) {
  const res: CodeMirrorLintError[] = [];
  const tokens = line.split(' ');
  const name = tokens[0];
  const parameters = tokens
    .slice(1, tokens.length)
    .filter(t => t !== '' && t !== ' ');

  if (commandsByName.hasOwnProperty(name)) {
    if (parameters.length < commandsByName[name].parameters.length) {
      res.push({
        level: 'error',
        lineNumber,
        message: 'Command does not have enough parameters.',
      });
    } else if (parameters.length > commandsByName[name].parameters.length) {
      res.push({
        level: 'error',
        lineNumber,
        message: 'Command has too many parameters.',
      });
    } else {
      // TODO: More detailed command linting.
    }
  } else {
    res.push({
      level: 'error',
      lineNumber,
      message: 'Command is not in the command dictionary.',
    });
  }

  if (tokens[tokens.length - 1] === '') {
    res.push({
      level: 'warning',
      lineNumber,
      message: 'Command should not have any trailing whitespace.',
    });
  }

  return res;
}
