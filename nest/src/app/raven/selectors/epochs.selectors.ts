/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RavenEpoch } from '../../shared/models';
import { State } from '../raven-store';
import { EpochsState } from '../reducers/epochs.reducer';

const featureSelector = createFeatureSelector<State>('raven');
export const getEpochsState = createSelector(
  featureSelector,
  (state: State): EpochsState => state.epochs,
);

export const getDayCode = createSelector(
  getEpochsState,
  (state: EpochsState): string => state.dayCode,
);

export const getEarthSecToEpochSec = createSelector(
  getEpochsState,
  (state: EpochsState): number => state.earthSecToEpochSec,
);

export const getEpochs = createSelector(
  getEpochsState,
  (state: EpochsState): RavenEpoch[] => state.epochs,
);

export const getInUseEpochs = createSelector(
  getEpochsState,
  (state: EpochsState): RavenEpoch | null => state.inUseEpoch,
);
