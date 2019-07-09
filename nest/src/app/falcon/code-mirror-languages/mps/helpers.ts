/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  MpsCommand,
  MpsCommandParameter,
  StringTMap,
} from '../../../shared/models';

/**
 * Returns a template for an mps command used in autocomplete.
 */
export function getCommandTemplate(
  commandName: string,
  commandsByName: StringTMap<MpsCommand>,
): string {
  const command = commandsByName[commandName];
  let template = '';

  if (command) {
    template = `${command.name}`;
    for (let i = 0, l = command.parameters.length; i < l; ++i) {
      const parameter = command.parameters[i];
      template += ` ${parameter.defaultValue}`;
    }
  }

  return template;
}

/**
 * Returns a template of the parameters for a mps command
 */
export function getCommandParameterHelpTemplate(
  commandName: string,
  commandsByName: StringTMap<MpsCommand>,
): string {
  const command = commandsByName[commandName];

  if (command) {
    return `
      <h3>${commandName}</h3>
      ${command.parameters.length > 0 ? '<h4>Parameters</h4>' : 'No Parameters'}
      ${command.parameters
        .map(p => {
          return `<p class="parameter-help"><strong class="parameter-name">${p.name}</strong>: ${p.help}</p>`;
        })
        .join(' ')}
    `;
  }

  return '';
}

/**
 * Returns the tooltip template for hovering over a parameter
 * @param commandName The command we are looking up a parameter for
 * @param parameterName The parameter we are looking up
 * @param lineTokens The CodeMirror line that the user is hovering over
 * @param commandsByName The dictionary of commands
 */
export function getCommandParameterDescriptionTemplate(
  commandName: string,
  parameterName: string,
  lineTokens: CodeMirror.Token[],
  commandsByName: StringTMap<MpsCommand>,
) {
  const commandObject = commandsByName[commandName];

  // Remove the whitespace tokens
  const filteredLineTokens = lineTokens.filter(t => t.string !== ' ');

  // Find the parameter object that matches the one we're looking up
  const parameterIndex = filteredLineTokens.findIndex(
    t => t.string === parameterName,
  );

  if (parameterIndex !== -1) {
    const targetParameter = commandObject.parameters[parameterIndex - 1];

    return getParameterTooltip(targetParameter);
  }
  return '';
}

/**
 * Generates the tooltip content for a provided parameter
 */
export function getParameterTooltip(parameterObject: MpsCommandParameter) {
  if (parameterObject) {
    return `
    <h3>${parameterObject.name}</h3>
    <p>${parameterObject.help}</p>
    <p>Range: ${parameterObject.range}</p>
    <p>Type: ${parameterObject.type}</p>
    <p>Units: ${parameterObject.units}</p>
    <p>Default Value: ${parameterObject.defaultValue}</p>
  `;
  }
  return ``;
}
