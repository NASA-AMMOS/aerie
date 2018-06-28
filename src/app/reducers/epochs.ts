/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector } from '@ngrx/store';

import {
  EpochsAction,
  EpochsActionTypes,
} from './../actions/epochs';

import {
  RavenEpoch,
} from './../shared/models/raven-epoch';

// Epoch State Interface.
export interface EpochsState {
  dayCode: string;
  earthSecToEpochSec: number;
  epochPrefix: string;
  epochs: RavenEpoch[];
  inUseEpoch: RavenEpoch | null;
}

// Epoch Initial State.
export const initialState: EpochsState = {
  dayCode: '',
  earthSecToEpochSec: 1,
  epochPrefix: 'JOI',
  epochs: [],
  inUseEpoch: null,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: EpochsState = initialState, action: EpochsAction): EpochsState {
  switch (action.type) {
    case EpochsActionTypes.AddEpochs:
      return { ...state, epochs: state.epochs.concat(action.epochs) };
    case EpochsActionTypes.UpdateEpochs:
      return { ...state, ...action.update };
    default:
      return state;
  }
}

/**
 * Epoch state selector helper.
 */
export const getEpochsState = createFeatureSelector<EpochsState>('epochs');
