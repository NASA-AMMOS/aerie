/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { without } from 'lodash';

import {
  createFeatureSelector,
} from '@ngrx/store';

import {
  AddBand,
  AddPointsToSubBand,
  AddSubBand,
  RemoveBandsOrPointsForSource,
  RemoveSubBand,
  SelectBand,
  SelectPoint,
  SortBands,
  TimelineAction,
  TimelineActionTypes,
  UpdateBand,
  UpdateSubBand,
} from './../actions/timeline';

import {
  bandById,
  getMaxTimeRange,
  getPoint,
  updateSelectedBandIds,
  updateSelectedPoint,
  updateSortOrder,
  updateTimeRanges,
} from './../shared/util';

import {
  RavenCompositeBand,
  RavenPoint,
  RavenSubBand,
  RavenTimeRange,
} from './../shared/models';

// Timeline State Interface.
export interface TimelineState {
  bands: RavenCompositeBand[];
  maxTimeRange: RavenTimeRange;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  viewTimeRange: RavenTimeRange;
}

// Timeline Initial State.
export const initialState: TimelineState = {
  bands: [],
  maxTimeRange: { end: 0, start: 0 },
  selectedBandId: '',
  selectedPoint: null,
  selectedSubBandId: '',
  viewTimeRange: { end: 0, start: 0 },
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(state: TimelineState = initialState, action: TimelineAction): TimelineState {
  switch (action.type) {
    case TimelineActionTypes.AddBand:
      return addBand(state, action);
    case TimelineActionTypes.AddPointsToSubBand:
      return addPointsToSubBand(state, action);
    case TimelineActionTypes.AddSubBand:
      return addSubBand(state, action);
    case TimelineActionTypes.RemoveBandsOrPointsForSource:
      return removeBandsOrPointsForSource(state, action);
    case TimelineActionTypes.RemoveSubBand:
      return removeSubBand(state, action);
    case TimelineActionTypes.SelectBand:
      return selectBand(state, action);
    case TimelineActionTypes.SelectPoint:
      return selectPoint(state, action);
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.UpdateBand:
      return updateBand(state, action);
    case TimelineActionTypes.UpdateSubBand:
      return updateSubBand(state, action);
    case TimelineActionTypes.UpdateTimeline:
      return { ...state, ...action.update };
    case TimelineActionTypes.UpdateViewTimeRange:
      return { ...state, viewTimeRange: { ...action.viewTimeRange } };
    default:
      return state;
  }
}

/**
 * Reduction Helper. Called when reducing the 'AddBand' action.
 */
export function addBand(state: TimelineState, action: AddBand): TimelineState {
  const bands = state.bands.concat({
    ...action.band,
    containerId: '0',
    sortOrder: state.bands.filter(b => b.containerId === '0').length,
    subBands: action.band.subBands.map(subBand => {
      if (action.sourceId) {
        const sourceIds = without(subBand.sourceIds, action.sourceId).concat(action.sourceId);
        return {
          ...subBand,
          parentUniqueId: action.band.id,
          sourceIds,
        };
      } else {
        return {
          ...subBand,
          parentUniqueId: action.band.id,
        };
      }
    }),
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddPointsToSubBand' action.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function addPointsToSubBand(state: TimelineState, action: AddPointsToSubBand): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            const points = (subBand as any).points.concat(action.points);
            const maxTimeRange = getMaxTimeRange(points);
            const sourceIds = without(subBand.sourceIds, action.sourceId).concat(action.sourceId);

            return {
              ...subBand,
              maxTimeRange,
              points,
              sourceIds,
            };
          }

          return subBand;
        }),
      };
    }

    return band;
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddSubBand' action.
 */
export function addSubBand(state: TimelineState, action: AddSubBand): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.concat({
          ...action.subBand,
          parentUniqueId: band.id,
          sourceIds: [action.sourceId],
        }),
      };
    }
    return band;
  });

  return {
    ...state,
    bands,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBandsOrPointsForSource' action.
 * Removes all bands or points that reference the given source.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function removeBandsOrPointsForSource(state: TimelineState, action: RemoveBandsOrPointsForSource): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.reduce((subBands: RavenSubBand[], subBand: RavenSubBand) => {
        const subBandHasSource = subBand.sourceIds.includes(action.sourceId);
        const sourceIdsCount = subBand.sourceIds.length;

        if (!subBandHasSource) {
          subBands.push(subBand);
        } else if (subBandHasSource && sourceIdsCount > 1) {
          subBands.push({
            ...subBand,
            points: (subBand as any).points.filter((point: any) => point.sourceId !== action.sourceId),
            sourceIds: subBand.sourceIds.filter(sourceId => sourceId !== action.sourceId),
          });
        }

        return subBands;
      }, []),
    }))
    .filter(
      band => band.subBands.length !== 0,
    );

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(bands, state.selectedBandId, state.selectedSubBandId),
    ...updateSelectedPoint(state.selectedPoint, action.sourceId, null),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveSubBand' action.
 */
export function removeSubBand(state: TimelineState, action: RemoveSubBand): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(subBand => subBand.id !== action.subBandId),
    }))
    .filter(
      band => band.subBands.length !== 0,
    );

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(bands, state.selectedBandId, state.selectedSubBandId),
    ...updateSelectedPoint(state.selectedPoint, null, action.subBandId),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectBand' action.
 */
export function selectBand(state: TimelineState, action: SelectBand): TimelineState {
  if (action.bandId !== state.selectedBandId) {
    const band = bandById(state.bands, action.bandId);

    return {
      ...state,
      selectedBandId: action.bandId,
      selectedSubBandId: band && band.subBands.length && action.bandId !== '' ? band.subBands[0].id : '',
    };
  } else {
    return {
      ...state,
    };
  }
}

/**
 * Reduction Helper. Called when reducing the 'SelectPoint' action.
 * Make sure if a point is already selected that we de-select it if it's clicked again.
 */
export function selectPoint(state: TimelineState, action: SelectPoint): TimelineState {
  const alreadySelected = state.selectedPoint && state.selectedPoint.uniqueId === action.pointId;

  return {
    ...state,
    selectedPoint: alreadySelected ? null : getPoint(state.bands, action.bandId, action.subBandId, action.pointId),
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
