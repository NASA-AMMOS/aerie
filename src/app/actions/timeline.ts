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
  RavenBand,
  RavenPoint,
  StringTMap,
} from './../models/';

// Action Types.
export enum TimelineActionTypes {
  AddBands =                      '[timeline] add_bands',
  RemoveBands =                   '[timeline] remove_bands',
  AddPointsToBands =              '[timeline] add_points_to_bands',
  RemovePointsFromBands =         '[timeline] remove_points_from_bands',
  SelectBand =                    '[timeline] select_band',
  SettingsUpdateGlobal =          '[timeline] settings_update_global',
  SettingsUpdateSelectedBand =    '[timeline] settings_update_selected_band',
  SettingsUpdateSelectedSubBand = '[timeline] settings_update_selected_sub_band', // TODO.
}

// Actions.
export class AddBands implements Action {
  readonly type = TimelineActionTypes.AddBands;

  constructor(public sourceId: string, public bands: RavenBand[]) {}
}

export class RemoveBands implements Action {
  readonly type = TimelineActionTypes.RemoveBands;

  constructor(public sourceId: string, public bandIds: string[]) {}
}

export class AddPointsToBands implements Action {
  readonly type = TimelineActionTypes.AddPointsToBands;

  constructor(public sourceId: string, public bandIdsToPoints: StringTMap<RavenPoint[]>) {}
}

export class RemovePointsFromBands implements Action {
  readonly type = TimelineActionTypes.RemovePointsFromBands;

  constructor(public sourceId: string, public bandIds: string[]) {}
}

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
  AddBands |
  RemoveBands |
  AddPointsToBands |
  RemovePointsFromBands |
  SelectBand |
  SettingsUpdateGlobal |
  SettingsUpdateSelectedBand |
  SettingsUpdateSelectedSubBand;
