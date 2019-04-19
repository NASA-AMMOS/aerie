/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy, omit } from 'lodash';
import { ActivityInstance, Plan, StringTMap } from '../../shared/models';
import {
  ClearSelectedActivity,
  ClearSelectedPlan,
  CreateActivitySuccess,
  CreatePlanSuccess,
  DeleteActivitySuccess,
  DeletePlanSuccess,
  SelectActivity,
  SetActivities,
  SetActivitiesAndSelectedActivity,
  SetPlans,
  SetPlansAndSelectedPlan,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';
import { initialState, reducer } from './plan.reducer';

describe('Plan Reducer', () => {
  let activities: ActivityInstance[];
  let activitiesMap: StringTMap<ActivityInstance>;
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
    it('should clear the selected activity id', () => {
      const newInitialState = {
        ...initialState,
        selectedActivityId: '1',
      };

      const result = reducer(newInitialState, new ClearSelectedActivity());

      expect(result).toEqual({
        ...newInitialState,
        selectedActivityId: null,
      });
    });
  });

  describe('ClearSelectedPlan', () => {
    it('should clear the selected plan id', () => {
      const newInitialState = {
        ...initialState,
        selectedPlanId: plan.id,
      };

      const result = reducer(newInitialState, new ClearSelectedPlan());

      expect(result).toEqual({
        ...newInitialState,
        selectedPlanId: null,
      });
    });
  });

  describe('CreateActivitySuccess', () => {
    it('should add a new activity with the correct time ranges', () => {
      const activityId = '1';
      const newState = reducer(
        initialState,
        new CreateActivitySuccess(plan.id, activitiesMap[activityId]),
      );
      expect(newState).toEqual({
        ...initialState,
        activities: {
          [activityId]: activitiesMap[activityId],
        },
        maxTimeRange: { end: 0, start: 0 },
        viewTimeRange: { end: 0, start: 0 },
      });
    });
  });

  describe('CreatePlanSuccess', () => {
    it('should add a new plan to the plans', () => {
      const newState = reducer(initialState, new CreatePlanSuccess(plan));
      expect(newState).toEqual({
        ...initialState,
        plans: plansMap,
      });
    });
  });

  describe('DeleteActivitySuccess', () => {
    it('should delete an activity from activities', () => {
      const deleteActivityId = '1';
      const newInitialState = { ...initialState, activities: activitiesMap };
      const newState = reducer(
        newInitialState,
        new DeleteActivitySuccess(deleteActivityId),
      );
      expect(newState).toEqual({
        ...initialState,
        activities: omit(activitiesMap, deleteActivityId),
      });
    });
  });

  describe('DeletePlanSuccess', () => {
    it('should delete a plan from plans', () => {
      const deletePlanId = '1';
      const newInitialState = { ...initialState, plans: plansMap };
      const newState = reducer(
        newInitialState,
        new DeletePlanSuccess(deletePlanId),
      );
      expect(newState).toEqual({
        ...initialState,
        plans: omit(plansMap, deletePlanId),
      });
    });
  });

  describe('SelectActivity', () => {
    it('should properly set the selected activity id', () => {
      const selectedActivityId = 'foo';
      const newState = reducer(
        initialState,
        new SelectActivity(selectedActivityId),
      );
      expect(newState).toEqual({
        ...initialState,
        selectedActivityId,
      });
    });
  });

  describe('SetActivities', () => {
    it('should properly set activities and time ranges', () => {
      const newState = reducer(initialState, new SetActivities(activities));
      expect(newState).toEqual({
        ...initialState,
        activities: activitiesMap,
        maxTimeRange: { end: 0, start: 0 },
        viewTimeRange: { end: 0, start: 0 },
      });
    });
  });

  describe('SetActivitiesAndSelectedActivity', () => {
    it('should properly set activities, time ranges, and a selected activity id', () => {
      const selectedActivityId = '1';
      const newState = reducer(
        initialState,
        new SetActivitiesAndSelectedActivity(activities, selectedActivityId),
      );
      expect(newState).toEqual({
        ...initialState,
        activities: activitiesMap,
        maxTimeRange: { end: 0, start: 0 },
        selectedActivityId,
        viewTimeRange: { end: 0, start: 0 },
      });
    });
  });

  describe('SetPlans', () => {
    it('should properly set plans', () => {
      const newState = reducer(initialState, new SetPlans(plans));
      expect(newState).toEqual({
        ...initialState,
        plans: plansMap,
      });
    });
  });

  describe('SetPlansAndSelectedPlan', () => {
    it('should properly set plans and a selected plan id', () => {
      const selectedPlanId = 'foo';
      const newState = reducer(
        initialState,
        new SetPlansAndSelectedPlan(plans, selectedPlanId),
      );
      expect(newState).toEqual({
        ...initialState,
        plans: plansMap,
        selectedPlanId,
      });
    });
  });

  describe('UpdateActivitySuccess', () => {
    it('update an activity by id with the given update object', () => {
      const newInitialState = reducer(
        initialState,
        new SetActivities(activities),
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
