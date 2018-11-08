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
  PlanActions,
  PlanActionTypes,
  RemovePlan,
  SaveActivityDetailSuccess,
  SaveActivitySuccess,
  SavePlanSuccess,
} from '../actions/plan.actions';

import { RavenPlan, RavenPlanDetail, StringTMap } from '../../shared/models';

/**
 * Schema for PlanState
 */
export interface PlanState {
  plans: StringTMap<RavenPlan>;
  selectedPlan: RavenPlanDetail | null;
}

export const initialState: PlanState = {
  plans: {},
  selectedPlan: null,
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
      return { ...state, selectedPlan: action.data };
    case PlanActionTypes.FetchPlanListSuccess:
      return { ...state, plans: keyBy(action.data, 'id') };
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
    default:
      return state;
  }
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
