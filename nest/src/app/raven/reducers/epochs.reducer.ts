/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import { EpochsActions } from '../actions';
import { RavenEpoch } from '../models/raven-epoch';
import { getInUseEpochs } from '../util';

export interface EpochsState {
  dayCode: string;
  earthSecToEpochSec: number;
  epochs: RavenEpoch[];
  inUseEpoch: RavenEpoch | null;
  modified: boolean;
}

export const initialState: EpochsState = {
  dayCode: 'D',
  earthSecToEpochSec: 1,
  epochs: [],
  inUseEpoch: null,
  modified: false,
};

export const reducer = createReducer(
  initialState,
  on(EpochsActions.addEpochs, (state, { epochs }) => ({
    ...state,
    epochs,
    inUseEpoch: getInUseEpochs(epochs),
    modified: state.epochs.length === 0 ? state.modified : true,
  })),
  on(EpochsActions.appendAndReplaceEpochs, (state, action) => {
    let epochs: RavenEpoch[] = [];
    if (state.epochs.length === 0) {
      epochs = [...action.epochs];
    } else {
      epochs = [...state.epochs];
      const existingEpochNames = state.epochs.map(epoch => epoch.name);
      action.epochs.forEach(epoch => {
        if (existingEpochNames.includes(epoch.name)) {
          epochs = epochs.map(existingEpoch =>
            existingEpoch.name === epoch.name
              ? { ...existingEpoch, value: epoch.value }
              : existingEpoch,
          );
        } else {
          epochs.push(epoch);
        }
      });
    }
    return {
      ...state,
      epochs,
      inUseEpoch: getInUseEpochs(epochs),
      modified: state.epochs.length === 0 ? state.modified : true,
    };
  }),
  on(EpochsActions.removeEpochs, (state, { epochs }) => {
    const epochKeys = epochs.map(epoch => epoch.name);
    const updatedEpochs = state.epochs.filter(epoch => {
      if (!epochKeys.includes(epoch.name)) {
        return epoch;
      } else {
        return null;
      }
    });
    return {
      ...state,
      epochs: updatedEpochs,
      inUseEpoch: getInUseEpochs(updatedEpochs),
      modified: true,
    };
  }),
  on(EpochsActions.setInUseEpochByName, (state, { epochName }) => {
    const updatedEpochs = state.epochs.map(epoch => {
      if (epoch.name === epochName) {
        return { ...epoch, selected: true };
      } else return epoch;
    });
    return {
      ...state,
      epochs: updatedEpochs,
      inUseEpoch: getInUseEpochs(updatedEpochs),
    };
  }),
  on(EpochsActions.updateEpochData, (state, action) => {
    const updatedEpochs = state.epochs.map((epoch, index) => {
      if (index === action.index) {
        return action.data;
      } else return epoch;
    });
    return {
      ...state,
      epochs: updatedEpochs,
      inUseEpoch: getInUseEpochs(updatedEpochs),
      modified: true,
    };
  }),
  on(EpochsActions.updateEpochSetting, (state, { update }) => ({
    ...state,
    ...update,
  })),
);
