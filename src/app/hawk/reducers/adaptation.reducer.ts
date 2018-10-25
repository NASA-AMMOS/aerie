/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';

import { RavenAdaptation } from '../../shared/models/raven-adaptation';

import {
  AdaptationActions,
  AdaptationActionTypes,
} from '../actions/adaptation.actions';

import { AdaptationState } from './adaptation.reducer';

import { State } from '../hawk-store';

/**
 * Schema for Adaptation state
 */
export interface AdaptationState {
  adaptations: RavenAdaptation[];
  selectedAdaptationId: string | null;
}

export const initialState: AdaptationState = {
  adaptations: [],
  selectedAdaptationId: null,
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
    case AdaptationActionTypes.FetchAdaptationListSuccess:
      return { ...state, adaptations: action.data };

    default:
      return state;
  }
}

/**
 * State selector helper.
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
