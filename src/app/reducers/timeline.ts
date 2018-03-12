/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';

import {
  FetchGraphDataSuccess,
  RemoveBands,
  SourceExplorerAction,
  SourceExplorerActionTypes,
} from './../actions/source-explorer';

import {
  SelectBand,
  SelectDataPoint,
  SettingsUpdateAllBands,
  SettingsUpdateBand,
  SettingsUpdateSubBand,
  SortBands,
  TimelineAction,
  TimelineActionTypes,
} from '../actions/timeline';

import {
  hasActivityLegend,
  hasId,
  shouldOverlay,
  timestamp,
  toCompositeBand,
  updateSortOrder,
  updateTimeRanges,
} from './../shared/util';

import {
  RavenActivityBand,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPoint,
  RavenResourcePoint,
  RavenTimeRange,
} from './../shared/models';

// Timeline State Interface.
export interface TimelineState {
  bands: RavenCompositeBand[];
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  overlayMode: boolean;
  selectedBandId: string;
  viewTimeRange: RavenTimeRange;
  selectedDataPoint: RavenPoint;
}

const defaultDataPoint: RavenResourcePoint = {
  duration: 0,
  id: '',
  sourceId: '',
  start: 0,
  uniqueId: '',
  value: 0,
};

// Timeline Initial State.
export const initialState: TimelineState = {
  bands: [],
  labelWidth: 150,
  maxTimeRange: { end: 0, start: 0 },
  overlayMode: false,
  selectedBandId: '',
  selectedDataPoint: defaultDataPoint,
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
    case TimelineActionTypes.SelectDataPoint:
      return selectDataPoint(state, action);
    case TimelineActionTypes.SettingsUpdateAllBands:
      return settingsUpdateAllBands(state, action);
    case TimelineActionTypes.SettingsUpdateBand:
      return settingsUpdateBand(state, action);
    case TimelineActionTypes.SettingsUpdateSubBand:
      return settingsUpdateSubBand(state, action);
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.UpdateViewTimeRange:
      return { ...state, viewTimeRange: { ...action.viewTimeRange } };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'FetchGraphDataSuccess' action.
 * Handles overlaying on existing bands, or adding new bands to the list.
 * This action is defined in the sourceExplorer actions.
 */
export function addBands(state: TimelineState, action: FetchGraphDataSuccess): TimelineState {
  const bands = state.bands
    // Add new bands to current bands.
    .map((band: RavenCompositeBand) => {
      for (let i = action.newBands.length - 1; i >= 0; --i) { // Running backwards since we are splicing.
        const newBand = action.newBands[i];

        // Add new band to a currently existing band.
        if (shouldOverlay(state.overlayMode, state.selectedBandId, band.id) && !hasActivityLegend(state.bands, newBand as RavenActivityBand) ||
          hasActivityLegend([band], newBand as RavenActivityBand)) {
          band = {
            ...band,
            subBands: band.subBands.concat({
              ...newBand,
              parentUniqueId: band.id,
              sourceId: action.source.id,
              sourceName: action.source.name,
            }),
          };
          action.newBands.splice(i, 1);
        }
      }

      return band;
    })
    // Add new bands.
    .concat(action.newBands.map((newBand, index) => {
      const newCompositeBand = toCompositeBand(newBand);

      return {
        ...newCompositeBand,
        containerId: '0',
        sortOrder: state.bands.filter(b => b.containerId === '0').length + index,
        subBands: newCompositeBand.subBands.map(subBand => {
          return {
            ...subBand,
            sourceId: action.source.id,
            sourceName: action.source.name,
          };
        }),
      };
    }));

  return {
    ...state,
    bands,
    ...updateTimeRanges(state.viewTimeRange, bands),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBands' action.
 * This action is defined in the sourceExplorer actions.
 *
 * When we remove bands we also have to account for the selectedBandId.
 * If bands is empty, or if we remove a band that is selected, make sure to set selectedBandId to empty.
 */
export function removeBands(state: TimelineState, action: RemoveBands): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(subBand => !action.bandIds.includes(subBand.id)),
    }))
    .filter(band => band.subBands.length !== 0);

  // Update the sort order of all the bands for each container.
  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    selectedBandId: hasId(bands, state.selectedBandId) ? state.selectedBandId : '',
    ...updateTimeRanges(state.viewTimeRange, bands),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectBand' action.
 *
 * If we click on a band that's already selected, just de-select it.
 */
export function selectBand(state: TimelineState, action: SelectBand): TimelineState {
  return {
    ...state,
    selectedBandId: action.bandId === state.selectedBandId ? '' : action.bandId,
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectDataPoint' action.
 */
export function selectDataPoint(state: TimelineState, action: SelectDataPoint): TimelineState {
  // get the activityPoint from uniqueId and band; action.bandId is the composite bandId containing the band of action.interval
  let dataPoint = state.selectedDataPoint;
  for (let i = 0, l = state.bands.length; i < l; ++i) {
    if (state.bands[i].id === action.bandId) {
      for (let j = 0, ll = state.bands[i].subBands.length; j < ll; ++j) {
        const subBand = state.bands[i].subBands[j];
        for (let k = 0, lll = subBand.points.length; k < lll; ++k) {
          if (subBand.points[k].uniqueId === action.interval.uniqueId) {
            dataPoint = subBand.points[k];
          }
        }
      }
    }
  }

  return {
    ...state,
    selectedDataPoint: dataPoint,
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
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          [action.prop]: action.value,
        };
      }

      return band;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SettingsUpdateSubBand' action.
 */
export function settingsUpdateSubBand(state: TimelineState, action: SettingsUpdateSubBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              return {
                ...subBand,
                [action.prop]: action.value,
              };
            }
            return subBand;
          }),
        };
      }

      return band;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'NewSortOrder' action.
 */
export function sortBands(state: TimelineState, action: SortBands): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.sort[band.id]) {
        return {
          ...band,
          containerId: action.sort[band.id].containerId,
          sortOrder: action.sort[band.id].sortOrder,
        };
      }

      return band;
    }),
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
export const getOverlayMode = createSelector(getTimelineState, (state: TimelineState) => state.overlayMode);
export const getSelectedBandId = createSelector(getTimelineState, (state: TimelineState) => state.selectedBandId);
export const getViewTimeRange = createSelector(getTimelineState, (state: TimelineState) => state.viewTimeRange);
export const getSelectedDataPoint = createSelector(getTimelineState, (state: TimelineState) => state.selectedDataPoint);
