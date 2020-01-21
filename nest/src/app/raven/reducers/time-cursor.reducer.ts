/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import { TimeCursorActions } from '../actions';

export interface TimeCursorState {
  autoPage: boolean;
  clockRate: number;
  clockUpdateIntervalInSecs: number;
  currentTimeDelta: number;
  cursorColor: string;
  cursorTime: number | null;
  cursorWidth: number;
  followTimeCursor: boolean;
  setCursorTime: number | null;
  showTimeCursor: boolean;
}

export const initialState: TimeCursorState = {
  autoPage: false,
  clockRate: 1,
  clockUpdateIntervalInSecs: 5,
  currentTimeDelta: 0,
  cursorColor: '#ff0000',
  cursorTime: null,
  cursorWidth: 1,
  followTimeCursor: true,
  setCursorTime: null,
  showTimeCursor: false,
};

export const reducer = createReducer(
  initialState,
  on(TimeCursorActions.hideTimeCursor, state => ({
    ...state,
    cursorTime: null,
    showTimeCursor: false,
  })),
  on(TimeCursorActions.showTimeCursor, state => ({
    ...state,
    cursorTime: state.setCursorTime
      ? state.setCursorTime
      : Date.now() / 1000 + state.currentTimeDelta,
    showTimeCursor: true,
  })),
  on(TimeCursorActions.updateTimeCursorSettings, (state, { update }) => ({
    ...state,
    ...update,
  })),
);
