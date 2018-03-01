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
  SelectBand             = '[timeline] select_band',
  SortBands              = '[timeline] sort_bands',
  StateLoad              = '[timeline] state_load',
  StateLoadFailure       = '[timeline] state_load_failure',
  StateLoadSuccess       = '[timeline] state_load_success',
  StateSave              = '[timeline] state_save',
  StateSaveFailure       = '[timeline] state_save_failure',
  StateSaveSuccess       = '[timeline] state_save_success',
  UpdateBand             = '[timeline] update_band',
  UpdateSubBand          = '[timeline] update_sub_band',
  UpdateTimeline         = '[timeline] update_timeline',
  UpdateViewTimeRange    = '[timeline] update_view_time_range',
}

// Actions.
export class StateLoad implements Action {
  readonly type = TimelineActionTypes.StateLoad;

  constructor() {}
}

export class StateLoadFailure implements Action {
  readonly type = TimelineActionTypes.StateLoadFailure;

  constructor() {}
}

export class StateLoadSuccess implements Action {
  readonly type = TimelineActionTypes.StateLoadSuccess;

  constructor() {}
}

export class StateSave implements Action {
  readonly type = TimelineActionTypes.StateSave;

  constructor() {}
}

export class StateSaveFailure implements Action {
  readonly type = TimelineActionTypes.StateSaveFailure;

  constructor() {}
}

export class StateSaveSuccess implements Action {
  readonly type = TimelineActionTypes.StateSaveSuccess;

  constructor() {}
}

export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
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
  StateLoad |
  StateLoadFailure |
  StateLoadSuccess |
  StateSave |
  StateSaveFailure |
  StateSaveSuccess |
  SortBands |
  UpdateBand |
  UpdateSubBand |
  UpdateTimeline |
  UpdateViewTimeRange;
