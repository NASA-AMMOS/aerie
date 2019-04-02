/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { MpsServerSource } from '../models';
import { sanitizeSourceName } from './source';

export function getMpsPathForSource(source: MpsServerSource): string {
  const PATH_REGEX = /\/mpsserver\/api\/v2\/fs-[^\/]*(\/.+)$/;

  // Use the file_data_url if it exists; otherwise use the contents_url.
  // MPS Server provides one or both depending on what kind of source this is,
  // and in almost all cases we prefer `file_data_url` for deriving a path.
  const url = source['file_data_url'] || source['contents_url'];

  const path = url.match(PATH_REGEX)[1];

  return path
    .split('/')
    .map(sanitizeSourceName)
    .join('/');
}
