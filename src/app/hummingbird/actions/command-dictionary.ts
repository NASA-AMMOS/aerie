/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

export enum CommandDictionaryActionTypes {
  FetchCommandDictionary = '[command_dictionary] fetch_command_dictionary',
  FetchCommandDictionaryFailure = '[command_dictionary] fetch_command_dictionary_failure',
  FetchCommandDictionarySuccess = '[command_dictionary] fetch_command_dictionary_success',
  FetchCommandDictionaryList = '[command_dictionary] fetch_command_list',
  FetchCommandDictionaryListFailure = '[command_dictionary] fetch_command_list_failure',
  FetchCommandDictionaryListSuccess = '[command_dictionary] fetch_command_list_success',
  SelectCommandDictionary = '[command_dictionary] select_command_dictionary',
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

  constructor(public data: any) {}
}

export class FetchCommandDictionaryList implements Action {
  readonly type = CommandDictionaryActionTypes.FetchCommandDictionaryList;
}

export class FetchCommandDictionaryListFailure implements Action {
  readonly type =
    CommandDictionaryActionTypes.FetchCommandDictionaryListFailure;

  constructor(public error: Error) {}
}

export class FetchCommandDictionaryListSuccess implements Action {
  readonly type =
    CommandDictionaryActionTypes.FetchCommandDictionaryListSuccess;

  constructor(public data: any) {}
}

export class SelectCommandDictionary implements Action {
  readonly type = CommandDictionaryActionTypes.SelectCommandDictionary;

  constructor(public selectedId: string) {}
}

export type CommandDictionaryAction =
  | FetchCommandDictionary
  | FetchCommandDictionaryFailure
  | FetchCommandDictionarySuccess
  | FetchCommandDictionaryList
  | FetchCommandDictionaryListFailure
  | FetchCommandDictionaryListSuccess
  | SelectCommandDictionary;
