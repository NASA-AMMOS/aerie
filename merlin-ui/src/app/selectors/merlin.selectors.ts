import { createFeatureSelector, createSelector } from '@ngrx/store';
import { compare } from '../functions';
import { MerlinState } from '../reducers';
import {
  CActivityInstance,
  CActivityInstanceMap,
  CActivityType,
  CActivityTypeMap,
  CAdaptation,
  CPlan,
} from '../types';

const getMerlinState = createFeatureSelector<MerlinState>('merlin');

export const getActivityInstancesMap = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityInstanceMap => state.activityInstances,
);

export const getActivityInstancesForSelectedPlan = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityInstance[] => {
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
    return [];
  },
);

export const getActivityTypes = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityType[] =>
    state.activityTypes ? Object.values(state.activityTypes) : [],
);

export const getActivityTypesMap = createSelector(
  getMerlinState,
  (state: MerlinState): CActivityTypeMap | null => state.activityTypes,
);

export const getAdaptations = createSelector(
  getMerlinState,
  (state: MerlinState): CAdaptation[] =>
    state.adaptations ? Object.values(state.adaptations) : [],
);

export const getLoading = createSelector(
  getMerlinState,
  (state: MerlinState): boolean => state.loading,
);

export const getPlans = createSelector(
  getMerlinState,
  (state: MerlinState): CPlan[] =>
    state.plans ? Object.values(state.plans) : [],
);

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
