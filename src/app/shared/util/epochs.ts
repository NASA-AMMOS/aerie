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
