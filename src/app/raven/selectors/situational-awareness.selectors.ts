/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../raven-store';
import { SituationalAwarenessState } from '../reducers/situational-awareness.reducer';

export const getSituationalAwarenessState = createSelector(
  createFeatureSelector<State>('raven'),
  (state: State): SituationalAwarenessState => state.situationalAwareness,
);

export const getNowMinus = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.nowMinus,
);

export const getNowPlus = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.nowPlus,
);

export const getPageDuration = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.pageDuration,
);

export const getPefEntries = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.pefEntries,
);

export const getSituationalAware = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.situationalAware,
);

export const getStartTime = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.startTime,
);

export const getUseNow = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.useNow,
);

export const getSituationalAwarenessPending = createSelector(
  getSituationalAwarenessState,
  (state: SituationalAwarenessState) => state.fetchPending,
);
