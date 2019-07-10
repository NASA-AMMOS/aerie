/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  EpochsAction,
  EpochsActionTypes,
  UpdateEpochData,
} from '../actions/epochs.actions';
import { RavenEpoch } from '../models/raven-epoch';

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

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: EpochsState = initialState,
  action: EpochsAction,
): EpochsState {
  switch (action.type) {
    case EpochsActionTypes.AddEpochs:
      return addEpochs(state, action.epochs);
    case EpochsActionTypes.AppendAndReplaceEpochs:
      return appendAndReplaceEpochs(state, action.epochs);
    case EpochsActionTypes.RemoveEpochs:
      return removeEpochs(state, action.epochs);
    case EpochsActionTypes.SetInUseEpochByName:
      return setInUseEpochByName(state, action.epochName);
    case EpochsActionTypes.UpdateEpochSetting:
      return { ...state, ...action.update };
    case EpochsActionTypes.UpdateEpochData:
      return updateEpochData(state, action);
    default:
      return state;
  }
}

export function addEpochs(
  state: EpochsState,
  epochs: RavenEpoch[],
): EpochsState {
  return {
    ...state,
    epochs,
    inUseEpoch: getInUseEpochs(epochs),
    modified: state.epochs.length === 0 ? state.modified : true,
  };
}

export function appendAndReplaceEpochs(
  state: EpochsState,
  newEpochs: RavenEpoch[],
) {
  let epochs: RavenEpoch[] = [];
  const existingEpochs = state.epochs;
  if (existingEpochs.length === 0) {
    epochs = newEpochs;
  } else {
    epochs = existingEpochs.slice(0);
    const existingEpochNames = existingEpochs.map(epoch => epoch.name);
    newEpochs.forEach(epoch => {
      if (existingEpochNames.includes(epoch.name)) {
        // replace
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
}

export function removeEpochs(
  state: EpochsState,
  epochs: RavenEpoch[],
): EpochsState {
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
}

export function updateEpochData(
  state: EpochsState,
  action: UpdateEpochData,
): EpochsState {
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
}

function getInUseEpochs(epochs: RavenEpoch[]): RavenEpoch | null {
  const selectedEpochs = epochs.filter(epoch => epoch.selected);
  if (selectedEpochs.length > 0) {
    return selectedEpochs[0];
  } else {
    return null;
  }
}

export function setInUseEpochByName(
  state: EpochsState,
  epochName: string,
): EpochsState {
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
}
