import { createFeatureSelector, createSelector } from '@ngrx/store';
import { compare, getUnixEpochTime } from '../functions';
import { MerlinState } from '../reducers';
import {
  CActivityInstance,
  CActivityInstanceMap,
  CActivityType,
  CActivityTypeMap,
  CAdaptation,
  CPlan,
  TimeRange,
} from '../types';

const getMerlinState = createFeatureSelector<MerlinState>('merlin');

export const getActivityInstancesMap = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityInstanceMap => state.activityInstances,
);

export const getActivityInstancesForSelectedPlan = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityInstance[] | null => {
    if (state.selectedPlan && state.activityInstances) {
      const activityInstances = state.selectedPlan.activityInstanceIds.reduce(
        (instances, id) => {
          const activityInstance = state.activityInstances[id];
          if (activityInstance) {
            instances.push(activityInstance);
          }
          return instances;
        },
        [],
      );

      const sortedActivityInstances = activityInstances.sort((a, b) =>
        compare(a.startTimestamp, b.startTimestamp, true),
      );

      return sortedActivityInstances;
    }
    return null;
  },
);

export const getActivityTypes = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityType[] | null =>
    state.activityTypes ? Object.values(state.activityTypes) : null,
);

export const getActivityTypesMap = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityTypeMap | null => state.activityTypes,
);

export const getAdaptations = createSelector(
  getMerlinState,
  (state: MerlinState): CAdaptation[] | null =>
    state.adaptations ? Object.values(state.adaptations) : null,
);

export const getLoading = createSelector(
  getMerlinState,
  (state: MerlinState): boolean => state.loading,
);

export const getPlans = createSelector(getMerlinState, (state: MerlinState):
  | CPlan[]
  | null => (state.plans ? Object.values(state.plans) : null));

export const getSelectedActivityInstanceId = createSelector(
  getMerlinState,
  (state: MerlinState): string | null => state.selectedActivityInstanceId,
);

export const getSelectedActivityInstance = createSelector(
  getActivityInstancesMap,
  getSelectedActivityInstanceId,
  (
    activityInstances: CActivityInstanceMap | null,
    selectedActivityInstanceId: string | null,
  ) => {
    if (
      activityInstances &&
      selectedActivityInstanceId &&
      activityInstances[selectedActivityInstanceId]
    ) {
      return activityInstances[selectedActivityInstanceId];
    }
    return null;
  },
);

export const getSelectedPlan = createSelector(
  getMerlinState,
  (state: MerlinState): CPlan | null => state.selectedPlan,
);

export const getMaxTimeRange = createSelector(
  getSelectedPlan,
  (plan: CPlan | null): TimeRange => {
    if (plan) {
      return {
        end: getUnixEpochTime(plan.endTimestamp),
        start: getUnixEpochTime(plan.startTimestamp),
      };
    }
    return { start: 0, end: 0 };
  },
);

export const getViewTimeRange = createSelector(
  getMerlinState,
  (state: MerlinState): TimeRange => state.viewTimeRange,
);
