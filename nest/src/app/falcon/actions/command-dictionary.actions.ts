/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';

export const fetchCommandDictionaries = createAction(
  '[falcon-command-dictionary] fetch_command_dictionaries',
);

export const fetchCommandDictionariesFailure = createAction(
  '[falcon-command-dictionary] fetch_command_dictionaries_failure',
  props<{ error: Error }>(),
);

export const fetchCommandDictionariesSuccess = createAction(
  '[falcon-command-dictionary] fetch_command_dictionaries_success',
  props<{ data: any[] }>(),
);

export const fetchCommandDictionary = createAction(
  '[falcon-command-dictionary] fetch_command_dictionary',
  props<{ name: string }>(),
);

export const fetchCommandDictionaryFailure = createAction(
  '[falcon-command-dictionary] fetch_command_dictionary_failure',
  props<{ error: Error }>(),
);

export const fetchCommandDictionarySuccess = createAction(
  '[falcon-command-dictionary] fetch_command_dictionary_success',
  props<{ data: any[] }>(),
);

export const selectCommandDictionary = createAction(
  '[falcon-command-dictionary] select_command_dictionary',
  props<{ selectedId: string }>(),
);
