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
  RavenSortMessage,
  RavenTimeRange,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum TimelineActionTypes {
  SelectBand          = '[timeline] select_band',
  SelectDataPoint         = '[timeline] select_data_point',
  SortBands           = '[timeline] sort_bands',
  UpdateBand          = '[timeline] update_band',
  UpdateSubBand       = '[timeline] update_sub_band',
  UpdateTimeline      = '[timeline] update_timeline',
  UpdateViewTimeRange = '[timeline] update_view_time_range',
}

// Actions.
export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
}

export class SelectDataPoint implements Action {
  readonly type = TimelineActionTypes.SelectDataPoint;

  constructor(public interval: any, public bandId: string) {}
}
export class SortBands implements Action {
  readonly type = TimelineActionTypes.SortBands;

  constructor(public sort: StringTMap<RavenSortMessage>) {}
}

export class UpdateBand implements Action {
  readonly type = TimelineActionTypes.UpdateBand;

  constructor(public bandId: string, public update: StringTMap<BaseType>) {}
}

export class UpdateSubBand implements Action {
  readonly type = TimelineActionTypes.UpdateSubBand;

  constructor(public bandId: string, public subBandId: string, public update: StringTMap<BaseType>) {}
}

export class UpdateTimeline implements Action {
  readonly type = TimelineActionTypes.UpdateTimeline;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateViewTimeRange implements Action {
  readonly type = TimelineActionTypes.UpdateViewTimeRange;

  constructor(public viewTimeRange: RavenTimeRange) {}
}

// Union type of all actions.
export type TimelineAction =
  SelectBand |
  SelectDataPoint |
  SortBands |
  UpdateBand |
  UpdateSubBand |
  UpdateTimeline |
  UpdateViewTimeRange;
