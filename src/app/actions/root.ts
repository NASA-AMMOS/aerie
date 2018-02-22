/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { AppState } from './../store';

// Action Types.
export enum RootActionTypes {
  ResetState           = '[root] reset_state',
  ResetStateFromObject = '[root] reset_state_from_object',
}

// Actions.
export class ResetState implements Action {
  readonly type = RootActionTypes.ResetState;
}

export class ResetStateFromObject implements Action {
  readonly type = RootActionTypes.ResetStateFromObject;

  constructor(public state: AppState) {}
}

// Union type of all actions.
export type RootAction =
  ResetState |
  ResetStateFromObject;
