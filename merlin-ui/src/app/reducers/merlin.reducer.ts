import { createReducer, on } from '@ngrx/store';
import omit from 'lodash-es/omit';
import { MerlinActions } from '../actions';
import {
  CActivityInstanceMap,
  CActivityInstanceParameterMap,
  CActivityTypeMap,
  CAdaptationMap,
  CPlan,
  CPlanMap,
  StringTMap,
} from '../types';

export interface MerlinState {
  activityInstances: CActivityInstanceMap | null;
  activityTypes: CActivityTypeMap | null;
  adaptations: CAdaptationMap | null;
  loading: boolean;
  plans: CPlanMap | null;
  selectedActivityInstanceId: string | null;
  selectedPlan: CPlan | null;
}

export const initialState: MerlinState = {
  activityInstances: null,
  activityTypes: null,
  adaptations: null,
  loading: false,
  plans: null,
  selectedActivityInstanceId: null,
  selectedPlan: null,
};

export const reducer = createReducer(
  initialState,
  on(MerlinActions.createActivityInstanceSuccess, (state, action) => ({
    ...state,
    activityInstances: {
      ...state.activityInstances,
      [action.activityInstanceId]: {
        ...action.activityInstance,
        id: action.activityInstanceId,
        parameters: toActivityInstanceParameterMap(
          action.activityInstance.parameters,
        ),
      },
    },
    selectedPlan: {
      ...state.selectedPlan,
      activityInstanceIds: state.selectedPlan.activityInstanceIds.concat(
        action.activityInstanceId,
      ),
    },
  })),
  on(MerlinActions.createAdaptationSuccess, (state, action) => ({
    ...state,
    adaptations: {
      ...state.adaptations,
      [action.id]: {
        ...omit(action.adaptation, 'file'),
        id: action.id,
      },
    },
  })),
  on(MerlinActions.createPlanSuccess, (state, action) => ({
    ...state,
    plans: {
      ...state.plans,
      [action.id]: {
        ...omit(action.plan, 'activityInstances'),
        activityInstanceIds: [],
        id: action.id,
      },
    },
  })),
  on(MerlinActions.deleteActivityInstanceSuccess, (state, action) => ({
    ...state,
    activityInstances: omit(state.activityInstances, action.activityInstanceId),
    selectedActivityInstanceId:
      state.selectedActivityInstanceId === action.activityInstanceId
        ? null
        : state.selectedActivityInstanceId,
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
    MerlinActions.setSelectedActivityInstanceId,
    (state, { selectedActivityInstanceId }) => ({
      ...state,
      selectedActivityInstanceId,
    }),
  ),
  on(
    MerlinActions.setSelectedPlanAndActivityTypes,
    (state, { activityTypes, selectedPlan }) => ({
      ...state,
      activityTypes,
      selectedPlan,
    }),
  ),
  on(MerlinActions.updateActivityInstanceSuccess, (state, action) => ({
    ...state,
    activityInstances: {
      ...state.activityInstances,
      [action.activityInstanceId]: {
        ...state.activityInstances[action.activityInstanceId],
        ...action.activityInstance,
        parameters: toActivityInstanceParameterMap(
          action.activityInstance.parameters,
        ),
      },
    },
  })),
);

function toActivityInstanceParameterMap(
  parameters: StringTMap<any>,
): CActivityInstanceParameterMap {
  return Object.keys(parameters).reduce(
    (
      cActivityInstanceParameterMap: CActivityInstanceParameterMap,
      name: string,
    ) => {
      cActivityInstanceParameterMap[name] = {
        name,
        value: parameters[name],
      };
      return cActivityInstanceParameterMap;
    },
    {},
  );
}
