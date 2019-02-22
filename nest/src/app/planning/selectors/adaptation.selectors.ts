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
import { AdaptationState } from '../reducers/adaptation.reducer';

const featureSelector = createFeatureSelector<State>('planning');
export const getAdaptationState = createSelector(
  featureSelector,
  (state: State): AdaptationState => state.adaptation,
);

export const getActivityTypes = createSelector(
  getAdaptationState,
  (state: AdaptationState) => Object.values(state.activityTypes),
);

export const getAdaptations = createSelector(
  getAdaptationState,
  (state: AdaptationState) => state.adaptations,
);
