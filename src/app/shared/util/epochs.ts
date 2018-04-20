/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  MpsServerEpoch,
  RavenEpoch,
} from './../models';

/**
 * Transform an array of MPS Server epochs to Raven epochs.
 */
export function toRavenEpochs(serverEpochs: MpsServerEpoch[]) {
  const epochs: RavenEpoch[] = [];

  for (let i = 0, l = serverEpochs.length; i < l; ++i) {
    const serverEpoch: MpsServerEpoch = serverEpochs[i];

    epochs.push({
      name: serverEpoch.name,
      value: serverEpoch.value,
    });
  }

  return epochs;
}
