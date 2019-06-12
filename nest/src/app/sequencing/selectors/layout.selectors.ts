/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { LayoutState } from '../reducers/layout.reducer';
import { State } from '../sequencing-store';

const featureSelector = createFeatureSelector<State>('sequencing');
export const getLayoutState = createSelector(
  featureSelector,
  (state: State): LayoutState => state.layout,
);

export const getEditorPanelsDirection = createSelector(
  getLayoutState,
  (state: LayoutState) => state.editorPanelsDirection,
);

export const getLeftPanelSize = createSelector(
  getLayoutState,
  (state: LayoutState): number => state.leftPanelSize,
);

export const getLeftPanelVisible = createSelector(
  getLayoutState,
  (state: LayoutState): boolean => state.leftPanelVisible,
);

export const getMiddlePanelSize = createSelector(
  getLayoutState,
  (state: LayoutState): number => state.middlePanelSize,
);

export const getMiddlePanelVisible = createSelector(
  getLayoutState,
  (state: LayoutState): boolean => state.middlePanelVisible,
);

export const getRightPanelSize = createSelector(
  getLayoutState,
  (state: LayoutState): number => state.rightPanelSize,
);

export const getRightPanelVisible = createSelector(
  getLayoutState,
  (state: LayoutState): boolean => state.rightPanelVisible,
);

export const getShowLoadingBar = createSelector(
  getLayoutState,
  (state: LayoutState): boolean => state.showLoadingBar > 0,
);
