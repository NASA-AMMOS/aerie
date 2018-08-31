/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { config, ConfigState } from '../../../config';
import {
  ConfigAction,
  ConfigActionTypes,
  UpdateDefaultBandSettings,
} from '../actions/config';

// Config State Interface.
// Defined in the top-level `config.ts` file.

// Config State.
export const initialState: ConfigState = config;

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: ConfigState = initialState,
  action: ConfigAction
): ConfigState {
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
export function updateDefaultBandSettings(
  state: ConfigState,
  action: UpdateDefaultBandSettings
): ConfigState {
  return {
    ...state,
    raven: {
      ...state.raven,
      defaultBandSettings: {
        ...state.raven.defaultBandSettings,
        ...action.update,
      },
    },
  };
}

/**
 * Feature selectors.
 */
export const getConfigState = createFeatureSelector('config');

/**
 * Selection Helpers.
 */
export const getDefaultBandSettings = createSelector(
  getConfigState,
  (state: ConfigState) => state.raven.defaultBandSettings
);
export const getItarMessage = createSelector(
  getConfigState,
  (state: ConfigState) => state.raven.itarMessage
);
export const getVersion = createSelector(
  getConfigState,
  (state: ConfigState) => ({
    branch: state.app.branch,
    commit: state.app.commit,
    version: state.app.version,
  })
);
export const getUrls = createSelector(getConfigState, (state: ConfigState) => ({
  apiUrl: state.mpsServer.apiUrl,
  baseUrl: state.app.baseUrl,
  epochsUrl: state.mpsServer.epochsUrl,
  ravenUrl: state.mpsServer.ravenUrl,
  socketUrl: state.mpsServer.socketUrl,
}));
