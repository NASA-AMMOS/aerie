/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { State } from '../raven-store';
import { LayoutState } from '../reducers/layout.reducer';

const featureSelector = createFeatureSelector<State>('raven');
export const getLayoutState = createSelector(
  featureSelector,
  (state: State): LayoutState => state.layout,
);

export const getMode = createSelector(
  getLayoutState,
  (state: LayoutState) => state.mode,
);

export const getLayoutPending = createSelector(
  getLayoutState,
  (state: LayoutState) => state.fetchPending,
);

export const getShowActivityPointMetadata = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showActivityPointMetadata,
);

export const getShowActivityPointParameters = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showActivityPointParameters,
);

export const getShowApplyLayoutDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showApplyLayoutDrawer,
);

export const getShowDetailsPanel = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showDetailsPanel,
);

export const getShowEpochsDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showEpochsDrawer,
);

export const getShowFileMetadataDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showFileMetadataDrawer,
);

export const getShowGlobalSettingsDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showGlobalSettingsDrawer,
);

export const getShowLeftPanel = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showLeftPanel,
);

export const getShowOutputDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showOutputDrawer,
);

export const getShowRightPanel = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showRightPanel,
);

export const getShowSituationalAwarenessDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showSituationalAwarenessDrawer,
);

export const getShowSouthBandsPanel = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showSouthBandsPanel,
);

export const getShowTimeCursorDrawer = createSelector(
  getLayoutState,
  (state: LayoutState) => state.showTimeCursorDrawer,
);

export const getTimelinePanelSize = createSelector(
  getLayoutState,
  (state: LayoutState) => state.timelinePanelSize,
);
