import {
  MpsServerEpoch,
  RavenEpoch,
} from './../models';

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
