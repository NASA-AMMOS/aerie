/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import { OutputActions } from '../actions';
import { StringTMap } from '../models';

export interface OutputState {
  allInOneFile: boolean;
  allInOneFilename: string;
  decimateOutputData: boolean;
  outputData: string;
  outputFormat: string;
  outputSourceIdsByLabel: StringTMap<string[]>;
}

export const initialState: OutputState = {
  allInOneFile: false,
  allInOneFilename: '',
  decimateOutputData: true,
  outputData: '',
  outputFormat: 'CSV',
  outputSourceIdsByLabel: {},
};

export const reducer = createReducer(
  initialState,
  on(OutputActions.appendData, (state, { data }) => ({
    ...state,
    outputData: state.outputData.concat(data),
  })),
  on(OutputActions.updateOutputSettings, (state, { update }) => ({
    ...state,
    ...update,
  })),
);
