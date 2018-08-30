/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { StringTMap } from '../../shared/models';
import { OutputAction, OutputActionTypes } from '../actions/output';
import { State } from '../raven-store';

// Output State Interface.
export interface OutputState {
  allInOneFile: boolean;
  allInOneFilename: string;
  decimateOutputData: boolean;
  outputData: string;
  outputFormat: string;
  outputSourceIdsByLabel: StringTMap<string[]>;
}

// Output Initial State.
export const initialState: OutputState = {
  allInOneFile: false,
  allInOneFilename: '',
  decimateOutputData: true,
  outputData: '',
  outputFormat: 'CSV',
  outputSourceIdsByLabel: {},
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: OutputState = initialState, action: OutputAction): OutputState {
  switch (action.type) {
    case OutputActionTypes.UpdateOutputSettings:
      return { ...state, ...action.update };
    case OutputActionTypes.AppendData:
      return { ...state, outputData: state.outputData.concat(action.data) };
    default:
      return state;
  }
}

/**
 * Output state selector helper.
 */
const featureSelector = createFeatureSelector<State>('raven');
export const getOutputState = createSelector(
  featureSelector,
  (state: State): OutputState => state.output,
);
