/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { omit, without } from 'lodash';

import {
  AddBand,
  AddPointsToSubBand,
  AddSubBand,
  ExpandChildrenOrDescendants,
  FilterActivityInSubBand,
  HoverBand,
  RemoveAllPointsInSubBandWithParentSource,
  RemoveBandsOrPointsForSource,
  RemoveBandsWithNoPoints,
  RemoveChildrenOrDescendants,
  RemoveSourceIdFromSubBands,
  RemoveSubBand,
  SelectBand,
  SelectPoint,
  SetCompositeYLabelDefault,
  SetPointsForSubBand,
  SortBands,
  SourceIdAdd,
  TimelineAction,
  TimelineActionTypes,
  ToggleGuide,
  UpdateBand,
  UpdateSubBand,
} from '../actions/timeline.actions';

import {
  bandById,
  changeZoom,
  getMaxTimeRange,
  getParentSourceIds,
  getPoint,
  hasTwoResourceBands,
  sortOrderForBand,
  updateSelectedBandIds,
  updateSelectedPoint,
  updateSortOrder,
  updateTimeRanges,
} from '../../shared/util';

import {
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPoint,
  RavenState,
  RavenSubBand,
  RavenTimeRange,
  StringTMap,
} from '../../shared/models';

export interface TimelineState {
  bands: RavenCompositeBand[];
  currentState: RavenState | null;
  currentStateChanged: boolean;
  currentStateId: string;
  expansionByActivityId: StringTMap<string>;
  fetchPending: boolean;
  guides: number[]; // in secs
  hoveredBandId: string;
  lastClickTime: number | null;
  maxTimeRange: RavenTimeRange;
  panDelta: number;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  viewTimeRange: RavenTimeRange;
  zoomDelta: number;
}

export const initialState: TimelineState = {
  bands: [],
  currentState: null,
  currentStateChanged: false,
  currentStateId: '',
  expansionByActivityId: {},
  fetchPending: false,
  guides: [],
  hoveredBandId: '',
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
    case TimelineActionTypes.AddPointsToSubBand:
      return addPointsToSubBand(state, action);
    case TimelineActionTypes.AddSubBand:
      return addSubBand(state, action);
    case TimelineActionTypes.ExpandChildrenOrDescendants:
      return expandChildrenOrDescendants(state, action);
    case TimelineActionTypes.FilterActivityInSubBand:
      return filterActivityInSubBand(state, action);
    case TimelineActionTypes.HoverBand:
      return hoverBand(state, action);
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
    case TimelineActionTypes.RemoveChildrenOrDescendants:
      return removeChildrenOrDescendants(state, action);
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
    case TimelineActionTypes.SetCompositeYLabelDefault:
      return setCompositeYLabelDefault(state, action);
    case TimelineActionTypes.SetPointsForSubBand:
      return setPointsForSubBand(state, action);
    case TimelineActionTypes.SortBands:
      return sortBands(state, action);
    case TimelineActionTypes.SourceIdAdd:
      return sourceIdAdd(state, action);
    case TimelineActionTypes.ToggleGuide:
      return toggleGuide(state, action);
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
  const prevSort = sortOrderForBand(
    state.bands,
    action.modifiers.afterBandId || '',
  );

  const bands = state.bands
    .map(band => {
      if (band.containerId !== '0') {
        return band;
      }

      if (band.sortOrder > prevSort) {
        return { ...band, sortOrder: band.sortOrder + 1 };
      } else {
        return band;
      }
    })
    .concat({
      ...action.band,
      containerId: '0',
      sortOrder: prevSort + 1,
      subBands: action.band.subBands.map(subBand => {
        if (action.sourceId) {
          return {
            ...subBand,
            parentUniqueId: action.band.id,
            sourceIds: without(subBand.sourceIds, action.sourceId).concat(
              action.sourceId,
            ),
            ...action.modifiers.additionalSubBandProps,
          };
        } else {
          return {
            ...subBand,
            parentUniqueId: action.band.id,
            ...action.modifiers.additionalSubBandProps,
          };
        }
      }),
    });

  return {
    ...state,
    bands,
    selectedBandId: action.band.id,
    selectedSubBandId:
      action.band && action.band.subBands.length && action.band.id !== ''
        ? action.band.subBands[0].id
        : '',
    ...updateTimeRanges(bands, state.viewTimeRange),
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
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState ? true : false,
    ...updateTimeRanges(bands, state.viewTimeRange),
  };
}

/**
 * Reduction Helper. Called when reducing 'ExpandChildrenOrDescendants' actions.
 */
