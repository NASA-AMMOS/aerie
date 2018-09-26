/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenPlan } from '../../shared/models/raven-plan';
import {
  FetchPlanListSuccess,
  RemovePlan,
  SavePlanSuccess,
} from '../actions/plan.actions';
import { initialState, PlanState, reducer } from './plan.reducer';

function getInitialState() {
  return { ...initialState };
}

describe('Plan Reducer', () => {
  let plan: RavenPlan;
  let updated: RavenPlan;

  beforeEach(() => {
    plan = {
      end: '1995-12-17T03:28:00',
      hsoc: 30,
      id: 'foo',
      mpow: 24,
      msoc: 10,
      name: 'Foo',
      start: '1995-12-17T03:24:00',
    };
    updated = {
      end: '1995-12-17T03:28:00',
      hsoc: 30,
      id: 'foo',
      mpow: 24,
      msoc: 10,
      name: 'FooBar',
      start: '1995-12-17T03:24:00',
    };
  });

  describe('FetchPlanListSuccess', () => {
    it('should return a list of plan types', () => {
      const expected = [plan];
      const result: PlanState = reducer(
        getInitialState(),
        new FetchPlanListSuccess(expected),
      );

      expect(result).toEqual({
        plans: expected,
        selectedPlanId: null,
      });
    });
  });

  describe('SavePlanSuccess', () => {
    it('should append an plan type when a new plan type is passed', () => {
      const result: PlanState = reducer(
        getInitialState(),
        new SavePlanSuccess(plan, true),
      );

      expect(result).toEqual({
        plans: [plan],
        selectedPlanId: null,
      });
    });

    it('should update an plan type when an existing plan type is passed', () => {
      const result: PlanState = reducer(
        {
          ...initialState,
          plans: [plan],
        },
        new SavePlanSuccess(updated, false),
      );

      expect(result).toEqual({
        ...initialState,
        plans: [updated],
      });
    });
  });

  describe('RemovePlan', () => {
    it('should remove an plan with the provided ID', () => {
      const result: PlanState = reducer(
        {
          ...initialState,
          plans: [plan],
        },
        new RemovePlan(plan.id),
      );

      expect(result).toEqual({
        ...initialState,
      });
    });
  });
});
