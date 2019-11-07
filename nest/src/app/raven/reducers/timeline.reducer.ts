/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createReducer, on } from '@ngrx/store';
import omit from 'lodash-es/omit';
import without from 'lodash-es/without';
import { TimelineActions } from '../actions';
import {
  RavenActivityBand,
  RavenActivityPoint,
  RavenCompositeBand,
  RavenPoint,
  RavenResourceBand,
  RavenState,
  RavenSubBand,
  StringTMap,
  TimeRange,
} from '../models';
import {
  bandById,
  changeZoom,
  filterActivityPoints,
  getMaxTimeRange,
  getParentSourceIds,
  getPoint,
  hasTwoResourceBands,
  sortOrderForBand,
  updateSelectedBandIds,
  updateSelectedPoint,
  updateSortOrder,
  updateTimeRanges,
} from '../util';

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
  maxTimeRange: TimeRange;
  panDelta: number;
  selectedBandId: string;
  selectedPoint: RavenPoint | null;
  selectedSubBandId: string;
  viewTimeRange: TimeRange;
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

export const reducer = createReducer(
  initialState,
  on(TimelineActions.addBand, (state, action) => {
    const modifiers = action.modifiers || {};
    const prevSort = sortOrderForBand(state.bands, modifiers.afterBandId || '');

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
        subBands: action.band.subBands.map((subBand: RavenSubBand) => {
          if (action.sourceId) {
            return {
              ...subBand,
              parentUniqueId: action.band.id,
              sourceIds: without(subBand.sourceIds, action.sourceId).concat(
                action.sourceId,
              ),
              ...modifiers.additionalSubBandProps,
            };
          } else {
            return {
              ...subBand,
              parentUniqueId: action.band.id,
              ...modifiers.additionalSubBandProps,
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
  }),
  on(TimelineActions.addPointAtIndex, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              const points = [...(subBand as any).points];
              points.splice(action.index, 0, action.point);
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
      currentStateChanged: state.currentState !== null,
    };
  }),
  on(TimelineActions.addPointsToSubBand, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              const newPoints =
                subBand.type === 'resource'
                  ? action.points.map(point => ({
                      ...point,
                      start:
                        point.start + (subBand as RavenResourceBand).timeDelta,
                    }))
                  : action.points.map(point => ({
                      ...point,
                      end: point.end + (subBand as RavenResourceBand).timeDelta,
                      start:
                        point.start + (subBand as RavenResourceBand).timeDelta,
                    }));
              const points = (subBand as any).points.concat(newPoints);
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
  }),
  on(TimelineActions.addSubBand, (state, action) => {
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
      currentStateChanged: state.currentState !== null,
      ...updateTimeRanges(bands, state.viewTimeRange),
    };
  }),
  on(TimelineActions.expandChildrenOrDescendants, (state, action) => {
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
  }),
  on(TimelineActions.filterActivityInSubBand, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (
              action.subBandId === subBand.id &&
              subBand.type === 'activity'
            ) {
              return {
                ...subBand,
                points: filterActivityPoints(
                  (subBand as RavenActivityBand).points,
                  action.filter,
                  action.activityInitiallyHidden,
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
    };
  }),
  on(TimelineActions.hoverBand, (state, { bandId }) => ({
    ...state,
    hoveredBandId: bandId,
  })),
  on(TimelineActions.markRemovePointsInSubBand, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      const deletePoints = action.points.map(
        deletePoint => deletePoint.uniqueId,
      );
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              return {
                ...subBand,
                points: (subBand as any).points.map(
                  (point: RavenActivityPoint) =>
                    deletePoints.includes(point.uniqueId)
                      ? { ...point, pointStatus: 'deleted' }
                      : point,
                ),
                pointsChanged: true,
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
  }),
  on(TimelineActions.panLeftViewTimeRange, state => {
    const currentViewWindow =
      state.viewTimeRange.end - state.viewTimeRange.start;
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
  }),
  on(TimelineActions.panRightViewTimeRange, state => {
    const currentViewWindow =
      state.viewTimeRange.end - state.viewTimeRange.start;
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
  }),
  on(TimelineActions.removeAllGuides, state => ({
    ...state,
    guides: [],
  })),
  on(
    TimelineActions.removeAllPointsInSubBandWithParentSource,
    (state, action) => {
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
    },
  ),
  on(TimelineActions.removeBandsOrPointsForSource, (state, action) => {
    let bands = state.bands
      .map(band => ({
        ...band,
        subBands: band.subBands
          .reduce((subBands: RavenSubBand[], subBand: RavenSubBand) => {
            const subBandHasSource = subBand.sourceIds.includes(
              action.sourceId,
            );
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
                  (sourceId: string) => sourceId !== action.sourceId,
                ),
              });
            }

            return subBands;
          }, [])
          .filter(
            subBand =>
              subBand.points.length !== 0 || subBand.type === 'divider',
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
  }),
  on(TimelineActions.removeBandsWithNoPoints, state => {
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
  }),
  on(TimelineActions.removeChildrenOrDescendants, (state, action) => {
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
                .map((point: RavenActivityPoint) =>
                  point.uniqueId === action.activityPoint.uniqueId
                    ? { ...point, expansion: 'noExpansion' }
                    : point,
                ),
            });

            return subBands;
          }, [])
          .filter((subBand: RavenSubBand) => subBand.points.length !== 0),
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
  }),
  on(TimelineActions.removePointsInSubBand, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      const deletePoints = action.points.map(
        deletePoint => deletePoint.uniqueId,
      );
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              return {
                ...subBand,
                points: (subBand as any).points.filter(
                  (point: RavenActivityPoint) =>
                    !deletePoints.includes(point.uniqueId),
                ),
                pointsChanged: true,
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
  }),
  on(TimelineActions.removeSourceIdFromSubBands, (state, action) => {
    const bands = state.bands.map(band => ({
      ...band,
      subBands: band.subBands.reduce(
        (subBands: RavenSubBand[], subBand: RavenSubBand) => {
          subBands.push({
            ...subBand,
            sourceIds: subBand.sourceIds.filter(
              (sourceId: string) => sourceId !== action.sourceId,
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
  }),
  on(TimelineActions.removeSubBand, (state, action) => {
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
  }),
  on(TimelineActions.resetViewTimeRange, state => ({
    ...state,
    viewTimeRange: { ...state.maxTimeRange },
  })),
  on(TimelineActions.selectBand, (state, action) => {
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
  }),
  on(TimelineActions.selectPoint, (state, action) => {
    const alreadySelected =
      state.selectedPoint && state.selectedPoint.uniqueId === action.pointId;
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
                    if (point.uniqueId === action.pointId) {
                      return { ...point, selected: !point.selected };
                    } else {
                      return { ...point, selected: false };
                    }
                  },
                ),
              };
            }
            return subBand;
          }),
        };
      } else {
        return band;
      }
    });

    return {
      ...state,
      bands,
      selectedPoint: alreadySelected
        ? null
        : getPoint(
            state.bands,
            action.bandId,
            action.subBandId,
            action.pointId,
          ),
    };
  }),
  on(TimelineActions.setCompositeYLabelDefault, (state, action) => ({
    ...state,
    bands: state.bands.map(band => {
      if (band.id === action.bandId && hasTwoResourceBands(band)) {
        return { ...band, compositeYAxisLabel: true };
      } else {
        return band;
      }
    }),
    currentStateChanged: state.currentState !== null,
  })),
  on(TimelineActions.setPointsForSubBand, (state, action) => {
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
  }),
  on(TimelineActions.sortBands, (state, action) => ({
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
  })),
  on(TimelineActions.sourceIdAdd, (state, action) => {
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
  }),
  on(TimelineActions.toggleGuide, (state, action) => {
    const withinThreePixels = (guide: number) =>
      (Math.abs(action.guide.guideTime - guide) * 1000) /
        action.guide.milliSecPerPixel <=
      3;
    const existingGuide = state.guides.filter(guide =>
      withinThreePixels(guide),
    );
    return {
      ...state,
      currentStateChanged: state.currentState !== null,
      guides:
        existingGuide.length > 0
          ? state.guides.filter(guide => guide !== existingGuide[0])
          : state.guides.concat(action.guide.guideTime),
    };
  }),
  on(TimelineActions.updateAllActivityBandFilter, (state, action) => ({
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      return {
        ...band,
        subBands: band.subBands.map(subBand => {
          if (
            subBand.type === 'activity' &&
            (subBand as RavenActivityBand).activityFilter === ''
          ) {
            return {
              ...subBand,
              activityFilter: action.filter,
            };
          }
          return subBand;
        }),
      };
    }),
  })),
  on(TimelineActions.updateBand, (state, action) => ({
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
  })),
  on(TimelineActions.updateLastClickTime, (state, { time }) => ({
    ...state,
    lastClickTime: time,
  })),
  on(TimelineActions.updatePointInSubBand, (state, action) => {
    const bands = state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            if (action.subBandId === subBand.id) {
              const points = (subBand as any).points.map(
                (point: RavenActivityPoint) => {
                  if (point.id === action.pointId) {
                    return {
                      ...point,
                      ...action.update,
                    };
                  } else {
                    return point;
                  }
                },
              );
              const maxTimeRange = getMaxTimeRange(points);
              return {
                ...subBand,
                maxTimeRange,
                points,
                pointsChanged: true,
              };
            }
            return subBand;
          }),
        };
      } else {
        return band;
      }
    });

    return {
      ...state,
      bands,
      ...updateTimeRanges(bands, state.viewTimeRange),
    };
  }),
  on(TimelineActions.updateSubBand, (state, action) => ({
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
  })),
  on(TimelineActions.updateSubBandTimeDelta, (state, action) => ({
    ...state,
    bands: state.bands.map((band: RavenCompositeBand) => {
      if (action.bandId === band.id) {
        return {
          ...band,
          subBands: band.subBands.map(subBand => {
            const currentTimeDelta = (subBand as RavenActivityBand).timeDelta;
            if (action.subBandId === subBand.id) {
              return {
                ...subBand,
                points: (subBand as any).points.map(
                  (point: RavenActivityPoint) => {
                    if (subBand.type === 'resource') {
                      return {
                        ...point,
                        start:
                          point.start - currentTimeDelta + action.timeDelta,
                      };
                    } else {
                      return {
                        ...point,
                        end: point.end - currentTimeDelta + action.timeDelta,
                        start:
                          point.start - currentTimeDelta + action.timeDelta,
                      };
                    }
                  },
                ),
                timeDelta: action.timeDelta,
              };
            }
            return subBand;
          }),
        };
      }

      return band;
    }),
    currentStateChanged: state.currentState !== null,
  })),
  on(TimelineActions.updateTimeline, (state, { update }) => ({
    ...state,
    ...update,
  })),
  on(TimelineActions.updateViewTimeRange, (state, { viewTimeRange }) => ({
    ...state,
    viewTimeRange: { ...viewTimeRange },
  })),
  on(TimelineActions.zoomInViewTimeRange, state => ({
    ...state,
    viewTimeRange: changeZoom(state.zoomDelta, state.viewTimeRange),
  })),
  on(TimelineActions.zoomOutViewTimeRange, state => ({
    ...state,
    viewTimeRange: changeZoom(-state.zoomDelta, state.viewTimeRange),
  })),
);
