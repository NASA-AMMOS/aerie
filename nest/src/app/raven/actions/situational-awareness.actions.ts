/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { BaseType, StringTMap } from './../../shared/models';

// Action Types.
export enum SituationalAwarenessActionTypes {
  ChangeSituationalAwareness = '[situationalAwareness] change_situational_awareness',
  FetchPefEntries = '[situationalAwareness] fetch_pef_entries',
  UpdateSituationalAwarenessSettings = '[situationalAwareness] update_situational_awareness_settings',
}

// Actions.
export class ChangeSituationalAwareness implements Action {
  readonly type = SituationalAwarenessActionTypes.ChangeSituationalAwareness;

  constructor(public url: string, public situAware: boolean) {}
}

export class FetchPefEntries implements Action {
  readonly type = SituationalAwarenessActionTypes.FetchPefEntries;

  constructor(public url: string) {}
}

export class UpdateSituationalAwarenessSettings implements Action {
  readonly type =
    SituationalAwarenessActionTypes.UpdateSituationalAwarenessSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type SituationalAwarenessAction =
  | ChangeSituationalAwareness
  | FetchPefEntries
  | UpdateSituationalAwarenessSettings;
