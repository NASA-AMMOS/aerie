/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as CodeMirror from 'codemirror';
import { MpsCommand, StringTMap } from '../../../shared/models';
import { CodeMirrorLintError } from '../../models';

/**
 * Register a custom `mps` linter with Code Mirror.
 */
export function buildMpsLint(commandsByName: StringTMap<MpsCommand>) {
  CodeMirror.registerHelper('lint', 'mps', (text: string) => {
    const found: CodeMirror.Annotation[] = [];

    const parseError = (err: CodeMirrorLintError) => {
      const loc = err.lineNumber;
      const markStart = err.start || 0;
      const markEnd = err.end || 0;
      const lineEnd = err.end ? loc - 1 : loc;

      found.push({
        from: CodeMirror.Pos(loc - 1, markStart),
        message: err.message,
        severity: err.level,
        to: CodeMirror.Pos(lineEnd, markEnd),
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

  const validCommand = commandsByName.hasOwnProperty(name);
  let commandParameters;

  if (validCommand) {
    const actualNumParams = parameters.length;
    const expectedNumParams = commandsByName[name].parameters.length;
    commandParameters = commandsByName[name].parameters;

    let message = '';
    let hasArgError = false;

    if (actualNumParams !== expectedNumParams) {
      hasArgError = true;
      message = `${name} received the wrong number of parameters. Expected ${expectedNumParams} parameters, got ${actualNumParams}.`;
    }

    if (hasArgError) {
      message = `${message}
      -----
      ${commandsByName[name].parameters
        .map(p => {
          return `${p.name}: ${p.type} (${p.units})
          Description:\t ${p.help}
          Default:\t ${p.defaultValue}
          Range:\t ${p.range}`;
        })
        .join('\n\n')}
      `;

      res.push({
        end: null,
        level: 'error',
        lineNumber,
        message,
        start: null,
      });
    }
  } else {
    res.push({
      end: null,
      level: 'error',
      lineNumber,
      message: `${name} is not in the command dictionary.`,
      start: null,
    });
  }

  if (tokens[tokens.length - 1] === '') {
    res.push({
      end: null,
      level: 'warning',
      lineNumber,
      message: `${name} should not have any trailing whitespace.`,
      start: null,
    });
  }

  // Check if arguments are in range as defined in command dictionary
  if (validCommand && commandParameters) {
    let curCharPos = name.length;

    for (let i = 0, length = parameters.length; i < length; i++) {
      const curCommand = commandParameters[i];
      // Add 1 to pos to account for whitespace because we split based on whitespace
      curCharPos += parameters[i].length + 1;

      if (
        curCommand &&
        (curCommand.type === 'UNSIGNED_DECIMAL' ||
          curCommand.type === 'SIGNED_DECIMAL' ||
          curCommand.type === 'ENGINEERING') &&
        curCommand.range
      ) {
        const [lowerBound, upperBound] = curCommand.range
          .split('...')
          .map(num => parseFloat(num));
        const curArgument = parseFloat(parameters[i]);

        // If the argument is out of range, push an error
        if (!(curArgument >= lowerBound && curArgument <= upperBound)) {
          const start = curCharPos - parameters[i].length;
          const end = curCharPos;

          res.push({
            end,
            level: 'error',
            lineNumber,
            message: `${name} is expecting an argument between ${lowerBound} and ${upperBound}, received ${curArgument}.
            -----
            ${curCommand.name}: ${curCommand.type} (${curCommand.units})
            Description:\t ${curCommand.help}
            Default:\t ${curCommand.defaultValue}
            `,
            start,
          });
        }
      }
    }
  }

  return res;
}
