/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import keyBy from 'lodash/keyBy';
import omit from 'lodash/omit';
import {
  ActivityInstance,
  Plan,
  StringTMap,
  TimeRange,
} from '../../shared/models';
import { getMaxTimeRange } from '../../shared/util';
import {
  CreateActivitySuccess,
  CreatePlanSuccess,
  DeleteActivitySuccess,
  DeletePlanSuccess,
  PlanActions,
  PlanActionTypes,
  SetActivities,
  SetActivitiesAndSelectedActivity,
  SetPlans,
  SetPlansAndSelectedPlan,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';

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

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: PlanState = initialState,
  action: PlanActions,
): PlanState {
  switch (action.type) {
    case PlanActionTypes.ClearSelectedActivity:
      return { ...state, selectedActivityId: null };
    case PlanActionTypes.ClearSelectedPlan:
      return { ...state, selectedPlanId: null };
    case PlanActionTypes.CreateActivitySuccess:
      return createActivitySuccess(state, action);
    case PlanActionTypes.CreatePlanSuccess:
      return createPlanSuccess(state, action);
    case PlanActionTypes.DeleteActivitySuccess:
      return deleteActivitySuccess(state, action);
    case PlanActionTypes.DeletePlanSuccess:
      return deletePlanSuccess(state, action);
    case PlanActionTypes.SelectActivity:
      return { ...state, selectedActivityId: action.id };
    case PlanActionTypes.SetActivities:
      return setActivities(state, action);
    case PlanActionTypes.SetActivitiesAndSelectedActivity:
      return setActivitiesAndSelectedActivity(state, action);
    case PlanActionTypes.SetPlans:
      return setPlans(state, action);
    case PlanActionTypes.SetPlansAndSelectedPlan:
      return setPlansAndSelectedPlan(state, action);
    case PlanActionTypes.UpdateActivitySuccess:
      return updateActivitySuccess(state, action);
    case PlanActionTypes.UpdateViewTimeRange:
      return updateViewTimeRange(state, action);
    default:
      return state;
  }
}

/**
 * Reduction helper. Called when a 'CreateActivitySuccess' action occurs.
 */
function createActivitySuccess(
  state: PlanState,
  action: CreateActivitySuccess,
): PlanState {
  const activities = {
    ...state.activities,
    [action.activity.activityId]: {
      ...action.activity,
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
}

/**
 * Reduction helper. Called when a 'CreatePlanSuccess' action occurs.
 */
function createPlanSuccess(
  state: PlanState,
  action: CreatePlanSuccess,
): PlanState {
  return {
    ...state,
    plans: {
      ...state.plans,
      [action.plan.id]: {
        ...action.plan,
      },
    },
  };
}

/**
 * Reduction helper. Called when a 'DeleteActivitySuccess' action occurs.
 */
function deleteActivitySuccess(
  state: PlanState,
  action: DeleteActivitySuccess,
): PlanState {
  return {
    ...state,
    activities: omit(state.activities, action.activityId),
  };
}

/**
 * Reduction helper. Called when a 'DeletePlanSuccess' action occurs.
 */
function deletePlanSuccess(
  state: PlanState,
  action: DeletePlanSuccess,
): PlanState {
  return {
    ...state,
    plans: omit(state.plans, action.deletedPlanId),
  };
}

/**
 * Reduction helper. Called when a 'SetActivities' action occurs.
 */
function setActivities(state: PlanState, action: SetActivities): PlanState {
  const activities = keyBy(action.activities, 'activityId');
  const maxTimeRange = getMaxTimeRange(action.activities);
  const viewTimeRange = { ...maxTimeRange };

  return {
    ...state,
    activities,
    maxTimeRange,
    viewTimeRange,
  };
}

/**
 * Reduction helper. Called when a 'SetActivitiesAndSelectedActivity' action occurs.
 */
function setActivitiesAndSelectedActivity(
  state: PlanState,
  action: SetActivitiesAndSelectedActivity,
): PlanState {
  return {
    ...setActivities(state, (action as unknown) as SetActivities),
    selectedActivityId: action.activityId,
  };
}

/**
 * Reduction helper. Called when a 'SetPlans' action occurs.
 */
function setPlans(state: PlanState, action: SetPlans): PlanState {
  return {
    ...state,
    plans: keyBy(action.plans, 'id'),
  };
}

/**
 * Reduction helper. Called when a 'SetPlansAndSelectedPlan' action occurs.
 */
function setPlansAndSelectedPlan(
  state: PlanState,
  action: SetPlansAndSelectedPlan,
): PlanState {
  return {
    ...state,
    plans: keyBy(action.plans, 'id'),
    selectedPlanId: action.planId,
  };
}

/**
 * Reduction helper. Called when a 'UpdateActivitySuccess' action occurs.
 */
function updateActivitySuccess(
  state: PlanState,
  action: UpdateActivitySuccess,
): PlanState {
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
}

/**
 * Reduction helper. Called when a 'UpdateViewTimeRange' action occurs.
 */
function updateViewTimeRange(
  state: PlanState,
  action: UpdateViewTimeRange,
): PlanState {
  return {
    ...state,
    viewTimeRange: { ...action.viewTimeRange },
  };
}
