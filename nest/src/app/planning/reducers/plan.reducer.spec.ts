/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy } from 'lodash';
import { Activity, Plan } from '../../../../libs/schemas/types/ts';
import { StringTMap } from '../../shared/models';
import {
  ClearSelectedActivity,
  ClearSelectedPlan,
  FetchActivitiesSuccess,
  FetchPlansSuccess,
  SelectActivity,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';
import { initialState, reducer } from './plan.reducer';

describe('Plan Reducer', () => {
  let activities: Activity[];
  let activitiesMap: StringTMap<Activity>;
  let plan: Plan;
  let plans: Plan[];
  let plansMap: StringTMap<Plan>;

  beforeEach(() => {
    activities = [
      {
        activityId: '1',
        activityType: '000',
        color: '#7cbfb7',
        constraints: [],
        duration: 0,
        end: 0,
        endTimestamp: '',
        intent: 'Some science intent for this activity...',
        name: 'Instrument 1, Activity ABC',
        parameters: [],
        start: 0,
        startTimestamp: '2022-10-29T14:55:00',
        y: null,
      },
      {
        activityId: '2',
        activityType: '001',
        color: '#7cbfb7',
        constraints: [],
        duration: 0,
        end: 0,
        endTimestamp: '',
        intent: 'Some science intent for this activity...',
        name: 'Instrument 2, Activity ABC',
        parameters: [],
        start: 0,
        startTimestamp: '2023-11-28T15:54:10',
        y: null,
      },
    ];

    activitiesMap = keyBy(activities, 'activityId');

    plan = {
      adaptationId: 'ops',
      endTimestamp: '1995-12-17T03:28:00',
      id: 'foo',
      name: 'Foo',
      startTimestamp: '1995-12-17T03:24:00',
    };

    plans = [plan];

    plansMap = {
      foo: plan,
    };
  });

  describe('ClearSelectedActivity', () => {
    it('should clear the selected activity', () => {
      const newInitialState = {
        ...initialState,
        selectedActivity: activities['1'],
      };

      const result = reducer(newInitialState, new ClearSelectedActivity());

      expect(result).toEqual({
        ...newInitialState,
        selectedActivity: null,
      });
    });
  });

  describe('ClearSelectedPlan', () => {
    it('should clear the selected plan', () => {
      const newInitialState = {
        ...initialState,
        selectedPlan: plan,
      };

      const result = reducer(newInitialState, new ClearSelectedPlan());

      expect(result).toEqual({
        ...newInitialState,
        selectedPlan: null,
      });
    });
  });

  describe('FetchActivitiesSuccess', () => {
    it('should set an activities map upon fetch activities success', () => {
      const result = reducer(
        initialState,
        new FetchActivitiesSuccess(plan.id, null, activities),
      );

      expect(result).toEqual({
        ...initialState,
        activities: activitiesMap,
      });
    });
  });

  describe('FetchPlansSuccess', () => {
    it('should set the list of plans as a map upon fetch plan list success', () => {
      const result = reducer(initialState, new FetchPlansSuccess(plans));

      expect(result).toEqual({
        ...initialState,
        plans: plansMap,
      });
    });
  });

  describe('SelectActivity', () => {
    it('should set the selectedActivity', () => {
      const newInitialState = reducer(
        initialState,
        new FetchActivitiesSuccess(plan.id, null, activities),
      );

      const result = reducer(newInitialState, new SelectActivity('1'));

      expect(result).toEqual({
        ...newInitialState,
        selectedActivity: activitiesMap['1'],
      });
    });
  });

  describe('UpdateActivitySuccess', () => {
    it('update an activity by id with the given update object', () => {
      const newInitialState = reducer(
        initialState,
        new FetchActivitiesSuccess(plan.id, null, activities),
      );

      const activityId = '1';
      const update = { color: '#000000' };
      const result = reducer(
        newInitialState,
        new UpdateActivitySuccess(activityId, update),
      );

      expect(result).toEqual({
        ...newInitialState,
        activities: {
          ...newInitialState.activities,
          [activityId]: {
            ...activitiesMap[activityId],
            ...update,
          },
        },
      });
    });
  });

  describe('UpdateViewTimeRange', () => {
    it('should update the viewTimeRange', () => {
      const result = reducer(
        initialState,
        new UpdateViewTimeRange({ end: 314, start: 272 }),
      );

      expect(result).toEqual({
        ...initialState,
        viewTimeRange: { end: 314, start: 272 },
      });
    });
  });
});
