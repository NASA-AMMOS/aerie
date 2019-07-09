/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import * as CodeMirror from 'codemirror';
import { MpsCommandParameter } from '../../../../../../schemas';
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
  const lintMessages: CodeMirrorLintError[] = [];
  const tokens = line.split(' ');
  const command = tokens[0];
  const parameters = tokens
    .slice(1, tokens.length)
    .filter(t => t !== '' && t !== ' ');

  const commandDefinition = commandsByName[command];
  let commandParameters;

  if (commandDefinition) {
    // If the command exists in the command dictionary
    const actualNumParams = parameters.length;
    const expectedNumParams = commandDefinition.parameters.length;
    commandParameters = commandDefinition.parameters;

    // Lint for correct number of parameters
    if (actualNumParams !== expectedNumParams) {
      let message = `${command} received the wrong number of parameters. Expected ${expectedNumParams} parameters, got ${actualNumParams}.`;
      message = `${message} ${generateCommandHelp(
        commandDefinition.parameters,
      )}`;

      lintMessages.push({
        end: null,
        level: 'error',
        lineNumber,
        message,
        start: null,
      });
    }

    // Check if parameters are within specified ranges
  } else {
    // If the command does not exist in the command dictionary OR it's not a comment
    if (!command.startsWith('#')) {
      lintMessages.push({
        end: null,
        level: 'error',
        lineNumber,
        message: `${command} is not in the command dictionary.`,
        start: null,
      });
    }
  }

  // Linting for whitespace at the end
  if (tokens[tokens.length - 1] === '') {
    lintMessages.push({
      end: null,
      level: 'warning',
      lineNumber,
      message: `${command} should not have any trailing whitespace.`,
      start: null,
    });
  }

  // Check if arguments are in range as defined in command dictionary
  if (commandParameters) {
    let curCharPos = command.length;

    for (let i = 0, length = parameters.length; i < length; i++) {
      const expectedParameter = commandParameters[i];
      // Add 1 to pos to account for whitespace because we split based on whitespace
      curCharPos += parameters[i].length + 1;

      if (expectedParameter) {
        // Linting for numerical types
        if (
          (expectedParameter.type === 'UNSIGNED_DECIMAL' ||
            expectedParameter.type === 'SIGNED_DECIMAL' ||
            expectedParameter.type === 'ENGINEERING' ||
            expectedParameter.type === 'INTEGER' ||
            expectedParameter.type === 'HEXADECIMAL' ||
            expectedParameter.type === 'OCTAL' ||
            expectedParameter.type === 'BINARY' ||
            expectedParameter.type === 'FLOAT' ||
            expectedParameter.type === 'DURATION' ||
            expectedParameter.type === 'TIME') &&
          expectedParameter.range
        ) {
          const [lowerBound, upperBound] = expectedParameter.range
            .split('...')
            .map(num => parseFloat(num));
          const curParameter = parseFloat(parameters[i]);
          const start = curCharPos - parameters[i].length;
          const end = curCharPos;

          // If the argument is out of range, push an error
          if (!(curParameter >= lowerBound && curParameter <= upperBound)) {
            lintMessages.push({
              end,
              level: 'error',
              lineNumber,
              message: `${command} is expecting an argument between ${lowerBound} and ${upperBound}, received ${curParameter}.
            -----
            ${expectedParameter.name}: ${expectedParameter.type} (${expectedParameter.units})
            Description:\t ${expectedParameter.help}
            Default:\t ${expectedParameter.defaultValue}
            `,
              start,
            });
          }
        }

        // Linting for booleans
        if (expectedParameter.type === 'BOOLEAN') {
          const curParameter = parameters[i];
          if (curParameter !== 'TRUE' && curParameter !== 'FALSE') {
            const start = curCharPos - parameters[i].length;
            const end = curCharPos;

            lintMessages.push({
              end,
              level: 'error',
              lineNumber,
              message: `${expectedParameter.name} is expecting TRUE or FALSE. Received ${curParameter}.\n---\n${expectedParameter.help}
              `,
              start,
            });
          }
        }

        // Linting for enums (called STRING in mps command dictionary)
        if (expectedParameter.type === 'STRING') {
          // Remove quotes around enum
          const curParameter = parameters[i].substr(
            1,
            parameters[i].length - 2,
          );
          if (expectedParameter.range) {
            const enums = expectedParameter.range
              .split(',')
              .map(e => e.substring(1, e.length - 1));

            if (!enums.includes(curParameter)) {
              const start = curCharPos - parameters[i].length;
              const end = curCharPos;

              lintMessages.push({
                end,
                level: 'error',
                lineNumber,
                message: `${expectedParameter.name} is expecting enum: ${enums}. Received ${curParameter}`,
                start,
              });
            }
          }
        }
      }
    }
  }
  return lintMessages;
}

function generateCommandHelp(parameters: MpsCommandParameter[]) {
  return `
      -----
      ${parameters
        .map(p => {
          return `${p.name}: ${p.type} (${p.units})
          Description:\t ${p.help}
          Default:\t ${p.defaultValue}
          Range:\t ${p.range}`;
        })
        .join('\n\n')}
      `;
}
