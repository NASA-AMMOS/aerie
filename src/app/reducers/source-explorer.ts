/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createSelector, createFeatureSelector } from '@ngrx/store';
import { SourceExplorerActionTypes, SourceExplorerActions } from '../actions/source-explorer';

// Source Explorer Interface.
export interface SourceExplorerState {
  fetchGraphDataRequestPending: boolean;
  fetchInitialSourcesRequestPending: boolean;
  fetchSourcesRequestPending: boolean;
}

// Source Explorer Initial State.
const initialState: SourceExplorerState = {
  fetchGraphDataRequestPending: false,
  fetchInitialSourcesRequestPending: false,
  fetchSourcesRequestPending: false,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: SourceExplorerState = initialState, action: SourceExplorerActions): SourceExplorerState {
  switch (action.type) {
    case SourceExplorerActionTypes.FetchGraphData:
      return { ...state, fetchGraphDataRequestPending: true };
    case SourceExplorerActionTypes.FetchInitialSources:
      return { ...state, fetchInitialSourcesRequestPending: true };
    case SourceExplorerActionTypes.FetchInitialSourcesFailure:
      return { ...state, fetchInitialSourcesRequestPending: false };
    case SourceExplorerActionTypes.FetchInitialSourcesSuccess:
      return { ...state, fetchInitialSourcesRequestPending: false };
    case SourceExplorerActionTypes.FetchSources:
      return { ...state, fetchSourcesRequestPending: true };
    case SourceExplorerActionTypes.FetchSourcesFailure:
      return { ...state, fetchSourcesRequestPending: false };
    case SourceExplorerActionTypes.FetchSourcesSuccess:
      return { ...state, fetchSourcesRequestPending: false };
    default:
      return state;
  }
}

/**
 * Layout state selector helper.
 */
export const getSourceExplorerState = createFeatureSelector<SourceExplorerState>('source-explorer');

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
export const getFetchGraphDataRequestPending = createSelector(getSourceExplorerState, (state: SourceExplorerState) => state.fetchGraphDataRequestPending);
