/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { without } from 'lodash';
import { State } from '../raven-store';

import {
  AddBand,
  AddGuide,
  AddPointsToSubBand,
  AddSubBand,
  RemoveAllPointsInSubBandWithParentSource,
  RemoveBandsOrPointsForSource,
  RemoveBandsWithNoPoints,
  RemoveGuide,
  RemoveSourceIdFromSubBands,
  RemoveSubBand,
  SelectBand,
  SelectPoint,
  SetPointsForSubBand,
  SortBands,
  SourceIdAdd,
  TimelineAction,
  TimelineActionTypes,
  UpdateBand,
  UpdateSubBand,
} from '../actions/timeline.actions';

import {
  bandById,
  changeZoom,
  getMaxTimeRange,
  getParentSourceIds,
  getPoint,
  updateSelectedBandIds,
  updateSelectedPoint,
  updateSortOrder,
  updateTimeRanges,
} from '../../shared/util';

import {
  RavenCompositeBand,
  RavenPoint,
  RavenSubBand,
  RavenTimeRange,
} from '../../shared/models';

// Timeline State Interface.
export interface TimelineState {
  bands: RavenCompositeBand[];
  fetchPending: boolean;
  guides: number[]; // in secs
  lastClickTime: number | null;
  maxTimeRange: RavenTimeRange;
  panDelta: number;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  viewTimeRange: RavenTimeRange;
  zoomDelta: number;
}

