/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ActivityType, Adaptation } from '../../shared/models';
import {
  SetActivityTypes,
  SetAdaptations,
} from '../actions/adaptation.actions';
import {
  getMockActivityTypes,
  getMockAdaptations,
} from '../services/adaptation-mock.service';
import { AdaptationState, initialState, reducer } from './adaptation.reducer';

describe('Adaptation Reducer', () => {
  describe('SetActivityTypes', () => {
    it('should set a list of activity types', () => {
      const activityTypes: ActivityType[] = getMockActivityTypes();
      const result: AdaptationState = reducer(
        { ...initialState },
        new SetActivityTypes(activityTypes),
      );

      expect(result).toEqual({
        ...initialState,
        activityTypes,
      });
    });
  });

  describe('SetAdaptations', () => {
    it('should set a list of adaptations', () => {
      const adaptations: Adaptation[] = getMockAdaptations();
      const result: AdaptationState = reducer(
        initialState,
        new SetAdaptations(adaptations),
      );

      expect(result).toEqual({
        ...initialState,
        adaptations,
      });
    });
  });
});
