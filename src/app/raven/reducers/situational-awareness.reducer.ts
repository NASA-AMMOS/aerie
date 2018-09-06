/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RavenSituationalAwarenessPefEntry } from '../../shared/models';
import { State } from '../raven-store';

import {
  SituationalAwarenessAction,
  SituationalAwarenessActionTypes,
} from './../actions/situational-awareness.actions';

export interface SituationalAwarenessState {
  fetchPending: boolean;
  nowMinus: number | null;
  nowPlus: number | null;
  pageDuration: number | null;
  pefEntries: RavenSituationalAwarenessPefEntry[] | null;
  situationalAware: boolean;
  startTime: number | null;
  useNow: boolean;
}

// Situational Awareness State.
export const initialState: SituationalAwarenessState = {
  fetchPending: false,
  nowMinus: 604800, // 7 days.
  nowPlus: 5184000, // 60 days.
  pageDuration: 604800,
  pefEntries: [],
  situationalAware: false,
  startTime: null,
  useNow: true,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: SituationalAwarenessState = initialState,
  action: SituationalAwarenessAction,
): SituationalAwarenessState {
  switch (action.type) {
    case SituationalAwarenessActionTypes.ChangeSituationalAwareness:
      return { ...state, fetchPending: true };
    case SituationalAwarenessActionTypes.UpdateSituationalAwarenessSettings:
      return { ...state, ...action.update };
    default:
      return state;
  }
}

/**
 * SituationalAwareness state selector helper.
 */
export const getSituationalAwarenessState = createSelector(
  createFeatureSelector<State>('raven'),
  (state: State): SituationalAwarenessState => state.situationalAwareness,
);

/**
 * Create selector helper for selecting state slice.
 *
 * Every reducer module exports selector functions, however child reducers
 * have no knowledge of the overall state tree. To make them usable, we
 * need to make new selectors that wrap them.
 *
 * The createSelector function creates very efficient selectors that are memoized and
 * only recompute when arguments change. The created selectors can also be composed
 * together to select different pieces of state.
 */
export const getPending = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.fetchPending,
);
