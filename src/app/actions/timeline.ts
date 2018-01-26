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
  RavenSortMessage,
  RavenTimeRange,
  StringTMap,
} from './../models';

// Action Types.
export enum TimelineActionTypes {
  SelectBand             = '[timeline] select_band',
  SettingsUpdateAllBands = '[timeline] settings_update_all_bands',
  SettingsUpdateBand     = '[timeline] settings_update_band',
  SortBands              = '[timeline] sort_bands',
  UpdateViewTimeRange    = '[timeline] update_view_time_range',
}

// Actions.
export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
}

export class SettingsUpdateAllBands implements Action {
  readonly type = TimelineActionTypes.SettingsUpdateAllBands;

  constructor(public prop: string, public value: any) {}
}

export class SettingsUpdateBand implements Action {
  readonly type = TimelineActionTypes.SettingsUpdateBand;

  constructor(public prop: string, public value: any) {}
}

export class SortBands implements Action {
  readonly type = TimelineActionTypes.SortBands;

  constructor(public sort: StringTMap<RavenSortMessage>) {}
}

export class UpdateViewTimeRange implements Action {
  readonly type = TimelineActionTypes.UpdateViewTimeRange;

  constructor(public viewTimeRange: RavenTimeRange) {}
}

// Union type of all actions.
export type TimelineAction =
  SelectBand |
  SettingsUpdateAllBands |
  SettingsUpdateBand |
  SortBands |
  UpdateViewTimeRange;
