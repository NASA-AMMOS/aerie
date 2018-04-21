/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  createFeatureSelector,
  createSelector,
} from '@ngrx/store';

import {
  ConfigAction,
  ConfigActionTypes,
  UpdateDefaultBandSettings,
} from './../actions/config';

import { environment } from './../../environments/environment';

import {
  RavenDefaultBandSettings,
} from './../shared/models';

// Config State Interface.
export interface ConfigState {
  baseSocketUrl: string;
  baseSourcesUrl: string;
  baseUrl: string;
  defaultBandSettings: RavenDefaultBandSettings;
  epochsUrl: string;
  itarMessage: string;
  production: boolean;
}

// Config State.
export const initialState: ConfigState = environment;

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: ConfigState = initialState, action: ConfigAction): ConfigState {
  switch (action.type) {
    case ConfigActionTypes.UpdateDefaultBandSettings:
      return updateDefaultBandSettings(state, action);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'UpdateDefaultBandSettings' action.
 */
export function updateDefaultBandSettings(state: ConfigState, action: UpdateDefaultBandSettings): ConfigState {
  return {
    ...state,
    defaultBandSettings: {
      ...state.defaultBandSettings,
      ...action.update,
    },
  };
}

/**
 * Config state selector helper.
 */
export const getConfigState = createFeatureSelector<ConfigState>('config');

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
export const getDefaultBandSettings = createSelector(getConfigState, (state: ConfigState) => state.defaultBandSettings);
export const getItarMessage = createSelector(getConfigState, (state: ConfigState) => state.itarMessage);
