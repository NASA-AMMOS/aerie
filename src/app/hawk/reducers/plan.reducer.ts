/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy, omit } from 'lodash';
import { getMaxTimeRange, timestamp } from '../../shared/util';

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
  RemovePlan,
  SaveActivityDetailSuccess,
  SaveActivitySuccess,
  SavePlanSuccess,
  SelectActivity,
  UpdateSelectedActivity,
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
    case PlanActionTypes.FetchPlanDetailSuccess:
      return fetchPlanDetailSuccess(state, action);
    case PlanActionTypes.FetchPlanListSuccess:
      return fetchPlanListSuccess(state, action);
    case PlanActionTypes.SavePlanSuccess:
      return planExists(state, action)
        ? insertPlan(state, action)
        : updatePlan(state, action);
    case PlanActionTypes.RemovePlan:
      return removePlan(state, action);
    case PlanActionTypes.SaveActivitySuccess:
    case PlanActionTypes.SaveActivityDetailSuccess:
      return activityExists(state, action)
        ? insertActivity(state, action)
        : updateActivity(state, action);
    case PlanActionTypes.SelectActivity:
      return selectActivity(state, action);
    case PlanActionTypes.UpdateSelectedActivity:
      return updateSelectedActivity(state, action);
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
  const activities = Object.values(selectedPlan.activities);
  const maxTimeRange = getMaxTimeRange(activities);
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
    const activities = state.selectedPlan.activities;

    if (action.id !== null && activities[action.id]) {
      return {
        ...state,
        selectedActivity: activities[action.id],
      };
    }
  }

  return {
    ...state,
  };
}

/**
 * Reduction helper. Called when a 'UpdateSelectedActivity' action occurs.
 */
function updateSelectedActivity(
  state: PlanState,
  action: UpdateSelectedActivity,
): PlanState {
  if (state.selectedPlan) {
    const { activityId, end, start, y } = action.update;

    return {
      ...state,
      selectedPlan: {
        ...state.selectedPlan,
        activities: {
          ...state.selectedPlan.activities,
          [activityId]: {
            ...state.selectedPlan.activities[activityId],
            end,
            endTimestamp: timestamp(end),
            start,
            startTimestamp: timestamp(start),
            y,
          },
        },
      },
    };
  }

  return {
    ...state,
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

/**
 * Determine if an activity exists
 */
function activityExists(
  state: PlanState,
  action: SaveActivitySuccess | SaveActivityDetailSuccess,
): boolean {
  if (!state.selectedPlan) throw new Error('NoPlanSelected');
  return state.selectedPlan.activities.hasOwnProperty(action.data.id);
}

/**
 * Insert an activity into the selected plan. If there is no selected plan,
 * throw an error.
 */
function insertActivity(
  state: PlanState,
  action: SaveActivitySuccess | SaveActivityDetailSuccess,
): PlanState {
  if (!state.selectedPlan) throw new Error('NoPlanSelected');
  return {
    ...state,
    selectedPlan: {
      ...state.selectedPlan,
      activities: {
        ...state.selectedPlan.activities,
        [action.data.id]: { ...action.data },
      },
    },
  };
}

/**
 * Update an activity in the selected plan. If there is no selected plan,
 * throw an error.
 */
function updateActivity(
  state: PlanState,
  action: SaveActivitySuccess | SaveActivityDetailSuccess,
): PlanState {
  if (!state.selectedPlan) throw new Error('NoPlanSelected');
  return {
    ...state,
    selectedPlan: {
      ...state.selectedPlan,
      activities: {
        ...state.selectedPlan.activities,
        [action.data.id]: {
          ...state.selectedPlan.activities[action.data.id],
          ...action.data,
        },
      },
    },
  };
}

/**
 * Determine if a plan exists
 */
function planExists(state: PlanState, action: SavePlanSuccess): boolean {
  return state.plans.hasOwnProperty(action.data.id);
}

/**
 * Remove an plan from the plans list
 */
function removePlan(state: PlanState, action: RemovePlan): PlanState {
  return {
    ...state,
    plans: omit(state.plans, action.id),
  };
}

/**
 * Update an plan in the plans list
 */
function updatePlan(state: PlanState, action: SavePlanSuccess): PlanState {
  return {
    ...state,
    plans: {
      ...state.plans,
      [action.data.id]: {
        ...state.plans[action.data.id],
        ...action.data,
      },
    },
  };
}

/**
 * Insert an plan into the plans list
 */
function insertPlan(state: PlanState, action: SavePlanSuccess): PlanState {
  return {
    ...state,
    plans: {
      ...state.plans,
      [action.data.id]: {
        ...action.data,
      },
    },
  };
}
