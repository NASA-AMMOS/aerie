/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createSelector, createFeatureSelector } from '@ngrx/store';
import { LayoutActionTypes, LayoutActions } from '../actions/layout';

// Layout Interface.
export interface LayoutState {
  showLeftDrawer: boolean;
}

// Layout State.
const initialState: LayoutState = {
  showLeftDrawer: true,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: LayoutState = initialState, action: LayoutActions): LayoutState {
  switch (action.type) {
    case LayoutActionTypes.ToggleLeftDrawer:
      return { showLeftDrawer: !state.showLeftDrawer };
    default:
      return state;
  }
}

/**
 * Layout state selector helper.
 */
export const getLayoutState = createFeatureSelector<LayoutState>('layout');

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
export const getShowLeftDrawer = createSelector(getLayoutState, (state: LayoutState) => state.showLeftDrawer);
