/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy, omit } from 'lodash';
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
  FetchActivitiesSuccess,
  FetchPlansSuccess,
  PlanActions,
  PlanActionTypes,
  SelectActivity,
  SelectPlan,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';

export interface PlanState {
  activities: StringTMap<ActivityInstance> | null;
  maxTimeRange: TimeRange;
  plans: StringTMap<Plan>;
  selectedActivity: ActivityInstance | null;
  selectedPlan: Plan | null;
  viewTimeRange: TimeRange;
}

export const initialState: PlanState = {
  activities: null,
  maxTimeRange: { end: 0, start: 0 },
  plans: {},
  selectedActivity: null,
  selectedPlan: null,
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
      return { ...state, selectedActivity: null };
    case PlanActionTypes.ClearSelectedPlan:
      return { ...state, selectedPlan: null };
    case PlanActionTypes.CreateActivitySuccess:
      return createActivitySuccess(state, action);
    case PlanActionTypes.CreatePlanSuccess:
      return createPlanSuccess(state, action);
    case PlanActionTypes.DeleteActivitySuccess:
      return deleteActivitySuccess(state, action);
    case PlanActionTypes.DeletePlanSuccess:
      return deletePlanSuccess(state, action);
    case PlanActionTypes.FetchActivitiesSuccess:
      return fetchActivitiesSuccess(state, action);
    case PlanActionTypes.FetchPlansSuccess:
      return fetchPlansSuccess(state, action);
    case PlanActionTypes.SelectActivity:
      return selectActivity(state, action);
    case PlanActionTypes.SelectPlan:
      return selectPlan(state, action);
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
  const maxTimeRange = getMaxTimeRange([action.activity]);
  const viewTimeRange = { ...maxTimeRange };

  return {
    ...state,
    activities: {
      ...state.activities,
      [action.activity.activityId]: {
        ...action.activity,
      },
    },
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
 * Reduction helper. Called when a 'FetchActivitiesSuccess' action occurs.
 * If `action.activityId` is non-null we set a selectedActivity.
 */
function fetchActivitiesSuccess(
  state: PlanState,
  action: FetchActivitiesSuccess,
): PlanState {
  const selectedPlan = state.plans[action.planId] || null;
  const maxTimeRange = getMaxTimeRange(action.activities);
  const viewTimeRange = { ...maxTimeRange };
  const activities = keyBy(action.activities, 'activityId');
  const selectedActivity = action.activityId
    ? activities[action.activityId]
    : null;

  return {
    ...state,
    activities,
    maxTimeRange,
    selectedActivity,
    selectedPlan,
    viewTimeRange,
  };
}

/**
 * Reduction helper. Called when a 'FetchPlansSuccess' action occurs.
 */
function fetchPlansSuccess(
  state: PlanState,
  action: FetchPlansSuccess,
): PlanState {
  return {
    ...state,
    plans: keyBy(action.data, 'id'),
  };
}

/**
 * Reduction helper. Called when a 'SelectActivity' action occurs.
 */
function selectActivity(state: PlanState, action: SelectActivity): PlanState {
  const { activities } = state;

  if (action.id !== null && activities && activities[action.id]) {
    return {
      ...state,
      selectedActivity: activities[action.id],
    };
  }

  return {
    ...state,
    selectedActivity: null,
  };
}

/**
 * Reduction helper. Called when a 'SelectPlan' action occurs.
 */
function selectPlan(state: PlanState, action: SelectPlan): PlanState {
  const { plans } = state;

  if (action.id !== null && plans && plans[action.id]) {
    return {
      ...state,
      selectedPlan: plans[action.id],
    };
  }

  return {
    ...state,
    selectedPlan: null,
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
