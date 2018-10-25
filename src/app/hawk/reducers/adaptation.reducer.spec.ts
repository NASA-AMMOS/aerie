/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenAdaptation } from '../../shared/models/raven-adaptation';
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';
import { FetchAdaptationListSuccess } from '../actions/adaptation.actions';
import { AdaptationState, initialState, reducer } from './adaptation.reducer';

describe('Adaptation Reducer', () => {
  describe('FetchAdaptationListSuccess', () => {
    it('should return a list of activity types', () => {
      const adaptations: RavenAdaptation[] = AdaptationMockService.getMockData();
      const result: AdaptationState = reducer(
        { ...initialState },
        new FetchAdaptationListSuccess(adaptations),
      );

      expect(result).toEqual({
        adaptations,
        selectedAdaptationId: null,
      });
    });
  });
});
