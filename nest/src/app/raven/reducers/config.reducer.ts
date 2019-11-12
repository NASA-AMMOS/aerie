/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import { config, ConfigState } from '../../../config';
import { ConfigActions } from '../actions';

export const initialState: ConfigState = config;

export const reducer = createReducer(
  initialState,
  on(ConfigActions.updateDefaultBandSettings, (state, action) => ({
    ...state,
    raven: {
      ...state.raven,
      defaultBandSettings: {
        ...state.raven.defaultBandSettings,
        ...action.update,
      },
    },
  })),
  on(ConfigActions.updateMpsServerSettings, (state, action) => ({
    ...state,
    mpsServer: {
      ...state.mpsServer,
      ...action.update,
    },
  })),
  on(ConfigActions.updateRavenSettings, (state, action) => ({
    ...state,
    raven: {
      ...state.raven,
      ...action.update,
    },
  })),
);
