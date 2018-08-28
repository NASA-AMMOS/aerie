/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { HBCommandDictionary } from '../../shared/models/hb-command-dictionary';
import { CommandDictionaryAction, CommandDictionaryActionTypes } from '../actions/command-dictionary';
import { State } from '../hummingbird-store';

/**
 * Schema for the command dictionary state
 */
export interface CommandDictionaryState {
  /**
   * List of available command dictionaries
   */
  list: HBCommandDictionary[];
  /**
   * The currently selected command dictionary
   */
  selected: HBCommandDictionary | null;
}

export const initialState: CommandDictionaryState = {
  list: [],
  selected: null,
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
    case CommandDictionaryActionTypes.FetchCommandDictionaryListSuccess:
      return { ...state, list: action.data };
    case CommandDictionaryActionTypes.SelectCommandDictionary:
      return { ...state, selected: { ...action.dictionary } };
    default:
      return state;
  }

}

/**
 * State selector helper
 */
export const getCommandDictionaryState = createSelector(
  createFeatureSelector<State>('hummingbird'),
  (state: State): CommandDictionaryState => state.commandDictionary,
);
