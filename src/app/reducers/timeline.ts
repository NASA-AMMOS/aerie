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
  AddBands,
  AddPointsToBands,
  RemoveBands,
  RemovePointsFromBands,
  SelectBand,
  SettingsUpdateGlobal,
  SettingsUpdateSelectedBand,
  TimelineActionTypes,
  TimelineAction,
} from '../actions/timeline';

import {
  RavenBand,
} from './../models';

// Timeline Interface.
export interface TimelineState {
  bands: RavenBand[];
  labelWidth: number;
  selectedBand: RavenBand | null;
}

// Timeline Initial State.
const initialState: TimelineState = {
  bands: [],
  labelWidth: 190,
  selectedBand: null,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: TimelineState = initialState, action: TimelineAction): TimelineState {
  switch (action.type) {
    case TimelineActionTypes.AddBands:
      return addBands(state, action);
    case TimelineActionTypes.RemoveBands:
      return removeBands(state, action);
    case TimelineActionTypes.AddPointsToBands:
      return addPointsToBands(state, action);
    case TimelineActionTypes.RemovePointsFromBands:
      return removePointsFromBands(state, action);
    case TimelineActionTypes.SelectBand:
      return selectBand(state, action);
    case TimelineActionTypes.SettingsUpdateGlobal:
      return settingsUpdateGlobal(state, action);
    case TimelineActionTypes.SettingsUpdateSelectedBand:
      return settingsUpdateSelectedBand(state, action);
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'AddBands' action.
 * Associates each band with the given source id.
 */
export function addBands(state: TimelineState, action: AddBands): TimelineState {
  return {
    ...state,
    bands: state.bands.concat(action.bands.map((band: RavenBand) => {
      return {
        ...band,
        sourceIds: {
          ...band.sourceIds,
          [action.sourceId]: true,
        },
      };
    })),
  };
}


/**
 * Reduction Helper. Called when reducing the 'RemoveBands' action.
 *
 * When we remove bands we also have to account for the selectedBand.
 * If bands is empty, or if we remove a band that is selected, make sure to set selectedBand to null.
 */
export function removeBands(state: TimelineState, action: RemoveBands): TimelineState {
  const bands = state.bands.filter(band => !action.bandIds.includes(band.id));

  if (bands.length > 0) {
    return {
      ...state,
      bands,
      selectedBand: state.selectedBand && action.bandIds.includes(state.selectedBand.id) ? null : state.selectedBand,
    };
  }

  return {
    ...state,
    bands: [],
    selectedBand: null,
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddPointsToBands' action.
 * TODO: Remove the 'any' type in favor of a RavenBand type.
 */
export function addPointsToBands(state: TimelineState, action: AddPointsToBands): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: any) => {
      // If there is a band that has new points, then add the points and update the corresponding source id.
      if (action.bandIdsToPoints[band.id]) {
        return {
          ...band,
          points: band.points.concat(action.bandIdsToPoints[band.id] as any[]),
          sourceIds: {
            ...band.sourceIds,
            [action.sourceId]: true,
          },
        };
      }

      // Otherwise just return the band as-is.
      return {
        ...band,
      };
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemovePointsFromBands' action.
 *
 * Note this does not actually remove a band, only points from potentially multiple bands.
 *
 * TODO: Remove the 'any' type in favor of a RavenBand type.
 */
export function removePointsFromBands(state: TimelineState, action: RemovePointsFromBands): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: any) => {
      // Remove points from bands with ids in the bandsIds list, and also update the source ids.
      if (action.bandIds.includes(band.id)) {
        return {
          ...band,
          points: band.points.filter((point: any) => point.sourceId !== action.sourceId),
          sourceIds: omit(band.sourceIds, action.sourceId),
        };
      }

      // Otherwise if the band id is not included in the bandIds list, then return it as-is.
      return {
        ...band,
      };
    }),
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
 * Reduction Helper. Called when reducing the 'SettingsUpdateGlobal' action.
 */
export function settingsUpdateGlobal(state: TimelineState, action: SettingsUpdateGlobal): TimelineState {
  return {
    ...state,
    [action.prop]: action.value,
  };
}

/**
 * Reduction Helper. Called when reducing the 'SettingsUpdateSelectedBand' action.
 */
export function settingsUpdateSelectedBand(state: TimelineState, action: SettingsUpdateSelectedBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenBand) => {
      if (state.selectedBand && state.selectedBand.id === band.id) {
        return {
          ...band,
          [action.prop]: action.value,
        };
      }

      return {
        ...band,
      };
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
export const getSelectedBand = createSelector(getTimelineState, (state: TimelineState) => state.selectedBand);
