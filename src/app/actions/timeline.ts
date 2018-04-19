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
  RavenCompositeBand,
  RavenSortMessage,
  RavenSubBand,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum TimelineActionTypes {
  AddBand                      = '[timeline] add_band',
  AddPointsToSubBand           = '[timeline] add_points_to_sub_band',
  AddSubBand                   = '[timeline] add_sub_band',
  RemoveBandsOrPointsForSource = '[timeline] remove_bands_or_points_for_source',
  RemoveSubBand                = '[timeline] remove_sub_band',
  SelectBand                   = '[timeline] select_band',
  SelectPoint                  = '[timeline] select_point',
  SortBands                    = '[timeline] sort_bands',
  UpdateBand                   = '[timeline] update_band',
  UpdateDefaultSettings        = '[timeline] update_default_settings',
  UpdateSubBand                = '[timeline] update_sub_band',
  UpdateTimeline               = '[timeline] update_timeline',
}

// Actions.
export class AddBand implements Action {
  readonly type = TimelineActionTypes.AddBand;

  constructor(
    public sourceId: string | null,
    public band: RavenCompositeBand,
  ) {}
}

export class AddPointsToSubBand implements Action {
  readonly type = TimelineActionTypes.AddPointsToSubBand;

  constructor(
    public sourceId: string,
    public bandId: string,
    public subBandId: string,
    public points: any[],
  ) {}
}

export class AddSubBand implements Action {
  readonly type = TimelineActionTypes.AddSubBand;

  constructor(
    public sourceId: string,
    public bandId: string,
    public subBand: RavenSubBand,
  ) {}
}

export class RemoveBandsOrPointsForSource implements Action {
  readonly type = TimelineActionTypes.RemoveBandsOrPointsForSource;

  constructor(public sourceId: string) {}
}

export class RemoveSubBand implements Action {
  readonly type = TimelineActionTypes.RemoveSubBand;

  constructor(public subBandId: string) {}
}

export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
}

export class SelectPoint implements Action {
  readonly type = TimelineActionTypes.SelectPoint;

  constructor(public bandId: string, public subBandId: string, public pointId: string) {}
}

export class SortBands implements Action {
  readonly type = TimelineActionTypes.SortBands;

  constructor(public sort: StringTMap<RavenSortMessage>) {}
}

export class UpdateBand implements Action {
  readonly type = TimelineActionTypes.UpdateBand;

  constructor(public bandId: string, public update: StringTMap<BaseType>) {}
}

export class UpdateDefaultSettings implements Action {
  readonly type = TimelineActionTypes.UpdateDefaultSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateSubBand implements Action {
  readonly type = TimelineActionTypes.UpdateSubBand;

  constructor(public bandId: string, public subBandId: string, public update: StringTMap<BaseType>) {}
}

export class UpdateTimeline implements Action {
  readonly type = TimelineActionTypes.UpdateTimeline;

  constructor(public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type TimelineAction =
  AddBand |
  AddPointsToSubBand |
  AddSubBand |
  RemoveBandsOrPointsForSource |
  RemoveSubBand |
  SelectBand |
  SelectPoint |
  SortBands |
  UpdateBand |
  UpdateDefaultSettings |
  UpdateSubBand |
  UpdateTimeline;
