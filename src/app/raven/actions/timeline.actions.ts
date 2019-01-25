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
  AddBandModifiers,
  BaseType,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPin,
  RavenSortMessage,
  RavenSubBand,
  RavenTimeRange,
  StringTMap,
} from '../../shared/models';

// Action Types.
export enum TimelineActionTypes {
  AddBand = '[timeline] add_band',
  AddGuide = '[timeline] add_guide',
  AddPointsToSubBand = '[timeline] add_points_to_sub_band',
  AddSubBand = '[timeline] add_sub_band',
  ExpandChildrenOrDescendants = '[timeline] expand_children_or_descendants',
  FetchChildrenOrDescendants = '[timeline] fetch_children_or_descendants',
  FetchChildrenOrDescendantsSuccess = '[timeline] fetch_children_or_descendants_success',
  PanLeftViewTimeRange = '[timeline] pan_left_view_time_range',
  PanRightViewTimeRange = '[timeline] pan_right_view_time_range',
  PinAdd = '[timeline] pin_add',
  PinRemove = '[timeline] pin_remove',
  PinRename = '[timeline] pin_rename',
  RemoveAllBands = '[timeline] remove_all_bands',
  RemoveAllGuides = '[timeline] remove_all_guides',
  RemoveAllPointsInSubBandWithParentSource = '[timeline] remove_all_points_in_sub_band_with_parent_source',
  RemoveBandsOrPointsForSource = '[timeline] remove_bands_or_points_for_source',
  RemoveBandsWithNoPoints = '[timeline] remove_bands_with_no_points',
  RemoveChildrenOrDescendants = '[timeline] remove_children_or_descendants',
  RemoveGuide = '[timeline] remove_guide',
  RemoveSourceIdFromSubBands = '[timeline] remove_source_from_sub_bands',
  RemoveSubBand = '[timeline] remove_sub_band',
  ResetViewTimeRange = '[timeline] reset_view_time_range',
  SelectBand = '[timeline] select_band',
  SelectPoint = '[timeline] select_point',
  SetPointsForSubBand = '[timeline] set_points_for_sub_band',
  SortBands = '[timeline] sort_bands',
  SourceIdAdd = '[timeline] source_id_add',
  UpdateBand = '[timeline] update_band',
  UpdateLastClickTime = '[timeline] last_click_time',
  UpdateSubBand = '[timeline] update_sub_band',
  UpdateTimeline = '[timeline] update_timeline',
  UpdateViewTimeRange = '[timeline] update_view_time_range',
  ZoomInViewTimeRange = '[timeline] zoom_in_view_time_range',
  ZoomOutViewTimeRange = '[timeline] zoom_out_view_time_range',
}

// Actions.
export class AddBand implements Action {
  readonly type = TimelineActionTypes.AddBand;

  constructor(
    public sourceId: string | null,
    public band: RavenCompositeBand,
    public modifiers: AddBandModifiers = {},
  ) {}
}

export class AddGuide implements Action {
  readonly type = TimelineActionTypes.AddGuide;
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

export class ExpandChildrenOrDescendants implements Action {
  readonly type = TimelineActionTypes.ExpandChildrenOrDescendants;

  constructor(
    public bandId: string,
    public subBandId: string,
    public activityPoint: RavenActivityPoint,
    public expandType: string,
  ) {}
}

export class FetchChildrenOrDescendants implements Action {
  readonly type = TimelineActionTypes.FetchChildrenOrDescendants;

  constructor(
    public bandId: string,
    public subBandId: string,
    public activityPoint: RavenActivityPoint,
    public expandType: string,
  ) {}
}

export class FetchChildrenOrDescendantsSuccess implements Action {
  readonly type = TimelineActionTypes.FetchChildrenOrDescendantsSuccess;
}

export class PanLeftViewTimeRange implements Action {
  readonly type = TimelineActionTypes.PanLeftViewTimeRange;
}

export class PanRightViewTimeRange implements Action {
  readonly type = TimelineActionTypes.PanRightViewTimeRange;
}

export class PinAdd implements Action {
  readonly type = TimelineActionTypes.PinAdd;

  constructor(public pin: RavenPin) {}
}

export class PinRemove implements Action {
  readonly type = TimelineActionTypes.PinRemove;

  constructor(public sourceId: string) {}
}

export class PinRename implements Action {
  readonly type = TimelineActionTypes.PinRename;

