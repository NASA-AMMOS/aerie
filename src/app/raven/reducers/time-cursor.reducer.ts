/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import {
  TimeCursorAction,
  TimeCursorActionTypes,
} from '../actions/time-cursor.actions';
import { State } from '../raven-store';

export interface TimeCursorState {
  autoPage: boolean;
  clockRate: number;
  clockUpdateIntervalInSecs: number;
  currentTimeDelta: number;
  cursorColor: string;
  cursorTime: number | null;
  cursorWidth: number;
  setCursorTime: number | null;
  showTimeCursor: boolean;
}

// Time cursor State.
export const initialState: TimeCursorState = {
  autoPage: false,
  clockRate: 1,
  clockUpdateIntervalInSecs: 5,
  currentTimeDelta: 0,
  cursorColor: '#ff0000',
  cursorTime: null,
  cursorWidth: 1,
  setCursorTime: null,
  showTimeCursor: false,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: TimeCursorState = initialState,
  action: TimeCursorAction
): TimeCursorState {
  switch (action.type) {
    case TimeCursorActionTypes.HideTimeCursor:
      return { ...state, cursorTime: null, showTimeCursor: false };
    case TimeCursorActionTypes.ShowTimeCursor:
      return showTimeCursor(state);
    case TimeCursorActionTypes.UpdateTimeCursorSettings:
      return { ...state, ...action.update };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'ShowTimeCursor' action.
 */
export function showTimeCursor(state: TimeCursorState): TimeCursorState {
  return {
    ...state,
    cursorTime: state.setCursorTime
      ? state.setCursorTime
      : Date.now() / 1000 + state.currentTimeDelta,
    showTimeCursor: true,
  };
}

/**
 * Config state selector helper.
 */
const featureSelector = createFeatureSelector<State>('raven');
export const getTimeCursorState = createSelector(
  featureSelector,
  (state: State): TimeCursorState => state.timeCursor
);
