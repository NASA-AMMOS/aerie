/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ConfigState } from '../../../config';

export const getConfigState = createFeatureSelector('config');

export const getBaseUrl = createSelector(
  getConfigState,
  (state: ConfigState) => state.app.baseUrl,
);

export const getDefaultBandSettings = createSelector(
  getConfigState,
  (state: ConfigState) => state.raven.defaultBandSettings,
);

export const getExcludeActivityTypes = createSelector(
  getConfigState,
  (state: ConfigState) => state.raven.excludeActivityTypes,
);

export const getItarMessage = createSelector(
  getConfigState,
  (state: ConfigState) => state.raven.itarMessage,
);

export const getVersion = createSelector(
  getConfigState,
  (state: ConfigState) => ({
    packageJsonVersion: state.app.packageJsonVersion,
    version: state.app.version,
  }),
);

export const getUrls = createSelector(
  getConfigState,
  (state: ConfigState) => ({
    apiUrl: state.mpsServer.apiUrl,
    baseUrl: state.app.baseUrl,
    epochsUrl: state.mpsServer.epochsUrl,
    ravenConfigUrl: state.mpsServer.ravenConfigUrl,
    ravenUrl: state.mpsServer.ravenUrl,
    socketUrl: state.mpsServer.socketUrl,
  }),
);

export const getProjectEpochsUrl = createSelector(
  getConfigState,
  (state: ConfigState) => state.mpsServer.epochsUrl,
);
