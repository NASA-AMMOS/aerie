/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector } from '@ngrx/store';

import {
  FetchGraphDataSuccess,
  RemoveBands,
  SourceExplorerAction,
  SourceExplorerActionTypes,
} from './../actions/source-explorer';

import {
  SelectBand,
  SortBands,
  TimelineAction,
  TimelineActionTypes,
  UpdateBand,
  UpdateSubBand,
  UpdateTimeline,
} from '../actions/timeline';

import {
  hasActivityLegend,
  hasId,
  toCompositeBand,
  updateSortOrder,
  updateTimeRanges,
} from './../shared/util';

import {
  RavenActivityBand,
  RavenCompositeBand,
  RavenTimeRange,
} from './../shared/models';

// Timeline State Interface.
export interface TimelineState {
  bands: RavenCompositeBand[];
  labelWidth: number;
  maxTimeRange: RavenTimeRange;
  selectedBandId: string;
  viewTimeRange: RavenTimeRange;
}

// Timeline Initial State.
export const initialState: TimelineState = {
  bands: [],
  labelWidth: 150,
  maxTimeRange: { end: 0, start: 0 },
  selectedBandId: '',
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
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.UpdateBand:
      return updateBand(state, action);
    case TimelineActionTypes.UpdateSubBand:
      return updateSubBand(state, action);
    case TimelineActionTypes.UpdateTimeline:
      return updateTimeline(state, action);
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
        if (band.overlay && state.selectedBandId === band.id && !hasActivityLegend(state.bands, newBand as RavenActivityBand) ||
            hasActivityLegend([band], newBand as RavenActivityBand)) {
          band = {
            ...band,
            subBands: band.subBands.concat({
              ...newBand,
              parentUniqueId: band.id,
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
 * Reduction Helper. Called when reducing the 'UpdateBand' action.
 */
export function updateBand(state: TimelineState, action: UpdateBand): TimelineState {
  return {
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          ...action.update,
        };
      }

      return band;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'UpdateSubBand' action.
 */
export function updateSubBand(state: TimelineState, action: UpdateSubBand): TimelineState {
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
                ...action.update,
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
 * Reduction Helper. Called when reducing the 'UpdateTimeline' action.
 * This is just a top level reducer for the timeline state (top level meaning it updates base timeline state props).
 */
export function updateTimeline(state: TimelineState, action: UpdateTimeline): TimelineState {
  return {
    ...state,
    ...action.update,
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
// TODO: Add more specific selectors if needed.
