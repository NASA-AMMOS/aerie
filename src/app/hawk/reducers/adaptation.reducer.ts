/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { omit } from 'lodash';

import { RavenAdaptation } from '../../shared/models/raven-adaptation';
import { RavenAdaptationDetail } from '../../shared/models/raven-adaptation-detail';

import {
  AdaptationActions,
  AdaptationActionTypes,
  RemoveActivityType,
  SaveActivityTypeSuccess,
} from '../actions/adaptation.actions';

import { AdaptationState } from './adaptation.reducer';

import { State } from '../hawk-store';

/**
 * Schema for Adaptation state
 */
export interface AdaptationState {
  adaptations: RavenAdaptation[];
  selectedAdaptation: RavenAdaptationDetail | null;
}

export const initialState: AdaptationState = {
  adaptations: [],
  selectedAdaptation: null,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: AdaptationState = initialState,
  action: AdaptationActions,
): AdaptationState {
  switch (action.type) {
    case AdaptationActionTypes.FetchAdaptationSuccess:
      return { ...state, selectedAdaptation: action.data };
    case AdaptationActionTypes.FetchAdaptationListSuccess:
      return { ...state, adaptations: action.data };
    case AdaptationActionTypes.SaveActivityTypeSuccess:
      return action.isNew ? insert(state, action) : update(state, action);
    case AdaptationActionTypes.RemoveActivityType:
      return remove(state, action);

    default:
      return state;
  }
}

/**
 * Remove an activity type from the activityTypes list
 */
function remove(
  state: AdaptationState,
  action: RemoveActivityType,
): AdaptationState {
  if (state.selectedAdaptation) {
    return {
      ...state,
      selectedAdaptation: {
        ...state.selectedAdaptation,
        activityTypes: omit(state.selectedAdaptation.activityTypes, [
          action.id,
        ]),
      },
    };
  }

  return { ...state };
}

/**
 * Update an activity type in the activityTypes list
 */
function update(
  state: AdaptationState,
  action: SaveActivityTypeSuccess,
): AdaptationState {
  if (state.selectedAdaptation) {
    return {
      ...state,
      selectedAdaptation: {
        ...state.selectedAdaptation,
        activityTypes: {
          ...state.selectedAdaptation.activityTypes,
          [action.data.id]: {
            ...state.selectedAdaptation.activityTypes[action.data.id],
            ...action.data,
          },
        },
      },
    };
  }
  return { ...state };
}

/**
 * Insert an activity type into the activity types list
 */
function insert(
  state: AdaptationState,
  action: SaveActivityTypeSuccess,
): AdaptationState {
  if (state.selectedAdaptation) {
    return {
      ...state,
      selectedAdaptation: {
        ...state.selectedAdaptation,
        activityTypes: {
          ...state.selectedAdaptation.activityTypes,
          [action.data.id]: { ...action.data },
        },
      },
    };
  }

  return { ...state };
}

/**
 * State selector helpers
 */
const featureSelector = createFeatureSelector<State>('hawk');
export const getAdaptationState = createSelector(
  featureSelector,
  (state: State): AdaptationState => state.adaptation,
);

export const getAdaptations = createSelector(
  getAdaptationState,
  (state: AdaptationState) => state.adaptations,
);

export const getSelectedAdaptation = createSelector(
  getAdaptationState,
  (state: AdaptationState) => state.selectedAdaptation,
);

export const getSelectedActivityTypeState = createSelector(
  getAdaptationState,
  (state: AdaptationState) => {
    if (state.selectedAdaptation) {
      return state.selectedAdaptation.activityTypes;
    }
    return null;
  },
);
