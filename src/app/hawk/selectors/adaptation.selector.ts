/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../hawk-store';
import { AdaptationState } from '../reducers/adaptation.reducer';

const featureSelector = createFeatureSelector<State>('hawk');
export const getAdaptationState = createSelector(
  featureSelector,
  (state: State): AdaptationState => state.adaptation,
);

export const getActivityTypes = createSelector(
  getAdaptationState,
  (state: AdaptationState) =>
    state.selectedAdaptation
      ? Object.values(state.selectedAdaptation.activityTypes)
      : null,
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
