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
  ChangeCurrentTimeCursor      = '[timeline] change_current_time_cursor',
  ChangeDateFormat             = '[timeline] change_date_format',
  ChangeDefaultActivityLayout  = '[timeline] change_default_activity_layout',
  ChangeDefaultFillColor       = '[timeline] change_default_fill_color',
  ChangeDefaultIcon            = '[timeline] change_default_icon',
  ChangeDefaultLabelFontSize   = '[timeline] change_default_label_fonr_size',
  ChangeDefaultLabelFont       = '[timeline] change_default_label_font',
  ChangeDefaultResourceColor   = '[timeline] change_default_resource_color',
  ChangeLabelWidth             = '[timeline] change_label_width',
  ChangeTooltip                = '[timeline] change_tooltip',
  RemoveBandsOrPointsForSource = '[timeline] remove_bands_or_points_for_source',
  RemoveSubBand                = '[timeline] remove_sub_band',
  SelectBand                   = '[timeline] select_band',
  SelectPoint                  = '[timeline] select_point',
  SortBands                    = '[timeline] sort_bands',
  UpdateBand                   = '[timeline] update_band',
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

export class ChangeCurrentTimeCursor implements Action {
  readonly type = TimelineActionTypes.ChangeCurrentTimeCursor;

  constructor(public currentTimeCursor: boolean) {}
}

export class ChangeDateFormat implements Action {
  readonly type = TimelineActionTypes.ChangeDateFormat;

  constructor(public dateFormat: string) {}
}

export class ChangeDefaultActivityLayout implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultActivityLayout;

  constructor(public defaultActivityLayout: number) {}
}
export class ChangeDefaultFillColor implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultFillColor;

  constructor(public defaultFillColor: string) {}
}

export class ChangeDefaultIcon implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultIcon;

  constructor(public defaultIcon: string) {}
}

export class ChangeDefaultLabelFontSize implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultLabelFontSize;

  constructor(public labelFontSize: number) {}
}

export class ChangeDefaultLabelFont implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultLabelFont;

  constructor(public labelFont: string) {}
}

export class ChangeDefaultResourceColor implements Action {
  readonly type = TimelineActionTypes.ChangeDefaultResourceColor;

  constructor(public defaultResourceColor: string) {}
}

export class ChangeLabelWidth implements Action {
  readonly type = TimelineActionTypes.ChangeLabelWidth;

  constructor(public labelWidth: number) {}
}

export class ChangeTooltip implements Action {
  readonly type = TimelineActionTypes.ChangeTooltip;

  constructor(public tooltip: boolean) {}
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
  ChangeDefaultActivityLayout |
  ChangeCurrentTimeCursor |
  ChangeDateFormat |
  ChangeDefaultFillColor |
  ChangeDefaultIcon |
  ChangeDefaultLabelFontSize |
  ChangeDefaultLabelFont |
  ChangeDefaultResourceColor |
  ChangeLabelWidth |
  ChangeTooltip |
  RemoveBandsOrPointsForSource |
  RemoveSubBand |
  SelectBand |
  SelectPoint |
  SortBands |
  UpdateBand |
  UpdateSubBand |
  UpdateTimeline;
