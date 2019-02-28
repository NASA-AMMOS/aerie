/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Command, CommandDictionary } from '../../../../../schemas/types/ts';
import { StringTMap } from '../../shared/models';
import {
  CommandDictionaryAction,
  CommandDictionaryActionTypes,
  FetchCommandDictionarySuccess,
} from '../actions/command-dictionary.actions';
import { keyCommandsByName } from '../util';

export interface CommandDictionaryState {
  commandsByName: StringTMap<Command> | null;
  dictionaries: CommandDictionary[];
  selectedDictionaryId: string | null;
}

export const initialState: CommandDictionaryState = {
  commandsByName: null,
  dictionaries: [],
  selectedDictionaryId: null,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: CommandDictionaryState = initialState,
  action: CommandDictionaryAction,
): CommandDictionaryState {
  switch (action.type) {
    case CommandDictionaryActionTypes.FetchCommandDictionarySuccess:
      return fetchCommandDictionarySuccess(state, action);
    case CommandDictionaryActionTypes.FetchCommandDictionaryListSuccess:
      return { ...state, dictionaries: action.data };
    case CommandDictionaryActionTypes.SelectCommandDictionary:
      return { ...state, selectedDictionaryId: action.selectedId };
    default:
      return state;
  }
}

/**
 * Reduction helper. Called when reducing the `FetchCommandDictionarySuccess` action.
 */
function fetchCommandDictionarySuccess(
  state: CommandDictionaryState,
  action: FetchCommandDictionarySuccess,
) {
  return {
    ...state,
    commandsByName: keyCommandsByName(action.data),
  };
}
