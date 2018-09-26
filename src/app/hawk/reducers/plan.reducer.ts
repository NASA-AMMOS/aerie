/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RavenPlan } from '../../shared/models/raven-plan';

import {
  PlanActions,
  PlanActionTypes,
  RemovePlan,
  SavePlanSuccess,
} from '../actions/plan.actions';

import { State } from '../hawk-store';

/**
 * Schema for PlanState
 */
export interface PlanState {
  plans: RavenPlan[];
  selectedPlanId: string | null;
}

export const initialState: PlanState = {
  plans: [],
  selectedPlanId: null,
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
    case PlanActionTypes.FetchPlanListSuccess:
      return { ...state, plans: action.data };
    case PlanActionTypes.SavePlanSuccess:
      return action.isNew ? insert(state, action) : update(state, action);
    case PlanActionTypes.RemovePlan:
      return remove(state, action);
    case PlanActionTypes.SelectPlan:
      return { ...state, selectedPlanId: action.id };

    default:
      return state;
  }
}

/**
 * State selector helper.
 */
const featureSelector = createFeatureSelector<State>('hawk');
export const getPlanState = createSelector(
  featureSelector,
  (state: State): PlanState => state.plan,
);

export const getPlans = createSelector(
  getPlanState,
  (state: PlanState) => state.plans,
);

export const getSelectedPlan = createSelector(
  getPlanState,
  (state: PlanState) => state.plans.find(p => p.id === state.selectedPlanId),
);

/**
 * Remove an plan from the plans list
 */
function remove(state: PlanState, action: RemovePlan): PlanState {
  return {
    ...state,
    plans: state.plans.filter((a: RavenPlan) => a.id !== action.id),
  };
}

/**
 * Update an plan in the plans list
 */
function update(state: PlanState, action: SavePlanSuccess): PlanState {
  return {
    ...state,
    plans: state.plans.map((a: RavenPlan) => {
      return a.id === action.data.id ? action.data : a;
    }),
  };
}

/**
 * Insert an plan into the plans list
 */
function insert(state: PlanState, action: SavePlanSuccess): PlanState {
  return {
    ...state,
    plans: [...state.plans, action.data],
  };
}
