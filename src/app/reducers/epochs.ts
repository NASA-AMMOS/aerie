import { EpochsAction, EpochsActionTypes } from '../actions/epochs';
import { RavenEpoch } from '../shared/models/raven-epoch';

import { createFeatureSelector } from '@ngrx/store';

export interface EpochsState {
  epochs: RavenEpoch[];
  inUseEpoch: RavenEpoch | null;
}

export const initialState: EpochsState = {
  epochs: [],
  inUseEpoch: null,
};

export function reducer(state: EpochsState = initialState, action: EpochsAction): EpochsState {
  switch (action.type) {
    case EpochsActionTypes.AddEpochs:
      return { ...state, epochs: state.epochs.concat(action.epochs) };
    case EpochsActionTypes.SelectEpoch:
      console.log('in reducer action.epoch:' + JSON.stringify(action.epoch));
      return { ...state, inUseEpoch: action.epoch };
    default:
      return state;
  }
}

export const getEpochsState = createFeatureSelector<EpochsState>('epochs');
