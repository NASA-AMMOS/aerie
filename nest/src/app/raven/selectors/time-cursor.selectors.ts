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
import { TimeCursorState } from '../reducers/time-cursor.reducer';

const featureSelector = createFeatureSelector<State>('raven');
export const getTimeCursorState = createSelector(
  featureSelector,
  (state: State): TimeCursorState => state.timeCursor,
);

export const getAutoPage = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.autoPage,
);

export const getClockRate = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.clockRate,
);

export const getCurrentTimeDelta = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.currentTimeDelta,
);

export const getCursorColor = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.cursorColor,
);

export const getCursorTime = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.cursorTime,
);

export const getCursorWidth = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.cursorWidth,
);

export const getFollowTimeCursor = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.followTimeCursor,
);

export const getShowTimeCursor = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.showTimeCursor,
);

export const getSetCursorTime = createSelector(
  getTimeCursorState,
  (state: TimeCursorState) => state.setCursorTime,
);
