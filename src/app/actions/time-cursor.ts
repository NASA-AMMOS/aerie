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
  BaseType,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum TimeCursorActionTypes {
  HideTimeCursor           = '[timeCursor] hide_time_cursor',
  ShowTimeCursor           = '[timeCursor] show_time_cursor',
  UpdateTimeCursorSettings = '[timeCursor] update_time_cursor_settings',
}

// Actions.
export class HideTimeCursor implements Action {
  readonly type = TimeCursorActionTypes.HideTimeCursor;
}

export class ShowTimeCursor implements Action {
  readonly type = TimeCursorActionTypes.ShowTimeCursor;
}

export class UpdateTimeCursorSettings implements Action {
  readonly type = TimeCursorActionTypes.UpdateTimeCursorSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type TimeCursorAction =
  HideTimeCursor |
  ShowTimeCursor |
  UpdateTimeCursorSettings;
