/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy } from 'lodash';
import { getMaxTimeRange } from '../../shared/util';

import {
  RavenActivity,
  RavenPlan,
  RavenPlanDetail,
  RavenTimeRange,
  StringTMap,
} from '../../shared/models';

import {
  FetchPlanDetailSuccess,
  FetchPlanListSuccess,
  PlanActions,
  PlanActionTypes,
  SelectActivity,
  UpdateActivitySuccess,
  UpdateViewTimeRange,
} from '../actions/plan.actions';

export interface PlanState {
  maxTimeRange: RavenTimeRange;
  plans: StringTMap<RavenPlan>;
  selectedActivity: RavenActivity | null;
  selectedPlan: RavenPlanDetail | null;
  viewTimeRange: RavenTimeRange;
}

export const initialState: PlanState = {
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
    case PlanActionTypes.FetchPlanDetailSuccess:
      return fetchPlanDetailSuccess(state, action);
    case PlanActionTypes.FetchPlanListSuccess:
      return fetchPlanListSuccess(state, action);
    case PlanActionTypes.SelectActivity:
      return selectActivity(state, action);
    case PlanActionTypes.UpdateActivitySuccess:
      return updateActivitySuccess(state, action);
    case PlanActionTypes.UpdateViewTimeRange:
      return updateViewTimeRange(state, action);
    default:
      return state;
  }
}

/**
 * Reduction helper. Called when a 'FetchPlanDetailSuccess' action occurs.
 */
function fetchPlanDetailSuccess(
  state: PlanState,
  action: FetchPlanDetailSuccess,
): PlanState {
  const selectedPlan = action.data;
  const activityInstances = Object.values(selectedPlan.activityInstances);
  const maxTimeRange = getMaxTimeRange(activityInstances);
  const viewTimeRange = { ...maxTimeRange };

  return {
    ...state,
    maxTimeRange,
    selectedPlan,
    viewTimeRange,
  };
}

/**
 * Reduction helper. Called when a 'FetchPlanListSuccess' action occurs.
 */
function fetchPlanListSuccess(
  state: PlanState,
  action: FetchPlanListSuccess,
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
  if (state.selectedPlan) {
    const activityInstances = state.selectedPlan.activityInstances;

    if (action.id !== null && activityInstances[action.id]) {
      return {
        ...state,
        selectedActivity: activityInstances[action.id],
      };
    }
  }

  return {
    ...state,
  };
}

/**
 * Reduction helper. Called when a 'UpdateActivitySuccess' action occurs.
 */
function updateActivitySuccess(
  state: PlanState,
  action: UpdateActivitySuccess,
): PlanState {
  if (!state.selectedPlan) {
    throw new Error('plan.reducer.ts: updateActivitySuccess: NoPlanSelected');
  }

  return {
    ...state,
    selectedPlan: {
      ...state.selectedPlan,
      activityInstances: {
        ...state.selectedPlan.activityInstances,
        [action.activityId]: {
          ...state.selectedPlan.activityInstances[action.activityId],
          ...action.update,
        },
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
