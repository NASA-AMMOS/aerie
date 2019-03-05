/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Command, CommandParameter } from '../../../../../schemas';
import { StringTMap } from '../../shared/models';

export interface CommandLintError {
  level: string;
  lineNumber: number;
  message: string;
}

/**
 * Helper that takes a list of commands and returns a map of commands keyed by command name.
 * This function also adds some extra properties to the command that help with
 * editor hinting (autocomplete with templates) and linting (syntax correctness).
 */
export function keyCommandsByName(commands: Command[]): StringTMap<Command> {
  const newCommands: StringTMap<Command> = {};

  for (let i = 0, l = commands.length; i < l; ++i) {
    let template = `${commands[i].name}`;

    const command = {
      ...commands[i],
      parameters: commands[i].parameters.map(param => {
        const type = param.type.toLowerCase();
        let range = [];
        template += ` ${param.defaultValue}`;

        switch (type) {
          case 'boolean':
            return {
              ...param,
              regex: '(^TRUE$)|(^FALSE$)|(^true$)|(^false$)',
            };
          case 'string':
            range = param.range.split(',');
            return {
              ...param,
              regex: regexFromRangeString(range),
            };
          case 'engineering':
          case 'signed_decimal':
          case 'unsigned_decimal':
            range = param.range.split('...');
            return {
              ...param,
              max: parseFloat(range[1]),
              min: parseFloat(range[0]),
            };
          default:
            return {
              ...param,
            };
        }
      }),
    };

    newCommands[command.name] = {
      ...command,
      template,
    };
  }

  return newCommands;
}

/**
 * Get a param regex for a range of string values.
 */
export function regexFromRangeString(range: string[]): string {
  let regex = '';

  range.forEach((str: string, i: number) => {
    if (i !== range.length - 1) {
      regex += `(^${str}$)|`;
    } else {
      regex += `(^${str}$)`;
    }
  });

  return regex;
}

/**
 * Verify a blob of text line-by-line based on a command dictionary.
 */
export function verify(
  commandsByName: StringTMap<Command>,
  text: string,
): CommandLintError[] {
  let errors: CommandLintError[] = [];

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
  commandsByName: StringTMap<Command>,
  line: string,
  lineNumber: number,
) {
  const res: CommandLintError[] = [];
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
      commandsByName[name].parameters.forEach(
        (param: CommandParameter, i: number) => {
          const inputParam = parameters[i];
          let value = 0.0;

          switch (param.type) {
            case 'string':
            case 'boolean':
              if (param.regex && !new RegExp(param.regex).exec(inputParam)) {
                res.push({
                  level: 'error',
                  lineNumber,
                  message: `Parameter ${i + 1} "${
                    param.name
                  }" has an incorrect value.`,
                });
              }
              break;
            case 'engineering':
            case 'signed_decimal':
            case 'unsigned_decimal':
              value = parseFloat(inputParam);

              if (Number.isNaN(value)) {
                res.push({
                  level: 'error',
                  lineNumber,
                  message: `Parameter ${i + 1} "${
                    param.name
                  }" is not a number.`,
                });
              } else if (
                (param.min && value < param.min) ||
                (param.max && value > param.max)
              ) {
                res.push({
                  level: 'error',
                  lineNumber,
                  message: `Parameter ${i + 1} "${
                    param.name
                  }" is out of range [${param.min}, ${param.max}].`,
                });
              }
              break;
            default:
              break;
          }
        },
      );
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
