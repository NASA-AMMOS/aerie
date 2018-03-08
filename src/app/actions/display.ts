/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

import {
  RavenSource,
} from './../shared/models';

// Action Types.
export enum DisplayActionTypes {
  StateDelete        = '[display] state_delete',
  StateDeleteFailure = '[display] state_delete_failure',
  StateDeleteSuccess = '[display] state_delete_success',
  StateLoad          = '[display] state_load',
  StateLoadFailure   = '[display] state_load_failure',
  StateLoadSuccess   = '[display] state_load_success',
  StateSave          = '[display] state_save',
  StateSaveFailure   = '[display] state_save_failure',
  StateSaveSuccess   = '[display] state_save_success',
}

// Actions.
export class StateDelete implements Action {
  readonly type = DisplayActionTypes.StateDelete;

  constructor(public source: RavenSource) {}
}

export class StateDeleteFailure implements Action {
  readonly type = DisplayActionTypes.StateDeleteFailure;

  constructor() {}
}

export class StateDeleteSuccess implements Action {
  readonly type = DisplayActionTypes.StateDeleteSuccess;

  constructor() {}
}

export class StateLoad implements Action {
  readonly type = DisplayActionTypes.StateLoad;

  constructor(public source: RavenSource) {}
}

export class StateLoadFailure implements Action {
  readonly type = DisplayActionTypes.StateLoadFailure;

  constructor() {}
}

export class StateLoadSuccess implements Action {
  readonly type = DisplayActionTypes.StateLoadSuccess;

  constructor() {}
}

export class StateSave implements Action {
  readonly type = DisplayActionTypes.StateSave;

  constructor(public name: string, public source: RavenSource) {}
}

export class StateSaveFailure implements Action {
  readonly type = DisplayActionTypes.StateSaveFailure;

  constructor() {}
}

export class StateSaveSuccess implements Action {
  readonly type = DisplayActionTypes.StateSaveSuccess;

  constructor() {}
}

// Union type of all actions.
export type DisplayAction =
  StateDelete |
  StateDeleteFailure |
  StateDeleteSuccess |
  StateLoad |
  StateLoadFailure |
  StateLoadSuccess |
  StateSave |
  StateSaveFailure |
  StateSaveSuccess;
