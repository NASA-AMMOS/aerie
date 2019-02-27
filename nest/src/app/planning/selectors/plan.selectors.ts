/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../planning-store';
import { PlanState } from '../reducers/plan.reducer';

const featureSelector = createFeatureSelector<State>('planning');
export const getPlanState = createSelector(
  featureSelector,
  (state: State): PlanState => state.plan,
);

export const getActivities = createSelector(
  getPlanState,
  (state: PlanState) =>
    state.activities ? Object.values(state.activities) : [],
);

export const getMaxTimeRange = createSelector(
  getPlanState,
  (state: PlanState) => state.maxTimeRange,
);

export const getPlans = createSelector(getPlanState, (state: PlanState) =>
  Object.values(state.plans),
);

export const getSelectedActivity = createSelector(
  getPlanState,
  (state: PlanState) => state.selectedActivity,
);

export const getSelectedPlan = createSelector(
  getPlanState,
  (state: PlanState) => state.selectedPlan,
);

export const getViewTimeRange = createSelector(
  getPlanState,
  (state: PlanState) => state.viewTimeRange,
);
