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
  DisplayAction,
  DisplayActionTypes,
} from './../actions/display';

// Display State Interface.
export interface DisplayState {
  stateLoadPending: boolean;
  stateSavePending: boolean;
}

// Display Initial State.
export const initialState: DisplayState = {
  stateLoadPending: false,
  stateSavePending: false,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: DisplayState = initialState, action: DisplayAction): DisplayState {
  switch (action.type) {
    case DisplayActionTypes.StateLoad:
      return { ...state, stateLoadPending: true };
    case DisplayActionTypes.StateLoadFailure:
    case DisplayActionTypes.StateLoadSuccess:
      return { ...state, stateLoadPending: false };
    case DisplayActionTypes.StateSave:
      return { ...state, stateSavePending: true };
    case DisplayActionTypes.StateSaveFailure:
    case DisplayActionTypes.StateSaveSuccess:
      return { ...state, stateSavePending: false };
    default:
      return state;
  }
}

/**
 * Display state selector helper.
 */
export const getDisplayState = createFeatureSelector<DisplayState>('display');

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
export const getPending = createSelector(getDisplayState, (state: DisplayState) => state.stateLoadPending || state.stateSavePending);