export function expandChildrenOrDescendants(
  state: TimelineState,
  action: ExpandChildrenOrDescendants,
): TimelineState {
  const activityPoint = action.activityPoint
    ? {
        ...action.activityPoint,
        expansion: action.expandType,
      }
    : null;
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            return {
              ...subBand,
              points: (subBand as any).points.map(
                (point: RavenActivityPoint) => {
                  if (point.uniqueId === action.activityPoint.uniqueId) {
                    return { ...activityPoint };
                  }
                  return point;
                },
              ),
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
    currentStateChanged: state.currentState !== null,
    expansionByActivityId: {
      ...state.expansionByActivityId,
      [action.activityPoint.activityId]: action.expandType,
    },
    selectedPoint: activityPoint,
  };
}

/**
 * Reduction Helper. Called when reducing the 'FilterActivityInSubBand' action.
 */
export function filterActivityInSubBand(
  state: TimelineState,
  action: FilterActivityInSubBand,
): TimelineState {
  const bands = state.bands.map((band: RavenCompositeBand) => {
    if (action.bandId === band.id) {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (action.subBandId === subBand.id) {
            let points = (subBand as any).points;
            points = points.map((point: RavenActivityPoint) => {
              if (action.filter.length > 0) {
                const match = point.activityName.match(
                  new RegExp(action.filter),
                );
                return { ...point, hidden: match !== null };
              } else {
                return { ...point, hidden: false };
              }
            });

            return {
              ...subBand,
              points,
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
    currentStateChanged: state.currentState !== null,
    viewTimeRange: {
      end: newStart,
      start: newStart - currentViewWindow,
    },
  };
}

export function hoverBand(
  state: TimelineState,
  action: HoverBand,
): TimelineState {
  return {
    ...state,
    hoveredBandId: action.bandId,
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
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState !== null,
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
      subBands: band.subBands
        .reduce((subBands: RavenSubBand[], subBand: RavenSubBand) => {
          const subBandHasSource = subBand.sourceIds.includes(action.sourceId);
          if (!subBandHasSource) {
            subBands.push(subBand);
          } else {
            subBands.push({
              ...subBand,
              points: (subBand as any).points.filter(
                (point: any) =>
                  point.sourceId !== action.sourceId ||
                  point.expandedFromPointId,
              ),
              sourceIds: subBand.sourceIds.filter(
                sourceId => sourceId !== action.sourceId,
              ),
            });
          }

          return subBands;
        }, [])
        .filter(subBand => subBand.points.length !== 0),
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
    currentStateChanged: state.currentState !== null,
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
    // First remove subBands that have no points and keep the subBands that are dividers.
    .map(band => ({
      ...band,
      subBands: band.subBands.filter(
        subBand => subBand.type === 'divider' || subBand.points.length,
      ),
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
 * Reduction Helper. Called when reducing the 'RemoveChildrenOrDescendants' action.
 */
export function removeChildrenOrDescendants(
  state: TimelineState,
  action: RemoveChildrenOrDescendants,
): TimelineState {
  const bands = state.bands
    .map(band => ({
      ...band,
      subBands: band.subBands
        .reduce((subBands: RavenSubBand[], subBand: RavenSubBand) => {
          subBands.push({
            ...subBand,
            points: (subBand as any).points
              .filter((point: RavenActivityPoint) => {
                return (
                  point.expandedFromPointId !== action.activityPoint.uniqueId
                );
              })
              .map(
                (point: RavenActivityPoint) =>
                  point.uniqueId === action.activityPoint.uniqueId
                    ? { ...point, expansion: 'noExpansion' }
                    : point,
              ),
          });

          return subBands;
        }, [])
        .filter(subBand => subBand.points.length !== 0),
    }))
    .filter(band => band.subBands.length !== 0);

  return {
    ...state,
    bands,
    currentStateChanged: state.currentState !== null,
    expansionByActivityId: omit(
      state.expansionByActivityId,
      action.activityPoint.activityId,
    ),
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
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState !== null,
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
 * Reduction Helper. Called when reducing the 'SetCompositeYLabelDefault' action.
 */
export function setCompositeYLabelDefault(
  state: TimelineState,
  action: SetCompositeYLabelDefault,
): TimelineState {
  return {
    ...state,
    bands: state.bands.map(band => {
      if (band.id === action.bandId && hasTwoResourceBands(band)) {
        return { ...band, compositeYAxisLabel: true };
      } else {
        return band;
      }
    }),
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState !== null,
  };
}

/**
 * Reduction Helper. Called when reducing the 'ToggleGuide' action.
 */
export function toggleGuide(state: TimelineState, action: ToggleGuide) {
  const existingGuide = state.guides.filter(
    guide =>
      action.guide.guideTime > guide - 2 * action.guide.timePerPixel &&
      action.guide.guideTime < guide + 2 * action.guide.timePerPixel,
  );
  console.log('existingGuide: ' + existingGuide);
  return {
    ...state,
    currentStateChanged: state.currentState !== null,
    guides:
      existingGuide.length > 0
        ? state.guides.filter(guide => guide !== existingGuide[0])
        : state.guides.concat(action.guide.guideTime),
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
    currentStateChanged: state.currentState !== null,
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
    currentStateChanged: state.currentState !== null,
  };
}
