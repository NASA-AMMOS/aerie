import {
  toRavenEpochs,
} from './epochs';

import {
  mpsServerEpochs,
  ravenEpochs,
} from './../mocks';

describe('epochs.ts', () => {
  describe('toRavenEpochs', () => {
    it(`convert mpsserver epochs to raven epochs`, () => {
      expect(toRavenEpochs(mpsServerEpochs)).toEqual(ravenEpochs);
    });
  });
});
