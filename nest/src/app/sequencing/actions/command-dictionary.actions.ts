/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { CommandDictionary, MpsCommand } from '../../shared/models';

export enum CommandDictionaryActionTypes {
  FetchCommandDictionaries = '[sequencing-command_dictionary] fetch_command_dictionaries',
  FetchCommandDictionariesFailure = '[sequencing-command_dictionary] fetch_command_dictionaries_failure',
  FetchCommandDictionariesSuccess = '[sequencing-command_dictionary] fetch_command_dictionaries_success',
  FetchCommandDictionary = '[sequencing-command_dictionary] fetch_command_dictionary',
  FetchCommandDictionaryFailure = '[sequencing-command_dictionary] fetch_command_dictionary_failure',
  FetchCommandDictionarySuccess = '[sequencing-command_dictionary] fetch_command_dictionary_success',
  SelectCommandDictionary = '[sequencing-command_dictionary] select_command_dictionary',
}

export class FetchCommandDictionaries implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionaries;
}

export class FetchCommandDictionariesFailure implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionariesFailure;
  constructor(public error: Error) {}
}

export class FetchCommandDictionariesSuccess implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionariesSuccess;
  constructor(public data: CommandDictionary[]) {}
}

export class FetchCommandDictionary implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionary;
  constructor(public name: string) {}
}

export class FetchCommandDictionaryFailure implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionaryFailure;
  constructor(public error: Error) {}
}

export class FetchCommandDictionarySuccess implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionarySuccess;
  constructor(public data: MpsCommand[]) {}
}

export class SelectCommandDictionary implements Action {
  readonly type = CommandDictionaryActionTypes.SelectCommandDictionary;
  constructor(public selectedId: string) {}
}

export type CommandDictionaryActions =
  | FetchCommandDictionaries
  | FetchCommandDictionariesFailure
  | FetchCommandDictionariesSuccess
  | FetchCommandDictionary
  | FetchCommandDictionaryFailure
  | FetchCommandDictionarySuccess
  | SelectCommandDictionary;