// Timeline Initial State.
export const initialState: TimelineState = {
  bands: [],
  fetchPending: false,
  guides: [],
  lastClickTime: null,
  maxTimeRange: { end: 0, start: 0 },
  panDelta: 10,
  selectedBandId: '',
  selectedPoint: null,
  selectedSubBandId: '',
  viewTimeRange: { end: 0, start: 0 },
  zoomDelta: 10,
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: TimelineState = initialState,
  action: TimelineAction,
): TimelineState {
  switch (action.type) {
    case TimelineActionTypes.AddBand:
      return addBand(state, action);
    case TimelineActionTypes.AddGuide:
      return addGuide(state, action);
    case TimelineActionTypes.AddPointsToSubBand:
      return addPointsToSubBand(state, action);
    case TimelineActionTypes.AddSubBand:
      return addSubBand(state, action);
    case TimelineActionTypes.PanLeftViewTimeRange:
      return panLeftViewTimeRange(state);
    case TimelineActionTypes.PanRightViewTimeRange:
      return panRightViewTimeRange(state);
    case TimelineActionTypes.RemoveAllGuides:
      return { ...state, guides: [] };
    case TimelineActionTypes.RemoveAllPointsInSubBandWithParentSource:
      return removeAllPointsInSubBandWithParentSource(state, action);
    case TimelineActionTypes.RemoveBandsOrPointsForSource:
      return removeBandsOrPointsForSource(state, action);
    case TimelineActionTypes.RemoveBandsWithNoPoints:
      return removeBandsWithNoPoints(state, action);
    case TimelineActionTypes.RemoveGuide:
      return removeGuide(state, action);
    case TimelineActionTypes.RemoveSourceIdFromSubBands:
      return removeSourceIdFromSubBands(state, action);
    case TimelineActionTypes.RemoveSubBand:
      return removeSubBand(state, action);
    case TimelineActionTypes.ResetViewTimeRange:
      return { ...state, viewTimeRange: { ...state.maxTimeRange } };
    case TimelineActionTypes.SelectBand:
      return selectBand(state, action);
    case TimelineActionTypes.SelectPoint:
      return selectPoint(state, action);
    case TimelineActionTypes.SetPointsForSubBand:
      return setPointsForSubBand(state, action);
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.SourceIdAdd:
      return sourceIdAdd(state, action);
    case TimelineActionTypes.UpdateBand:
      return updateBand(state, action);
    case TimelineActionTypes.UpdateLastClickTime:
      return { ...state, lastClickTime: action.time };
    case TimelineActionTypes.UpdateSubBand:
      return updateSubBand(state, action);
    case TimelineActionTypes.UpdateTimeline:
      return { ...state, ...action.update };
    case TimelineActionTypes.UpdateViewTimeRange:
      return { ...state, viewTimeRange: { ...action.viewTimeRange } };
    case TimelineActionTypes.ZoomInViewTimeRange:
      return {
        ...state,
        viewTimeRange: changeZoom(state.zoomDelta, state.viewTimeRange),
      };
    case TimelineActionTypes.ZoomOutViewTimeRange:
      return {
        ...state,
        viewTimeRange: changeZoom(-state.zoomDelta, state.viewTimeRange),
      };
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
        return {
          ...subBand,
          parentUniqueId: action.band.id,
          sourceIds: without(subBand.sourceIds, action.sourceId).concat(
            action.sourceId,
          ),
          ...action.additionalSubBandProps,
        };
      } else {
        return {
          ...subBand,
          parentUniqueId: action.band.id,
          ...action.additionalSubBandProps,
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
 * Reduction Helper. Called when reducing the 'AddGuide' action.
 */
export function addGuide(state: TimelineState, action: AddGuide) {
  return {
    ...state,
    guides: state.lastClickTime
      ? state.guides.concat(state.lastClickTime)
      : state.guides,
  };
}

/**
 * Reduction Helper. Called when reducing the 'AddPointsToSubBand' action.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function addPointsToSubBand(
  state: TimelineState,
  action: AddPointsToSubBand,
): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            const points = (subBand as any).points.concat(action.points);
            const maxTimeRange = getMaxTimeRange(points);
            const sourceIds = without(
              subBand.sourceIds,
              action.sourceId,
            ).concat(action.sourceId);

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
export function addSubBand(
  state: TimelineState,
  action: AddSubBand,
): TimelineState {
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
 * Reduction Helper. Called when reducing the 'PanLeftViewTimeRange' action.
 */
export function panLeftViewTimeRange(state: TimelineState): TimelineState {
  const currentViewWindow = state.viewTimeRange.end - state.viewTimeRange.start;
  const delta = currentViewWindow / state.panDelta;
  const newStart = state.viewTimeRange.start + delta;

  return {
    ...state,
    viewTimeRange: {
      end: newStart,
      start: newStart - currentViewWindow,
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'PanRightViewTimeRange' action.
 */
export function panRightViewTimeRange(state: TimelineState): TimelineState {
  const currentViewWindow = state.viewTimeRange.end - state.viewTimeRange.start;
  const delta = currentViewWindow / state.panDelta;
  const newStart = state.viewTimeRange.end - delta;

  return {
    ...state,
    viewTimeRange: {
      end: newStart + currentViewWindow,
      start: newStart,
    },
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveAllPointsInSubBandWithParentSource' action.
 * Remove all points in the subBand whose parent source ids contain the action sourceId.
 * All sourceIds for this band share a common parent. Therefore, we can use any sourceIds in the band, thus subBand.sourceIds[0].
 */
export function removeAllPointsInSubBandWithParentSource(
  state: TimelineState,
  action: RemoveAllPointsInSubBandWithParentSource,
): TimelineState {
  const bands = state.bands.map(band => ({
    ...band,
    subBands: band.subBands.reduce(
      (subBands: RavenSubBand[], subBand: RavenSubBand) => {
        if (
          subBand.sourceIds.length > 0 &&
          !getParentSourceIds(subBand.sourceIds[0]).includes(
            action.parentSourceId,
          )
        ) {
          subBands.push(subBand);
        } else {
          subBands.push({
            ...subBand,
            points: [],
          });
        }

        return subBands;
      },
      [],
    ),
  }));

  return {
    ...state,
    bands,
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBandsOrPointsForSource' action.
 * Removes all bands or points that reference the given source.
 *
 * TODO: Replace 'any' with a concrete type.
 */
export function removeBandsOrPointsForSource(
  state: TimelineState,
  action: RemoveBandsOrPointsForSource,
): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.reduce(
        (subBands: RavenSubBand[], subBand: RavenSubBand) => {
          const subBandHasSource = subBand.sourceIds.includes(action.sourceId);
          const sourceIdsCount = subBand.sourceIds.length;

          if (!subBandHasSource) {
            subBands.push(subBand);
          } else if (subBandHasSource && sourceIdsCount > 1) {
            subBands.push({
              ...subBand,
              points: (subBand as any).points.filter(
                (point: any) => point.sourceId !== action.sourceId,
              ),
              sourceIds: subBand.sourceIds.filter(
                sourceId => sourceId !== action.sourceId,
              ),
            });
          }

          return subBands;
        },
        [],
      ),
    }))
    .filter(band => band.subBands.length !== 0)
    .map(band => ({
      ...band,
      compositeYAxisLabel: band.compositeYAxisLabel
        ? band.subBands.length > 1
        : false,
    }));

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(
      bands,
      state.selectedBandId,
      state.selectedSubBandId,
    ),
    ...updateSelectedPoint(bands, state.selectedPoint),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveBandsWithNoPoints' action.
 */
export function removeBandsWithNoPoints(
  state: TimelineState,
  action: RemoveBandsWithNoPoints,
): TimelineState {
  let bands = state.bands
    // First remove subBands that have no points.
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(subBand => subBand.points.length),
    }))
    // Then remove bands if they have no subBands left (i.e. all subBands had no points).
    .filter(band => band.subBands.length);

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveGuide' action.
 */
export function removeGuide(state: TimelineState, action: RemoveGuide) {
  return {
    ...state,
    guides: state.guides.filter(time => {
      if (state.lastClickTime) {
        // remove guides within 10 sec threshold
        const min = state.lastClickTime - 10;
        const max = state.lastClickTime + 10;
        return time >= max || time <= min;
      } else return true;
    }),
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveSourceIdFromSubBands' action.
 */
export function removeSourceIdFromSubBands(
  state: TimelineState,
  action: RemoveSourceIdFromSubBands,
): TimelineState {
  const bands = state.bands.map(band => ({
    ...band,
    subBands: band.subBands.reduce(
      (subBands: RavenSubBand[], subBand: RavenSubBand) => {
        subBands.push({
          ...subBand,
          sourceIds: subBand.sourceIds.filter(
            sourceId => sourceId !== action.sourceId,
          ),
        });
        return subBands;
      },
      [],
    ),
  }));

  return {
    ...state,
    bands,
  };
}

/**
 * Reduction Helper. Called when reducing the 'RemoveSubBand' action.
 */
export function removeSubBand(
  state: TimelineState,
  action: RemoveSubBand,
): TimelineState {
  let bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(
        subBand => subBand.id !== action.subBandId,
      ),
    }))
    .filter(band => band.subBands.length !== 0)
    .map(band => ({
      ...band,
      compositeYAxisLabel: band.compositeYAxisLabel
        ? band.subBands.length > 1
        : false,
    }));

  bands = updateSortOrder(bands);

  return {
    ...state,
    bands,
    ...updateSelectedBandIds(
      bands,
      state.selectedBandId,
      state.selectedSubBandId,
    ),
    ...updateSelectedPoint(bands, state.selectedPoint),
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SelectBand' action.
 */
export function selectBand(
  state: TimelineState,
  action: SelectBand,
): TimelineState {
  if (action.bandId !== state.selectedBandId) {
    const band = bandById(state.bands, action.bandId) as RavenCompositeBand;

    return {
      ...state,
      selectedBandId: action.bandId,
      selectedSubBandId:
        band && band.subBands.length && action.bandId !== ''
          ? band.subBands[0].id
          : '',
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
export function selectPoint(
  state: TimelineState,
  action: SelectPoint,
): TimelineState {
  const alreadySelected =
    state.selectedPoint && state.selectedPoint.uniqueId === action.pointId;

  return {
    ...state,
    selectedPoint: alreadySelected
      ? null
      : getPoint(state.bands, action.bandId, action.subBandId, action.pointId),
  };
}

/**
 * Reduction Helper. Called when reducing the 'SetPointsForSubBand' action.
 * Set points in a subBand with a specified band id and update time range.
 */
export function setPointsForSubBand(
  state: TimelineState,
  action: SetPointsForSubBand,
): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            const maxTimeRange = getMaxTimeRange(action.points);
            return {
              ...subBand,
              maxTimeRange,
              points: action.points,
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
 * Reduction Helper. Called when reducing the 'NewSortOrder' action.
 */
export function sortBands(
  state: TimelineState,
  action: SortBands,
): TimelineState {
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
 * Reduction Helper. Called when reducing the 'SourceIdAdd' action.
 */
export function sourceIdAdd(
  state: TimelineState,
  action: SourceIdAdd,
): TimelineState {
  const bands = state.bands.map(band => ({
    ...band,
    subBands: band.subBands.reduce(
      (subBands: RavenSubBand[], subBand: RavenSubBand) => {
        subBands.push({
          ...subBand,
          sourceIds:
            subBand.id === action.subBandId
              ? without(subBand.sourceIds, action.sourceId).concat(
                  action.sourceId,
                )
              : subBand.sourceIds,
        });
        return subBands;
      },
      [],
    ),
  }));
  return {
    ...state,
    bands,
  };
}

/**
 * Reduction Helper. Called when reducing the 'UpdateBand' action.
 */
export function updateBand(
  state: TimelineState,
  action: UpdateBand,
): TimelineState {
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
export function updateSubBand(
  state: TimelineState,
  action: UpdateSubBand,
): TimelineState {
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
const featureSelector = createFeatureSelector<State>('raven');
export const getTimelineState = createSelector(
  featureSelector,
  (state: State): TimelineState => state.timeline,
);

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
export const getPending = createSelector(
  getTimelineState,
  (state: TimelineState) => state.fetchPending,
);