  constructor(public sourceId: string, public newName: string) {}
}

export class RemoveAllBands implements Action {
  readonly type = TimelineActionTypes.RemoveAllBands;
}

export class RemoveAllGuides implements Action {
  readonly type = TimelineActionTypes.RemoveAllGuides;
}

export class RemoveAllPointsInSubBandWithParentSource implements Action {
  readonly type = TimelineActionTypes.RemoveAllPointsInSubBandWithParentSource;

  constructor(public parentSourceId: string) {}
}

export class RemoveBandsOrPointsForSource implements Action {
  readonly type = TimelineActionTypes.RemoveBandsOrPointsForSource;

  constructor(public sourceId: string) {}
}

export class RemoveBandsWithNoPoints implements Action {
  readonly type = TimelineActionTypes.RemoveBandsWithNoPoints;
}

export class RemoveChildrenOrDescendants implements Action {
  readonly type = TimelineActionTypes.RemoveChildrenOrDescendants;

  constructor(
    public bandId: string,
    public subBandId: string,
    public activityPoint: RavenActivityPoint,
  ) {}
}

export class RemoveGuide implements Action {
  readonly type = TimelineActionTypes.RemoveGuide;
}

export class RemoveSourceIdFromSubBands implements Action {
  readonly type = TimelineActionTypes.RemoveSourceIdFromSubBands;

  constructor(public sourceId: string) {}
}

export class RemoveSubBand implements Action {
  readonly type = TimelineActionTypes.RemoveSubBand;

  constructor(public subBandId: string) {}
}

export class ResetViewTimeRange implements Action {
  readonly type = TimelineActionTypes.ResetViewTimeRange;
}

export class SelectBand implements Action {
  readonly type = TimelineActionTypes.SelectBand;

  constructor(public bandId: string) {}
}

export class SelectPoint implements Action {
  readonly type = TimelineActionTypes.SelectPoint;

  constructor(
    public bandId: string,
    public subBandId: string,
    public pointId: string,
  ) {}
}

export class SetPointsForSubBand implements Action {
  readonly type = TimelineActionTypes.SetPointsForSubBand;

  constructor(
    public bandId: string,
    public subBandId: string,
    public points: any[],
  ) {}
}

export class SourceIdAdd implements Action {
  readonly type = TimelineActionTypes.SourceIdAdd;

  constructor(public sourceId: string, public subBandId: string) {}
}

export class SortBands implements Action {
  readonly type = TimelineActionTypes.SortBands;

  constructor(public sort: StringTMap<RavenSortMessage>) {}
}

export class UpdateBand implements Action {
  readonly type = TimelineActionTypes.UpdateBand;

  constructor(public bandId: string, public update: StringTMap<BaseType>) {}
}

export class UpdateLastClickTime implements Action {
  readonly type = TimelineActionTypes.UpdateLastClickTime;

  constructor(public time: number) {}
}

export class UpdateSubBand implements Action {
  readonly type = TimelineActionTypes.UpdateSubBand;

  constructor(
    public bandId: string,
    public subBandId: string,
    public update: StringTMap<BaseType>,
  ) {}
}

export class UpdateTimeline implements Action {
  readonly type = TimelineActionTypes.UpdateTimeline;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateViewTimeRange implements Action {
  readonly type = TimelineActionTypes.UpdateViewTimeRange;

  constructor(public viewTimeRange: RavenTimeRange) {}
}

export class ZoomInViewTimeRange implements Action {
  readonly type = TimelineActionTypes.ZoomInViewTimeRange;
}

export class ZoomOutViewTimeRange implements Action {
  readonly type = TimelineActionTypes.ZoomOutViewTimeRange;
}

// Union type of all actions.
export type TimelineAction =
  | AddBand
  | AddGuide
  | AddPointsToSubBand
  | AddSubBand
  | ExpandChildrenOrDescendants
  | FetchChildrenOrDescendants
  | PanLeftViewTimeRange
  | PanRightViewTimeRange
  | PinAdd
  | PinRemove
  | PinRename
  | RemoveAllBands
  | RemoveAllGuides
  | RemoveAllPointsInSubBandWithParentSource
  | RemoveBandsOrPointsForSource
  | RemoveBandsWithNoPoints
  | RemoveChildrenOrDescendants
  | RemoveGuide
  | RemoveSourceIdFromSubBands
  | RemoveSubBand
  | ResetViewTimeRange
  | SelectBand
  | SelectPoint
  | SetPointsForSubBand
  | SortBands
  | SourceIdAdd
  | UpdateBand
  | UpdateLastClickTime
  | UpdateSubBand
  | UpdateTimeline
  | UpdateViewTimeRange
  | ZoomInViewTimeRange
  | ZoomOutViewTimeRange;
