/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import keyBy from 'lodash-es/keyBy';
import { CommandDictionary, MpsCommand, StringTMap } from '../../shared/models';
import { CommandDictionaryActions } from '../actions';

export interface CommandDictionaryState {
  commandsByName: StringTMap<MpsCommand> | null;
  dictionaries: CommandDictionary[];
  selectedDictionaryId: string | null;
}

export const initialState: CommandDictionaryState = {
  commandsByName: null,
  dictionaries: [],
  selectedDictionaryId: null,
};

export const reducer = createReducer(
  initialState,
  on(
    CommandDictionaryActions.fetchCommandDictionarySuccess,
    (state, { data }) => ({
      ...state,
      commandsByName: keyBy(data, 'name'),
    }),
  ),
  on(
    CommandDictionaryActions.fetchCommandDictionariesSuccess,
    (state, { data }) => ({
      ...state,
      dictionaries: data,
    }),
  ),
  on(
    CommandDictionaryActions.selectCommandDictionary,
    (state, { selectedId }) => ({
      ...state,
      selectedDictionaryId: selectedId,
    }),
  ),
);
