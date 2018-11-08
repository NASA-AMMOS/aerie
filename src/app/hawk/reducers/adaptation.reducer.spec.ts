/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenActivityType } from '../../shared/models/raven-activity-type';
import { RavenAdaptation } from '../../shared/models/raven-adaptation';
import { RavenAdaptationDetail } from '../../shared/models/raven-adaptation-detail';
import { AdaptationMockService } from '../../shared/services/adaptation-mock.service';

import {
  FetchAdaptationListSuccess,
  FetchAdaptationSuccess,
  RemoveActivityType,
  SaveActivityTypeSuccess,
} from '../actions/adaptation.actions';

import { AdaptationState, initialState, reducer } from './adaptation.reducer';

describe('Adaptation Reducer', () => {
  describe('FetchAdaptationSuccess', () => {
    it('should properly set the selected adaptation', () => {
      const adaptationDetail: RavenAdaptationDetail = AdaptationMockService.getMockAdaptation(
        'test1',
      ) as RavenAdaptationDetail;
      const result: AdaptationState = reducer(
        { ...initialState },
        new FetchAdaptationSuccess(adaptationDetail),
      );

      expect(result).toEqual({
        ...initialState,
        selectedAdaptation: adaptationDetail,
      });
    });
  });

  describe('FetchAdaptationListSuccess', () => {
    it('should return a list of activity types', () => {
      const adaptations: RavenAdaptation[] = AdaptationMockService.getMockData();
      const result: AdaptationState = reducer(
        { ...initialState },
        new FetchAdaptationListSuccess(adaptations),
      );

      expect(result).toEqual({
        adaptations,
        selectedAdaptation: null,
      });
    });
  });

  describe('activityTypes', () => {
    let activity: RavenActivityType;
    let updated: RavenActivityType;
    let selectedState: AdaptationState;
    let selectedAdaptation: RavenAdaptationDetail;

    beforeEach(() => {
      selectedState = {
        adaptations: AdaptationMockService.getMockData(),
        selectedAdaptation: {
          activityTypes: {},
          id: 'ops',
          name: 'Ops',
          version: '1.0.0',
        },
      };

      selectedAdaptation = selectedState.selectedAdaptation as RavenAdaptationDetail;

      activity = {
        description: '*le foo',
        id: 'foo',
        name: 'Foo',
        start: '',
      };

      updated = {
        description: '*le foo',
        id: 'foo',
        name: 'FooBar',
        start: '',
      };
    });

    describe('SaveActivityTypeSuccess', () => {
      it('should append an activity type when a new activity type is passed', () => {
        const result: AdaptationState = reducer(
          selectedState,
          new SaveActivityTypeSuccess(activity, true),
        );

        const expected: AdaptationState = {
          ...selectedState,
          selectedAdaptation: {
            ...selectedAdaptation,
            activityTypes: {
              [activity.id]: { ...activity },
            },
          },
        };

        expect(result).toEqual(expected);
      });

      it('should update an activity type when an existing activity type is passed', () => {
        const result: AdaptationState = reducer(
          {
            ...selectedState,
            selectedAdaptation: {
              ...selectedAdaptation,
              activityTypes: {
                [activity.id]: { ...activity },
              },
            },
          },
          new SaveActivityTypeSuccess(updated, false),
        );

        const expected: AdaptationState = {
          ...selectedState,
          selectedAdaptation: {
            ...selectedAdaptation,
            activityTypes: {
              [updated.id]: { ...updated },
            },
          },
        };

        expect(result).toEqual(expected);
      });
    });

    describe('RemoveActivityType', () => {
      it('should remove an activity with the provided ID', () => {
        const result: AdaptationState = reducer(
          {
            ...selectedState,
            selectedAdaptation: {
              ...selectedAdaptation,
              activityTypes: {
                [activity.id]: { ...activity },
              },
            },
          },
          new RemoveActivityType(activity.id),
        );

        const expected: AdaptationState = {
          ...selectedState,
          selectedAdaptation: {
            ...selectedAdaptation,
            activityTypes: {},
          },
        };

        expect(result).toEqual(expected);
      });
    });
  });
});
