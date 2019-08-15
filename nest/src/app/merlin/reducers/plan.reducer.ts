/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import keyBy from 'lodash-es/keyBy';
import omit from 'lodash-es/omit';
import {
  ActivityInstance,
  Plan,
  StringTMap,
  TimeRange,
} from '../../shared/models';
import { getMaxTimeRange } from '../../shared/util';
import { PlanActions } from '../actions';

export interface PlanState {
  activities: StringTMap<ActivityInstance> | null;
  maxTimeRange: TimeRange;
  plans: StringTMap<Plan>;
  selectedActivityId: string | null;
  selectedPlanId: string | null;
  viewTimeRange: TimeRange;
}

export const initialState: PlanState = {
  activities: null,
  maxTimeRange: { end: 0, start: 0 },
  plans: {},
  selectedActivityId: null,
  selectedPlanId: null,
  viewTimeRange: { end: 0, start: 0 },
};

export const reducer = createReducer(
  initialState,
  on(PlanActions.clearSelectedActivity, state => ({
    ...state,
    selectedActivityId: null,
  })),
  on(PlanActions.clearSelectedPlan, state => ({
    ...state,
    selectedPlanId: null,
  })),
  on(PlanActions.createActivitySuccess, (state, { activity }) => {
    const activities = {
      ...state.activities,
      [activity.activityId as string]: {
        ...activity,
      },
    };
    const maxTimeRange = getMaxTimeRange(Object.values(activities));
    const viewTimeRange = { ...maxTimeRange };

    return {
      ...state,
      activities,
      maxTimeRange,
      viewTimeRange,
    };
  }),
  on(PlanActions.createPlanSuccess, (state, action) => ({
    ...state,
    plans: {
      ...state.plans,
      [action.plan.id]: {
        ...action.plan,
      },
    },
  })),
  on(PlanActions.deleteActivitySuccess, (state, action) => ({
    ...state,
    activities: omit(state.activities, action.activityId),
  })),
  on(PlanActions.deletePlanSuccess, (state, action) => ({
    ...state,
    plans: omit(state.plans, action.deletedPlanId),
  })),
  on(PlanActions.selectActivity, (state, action) => ({
    ...state,
    selectedActivityId: action.id,
  })),
  on(PlanActions.setActivities, (state, action) => {
    const activities = keyBy(action.activities, 'activityId');
    const maxTimeRange = getMaxTimeRange(action.activities);
    const viewTimeRange = { ...maxTimeRange };

    return {
      ...state,
      activities,
      maxTimeRange,
      selectedActivityId: action.activityId || state.selectedActivityId,
      viewTimeRange,
    };
  }),
  on(PlanActions.setPlans, (state, action) => ({
    ...state,
    plans: keyBy(action.plans, 'id'),
  })),
  on(PlanActions.setPlansAndSelectedPlan, (state, action) => ({
    ...state,
    plans: keyBy(action.plans, 'id'),
    selectedPlanId: action.planId,
  })),
  on(PlanActions.updateActivitySuccess, (state, action) => {
    if (!state.activities) {
      throw new Error(
        'plan.reducer.ts: updateActivitySuccess: NoActivityInstances',
      );
    }

    return {
      ...state,
      activities: {
        ...state.activities,
        [action.activityId]: {
          ...state.activities[action.activityId],
          ...action.update,
        },
      },
    };
  }),
  on(PlanActions.updateViewTimeRange, (state, action) => ({
    ...state,
    viewTimeRange: { ...action.viewTimeRange },
  })),
);
