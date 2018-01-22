/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { omit } from 'lodash';

import { createSelector, createFeatureSelector } from '@ngrx/store';

import {
  FetchGraphDataSuccess,
  RemoveBands,
  SourceExplorerAction,
  SourceExplorerActionTypes,
} from './../actions/source-explorer';

import {
  SelectBand,
  SettingsUpdateAllBands,
  SettingsUpdateBand,
  TimelineAction,
  TimelineActionTypes,
} from '../actions/timeline';

import {
  getTimeRanges,
} from './../util/bands';

import {
  RavenBand,
  RavenTimeRange,
} from './../models';

// Timeline Interface.
export interface TimelineState {
  bands: RavenBand[];
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  selectedBand: RavenBand | null;
  viewTimeRange: RavenTimeRange;
}

// Timeline Initial State.
const initialState: TimelineState = {
  bands: [],
  labelWidth: 99,
  maxTimeRange: { end: 0, start: 0 },
  selectedBand: null,
  viewTimeRange: { end: 0, start: 0 },
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: TimelineState = initialState, action: SourceExplorerAction | TimelineAction): TimelineState {
  switch (action.type) {
    case SourceExplorerActionTypes.FetchGraphDataSuccess:
      return addBands(state, action);
    case SourceExplorerActionTypes.RemoveBands:
      return removeBands(state, action);
    case TimelineActionTypes.SelectBand:
      return selectBand(state, action);
    case TimelineActionTypes.SettingsUpdateAllBands:
      return settingsUpdateAllBands(state, action);
    case TimelineActionTypes.SettingsUpdateBand:
      return settingsUpdateBand(state, action);
    case TimelineActionTypes.UpdateViewTimeRange:
      return { ...state, viewTimeRange: { ...action.viewTimeRange } };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'FetchGraphDataSuccess' action.
 * Associates each band with the given source id, and adds any new band.
 * This action is defined in the sourceExplorer actions.
 *
 * TODO: Remove the 'any' type in favor of a RavenBand type.
 */
export function addBands(state: TimelineState, action: FetchGraphDataSuccess): TimelineState {
  const bands = state.bands
    // 1. Map over existing bands and add any points from the action.
    .map((band: any) => {
      // If there is a band that has new points, then add the points and update the corresponding source id.
      if (action.bandIdsToPoints[band.id]) {
        return {
          ...band,
          points: band.points.concat(action.bandIdsToPoints[band.id] as any[]),
          sourceIds: {
            ...band.sourceIds,
            [action.source.id]: true,
          },
        };
      }

      return band;
    })
    // 2. Add and new bands from the action.
    .concat(action.bands.map((band: RavenBand) => {
      return {
        ...band,
        sourceIds: {
          ...band.sourceIds,
          [action.source.id]: true,
        },
      };
    }))
    // 3. Add sort order and container id to all bands.
    .map((band, index) => {
      return {
        ...band,
        containerId: '0',
        sortOrder: index,
      };
    });

  return {
    ...state,
    bands,
    ...getTimeRanges(state.viewTimeRange, bands),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBands' action.
 * This action is defined in the sourceExplorer actions.
 *
 * When we remove bands we also have to account for the selectedBand.
 * If bands is empty, or if we remove a band that is selected, make sure to set selectedBand to null.
 */
export function removeBands(state: TimelineState, action: RemoveBands): TimelineState {
  const bands = state.bands
    // 1. Filter any bands with an id in removeBandIds.
    .filter(band => {
      return !action.removeBandIds.includes(band.id);
    })
    // 2. Remove points from bands with an id in removePointsBandIds.
    .map((band: any) => {
      // Remove points from bands with ids in the bandsIds list, and also update the source ids.
      if (action.removePointsBandIds.includes(band.id)) {
        return {
          ...band,
          points: band.points.filter((point: any) => point.sourceId !== action.source.id),
          sourceIds: omit(band.sourceIds, action.source.id),
        };
      }

      // Otherwise if the band id is not included in the bandIds list, then return it as-is.
      return band;
    })
    // 3. Update sort order and container id to all bands.
    .map((band, index) => {
      return {
        ...band,
        sortOrder: index,
      };
    });

  return {
    ...state,
    bands,
    selectedBand: state.selectedBand && action.removeBandIds.includes(state.selectedBand.id) ? null : state.selectedBand,
    ...getTimeRanges(state.viewTimeRange, bands),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectBand' action.
 */
export function selectBand(state: TimelineState, action: SelectBand): TimelineState {
  return {
    ...state,
    selectedBand: state.bands.find(band => band.id === action.bandId) || null,
  };
}

/**
 * Reduction Helper. Called when reducing the 'SettingsUpdateAllBands' action.
 */
export function settingsUpdateAllBands(state: TimelineState, action: SettingsUpdateAllBands): TimelineState {
  return {
    ...state,
    [action.prop]: action.value,
  };
}

/**
 * Reduction Helper. Called when reducing the 'SettingsUpdateBand' action.
 */
export function settingsUpdateBand(state: TimelineState, action: SettingsUpdateBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenBand) => {
      if (state.selectedBand && state.selectedBand.id === band.id) {
        return {
          ...band,
          [action.prop]: action.value,
        };
      }

      return band;
    }),
    selectedBand: ({
      ...state.selectedBand,
      [action.prop]: action.value,
    } as RavenBand),
  };
}

/**
 * Timeline state selector helper.
 */
export const getTimelineState = createFeatureSelector<TimelineState>('timeline');

/**
 * Create selector helper for selecting state slice.
 *
 * Every reducer module exports selector functions, however child reducers
 * have no knowledge of the overall state tree. To make them usable, we
 * need to make new selectors that wrap them.
 *
 * The createSelector function creates very efficient selectors that are memoized and
 * only recompute when arguments change. The created selectors can also be composed
 * together to select different pieces of state.
 */
export const getBands = createSelector(getTimelineState, (state: TimelineState) => state.bands);
export const getLabelWidth = createSelector(getTimelineState, (state: TimelineState) => state.labelWidth);
export const getMaxTimeRange = createSelector(getTimelineState, (state: TimelineState) => state.maxTimeRange);
export const getSelectedBand = createSelector(getTimelineState, (state: TimelineState) => state.selectedBand);
export const getViewTimeRange = createSelector(getTimelineState, (state: TimelineState) => state.viewTimeRange);
