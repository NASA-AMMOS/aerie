/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ActivityInstance, Plan, StringTMap } from '../../shared/models';
import { State } from '../planning-store';
import { PlanState } from '../reducers/plan.reducer';

const featureSelector = createFeatureSelector<State>('planning');
export const getPlanState = createSelector(
  featureSelector,
  (state: State): PlanState => state.plan,
);

export const getActivities = createSelector(
  getPlanState,
  (state: PlanState) => state.activities,
);

export const getActivitiesAsList = createSelector(
  getActivities,
  (activities: StringTMap<ActivityInstance> | null) =>
    activities ? Object.values(activities) : [],
);

export const getMaxTimeRange = createSelector(
  getPlanState,
  (state: PlanState) => state.maxTimeRange,
);

export const getPlans = createSelector(
  getPlanState,
  (state: PlanState) => state.plans,
);

export const getPlansAsList = createSelector(
  getPlans,
  (plans: StringTMap<Plan> | null) => (plans ? Object.values(plans) : []),
);

export const getSelectedActivityId = createSelector(
  getPlanState,
  (state: PlanState) => state.selectedActivityId,
);

export const getSelectedActivity = createSelector(
  getActivities,
  getSelectedActivityId,
  (
    activities: StringTMap<ActivityInstance> | null,
    selectedActivityId: string | null,
  ) =>
    activities && selectedActivityId ? activities[selectedActivityId] : null,
);

export const getSelectedPlanId = createSelector(
  getPlanState,
  (state: PlanState) => state.selectedPlanId,
);

export const getSelectedPlan = createSelector(
  getPlans,
  getSelectedPlanId,
  (plans: StringTMap<Plan> | null, selectedPlanId: string | null) =>
    plans && selectedPlanId ? plans[selectedPlanId] : null,
);

export const getViewTimeRange = createSelector(
  getPlanState,
  (state: PlanState) => state.viewTimeRange,
);
