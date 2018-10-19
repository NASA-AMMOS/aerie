/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenActivityType } from '../../shared/models/raven-activity-type';
import {
  FetchActivityTypeListSuccess,
  RemoveActivityType,
  SaveActivityTypeSuccess,
} from '../actions/activity-type.actions';
import {
  ActivityTypeState,
  initialState,
  reducer,
} from './activity-type.reducer';

function getInitialState() {
  return { ...initialState };
}

describe('ActivityType Reducer', () => {
  let activity: RavenActivityType;
  let updated: RavenActivityType;

  beforeEach(() => {
    activity = {
      id: 'foo',
      name: 'Foo',
      start: '',
    };
    updated = {
      id: 'foo',
      name: 'FooBar',
      start: '',
    };
  });

  describe('FetchActivityTypeListSuccess', () => {
    it('should return a list of activity types', () => {
      const expected = [activity];
      const result: ActivityTypeState = reducer(
        getInitialState(),
        new FetchActivityTypeListSuccess(expected),
      );

      expect(result).toEqual({
        activityTypes: expected,
      });
    });
  });

  describe('SaveActivityTypeSuccess', () => {
    it('should append an activity type when a new activity type is passed', () => {
      const result: ActivityTypeState = reducer(
        getInitialState(),
        new SaveActivityTypeSuccess(activity, true),
      );

      expect(result).toEqual({
        activityTypes: [activity],
      });
    });

    it('should update an activity type when an existing activity type is passed', () => {
      const result: ActivityTypeState = reducer(
        {
          ...initialState,
          activityTypes: [activity],
        },
        new SaveActivityTypeSuccess(updated, false),
      );

      expect(result).toEqual({
        ...initialState,
        activityTypes: [updated],
      });
    });
  });

  describe('RemoveActivityType', () => {
    it('should remove an activity with the provided ID', () => {
      const result: ActivityTypeState = reducer(
        {
          ...initialState,
          activityTypes: [activity],
        },
        new RemoveActivityType(activity.id),
      );

      expect(result).toEqual({
        ...initialState,
      });
    });
  });
});
