import { createReducer, on } from '@ngrx/store';
import omit from 'lodash-es/omit';
import { MerlinActions } from '../actions';
import {
  CActivityInstanceMap,
  CActivityTypeMap,
  CAdaptationMap,
  CPlan,
  CPlanMap,
} from '../types';

export interface MerlinState {
  activityInstances: CActivityInstanceMap | null;
  activityTypes: CActivityTypeMap | null;
  adaptations: CAdaptationMap | null;
  loading: boolean;
  plans: CPlanMap | null;
  selectedPlan: CPlan | null;
}

export const initialState: MerlinState = {
  activityInstances: null,
  activityTypes: null,
  adaptations: null,
  loading: false,
  plans: null,
  selectedPlan: null,
};

export const reducer = createReducer(
  initialState,
  on(MerlinActions.createActivityInstanceSuccess, (state, action) => {
    return {
      ...state,
      activityInstances: {
        ...state.activityInstances,
        [action.activityInstanceId]: {
          ...action.activityInstance,
          id: action.activityInstanceId,
          parameters: {},
        },
      },
      selectedPlan: {
        ...state.selectedPlan,
        activityInstanceIds: state.selectedPlan.activityInstanceIds.concat(
          action.activityInstanceId,
        ),
      },
    };
  }),
  on(MerlinActions.createAdaptationSuccess, (state, action) => {
    return {
      ...state,
      adaptations: {
        ...state.adaptations,
        [action.id]: {
          ...omit(action.adaptation, 'file'),
          id: action.id,
        },
      },
    };
  }),
  on(MerlinActions.createPlanSuccess, (state, action) => {
    return {
      ...state,
      plans: {
        ...state.plans,
        [action.id]: {
          ...omit(action.plan, 'activityInstances'),
          id: action.id,
          activityInstanceIds: [],
        },
      },
    };
  }),
  on(MerlinActions.deleteActivityInstanceSuccess, (state, action) => ({
    ...state,
    activityInstances: omit(state.activityInstances, action.activityInstanceId),
  })),
  on(MerlinActions.deleteAdaptationSuccess, (state, action) => ({
    ...state,
    adaptations: omit(state.adaptations, action.id),
  })),
  on(MerlinActions.deletePlanSuccess, (state, action) => ({
    ...state,
    plans: omit(state.plans, action.id),
  })),
  on(MerlinActions.setActivityInstances, (state, { activityInstances }) => ({
    ...state,
    activityInstances,
  })),
  on(MerlinActions.setAdaptations, (state, { adaptations }) => ({
    ...state,
    adaptations,
  })),
  on(MerlinActions.setLoading, (state, { loading }) => ({
    ...state,
    loading,
  })),
  on(MerlinActions.setPlans, (state, { plans }) => ({
    ...state,
    plans,
  })),
  on(
    MerlinActions.setSelectedPlanAndActivityTypes,
    (state, { activityTypes, selectedPlan }) => ({
      ...state,
      activityTypes,
      selectedPlan,
    }),
  ),
);
