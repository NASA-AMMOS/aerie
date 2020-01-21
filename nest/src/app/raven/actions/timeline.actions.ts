/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { BaseType, StringTMap, TimeRange } from '../models';
import {
  AddBandModifiers,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenGuidePoint,
  RavenPin,
  RavenPoint,
  RavenSortMessage,
  RavenSubBand,
} from '../models';

export const addBand = createAction(
  '[raven-timeline] add_band',
  props<{
    sourceId: string | null;
    band: RavenCompositeBand;
    modifiers?: AddBandModifiers;
  }>(),
);

export const addPointAtIndex = createAction(
  '[raven-timeline] add_point_at_index',
  props<{
    bandId: string;
    subBandId: string;
    point: RavenPoint;
    index: number;
  }>(),
);

export const addPointsToSubBand = createAction(
  '[raven-timeline] add_points_to_sub_band',
  props<{
    sourceId: string;
    bandId: string;
    subBandId: string;
    points: any[];
  }>(),
);

export const addSubBand = createAction(
  '[raven-timeline] add_sub_band',
  props<{
    sourceId: string;
    bandId: string;
    subBand: RavenSubBand;
  }>(),
);

export const expandChildrenOrDescendants = createAction(
  '[raven-timeline] expand_children_or_descendants',
  props<{
    bandId: string;
    subBandId: string;
    activityPoint: RavenActivityPoint;
    expandType: string;
  }>(),
);

export const fetchChildrenOrDescendants = createAction(
  '[raven-timeline] fetch_children_or_descendants',
  props<{
    bandId: string;
    subBandId: string;
    activityPoint: RavenActivityPoint;
    expandType: string;
  }>(),
);

export const fetchChildrenOrDescendantsSuccess = createAction(
  '[raven-timeline] fetch_children_or_descendants_success',
);

export const filterActivityInSubBand = createAction(
  '[raven-timeline] filter_activity_in_sub_band',
  props<{
    bandId: string;
    subBandId: string;
    filter: string;
    activityInitiallyHidden: boolean;
  }>(),
);

export const hoverBand = createAction(
  '[raven-timeline] hover_band',
  props<{ bandId: string }>(),
);

export const markRemovePointsInSubBand = createAction(
  '[raven-timeline] mark_remove_points_in_sub_band',
  props<{
    bandId: string;
    subBandId: string;
    points: RavenPoint[];
  }>(),
);

export const panLeftViewTimeRange = createAction(
  '[raven-timeline] pan_left_view_time_range',
);

export const panRightViewTimeRange = createAction(
  '[raven-timeline] pan_right_view_time_range',
);

export const pinAdd = createAction(
  '[raven-timeline] pin_add',
  props<{ pin: RavenPin }>(),
);

export const pinRemove = createAction(
  '[raven-timeline] pin_remove',
  props<{ sourceId: string }>(),
);

export const pinRename = createAction(
  '[raven-timeline] pin_rename',
  props<{ sourceId: string; newName: string }>(),
);

export const removeAllBands = createAction('[raven-timeline] remove_all_bands');

export const removeAllGuides = createAction(
  '[raven-timeline] remove_all_guides',
);

export const removeAllPointsInSubBandWithParentSource = createAction(
  '[raven-timeline] remove_all_points_in_sub_band_with_parent_source',
  props<{ parentSourceId: string }>(),
);

export const removeBandsOrPointsForSource = createAction(
  '[raven-timeline] remove_bands_or_points_for_source',
  props<{ sourceId: string }>(),
);

export const removeBandsWithNoPoints = createAction(
  '[raven-timeline] remove_bands_with_no_points',
);

export const removeChildrenOrDescendants = createAction(
  '[raven-timeline] remove_children_or_descendants',
  props<{
    bandId: string;
    subBandId: string;
    activityPoint: RavenActivityPoint;
  }>(),
);

export const removePointsInSubBand = createAction(
  '[raven-timeline] remove_points_in_sub_band',
  props<{
    bandId: string;
    subBandId: string;
    points: RavenPoint[];
  }>(),
);

export const removeSourceIdFromSubBands = createAction(
  '[raven-timeline] remove_source_id_from_sub_bands',
  props<{ sourceId: string }>(),
);

export const removeSubBand = createAction(
  '[raven-timeline] remove_sub_band',
  props<{ subBandId: string }>(),
);

export const resetViewTimeRange = createAction(
  '[raven-timeline] reset_view_time_range',
);

export const selectBand = createAction(
  '[raven-timeline] select_band',
  props<{ bandId: string }>(),
);

export const selectPoint = createAction(
  '[raven-timeline] select_point',
  props<{ bandId: string; subBandId: string; pointId: string }>(),
);

export const setCompositeYLabelDefault = createAction(
  '[raven-timeline] set_composite_y_label_default',
  props<{ bandId: string }>(),
);

export const setPointsForSubBand = createAction(
  '[raven-timeline] set_points_for_sub_band',
  props<{ bandId: string; subBandId: string; points: any[] }>(),
);

export const sourceIdAdd = createAction(
  '[raven-timeline] source_id_add',
  props<{ sourceId: string; sourcePathInFile: string; subBandId: string }>(),
);

export const sortBands = createAction(
  '[raven-timeline] sort_bands',
  props<{ sort: StringTMap<RavenSortMessage> }>(),
);

export const toggleGuide = createAction(
  '[raven-timeline] toggle_guide',
  props<{ guide: RavenGuidePoint }>(),
);

export const updateAllActivityBandFilter = createAction(
  '[raven-timeline] update_all_activity_band_filter',
  props<{ filter: string }>(),
);

export const updateBand = createAction(
  '[raven-timeline] update_band',
  props<{ bandId: string; update: StringTMap<BaseType> }>(),
);

export const updateCsvFile = createAction(
  '[raven-timeline] update_csv_file',
  props<{
    bandId: string;
    subBandId: string;
    sourceId: string;
    points: RavenPoint[];
    csvHeaderMap: StringTMap<string>;
  }>(),
);

export const updateCsvFileSuccess = createAction(
  '[raven-timeline] update_csv_file_success',
);

export const updateLastClickTime = createAction(
  '[raven-timeline] update_last_click_time',
  props<{ time: number }>(),
);

export const updatePointInSubBand = createAction(
  '[raven-timeline] update_point_in_sub_band',
  props<{
    bandId: string;
    subBandId: string;
    pointId: string;
    update: StringTMap<BaseType>;
  }>(),
);

export const updateSubBand = createAction(
  '[raven-timeline] update_sub_band',
  props<{ bandId: string; subBandId: string; update: StringTMap<BaseType> }>(),
);

export const updateSubBandTimeDelta = createAction(
  '[raven-timeline] update_sub_band_time_delta',
  props<{ bandId: string; subBandId: string; timeDelta: number }>(),
);

export const updateTimeline = createAction(
  '[raven-timeline] update_timeline',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateViewTimeRange = createAction(
  '[raven-timeline] update_view_time_range',
  props<{ viewTimeRange: TimeRange }>(),
);

export const zoomInViewTimeRange = createAction(
  '[raven-timeline] zoom_in_view_time_range',
);

export const zoomOutViewTimeRange = createAction(
  '[raven-timeline] zoom_out_view_time_range',
);
