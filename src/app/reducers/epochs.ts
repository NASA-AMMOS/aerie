import {
  EpochsAction,
  EpochsActionTypes,
} from '../actions/epochs';

import { RavenEpoch } from '../shared/models/raven-epoch';

import { createFeatureSelector } from '@ngrx/store';

export interface EpochsState {
  dayCode: string;
  earthSecToEpochSec: number;
  epochs: RavenEpoch[];
  inUseEpoch: RavenEpoch | null;
}

export const initialState: EpochsState = {
  dayCode: '',
  earthSecToEpochSec: 1,
  epochs: [],
  inUseEpoch: null,
};

export function reducer(state: EpochsState = initialState, action: EpochsAction): EpochsState {
  switch (action.type) {
    case EpochsActionTypes.AddEpochs:
      return { ...state, epochs: state.epochs.concat(action.epochs) };
    case EpochsActionTypes.ChangeDayCode:
      return { ...state, dayCode: action.code };
    case EpochsActionTypes.ChangeEarthSecToEpochSec:
      return { ...state, earthSecToEpochSec: action.earthSecToEpochSec };
    case EpochsActionTypes.SelectEpoch:
      return { ...state, inUseEpoch: action.epoch };
    default:
      return state;
  }
}

export const getEpochsState = createFeatureSelector<EpochsState>('epochs');
