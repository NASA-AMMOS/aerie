import { createAction, props } from '@ngrx/store';
import {
  CActivityInstanceMap,
  CActivityTypeMap,
  CAdaptationMap,
  CPlan,
  CPlanMap,
  SActivityInstance,
  SCreateAdaption,
  SPlan,
} from '../types';

export const createActivityInstance = createAction(
  '[merlin] createActivityInstance',
  props<{ planId: string; activityInstance: SActivityInstance }>(),
);

export const createActivityInstanceSuccess = createAction(
  '[merlin] createActivityInstanceSuccess',
  props<{
    planId: string;
    activityInstanceId: string;
    activityInstance: SActivityInstance;
  }>(),
);

export const createAdaptation = createAction(
  '[merlin] createAdaptation',
  props<{ adaptation: SCreateAdaption }>(),
);

export const createAdaptationSuccess = createAction(
  '[merlin] createAdaptationSuccess',
  props<{ id: string; adaptation: SCreateAdaption }>(),
);

export const createPlan = createAction(
  '[merlin] createPlan',
  props<{ plan: SPlan }>(),
);

export const createPlanSuccess = createAction(
  '[merlin] createPlanSuccess',
  props<{ id: string; plan: SPlan }>(),
);

export const deleteActivityInstance = createAction(
  '[merlin] deleteActivityInstance',
  props<{ planId: string; activityInstanceId: string }>(),
);

export const deleteActivityInstanceSuccess = createAction(
  '[merlin] deleteActivityInstanceSuccess',
  props<{ activityInstanceId: string }>(),
);

export const deleteAdaptation = createAction(
  '[merlin] deleteAdaptation',
  props<{ id: string }>(),
);

export const deleteAdaptationSuccess = createAction(
  '[merlin] deleteAdaptationSuccess',
  props<{ id: string }>(),
);

export const deletePlan = createAction(
  '[merlin] deletePlan',
  props<{ id: string }>(),
);

export const deletePlanSuccess = createAction(
  '[merlin] deletePlanSuccess',
  props<{ id: string }>(),
);

export const setActivityInstances = createAction(
  '[merlin] setActivityInstances',
  props<{ planId: string; activityInstances: CActivityInstanceMap }>(),
);

export const setAdaptations = createAction(
  '[merlin] setAdaptations',
  props<{ adaptations: CAdaptationMap }>(),
);

export const setLoading = createAction(
  '[merlin] setLoading',
  props<{ loading: boolean }>(),
);

export const setPlans = createAction(
  '[merlin] setPlans',
  props<{ plans: CPlanMap }>(),
);

export const setSelectedPlanAndActivityTypes = createAction(
  '[merlin] setSelectedPlanAndActivityTypes',
  props<{ selectedPlan: CPlan; activityTypes: CActivityTypeMap }>(),
);
