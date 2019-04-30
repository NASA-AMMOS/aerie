/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RavenSubBand } from '../models';
import { State } from '../raven-store';
import { TimelineState } from '../reducers/timeline.reducer';
import { subBandById } from '../util';

const bandsState = (state: TimelineState) => state.bands;
const hoveredBandIdState = (state: TimelineState) => state.hoveredBandId;
const selectedBandIdState = (state: TimelineState) => state.selectedBandId;
const selectedSubBandIdState = (state: TimelineState) =>
  state.selectedSubBandId;

const featureSelector = createFeatureSelector<State>('raven');
export const getTimelineState = createSelector(
  featureSelector,
  (state: State): TimelineState => state.timeline,
);

export const getBands = createSelector(
  getTimelineState,
  bandsState,
);

export const getGuides = createSelector(
  getTimelineState,
  (state: TimelineState) => state.guides,
);

export const getCurrentState = createSelector(
  getTimelineState,
  (state: TimelineState) => state.currentState,
);

export const getCurrentStateChanged = createSelector(
  getTimelineState,
  (state: TimelineState) => state.currentStateChanged,
);

export const getCurrentStateId = createSelector(
  getTimelineState,
  (state: TimelineState) => state.currentStateId,
);

export const getHoveredBandId = createSelector(
  getTimelineState,
  hoveredBandIdState,
);

export const getLastClickTime = createSelector(
  getTimelineState,
  (state: TimelineState) => state.lastClickTime,
);

export const getMaxTimeRange = createSelector(
  getTimelineState,
  (state: TimelineState) => state.maxTimeRange,
);

export const getSelectedBandId = createSelector(
  getTimelineState,
  selectedBandIdState,
);

export const getSelectedPoint = createSelector(
  getTimelineState,
  (state: TimelineState) => state.selectedPoint,
);

export const getSelectedSubBandId = createSelector(
  getTimelineState,
  selectedSubBandIdState,
);

export const getSelectedSubBand = createSelector(
  getBands,
  getSelectedBandId,
  getSelectedSubBandId,
  (bands = [], selectedBandId, selectedSubBandId) =>
    subBandById(bands, selectedBandId, selectedSubBandId),
);

export const getSelectedSubBandPoints = createSelector(
  getSelectedSubBand,
  (subBand: RavenSubBand | null) => (subBand ? subBand.points : []),
);

export const getTimelinePending = createSelector(
  getTimelineState,
  (state: TimelineState) => state.fetchPending,
);

export const getViewTimeRange = createSelector(
  getTimelineState,
  (state: TimelineState) => state.viewTimeRange,
);
