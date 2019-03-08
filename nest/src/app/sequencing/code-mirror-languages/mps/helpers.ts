/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MpsCommand } from '../../../../../../schemas/types/ts';
import { StringTMap } from '../../../shared/models';

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
