/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

// Action Types.
export enum TimelineActionTypes {
  SelectBand =                    '[timeline] select_band',
  SettingsUpdateGlobal =          '[timeline] settings_update_global',
  SettingsUpdateSelectedBand =    '[timeline] settings_update_selected_band',
  SettingsUpdateSelectedSubBand = '[timeline] settings_update_selected_sub_band', // TODO.
}

// Actions.
export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
}

export class SettingsUpdateGlobal implements Action {
  readonly type = TimelineActionTypes.SettingsUpdateGlobal;

  constructor(public prop: string, public value: string | number | boolean) {}
}

export class SettingsUpdateSelectedBand implements Action {
  readonly type = TimelineActionTypes.SettingsUpdateSelectedBand;

  constructor(public prop: string, public value: string | number | boolean) {}
}

export class SettingsUpdateSelectedSubBand implements Action {
  readonly type = TimelineActionTypes.SettingsUpdateSelectedSubBand;
}

// Union type of all actions.
export type TimelineAction =
  SelectBand |
  SettingsUpdateGlobal |
  SettingsUpdateSelectedBand |
  SettingsUpdateSelectedSubBand;
