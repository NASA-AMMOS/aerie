/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, reducer } from './plan.reducer';

import {
  RavenActivity,
  RavenPlan,
  RavenPlanDetail,
  StringTMap,
} from '../../shared/models';

import {
  ClearSelectedActivity,
  ClearSelectedPlan,
  FetchPlanDetailSuccess,
  FetchPlanListSuccess,
  SelectActivity,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';

describe('Plan Reducer', () => {
  let activityInstances: StringTMap<RavenActivity>;
  let plan: StringTMap<RavenPlan>;
  let planDetail: RavenPlanDetail;

  beforeEach(() => {
    activityInstances = {
      '1': {
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
        subActivityIds: [],
        y: null,
      },
      '2': {
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
        subActivityIds: [],
        y: null,
      },
    };

    plan = {
      foo: {
        adaptationId: 'ops',
        endTimestamp: '1995-12-17T03:28:00',
        id: 'foo',
        name: 'Foo',
        startTimestamp: '1995-12-17T03:24:00',
      },
    } as StringTMap<RavenPlan>;

    planDetail = {
      ...plan,
      activityInstances,
    } as RavenPlanDetail;
  });

  describe('ClearSelectedActivity', () => {
    it('should clear the selected activity', () => {
      const newInitialState = {
        ...initialState,
        selectedActivity: activityInstances['1'],
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
        selectedPlan: planDetail,
      };

      const result = reducer(newInitialState, new ClearSelectedPlan());

      expect(result).toEqual({
        ...newInitialState,
        selectedPlan: null,
      });
    });
  });

  describe('FetchPlanDetailSuccess', () => {
    it('should set a selectedPlan upon fetch plan detail success', () => {
      const result = reducer(
        initialState,
        new FetchPlanDetailSuccess(planDetail),
      );

      expect(result).toEqual({
        ...initialState,
        selectedPlan: planDetail,
      });
    });
  });

  describe('FetchPlanListSuccess', () => {
    it('should set the list of plans upon fetch plan list success', () => {
      const result = reducer(
        initialState,
        new FetchPlanListSuccess([plan['foo']]),
      );

      expect(result).toEqual({
        ...initialState,
        plans: plan,
      });
    });
  });

  describe('SelectActivity', () => {
    it('should set the selectedActivity', () => {
      const newInitialState = reducer(
        initialState,
        new FetchPlanDetailSuccess(planDetail),
      );

      const result = reducer(newInitialState, new SelectActivity('1'));

      expect(result).toEqual({
        ...newInitialState,
        selectedActivity: activityInstances['1'],
      });
    });
  });

  describe('UpdateActivitySuccess', () => {
    it('update an activity instance by id with the given update object', () => {
      const newInitialState = reducer(
        initialState,
        new FetchPlanDetailSuccess(planDetail),
      );

      const activityId = '1';
      const update = { color: '#000000' };
      const result = reducer(
        newInitialState,
        new UpdateActivitySuccess(activityId, update),
      );

      const selectedPlan = newInitialState.selectedPlan as RavenPlanDetail;
      const instance = selectedPlan.activityInstances[activityId];

      expect(result).toEqual({
        ...newInitialState,
        selectedPlan: {
          ...selectedPlan,
          activityInstances: {
            ...selectedPlan.activityInstances,
            [activityId]: {
              ...instance,
              ...update,
            },
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
