/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { initialState, PlanState, reducer } from './plan.reducer';

import {
  RavenActivity,
  RavenActivityDetail,
  RavenPlan,
  RavenPlanDetail,
  StringTMap,
} from '../../shared/models';

import {
  FetchPlanDetailSuccess,
  FetchPlanListSuccess,
  RemovePlan,
  SaveActivitySuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

function getInitialState() {
  return { ...initialState };
}

describe('Plan Reducer', () => {
  let activities: StringTMap<RavenActivity>;
  let activityDetail: RavenActivityDetail;
  let plan: StringTMap<RavenPlan>;
  let planDetail: RavenPlanDetail;
  let updated: StringTMap<RavenPlan>;

  beforeEach(() => {
    activities = {
      '1': {
        activityTypeId: '000',
        duration: '00:10',
        id: '1',
        intent: 'Some science intent for this activity...',
        name: 'Instrument 1, Activity ABC',
        sequenceId: 'inst00035.0000.a',
        start: '2022-10-29T14:55:00',
      },
    };

    activityDetail = {
      activityTypeId: '001',
      duration: '00:19',
      id: '2',
      intent: 'Some science intent for this activity...',
      name: 'Instrument 2, Activity ABC',
      parameters: [],
      sequenceId: 'inst00036.0000ba',
      start: '2023-11-28T15:54:10',
    };

    plan = {
      foo: {
        adaptationId: 'ops',
        end: '1995-12-17T03:28:00',
        id: 'foo',
        name: 'Foo',
        start: '1995-12-17T03:24:00',
      },
    } as StringTMap<RavenPlan>;

    planDetail = {
      ...plan,
      activities,
    } as RavenPlanDetail;

    updated = {
      foo: {
        adaptationId: 'ops',
        end: '1995-12-17T03:28:00',
        id: 'foo',
        name: 'FooBar',
        start: '1995-12-17T03:24:00',
      },
    } as StringTMap<RavenPlan>;
  });

  describe('FetchPlanDetailSuccess', () => {
    it('should properly select a plan detail', () => {
      const initial = getInitialState();

      const result: PlanState = reducer(
        getInitialState(),
        new FetchPlanDetailSuccess(planDetail),
      );

      expect(result).toEqual({
        ...initial,
        selectedPlan: planDetail,
      } as PlanState);
    });
  });

  describe('FetchPlanListSuccess', () => {
    it('should return a list of plan types', () => {
      const initial = getInitialState();

      const result: PlanState = reducer(
        getInitialState(),
        new FetchPlanListSuccess([plan['foo']]),
      );

      expect(result).toEqual({
        ...initial,
        plans: plan,
      } as PlanState);
    });
  });

  describe('SavePlanSuccess', () => {
    it('should append an plan type when a new plan type is passed', () => {
      const initial = getInitialState();
      const result: PlanState = reducer(
        getInitialState(),
        // TODO: Verify that this works without the "new" flag
        new SavePlanSuccess(plan['foo']),
      );

      expect(result).toEqual({
        ...initial,
        plans: plan,
      } as PlanState);
    });

    it('should update an plan type when an existing plan type is passed', () => {
      const result: PlanState = reducer(
        {
          ...initialState,
          plans: plan,
        },
        // TODO: Verify that this works without the "new" flag
        new SavePlanSuccess(updated['foo']),
      );

      expect(result).toEqual({
        ...initialState,
        plans: updated,
      });
    });
  });

  describe('RemovePlan', () => {
    it('should remove an plan with the provided ID', () => {
      const result: PlanState = reducer(
        {
          ...initialState,
          plans: plan,
        },
        new RemovePlan(plan['foo'].id),
      );

      expect(result).toEqual({
        ...initialState,
      });
    });
  });

  describe('SaveActivitySuccess | SaveActivityDetailSuccess', () => {
    it('should append an activity when a new activity is passed', () => {
      // Set up initial condition so we have a selected plan.
      const initial: PlanState = reducer(
        getInitialState(),
        new FetchPlanDetailSuccess(planDetail),
      );

      const result: PlanState = reducer(
        initial,
        new SaveActivitySuccess(activityDetail),
      );

      expect(result.selectedPlan).toBeDefined();
      expect(result).toEqual({
        ...initial,
        selectedPlan: {
          ...initial.selectedPlan,
          activities: {
            ...(initial.selectedPlan as RavenPlanDetail).activities,
            [activityDetail.id]: activityDetail,
          },
        },
      } as PlanState);
    });

    it('should update an activity when an existing activity is passed', () => {
      // Set up initial condition so we have a selected plan.
      const initial: PlanState = reducer(
        getInitialState(),
        new FetchPlanDetailSuccess(planDetail),
      );

      const detail: RavenActivityDetail = {
        ...activities['1'],
        name: 'i like turtles',
        parameters: [],
      };

      const result: PlanState = reducer(
        initial,
        new SaveActivitySuccess(detail),
      );

      expect(result.selectedPlan).toBeDefined();
      expect(result).toEqual({
        ...initial,
        selectedPlan: {
          ...initial.selectedPlan,
          activities: {
            ...(initial.selectedPlan as RavenPlanDetail).activities,
            [detail.id]: detail,
          },
        },
      } as PlanState);
    });
  });
});
